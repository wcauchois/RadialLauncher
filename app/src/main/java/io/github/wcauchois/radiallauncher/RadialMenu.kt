package io.github.wcauchois.radiallauncher

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
        val ICON_START_ALPHA = 0.5F

        private val ICON_RADIUS_PROPERTY_NAME = "icon_radius"
        private val ICON_ALPHA_PROPERTY_NAME = "icon_alpha"
        private val HOVER_ON_PROPERTY_NAME = "hover_on"
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
                ICON_START_ALPHA,
                1.0F
            )
        )
        duration = 250
        start()
    }

    private val hoverAnimator = ValueAnimator()?.apply {
        setValues(
            PropertyValuesHolder.ofInt(
                HOVER_ON_PROPERTY_NAME,
                1,
                0,
                1,
                0
            )
        )
        duration = 500
    }

    private var listener: Listener? = null
    private var pointerDelta: PointF = PointF()
    private var cancelled = false

    fun cancel() {
        if (cancelled) {
            return
        }
        cancelled = true

        val currentRadius = iconAnimator.getAnimatedValue(ICON_RADIUS_PROPERTY_NAME) as Float
        val currentAlpha = iconAnimator.getAnimatedValue(ICON_ALPHA_PROPERTY_NAME) as Float
        iconAnimator.cancel()
        iconAnimator.setValues(
            PropertyValuesHolder.ofFloat(
                ICON_RADIUS_PROPERTY_NAME,
                currentRadius,
                0F
            ),
            PropertyValuesHolder.ofFloat(
                ICON_ALPHA_PROPERTY_NAME,
                currentAlpha,
                0F
            )
        )
        iconAnimator.start()
        iconAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator?) {
                animator?.removeAllListeners()
                listener?.onRemove()
            }
        })
    }

    interface Listener {
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
        val trigger: SelectionTrigger = SelectionTrigger.POINTER_UP,
        val onSelected: (() -> Unit)? = null
    )

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
        val hoverValue = hoverAnimator.getAnimatedValue(HOVER_ON_PROPERTY_NAME)
        for (i in 0 until numItems) {
            val halfRadius = MENU_TOTAL_RADIUS
            val active = activeIndex == i && (!hoverAnimator.isRunning || hoverValue != 0)
            val color = when (items[i].trigger) {
                SelectionTrigger.POINTER_UP -> Color.WHITE
                SelectionTrigger.HOVER -> Color.rgb(245, 176, 66)
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = color
            if (!active) {
                paint.alpha = 120
            }
            canvas.drawArc(
                -halfRadius, -halfRadius, halfRadius, halfRadius,
                (i / numItems.toFloat()) * 360F - 360F / numItems / 2,
                360F / numItems,
                true,
                paint
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
            if (distanceFromCenter < DEADZONE_RADIUS || distanceFromCenter > MENU_TOTAL_RADIUS) {
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


        if (!cancelled) {
            drawPieSlices(canvas)
        }
        drawIcons(canvas)

        canvas.restore()
    }

    private var hoveringIndex: Int? = null

    fun onTouchEvent(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_MOVE) {
            pointerDelta = PointF(
                event.x - pointerStartPosition.x,
                event.y - pointerStartPosition.y
            )

            val index = activeIndex
            if (hoveringIndex != null) {
                if (index != hoveringIndex) {
                    hoverAnimator.cancel()
                    hoverAnimator.removeAllListeners()
                    hoveringIndex = null
                }
            } else {
                if (index != null) {
                    val item = items[index]
                    if (item.trigger == SelectionTrigger.HOVER) {
                        hoveringIndex = index
                        hoverAnimator.start()
                        hoverAnimator.addListener(object : AnimatorListenerAdapter() {
                            private var animationCancelled = false

                            override fun onAnimationCancel(animator: Animator?) {
                                animationCancelled = true
                            }

                            override fun onAnimationEnd(animator: Animator?) {
                                animator?.removeAllListeners()
                                if (!animationCancelled) {
                                    item.onSelected?.invoke()
                                    this@RadialMenu.cancel()
                                }
                            }
                        })
                    }
                }
            }
        } else if (event.action == MotionEvent.ACTION_UP) {
            if (!cancelled) {
                cancel()
                if (hoverAnimator.isRunning) {
                    hoverAnimator.cancel()
                }
                val index = activeIndex
                if (index != null) {
                    val item = this.items[index]
                    if (item.trigger == SelectionTrigger.POINTER_UP) {
                        item.onSelected?.invoke()
                    }
                }
            }
        }
    }
}
