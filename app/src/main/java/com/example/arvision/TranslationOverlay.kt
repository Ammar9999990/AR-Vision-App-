package com.example.arvision

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.text.Text

class TranslationOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val textBlockPaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.FILL
        alpha = 50 // Semi-transparent box for all detected text
    }

    private val selectedTextPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        alpha = 70 // A slightly more opaque highlight for the selected block
    }

    private var allTextBlocks: List<Text.TextBlock> = emptyList()
    private var selectedTextBlock: Text.TextBlock? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    fun setResults(text: Text, imageWidth: Int, imageHeight: Int) {
        this.allTextBlocks = text.textBlocks
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        invalidate() // Redraw with all new text blocks
    }

    fun selectTextBlock(block: Text.TextBlock?) {
        this.selectedTextBlock = block
        invalidate() // Redraw to highlight the new selection
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

        // Draw all detected text blocks with the default style
        for (block in allTextBlocks) {
            drawRect(canvas, block.boundingBox, textBlockPaint, scaleX, scaleY)
        }

        // If a block is selected, draw it on top with the highlighted style
        selectedTextBlock?.let {
            drawRect(canvas, it.boundingBox, selectedTextPaint, scaleX, scaleY)
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
