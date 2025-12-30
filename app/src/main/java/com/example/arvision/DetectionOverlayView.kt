package com.example.arvision

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.mlkit.vision.objects.DetectedObject

class DetectionOverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    interface DetectorTapListener {
        fun onObjectTapped(tappedObject: DetectedObject)
    }

    private var results: List<DetectedObject> = listOf()
    private var drawnObjects = mutableMapOf<RectF, DetectedObject>()
    var tapListener: DetectorTapListener? = null

    private var scaleFactorX: Float = 1f
    private var scaleFactorY: Float = 1f

    private val boxPaint = Paint().apply {
        color = Color.CYAN
        strokeWidth = 8F
        style = Paint.Style.STROKE
    }
    private val textBackgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = 50f
    }
    private var textBounds = Rect()

    fun clear() {
        results = listOf()
        drawnObjects.clear()
        invalidate()
    }

    fun setResults(
        detectionResults: List<DetectedObject>,
        imageHeight: Int,
        imageWidth: Int
    ) {
        results = detectionResults
        scaleFactorX = width.toFloat() / imageWidth
        scaleFactorY = height.toFloat() / imageHeight
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawnObjects.clear()

        for (result in results) {
            val boundingBox = result.boundingBox
            val top = boundingBox.top * scaleFactorY
            val bottom = boundingBox.bottom * scaleFactorY
            val left = boundingBox.left * scaleFactorX
            val right = boundingBox.right * scaleFactorX

            val drawableRect = RectF(left, top, right, bottom)
            drawnObjects[drawableRect] = result

            canvas.drawRect(drawableRect, boxPaint)

            result.labels.firstOrNull()?.let { label ->
                val percentageConfidence = (label.confidence * 100).toInt()
                val fullText = "${label.text}, $percentageConfidence%"
                
                textPaint.getTextBounds(fullText, 0, fullText.length, textBounds)
                val textWidth = textPaint.measureText(fullText)

                canvas.drawRect(left, top - textBounds.height(), left + textWidth, top, textBackgroundPaint)
                canvas.drawText(fullText, left, top - 4, textPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            for ((rect, detectedObject) in drawnObjects) {
                if (rect.contains(event.x, event.y)) {
                    tapListener?.onObjectTapped(detectedObject)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
