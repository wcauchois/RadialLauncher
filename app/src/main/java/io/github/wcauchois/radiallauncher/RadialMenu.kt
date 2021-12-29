package io.github.wcauchois.radiallauncher

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.MotionEvent
import kotlin.math.*

class RadialMenu(
    rawCenter: PointF,
    val pointerStartPosition: PointF,
    val viewBounds: RectF,
    val items: List<RadialMenu.Item>
) {
    companion object {
        val TAG = "RadialMenu"

        val ICON_DISTANCE_FROM_CENTER = 275F
        val ICON_START_DISTANCE_FROM_CENTER = 100F
        val ICON_SIZE = 150F
        val MENU_TOTAL_RADIUS = 400F
        val PIE_SEPARATOR_HEIGHT = 20F
        val DEADZONE_RADIUS = 150F

        private val ICON_RADIUS_PROPERTY_NAME = "icon_radius"
        private val ICON_ALPHA_PROPERTY_NAME = "icon_alpha"
    }

    val center = run {
        val minX = viewBounds.left + MENU_TOTAL_RADIUS
        val maxX = viewBounds.right - MENU_TOTAL_RADIUS
        val minY = viewBounds.top + MENU_TOTAL_RADIUS
        val maxY = viewBounds.bottom - MENU_TOTAL_RADIUS
        PointF(
            rawCenter.x.coerceIn(minX, maxX),
            rawCenter.y.coerceIn(minY, maxY),
        )
    }

    private val iconAnimator = ValueAnimator().apply {
        setValues(
            PropertyValuesHolder.ofFloat(
                ICON_RADIUS_PROPERTY_NAME,
                ICON_START_DISTANCE_FROM_CENTER,
                ICON_DISTANCE_FROM_CENTER
            ),
            PropertyValuesHolder.ofFloat(
                ICON_ALPHA_PROPERTY_NAME,
                0.5F,
                1.0F
            )
        )
        duration = 250
        start()
    }

    private var listener: Listener? = null
    private var pointerDelta: PointF = PointF()

    interface Listener {
        fun onSelect(index: Int) {}
        fun onRemove() {}
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    enum class SelectionTrigger {
        POINTER_UP,
        HOVER
    }

    data class Item(
        val drawable: Drawable,
        val trigger: SelectionTrigger = SelectionTrigger.POINTER_UP
    )

    private val transparentWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 70
    }

    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    private fun drawPieSlices(canvas: Canvas) {
        canvas.save()

        val numItems = items.size
        val clipPath = Path().apply {
            addCircle(0F, 0F, DEADZONE_RADIUS, Path.Direction.CW)
            for (i in 0 until numItems) {
                val separatorPath = Path()
                separatorPath.addRect(
                    0F,
                    -PIE_SEPARATOR_HEIGHT / 2,
                    MENU_TOTAL_RADIUS,
                    PIE_SEPARATOR_HEIGHT / 2,
                    Path.Direction.CW
                )
                val transform = Matrix()
                transform.setRotate(i / numItems.toFloat() * 360F - 360F / numItems / 2)
                addPath(separatorPath, transform)
            }
        }
        canvas.clipOutPath(clipPath)

        val activeIndex = this.activeIndex
        for (i in 0 until numItems) {
            val halfRadius = MENU_TOTAL_RADIUS
            val active = activeIndex == i
            canvas.drawArc(
                -halfRadius, -halfRadius, halfRadius, halfRadius,
                (i / numItems.toFloat()) * 360F - 360F / numItems / 2,
                360F / numItems,
                true,
                if (active) whitePaint else transparentWhitePaint
            )
        }

        canvas.restore()
    }

    private fun drawIcons(canvas: Canvas) {
        val numItems = items.size
        val currentRadius = iconAnimator.getAnimatedValue(ICON_RADIUS_PROPERTY_NAME) as Float
        val currentAlpha = iconAnimator.getAnimatedValue(ICON_ALPHA_PROPERTY_NAME) as Float
        val halfSize = ICON_SIZE / 2F
        for ((index, item) in items.withIndex()) {
            val angle = (index / numItems.toFloat()) * Math.PI * 2
            val destX = cos(angle) * currentRadius
            val destY = sin(angle) * currentRadius
            item.drawable.alpha = (currentAlpha * 255).toInt()
            item.drawable.setBounds(
                (destX - halfSize).toInt(), (destY - halfSize).toInt(),
                (destX + halfSize).toInt(), (destY + halfSize).toInt()
            )
            item.drawable.draw(canvas)
        }
    }

    private val activeIndex: Int?
        get() {
            val distanceFromCenter = if (pointerDelta.x == 0F && pointerDelta.y == 0F) 0F else sqrt(
                pointerDelta.x * pointerDelta.x +
                        pointerDelta.y * pointerDelta.y
            )
            if (distanceFromCenter < DEADZONE_RADIUS) {
                return null
            }
            var userAngle = atan2(pointerDelta.y.toDouble(), pointerDelta.x.toDouble())
            val numItems = items.size
            val sliceWidthRadians = Math.PI * 2 / numItems
            if (userAngle < 0) {
                userAngle += Math.PI * 2
            }
            var index = (userAngle / sliceWidthRadians).roundToInt()
            if (index == numItems) {
                index = 0
            }
            return index
        }


    fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(this.center.x, this.center.y)


        drawPieSlices(canvas)
        drawIcons(canvas)

        canvas.restore()
    }

    fun onTouchEvent(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_MOVE) {
            pointerDelta = PointF(
                event.x - pointerStartPosition.x,
                event.y - pointerStartPosition.y
            )
        }
    }
}
