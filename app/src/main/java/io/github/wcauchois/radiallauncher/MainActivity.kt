package io.github.wcauchois.radiallauncher

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val settingsButton = findViewById<ImageButton>(R.id.settings_button)
        val pm = packageManager
        val intent = pm.getLaunchIntentForPackage("com.android.settings")
        val resolveInfo = intent?.let { pm.resolveActivity(it, 0) }
        val icon = resolveInfo?.loadIcon(pm)
        icon?.let {
            settingsButton.setImageDrawable(it)
        }
        settingsButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}