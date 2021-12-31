package io.github.wcauchois.radiallauncher.view

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import io.github.wcauchois.radiallauncher.R
import java.util.concurrent.atomic.AtomicBoolean

class LauncherView(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs),
    SurfaceHolder.Callback {
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

    private val menus = mutableListOf<RadialMenu>()
    private var viewBounds = RectF()
    private var currentPointerPosition = PointF()

    private var allAppsClickedListener: (() -> Unit)? = null

    fun setAllAppsClickedListener(listener: (() -> Unit)?) {
        allAppsClickedListener = listener
    }

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

    private fun addNewMenu(items: List<RadialMenu.Item>) {
        val position = currentPointerPosition
        val newMenu = RadialMenu(
            rawCenter = position,
            pointerStartPosition = position,
            viewBounds = viewBounds,
            items = items
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
    }

    private fun menuItemForPackageName(packageName: String): RadialMenu.Item? {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        val resolveInfo = intent?.let { pm.resolveActivity(it, 0) }
        val icon = resolveInfo?.loadIcon(pm)
        return icon?.let {
            RadialMenu.Item(
                drawable = icon,
                trigger = RadialMenu.SelectionTrigger.POINTER_UP,
                onSelected = {
                    intent?.let {
                        context.startActivity(it)
                    }
                }
            )
        }?.also {
            if (it == null) {
                Log.w(TAG, "Could not create menu item for package: ${packageName}")
            }
        }
    }

    private fun addMessagingMenu() {
        addNewMenu(
            listOfNotNull(
                menuItemForPackageName("org.telegram.messenger"),
                menuItemForPackageName("com.facebook.orca"),
                menuItemForPackageName("com.google.android.apps.messaging"),
                menuItemForPackageName("com.google.android.gm"),
            )
        )
    }

    private fun addEntertainmentMenu() {
        addNewMenu(
            listOfNotNull(
                menuItemForPackageName("com.instagram.android"),
                menuItemForPackageName("com.andrewshu.android.reddit"),
                menuItemForPackageName("com.zhiliaoapp.musically"),
            )
        )
    }

    private fun addDefaultMenu() {
        val messagingIconDrawable = context.getDrawable(R.drawable.messaging_icon)
        val entertainmentIconDrawable = context.getDrawable(R.drawable.entertainment_icon)
        val allAppsIconDrawable = context.getDrawable(R.drawable.ic_all_apps)
        addNewMenu(listOfNotNull(
            menuItemForPackageName("com.android.chrome"),
//            menuItemForPackageName("com.google.android.googlequicksearchbox"),
            menuItemForPackageName("com.google.android.apps.photos"),
            menuItemForPackageName("com.google.android.calendar"),
            messagingIconDrawable?.let {
                RadialMenu.Item(
                    drawable = it,
                    trigger = RadialMenu.SelectionTrigger.HOVER,
                    onSelected = {
                        addMessagingMenu()
                    }
                )
            },
            entertainmentIconDrawable?.let {
                RadialMenu.Item(
                    drawable = it,
                    trigger = RadialMenu.SelectionTrigger.HOVER,
                    onSelected = {
                        addEntertainmentMenu()
                    }
                )
            },
            allAppsIconDrawable?.let {
                RadialMenu.Item(
                    drawable = it,
                    trigger = RadialMenu.SelectionTrigger.POINTER_UP,
                    onSelected = {
                        allAppsClickedListener?.invoke()
                    }
                )
            }
        ))
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
                currentPointerPosition = PointF(event.x, event.y)
                addDefaultMenu()
                true
            }
            MotionEvent.ACTION_MOVE -> {
                currentPointerPosition = PointF(event.x, event.y)
                true
            }
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
