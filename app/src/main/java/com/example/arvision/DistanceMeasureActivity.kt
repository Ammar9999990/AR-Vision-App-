package com.example.arvision

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.arvision.databinding.ActivityDistanceMeasureBinding
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.tan

class DistanceMeasureActivity : AppCompatActivity(), SensorEventListener {

    private enum class State {
        READY_FOR_POINT_1,
        READY_FOR_POINT_2,
        DONE
    }

    private lateinit var binding: ActivityDistanceMeasureBinding
    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var currentState = State.READY_FOR_POINT_1
    private var point1Data: Pair<Double, Double>? = null
    private var isMeasuringFeet = false
    private var lastMeasuredMeters: Double? = null

    private val cameraHeightMeters = 1.6 // Assumed height, can be made adjustable later

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDistanceMeasureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.markPointButton.setOnClickListener { onMarkPointTapped() }
        binding.unitToggleButton.setOnClickListener { toggleUnits() }
        resetMeasurement() // Initialize UI for the first time
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(binding.cameraPreview.surfaceProvider) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera binding failed.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun onMarkPointTapped() {
        if (currentState == State.DONE) {
            resetMeasurement()
            return
        }

        val pitch = orientationAngles[1].toDouble()
        val yaw = orientationAngles[0].toDouble()

        if (pitch.isNaN() || yaw.isNaN()) {
            binding.distanceTextView.text = "Sensor data is not yet available. Please wait."
            return
        }

        if (pitch > -0.1) { // Ensure device is tilted down at least ~5.7 degrees
            binding.distanceTextView.text = "Please aim further down at the floor."
            return
        }

        val angleOfDepression = -pitch
        val distanceToPoint = cameraHeightMeters / tan(angleOfDepression)

        if (currentState == State.READY_FOR_POINT_1) {
            point1Data = Pair(distanceToPoint, yaw)
            currentState = State.READY_FOR_POINT_2
            binding.distanceTextView.text = "Point 1 marked. Aim at the base of the second point."
            binding.markPointButton.text = "Mark Point 2"
        } else if (currentState == State.READY_FOR_POINT_2) {
            val (dist1, yaw1) = point1Data!!
            val dist2 = distanceToPoint
            val yaw2 = yaw

            var angleBetweenPoints = abs(yaw1 - yaw2)
            if (angleBetweenPoints > Math.PI) { // Handle yaw wrapping around from -PI to PI
                angleBetweenPoints = 2 * Math.PI - angleBetweenPoints
            }

            val finalDistance = sqrt(dist1 * dist1 + dist2 * dist2 - 2 * dist1 * dist2 * cos(angleBetweenPoints))
            lastMeasuredMeters = finalDistance
            displayDistance(finalDistance)

            val distanceString = binding.distanceTextView.text.toString()
            HistoryManager.addHistoryItem(this, HistoryItem(HistoryEventType.DISTANCE_MEASURED, distanceString))

            currentState = State.DONE
            binding.markPointButton.text = "New Measurement"
        }
    }

    private fun displayDistance(distanceInMeters: Double) {
        val distance = if (isMeasuringFeet) distanceInMeters * 3.28084 else distanceInMeters
        val unit = if (isMeasuringFeet) "feet" else "meters"
        binding.distanceTextView.text = String.format("Distance: %.2f %s", distance, unit)
    }

    private fun toggleUnits() {
        isMeasuringFeet = !isMeasuringFeet
        binding.unitToggleButton.text = if (isMeasuringFeet) "Switch to Meters" else "Switch to Feet"
        lastMeasuredMeters?.let { displayDistance(it) }
    }

    private fun resetMeasurement() {
        point1Data = null
        lastMeasuredMeters = null
        currentState = State.READY_FOR_POINT_1
        binding.distanceTextView.text = "Aim at the base of the first point and tap 'Mark Point'"
        binding.markPointButton.text = "Mark Point"
    }

    override fun onResume() {
        super.onResume()
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer != null && magneticField != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_UI)
        } else {
            Toast.makeText(this, "Required sensors not available.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val alpha = 0.97f
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            accelerometerReading[0] = alpha * accelerometerReading[0] + (1 - alpha) * event.values[0]
            accelerometerReading[1] = alpha * accelerometerReading[1] + (1 - alpha) * event.values[1]
            accelerometerReading[2] = alpha * accelerometerReading[2] + (1 - alpha) * event.values[2]
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetometerReading[0] = alpha * magnetometerReading[0] + (1 - alpha) * event.values[0]
            magnetometerReading[1] = alpha * magnetometerReading[1] + (1 - alpha) * event.values[1]
            magnetometerReading[2] = alpha * magnetometerReading[2] + (1 - alpha) * event.values[2]
        }
        updateOrientationAngles()
    }

    private fun updateOrientationAngles() {
        val rotationMatrixSuccess = SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
        if (rotationMatrixSuccess) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* Not used */ }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
