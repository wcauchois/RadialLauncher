package io.github.wcauchois.radiallauncher.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import io.github.wcauchois.radiallauncher.R
import io.github.wcauchois.radiallauncher.adapter.AppDrawerItemAdapter
import io.github.wcauchois.radiallauncher.adapter.AppDrawerItemData
import java.util.*

class AppDrawerFragment : Fragment(R.layout.fragment_app_drawer) {
    private fun setupView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.app_drawer_recycler_view)
        val context = requireContext()

        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val items = packages.mapNotNull { p ->
            val intent = pm.getLaunchIntentForPackage(p.packageName)
            val resolveInfo = intent?.let { pm.resolveActivity(it, 0) }
            val icon = resolveInfo?.loadIcon(pm)
            icon?.let {
                AppDrawerItemData(
                    icon,
                    pm.getApplicationLabel(p).toString(),
                ) {
                    context.startActivity(intent)
                }
            }
        }

        recyclerView.adapter = AppDrawerItemAdapter(context, items)
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