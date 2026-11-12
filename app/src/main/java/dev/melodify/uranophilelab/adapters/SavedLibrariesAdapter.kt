package dev.melodify.uranophilelab.adapters

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.ListActivity
import dev.melodify.uranophilelab.model.AlbumItem
import dev.melodify.uranophilelab.records.sharedpref.SavedLibraries.Library
import com.bumptech.glide.Glide
import androidx.core.net.toUri
import dev.melodify.uranophilelab.utils.MusicPlayerManager
import android.widget.Toast

class SavedLibrariesAdapter(private val data: MutableList<Library?>) :
    RecyclerView.Adapter<SavedLibrariesAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(parent.context, R.layout.activity_list_song_item, null)
        _v.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val library = data[position]!!
        holder.title.text = library.name
        holder.artist.text = library.description

        // Use first song's image if library cover is blank
        val coverUrl = library.image?.takeIf { it.isNotBlank() && it != "<shimmer>" }
            ?: library.songs?.firstOrNull { it?.image?.isNotBlank() == true && it.image != "<shimmer>" }?.image
        if (!coverUrl.isNullOrBlank() && coverUrl != "<shimmer>") {
            holder.coverImage?.let { 
                Glide.with(it.context).load(coverUrl.toUri())
                    .placeholder(if (library.id == "liked_songs") R.drawable.favorite_24px else R.drawable.baseline_album_24)
                    .into(it) 
            }
        } else {
            if (library.id == "liked_songs") {
                holder.coverImage?.setImageResource(R.drawable.favorite_24px)
                holder.coverImage?.setColorFilter(holder.itemView.context.resources.getColor(R.color.spotify_green, null))
            } else {
                holder.coverImage?.setImageResource(R.drawable.baseline_album_24)
                holder.coverImage?.clearColorFilter()
            }
        }

        // 3-dots more menu
        val moreIcon = holder.itemView.findViewById<ImageView>(R.id.more)
        moreIcon?.setOnClickListener { v ->
            val popup = androidx.appcompat.widget.PopupMenu(v.context, v)
            popup.menu.add("Play All")
            if (library.isCreatedByUser && library.id != "liked_songs") {
                popup.menu.add("Delete Playlist")
            }
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Play All" -> {
                        val songIds = library.songs?.mapNotNull { it?.id } ?: emptyList()
                        if (songIds.isNotEmpty()) {
                            MusicPlayerManager.trackQueue = ArrayList(songIds)
                            MusicPlayerManager.track_position = 0
                            val intent = Intent(v.context, dev.melodify.uranophilelab.activities.MusicOverviewActivity::class.java)
                                .putExtra("id", songIds[0])
                            v.context.startActivity(intent)
                        } else {
                            Toast.makeText(v.context, "No songs in this library", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    "Delete Playlist" -> {
                        val currentPos = holder.bindingAdapterPosition
                        if (currentPos >= 0 && currentPos < data.size) {
                            val prefs = dev.melodify.uranophilelab.utils.SharedPreferenceManager.getInstance(v.context)
                            val saved = prefs.savedLibrariesData
                            val lists = saved?.lists ?: mutableListOf()
                            val idx = lists.indexOfFirst { it?.id == library.id }
                            if (idx >= 0) {
                                prefs.removeLibraryFromSavedLibraries(idx)
                            }
                            data.removeAt(currentPos)
                            notifyItemRemoved(currentPos)
                            Toast.makeText(v.context, "Playlist deleted", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            val albumItem = AlbumItem(
                library.name,
                library.description,
                coverUrl,
                library.id
            )
            if (library.isCreatedByUser) {
                v!!.context.startActivity(
                    Intent(v.context, ListActivity::class.java)
                        .putExtra("id", library.id)
                        .putExtra("data", Gson().toJson(albumItem))
                        .putExtra("type", "playlist")
                        .putExtra("createdByUser", true)
                )
                return@OnClickListener
            }
            v!!.context.startActivity(
                Intent(v.context, ListActivity::class.java)
                    .putExtra("data", Gson().toJson(albumItem))
                    .putExtra("type", if (library.isAlbum) "album" else "playlist")
                    .putExtra("id", library.id)
            )
        })
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImage: ImageView? = itemView.findViewById(R.id.coverImage)
        val title: TextView = itemView.findViewById(R.id.title)
        val artist: TextView = itemView.findViewById(R.id.artist)
    }
}
