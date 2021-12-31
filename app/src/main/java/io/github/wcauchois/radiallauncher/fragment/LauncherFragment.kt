package io.github.wcauchois.radiallauncher.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import io.github.wcauchois.radiallauncher.R
import io.github.wcauchois.radiallauncher.view.LauncherView

class LauncherFragment : Fragment(R.layout.fragment_launcher) {
    private fun setupView(view: View) {
        context?.packageManager?.let { pm ->
            val settingsButton = view.findViewById<ImageButton>(R.id.settings_button)
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

        val launcherView = view.findViewById<LauncherView>(R.id.launcher_view)
        launcherView.setAllAppsClickedListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_up,
                    R.anim.no_op,
                    0,
                    R.anim.slide_down,
                )
                .replace(R.id.fragment_container_view, AppDrawerFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.let { setupView(it) }
        return view
    }
}