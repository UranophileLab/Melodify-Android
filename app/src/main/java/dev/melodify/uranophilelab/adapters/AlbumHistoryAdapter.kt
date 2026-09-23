package dev.melodify.uranophilelab.adapters

import android.content.Intent
import androidx.core.net.toUri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.bumptech.glide.Glide
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.ListActivity
import dev.melodify.uranophilelab.model.AlbumItem
import dev.melodify.uranophilelab.model.history.AlbumHistoryItem
import dev.melodify.uranophilelab.utils.SharedPreferenceManager

class AlbumHistoryAdapter(
    private var data: MutableList<AlbumHistoryItem>,
    private val onItemRemovedListener: (() -> Unit)? = null
) : RecyclerView.Adapter<AlbumHistoryAdapter.ViewHolder>() {

    fun updateData(newData: List<AlbumHistoryItem>) {
        if (data === newData) {
            notifyDataSetChanged()
            return
        }
        val temp = ArrayList(newData)
        data.clear()
        data.addAll(temp)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImage: ImageView? = itemView.findViewById(R.id.coverImage)
        val title: TextView? = itemView.findViewById(R.id.title)
        val subtitle: TextView? = itemView.findViewById(R.id.artist)
        val moreBtn: ImageView? = itemView.findViewById(R.id.more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.activity_list_song_item,
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        holder.title?.text = item.title ?: "Unknown Album"
        holder.subtitle?.text = item.subtitle ?: "Album"

        holder.title?.isSelected = true
        holder.subtitle?.isSelected = true

        val imageUrl = item.imageUrl
        if (!imageUrl.isNullOrEmpty() && holder.coverImage != null) {
            Glide.with(holder.coverImage.context).load(imageUrl.toUri()).into(holder.coverImage)
        } else {
            holder.coverImage?.setImageResource(R.drawable.baseline_album_24)
        }

        holder.itemView.findViewById<View>(R.id.favorite_item_icon)?.visibility = View.GONE

        holder.itemView.setOnClickListener { v ->
            if (!item.id.isNullOrEmpty()) {
                val albumItem = AlbumItem(
                    albumTitle = item.title,
                    albumSubTitle = item.subtitle,
                    albumCover = item.imageUrl,
                    id = item.id
                )
                val intent = Intent(v.context, ListActivity::class.java)
                    .putExtra("data", Gson().toJson(albumItem))
                    .putExtra("type", "album")
                    .putExtra("id", item.id)
                v.context.startActivity(intent)
            }
        }

        holder.moreBtn?.setOnClickListener { v ->
            val context = v.context
            val popup = PopupMenu(context, v)
            popup.menu.add("Remove from history")
            popup.setOnMenuItemClickListener { menuItem ->
                if (menuItem.title == "Remove from history") {
                    val currentPos = holder.bindingAdapterPosition
                    if (currentPos in 0 until data.size) {
                        data.removeAt(currentPos)
                        notifyItemRemoved(currentPos)
                        notifyItemRangeChanged(currentPos, data.size - currentPos)
                        val prefs = SharedPreferenceManager.getInstance(context)
                        prefs.albumHistory = data
                        onItemRemovedListener?.invoke()
                    }
                    true
                } else {
                    false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = data.size
}
