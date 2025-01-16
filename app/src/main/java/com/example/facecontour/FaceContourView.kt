package com.example.facecontour

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View

class FaceContourView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val faceRectangles = mutableListOf<Rect>()
    private val paint: Paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    fun setFaceRectangles(rectangles: List<Rect>) {
        Log.d(TAG, "Updating face rectangles: $rectangles")
        faceRectangles.clear()
        faceRectangles.addAll(rectangles)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (rect in faceRectangles) {
            canvas.drawRect(rect, paint)
        }
    }

    companion object {
        private const val TAG = "FaceContourView"
    }
}