package dev.melodify.uranophilelab.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.MusicOverviewActivity
import dev.melodify.uranophilelab.model.history.SongHistoryItem
import dev.melodify.uranophilelab.utils.SharedPreferenceManager

class SongHistoryAdapter(
    private val data: MutableList<SongHistoryItem>,
    private val onItemRemovedListener: (() -> Unit)? = null
) : RecyclerView.Adapter<SongHistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImage: ImageView? = itemView.findViewById(R.id.coverImage)
        val title: TextView? = itemView.findViewById(R.id.title)
        val artist: TextView? = itemView.findViewById(R.id.artist)
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

        holder.title?.text = item.title ?: "Unknown Song"
        holder.artist?.text = item.artist ?: "Unknown Artist"

        holder.title?.isSelected = true
        holder.artist?.isSelected = true

        val imageUrl = item.imageUrl
        if (!imageUrl.isNullOrEmpty() && holder.coverImage != null) {
            Picasso.get().load(Uri.parse(imageUrl)).into(holder.coverImage)
        } else {
            holder.coverImage?.setImageResource(R.drawable.baseline_album_24)
        }

        holder.itemView.setOnClickListener { v ->
            if (!item.id.isNullOrEmpty()) {
                val intent = Intent(v.context, MusicOverviewActivity::class.java)
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
                        val prefs = SharedPreferenceManager.getInstance(context)
                        prefs.songHistory = data
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
