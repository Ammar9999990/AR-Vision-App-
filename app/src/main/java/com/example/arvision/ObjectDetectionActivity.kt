package com.example.arvision

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.arvision.databinding.ActivityObjectDetectionBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ObjectDetectionActivity : AppCompatActivity(), DetectionOverlayView.DetectorTapListener {

    private lateinit var viewBinding: ActivityObjectDetectionBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var originalBitmap: Bitmap? = null
    private var currentObjectLabel: String? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                    runMLKitObjectDetection(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityObjectDetectionBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        viewBinding.captureButton.setOnClickListener { takePhoto() }
        viewBinding.galleryButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        }

        viewBinding.closeButton.setOnClickListener { hideStaticImage() }
        viewBinding.objectDetectionOverlay.tapListener = this
        viewBinding.tellMeMoreButton.setOnClickListener { onTellMeMoreTapped() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(viewBinding.objectDetectionCameraPreview.surfaceProvider) }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(this, "Use case binding failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AR-Vision")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                 output.savedUri?.let {
                    try {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                        runOnUiThread { runMLKitObjectDetection(bitmap) } 
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
            }
        })
    }

    private fun runMLKitObjectDetection(bitmap: Bitmap) {
        originalBitmap = bitmap 
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        val objectDetector = ObjectDetection.getClient(options)

        objectDetector.process(image)
            .addOnSuccessListener { results ->
                showStaticImage(bitmap)
                viewBinding.objectDetectionOverlay.setResults(results, image.height, image.width)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Object detection failed.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onObjectTapped(tappedObject: DetectedObject) {
        originalBitmap?.let { bmp ->
            val boundingBox = tappedObject.boundingBox
            val croppedBitmap = try {
                Bitmap.createBitmap(
                    bmp,
                    boundingBox.left,
                    boundingBox.top,
                    boundingBox.width(),
                    boundingBox.height()
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to crop bitmap", e)
                null
            }

            croppedBitmap?.let { croppedBmp ->
                currentObjectLabel = tappedObject.labels.firstOrNull()?.text ?: "object"
                viewBinding.descriptionCard.visibility = View.VISIBLE
                viewBinding.descriptionProgressBar.visibility = View.VISIBLE
                viewBinding.descriptionTextView.visibility = View.GONE
                viewBinding.tellMeMoreButton.visibility = View.GONE

                lifecycleScope.launch {
                    val description = GeminiPro.describeObject(croppedBmp, currentObjectLabel!!)
                    HistoryManager.addHistoryItem(this@ObjectDetectionActivity, HistoryItem(HistoryEventType.OBJECT_DESCRIBED, currentObjectLabel!!))

                    viewBinding.descriptionProgressBar.visibility = View.GONE
                    viewBinding.descriptionTextView.visibility = View.VISIBLE
                    viewBinding.descriptionTextView.text = description
                    viewBinding.tellMeMoreButton.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun onTellMeMoreTapped() {
        currentObjectLabel?.let { label ->
            viewBinding.descriptionProgressBar.visibility = View.VISIBLE
            viewBinding.tellMeMoreButton.visibility = View.GONE

            lifecycleScope.launch {
                val deeperAnalysis = GeminiPro.getDeeperAnalysis(label)
                viewBinding.descriptionProgressBar.visibility = View.GONE
                viewBinding.descriptionTextView.text = deeperAnalysis
            }
        }
    }

    private fun showStaticImage(bitmap: Bitmap) {
        viewBinding.resultImageView.setImageBitmap(bitmap)
        viewBinding.resultImageView.visibility = View.VISIBLE
        viewBinding.objectDetectionOverlay.visibility = View.VISIBLE
        viewBinding.closeButton.visibility = View.VISIBLE
        viewBinding.objectDetectionCameraPreview.visibility = View.INVISIBLE
        viewBinding.bottomActionBar.visibility = View.INVISIBLE
    }

    private fun hideStaticImage() {
        viewBinding.resultImageView.visibility = View.GONE
        viewBinding.objectDetectionOverlay.visibility = View.GONE
        viewBinding.closeButton.visibility = View.GONE
        viewBinding.descriptionCard.visibility = View.GONE
        viewBinding.tellMeMoreButton.visibility = View.GONE
        viewBinding.objectDetectionCameraPreview.visibility = View.VISIBLE
        viewBinding.bottomActionBar.visibility = View.VISIBLE
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "ObjectDetectionActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            arrayOf(Manifest.permission.CAMERA)
        }
    }
}
