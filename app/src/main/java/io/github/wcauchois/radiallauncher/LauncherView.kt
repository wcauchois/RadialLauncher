package io.github.wcauchois.radiallauncher

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.atomic.AtomicBoolean

class LauncherView(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    companion object {
        val TAG = "LauncherView"
    }

    init {
        // Make SurfaceView transparent so we can show the wallpaper in the BG
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSPARENT)
        holder.addCallback(this)
    }

    init {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (packageInfo in packages) {
            Log.d(TAG, "--> Package name: ${packageInfo.packageName}")
        }
    }

    private fun getActivityIcon(packageName: String): Drawable? {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)!!
        val resolveInfo = pm.resolveActivity(intent, 0)
        return resolveInfo?.loadIcon(pm)
    }

    private val menus = mutableListOf<RadialMenu>()
    private var viewBounds = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        viewBounds = RectF(0F, 0F, w.toFloat(), h.toFloat())
    }

    private fun performDraw() {
        val canvas = holder.lockHardwareCanvas()
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            synchronized(menus) {
                for (menu in menus) {
                    menu.draw(canvas)
                }
            }
            holder.unlockCanvasAndPost(canvas)
        } else {
            Log.w(TAG, "Canvas was null")
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        synchronized(menus) {
            for (menu in menus) {
                menu.onTouchEvent(event)
            }
        }
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.d(TAG, "TouchEvent: ACTION_DOWN")
                val pm = context.packageManager
                val newMenu = RadialMenu(
                    rawCenter = PointF(event.x, event.y),
                    pointerStartPosition = PointF(event.x, event.y),
                    viewBounds = viewBounds,
                    items = listOf(
                        "com.bumble.app",
                        "com.android.chrome",
                        "com.google.android.apps.maps",
                        "com.twitter.android",
                        "com.instagram.android",
                        "com.facebook.katana"
                    ).withIndex().map { (index, packageName) ->
                        val intent = pm.getLaunchIntentForPackage(packageName)!!
                        val resolveInfo = pm.resolveActivity(intent, 0)
                        val icon = resolveInfo?.loadIcon(pm)!!
                        RadialMenu.Item(
                            drawable = icon,
                            trigger = if (index == 0) {
                                RadialMenu.SelectionTrigger.HOVER
                            } else {
                                RadialMenu.SelectionTrigger.POINTER_UP
                            },
                            onSelected = {
                                context.startActivity(intent)
                            }
                        )
                    }
                )
                synchronized(menus) {
                    menus.add(newMenu)
                }
                newMenu.setListener(object : RadialMenu.Listener {
                    override fun onRemove() {
                        synchronized(menus) {
                            menus.remove(newMenu)
                        }
                    }
                })
                true
            }
            MotionEvent.ACTION_MOVE -> true
            MotionEvent.ACTION_UP -> true
            else -> super.onTouchEvent(event)
        }
    }

    private var drawThread: Thread? = null
    private val drawThreadRunning = AtomicBoolean(true)

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated")
        drawThreadRunning.set(true)
        drawThread = Thread {
            while (drawThreadRunning.get()) {
                performDraw()
                try {
                    Thread.sleep(16)
                } catch (e: InterruptedException) {
                }
            }
        }
        drawThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "surfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed")
        drawThreadRunning.set(false)
        drawThread?.let { t ->
            t.interrupt()
            t.join()
        }
        drawThread = null
    }
}
