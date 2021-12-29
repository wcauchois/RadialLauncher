package io.github.wcauchois.radiallauncher

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

abstract class LauncherViewState(val view: LauncherView) {
    open fun enter() {}
    open fun exit() {}
}

class LauncherView(context: Context) : View(context), ValueAnimator.AnimatorUpdateListener {
    companion object {
        val TAG = "LauncherView"

        val RADIUS_PROPERTY_NAME = "radius"
    }

    private val radiusAnimator = ValueAnimator().apply {
        setValues(
            PropertyValuesHolder.ofFloat(
                RADIUS_PROPERTY_NAME,
                275f,
                275f
            )
        )
        duration = 500
        addUpdateListener(this@LauncherView)
    }

    override fun onAnimationUpdate(animator: ValueAnimator?) {
        invalidate()
    }

    private val backgroundCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        alpha = 70
    }

    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    private var currentX = 0f
    private var currentY = 0f
    private var startX = 0f
    private var startY = 0f

    private fun getActivityIcon(packageName: String, activityName: String): Drawable? {
        val pm = context.packageManager
        val intent = Intent().apply {
            component = ComponentName(packageName, activityName)
        }
        val resolveInfo = pm.resolveActivity(intent, 0)
        return resolveInfo?.loadIcon(pm)
    }

    private fun getChromeIcon() =
        getActivityIcon("com.android.chrome", "com.google.android.apps.chrome.Main")

    private val chromeIcon = getChromeIcon()

    override fun onDraw(canvas: Canvas) {
        canvas.apply {
            val centerX = startX//width / 2
            val centerY = startY//height / 2

            canvas.save()
            canvas.translate(centerX, centerY)
            val currentRadius = radiusAnimator.getAnimatedValue(RADIUS_PROPERTY_NAME) as Float
            val numIcons = 6
            val iconSize = 150f
            val deltaX = (currentX - startX)
            val deltaY = (currentY - startY)

            var userAngle = Math.atan2(deltaY.toDouble(), deltaX.toDouble())
            if (userAngle < 0) {
                userAngle += Math.PI * 2
            }
//            Log.d(TAG, "User angle is: ${userAngle}")

//            Log.d("---")
            for (i in 0 until numIcons) {
                val angle = (i / numIcons.toFloat()) * Math.PI * 2
                val destX = cos(angle) * currentRadius
                val destY = sin(angle) * currentRadius
                val ovalSize = 800f

                var startMatchAngle = angle - Math.PI * 2 / numIcons / 2
                if (startMatchAngle < 0) startMatchAngle += Math.PI * 2
                var endMatchAngle = angle + Math.PI * 2 / numIcons / 2
                if (endMatchAngle < 0) endMatchAngle += Math.PI * 2
                val sliceIsActive = userAngle >= startMatchAngle && userAngle < endMatchAngle

                canvas.save()
                val clipPath = Path().apply {
                    addCircle(0f, 0f, 150f, Path.Direction.CW)
                    for (j in 0 until numIcons) {
                        val p2 = Path()
                        p2.addRect(0f, -10f, 900f, 10f, Path.Direction.CW)
                        val m = Matrix()
                        m.setRotate(j / numIcons.toFloat() * 360f - 360f / numIcons / 2)
                        addPath(p2, m)
                    }
                }
                canvas.clipOutPath(clipPath)
                drawArc(
                    ovalSize / -2f, ovalSize / -2f, ovalSize / 2f, ovalSize / 2f,
                    (i / numIcons.toFloat()) * 360f - 360f / numIcons / 2,
                    360f / numIcons,
                    true,
                    if (sliceIsActive) whitePaint else backgroundCirclePaint
                )
                canvas.restore()
//                drawCircle(destX.toFloat(), destY.toFloat(), iconSize / 2 + 25f, backgroundCirclePaint)
                chromeIcon?.let { ci ->
                    ci.setBounds(
                        (destX - iconSize / 2).toInt(), (destY - iconSize / 2).toInt(),
                        (destX + iconSize / 2).toInt(), (destY + iconSize / 2).toInt()
                    )
                    ci.draw(canvas)
                }
            }
            canvas.restore()

            val circleX = centerX + deltaX
            val circleY = centerY + deltaY
//            drawCircle(circleX, circleY, 50f, whitePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent) = when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            startX = event.x
            startY = event.y
            radiusAnimator.start()
            true
        }
        MotionEvent.ACTION_MOVE -> {
            currentX = event.x
            currentY = event.y
            invalidate()
            true
        }
        MotionEvent.ACTION_UP -> {
            radiusAnimator.cancel()
            true
        }
        else -> super.onTouchEvent(event)
    }
}