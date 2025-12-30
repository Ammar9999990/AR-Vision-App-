package com.example.arvision

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
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
import com.example.arvision.databinding.ActivityTranslationBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TranslationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTranslationBinding
    private lateinit var cameraExecutor: ExecutorService
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var latestTextResult: Text? = null
    private var selectedTextBlock: Text.TextBlock? = null
    private var imageAnalysis: ImageAnalysis? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTranslationBinding.inflate(layoutInflater)
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

            imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, ::analyzeImage) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed", exc)
                Toast.makeText(this, "Camera binding failed.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeImage(imageProxy: androidx.camera.core.ImageProxy) {
        if (selectedTextBlock != null) { // Pause analysis when a block is selected
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    latestTextResult = visionText
                    runOnUiThread {
                        binding.translationOverlay.setResults(visionText, imageProxy.width, imageProxy.height)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed", e)
                }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    private fun setupUI() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val tappedBlock = findTappedTextBlock(e)
                if (tappedBlock != null) {
                    selectTextBlock(tappedBlock)
                } else {
                    clearSelection()
                }
                return true
            }
        })

        binding.cameraPreview.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.copyButton.setOnClickListener { selectedTextBlock?.let { copyText(it.text) } }
        binding.googleTranslateButton.setOnClickListener { selectedTextBlock?.let { openInGoogleTranslate(it.text) } }
    }

    private fun findTappedTextBlock(e: MotionEvent): Text.TextBlock? {
        val textResult = latestTextResult ?: return null
        val view = binding.cameraPreview
        val imageAnalysis = this.imageAnalysis ?: return null

        val imageWidth = imageAnalysis.resolutionInfo?.resolution?.width ?: view.width
        val imageHeight = imageAnalysis.resolutionInfo?.resolution?.height ?: view.height

        val scaleX = view.width.toFloat() / imageWidth
        val scaleY = view.height.toFloat() / imageHeight

        val tapX = e.x / scaleX
        val tapY = e.y / scaleY

        for (block in textResult.textBlocks) {
            block.boundingBox?.let {
                if (it.contains(tapX.toInt(), tapY.toInt())) {
                    return block
                }
            }
        }
        return null
    }

    private fun selectTextBlock(block: Text.TextBlock) {
        selectedTextBlock = block
        binding.translationOverlay.selectTextBlock(block)
        binding.actionPanel.visibility = View.VISIBLE
        HistoryManager.addHistoryItem(this, HistoryItem(HistoryEventType.TEXT_SELECTED, block.text))
    }

    private fun clearSelection() {
        selectedTextBlock = null
        binding.translationOverlay.selectTextBlock(null) // Clear selection in overlay
        binding.actionPanel.visibility = View.GONE
    }

    private fun copyText(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        HistoryManager.addHistoryItem(this, HistoryItem(HistoryEventType.TEXT_COPIED, text))
    }

    private fun openInGoogleTranslate(text: String) {
        val encodedText = Uri.encode(text)
        val uri = "https://translate.google.com/?sl=auto&tl=en&text=$encodedText"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(intent)
        HistoryManager.addHistoryItem(this, HistoryItem(HistoryEventType.TEXT_TRANSLATED, text))
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
        private const val TAG = "TranslationActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
