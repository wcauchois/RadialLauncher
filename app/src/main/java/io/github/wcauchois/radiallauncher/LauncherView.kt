package io.github.wcauchois.radiallauncher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.MotionEvent
import android.view.View

class LauncherView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLUE
    }

    private var currentX = 0.0f
    private var currentY = 0.0f

    override fun onDraw(canvas: Canvas) {
        canvas.apply {
            val size = 50.0f
            drawRect(currentX - size, currentY - size, currentX + size, currentY + size, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent) = when (event.action) {
        MotionEvent.ACTION_DOWN -> true
        MotionEvent.ACTION_MOVE -> {
            currentX = event.x
            currentY = event.y
            invalidate()
            true
        }
        else -> super.onTouchEvent(event)
    }
}