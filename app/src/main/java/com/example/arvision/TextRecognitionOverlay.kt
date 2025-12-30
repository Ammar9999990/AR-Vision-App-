package com.example.arvision

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.text.Text

class TextRecognitionOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val textBlockPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val selectedTextBlockPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private var allTextBlocks: List<Text.TextBlock> = emptyList()
    var selectedTextBlock: Text.TextBlock? = null
        private set

    var imageWidth: Int = 0
        private set
    var imageHeight: Int = 0
        private set

    fun setResults(text: Text, imageWidth: Int, imageHeight: Int) {
        this.allTextBlocks = text.textBlocks
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.selectedTextBlock = null
        invalidate()
    }

    fun selectTextBlock(block: Text.TextBlock?) {
        selectedTextBlock = block
        invalidate()
    }

    fun clear() {
        allTextBlocks = emptyList()
        selectedTextBlock = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (imageWidth == 0 || imageHeight == 0) return

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        if (selectedTextBlock != null) {
            // If a block is selected, only draw that single block.
            drawRect(canvas, selectedTextBlock!!.boundingBox, selectedTextBlockPaint, scaleX, scaleY)
        } else {
            // If no block is selected, draw all detected blocks.
            for (block in allTextBlocks) {
                drawRect(canvas, block.boundingBox, textBlockPaint, scaleX, scaleY)
            }
        }
    }

    private fun drawRect(canvas: Canvas, boundingBox: android.graphics.Rect?, paint: Paint, scaleX: Float, scaleY: Float) {
        val box = boundingBox ?: return
        val rect = RectF(
            box.left * scaleX,
            box.top * scaleY,
            box.right * scaleX,
            box.bottom * scaleY
        )
        canvas.drawRect(rect, paint)
    }
}
