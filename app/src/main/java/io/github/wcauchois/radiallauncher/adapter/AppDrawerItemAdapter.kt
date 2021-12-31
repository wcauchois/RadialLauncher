package io.github.wcauchois.radiallauncher.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.wcauchois.radiallauncher.R

data class AppDrawerItemData(
    val drawable: Drawable,
    val title: String,
    val onClick: () -> Unit
)

class AppDrawerItemAdapter(
    private val context: Context,
    private val dataset: List<AppDrawerItemData>,
) : RecyclerView.Adapter<AppDrawerItemAdapter.ItemViewHolder>() {
    class ItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val imageButton = view.findViewById<ImageButton>(R.id.image_button)
        val textView = view.findViewById<TextView>(R.id.text_view)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_drawer_item, parent, false)
        return ItemViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.imageButton.setImageDrawable(item.drawable)
        holder.textView.text = item.title
        holder.imageButton.setOnClickListener {
            item.onClick.invoke()
        }
    }

    override fun getItemCount(): Int = dataset.size
}