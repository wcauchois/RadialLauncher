package io.github.wcauchois.radiallauncher.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import io.github.wcauchois.radiallauncher.adapter.AppDrawerItemData
import java.util.concurrent.atomic.AtomicReference

data class AppInfo(
    val icon: Drawable,
    val name: String,
    val packageName: String
)

class LauncherApplication : Application() {
    private val appInfoMap = AtomicReference<Map<String, AppInfo>>(mapOf())

    class LoadAppInfoTask : AsyncTask<Context, Void, List<AppInfo>>() {
        override fun doInBackground(vararg context: Context?): List<AppInfo> {
            val pm = context[0]?.packageManager
            pm ?: return listOf()
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            return packages.mapNotNull { p ->
                val intent = pm.getLaunchIntentForPackage(p.packageName)
                val resolveInfo = intent?.let { pm.resolveActivity(it, 0) }
                val icon = resolveInfo?.loadIcon(pm)
                icon?.let {
                    AppInfo(
                        icon = icon,
                        name = pm.getApplicationLabel(p).toString(),
                        packageName = p.packageName
                    )
                }
            }
        }

    }

    init {
        LoadAppInfoTask()
            .execute(this)
    }
}