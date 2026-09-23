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
import com.bumptech.glide.Glide
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.MusicOverviewActivity
import dev.melodify.uranophilelab.model.history.SongHistoryItem
import dev.melodify.uranophilelab.utils.SharedPreferenceManager

class SongHistoryAdapter(
    private var data: MutableList<SongHistoryItem>,
    private val isFavoritesMode: Boolean = false,
    private val onItemRemovedListener: (() -> Unit)? = null
) : RecyclerView.Adapter<SongHistoryAdapter.ViewHolder>() {

    fun updateData(newData: List<SongHistoryItem>) {
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
            Glide.with(holder.coverImage.context).load(Uri.parse(imageUrl)).into(holder.coverImage)
        } else {
            holder.coverImage?.setImageResource(R.drawable.baseline_album_24)
        }

        val context = holder.itemView.context
        val prefs = SharedPreferenceManager.getInstance(context)

        val favIcon: ImageView? = holder.itemView.findViewById(R.id.favorite_item_icon)
        val songId = item.id
        if (favIcon != null) {
            val isFav = !songId.isNullOrEmpty() && prefs.isFavorite(songId)
            favIcon.visibility = if (isFav) View.VISIBLE else View.GONE
            favIcon.setImageResource(R.drawable.favorite_24px)
            favIcon.setOnClickListener {
                if (songId.isNullOrEmpty()) return@setOnClickListener
                val newFavState = prefs.toggleFavorite(item)
                favIcon.visibility = if (newFavState) View.VISIBLE else View.GONE
            }
        }

        holder.itemView.setOnClickListener { v ->
            if (!item.id.isNullOrEmpty()) {
                val intent = Intent(v.context, MusicOverviewActivity::class.java)
                    .putExtra("id", item.id)
                v.context.startActivity(intent)
            }
        }

        holder.moreBtn?.setOnClickListener { v ->
            val popup = PopupMenu(context, v)
            popup.menu.add("Remove from history")
            popup.setOnMenuItemClickListener { menuItem ->
                if (menuItem.title == "Remove from history") {
                    val currentPos = holder.bindingAdapterPosition
                    if (currentPos in data.indices) {
                        data.removeAt(currentPos)
                        notifyItemRemoved(currentPos)
                        notifyItemRangeChanged(currentPos, data.size - currentPos)
                        if (isFavoritesMode) {
                            prefs.favoriteSongs = data
                        } else {
                            prefs.songHistory = data
                        }
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
