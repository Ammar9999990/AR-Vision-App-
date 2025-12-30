package com.example.arvision

import android.Manifest
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.arvision.databinding.ActivityTextRecognitionBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TextRecognitionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextRecognitionBinding
    private lateinit var cameraExecutor: ExecutorService
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var latestTextResult: Text? = null
    private var selectedTextBlock: Text.TextBlock? = null
    private var isAnalysisPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextRecognitionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        setupUI()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(binding.cameraPreview.surfaceProvider) }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, ::analyzeImage) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera binding failed.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeImage(imageProxy: androidx.camera.core.ImageProxy) {
        if (isAnalysisPaused) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    runOnUiThread {
                        binding.textOverlay.setResults(visionText, imageProxy.width, imageProxy.height)
                        latestTextResult = visionText
                    }
                }
                .addOnFailureListener { /* Error handling can be added here */ }
                .addOnCompleteListener { imageProxy.close() }
        }
    }

    private fun setupUI() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val tappedBlock = findTappedTextBlock(e.x, e.y)
                if (tappedBlock != null) {
                    selectTextBlock(tappedBlock)
                } else {
                    clearSelection()
                }
                return true
            }
        })

        binding.textOverlay.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.copyButton.setOnClickListener { selectedTextBlock?.let { copyText(it.text) } }
        binding.searchButton.setOnClickListener { selectedTextBlock?.let { searchText(it.text) } }
        binding.shareButton.setOnClickListener { selectedTextBlock?.let { shareText(it.text) } }
    }

    private fun findTappedTextBlock(x: Float, y: Float): Text.TextBlock? {
        val text = latestTextResult ?: return null
        val overlay = binding.textOverlay
        val scaleX = overlay.width.toFloat() / overlay.imageWidth
        val scaleY = overlay.height.toFloat() / overlay.imageHeight

        for (block in text.textBlocks) {
            val boundingBox = block.boundingBox ?: continue
            val rect = RectF(
                boundingBox.left * scaleX,
                boundingBox.top * scaleY,
                boundingBox.right * scaleX,
                boundingBox.bottom * scaleY
            )
            if (rect.contains(x, y)) {
                return block
            }
        }
        return null
    }

    private fun selectTextBlock(block: Text.TextBlock) {
        selectedTextBlock = block
        isAnalysisPaused = true
        binding.textOverlay.selectTextBlock(block)
        binding.actionPanel.visibility = View.VISIBLE
        HistoryManager.addHistoryItem(this, HistoryItem(HistoryEventType.TEXT_SELECTED, block.text))
    }

    private fun clearSelection() {
        selectedTextBlock = null
        isAnalysisPaused = false
        binding.textOverlay.clear()
        binding.actionPanel.visibility = View.GONE
    }

    private fun copyText(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        HistoryManager.addHistoryItem(this, HistoryItem(HistoryEventType.TEXT_COPIED, text))
    }

    private fun searchText(text: String) {
        val intent = Intent(Intent.ACTION_WEB_SEARCH)
        intent.putExtra(SearchManager.QUERY, text)
        startActivity(intent)
        HistoryManager.addHistoryItem(this, HistoryItem(HistoryEventType.TEXT_SEARCHED, text))
    }

    private fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(intent, "Share Text"))
        HistoryManager.addHistoryItem(this, HistoryItem(HistoryEventType.TEXT_SHARED, text))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
