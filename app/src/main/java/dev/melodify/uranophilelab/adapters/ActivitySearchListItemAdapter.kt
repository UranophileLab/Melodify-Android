package dev.melodify.uranophilelab.adapters

import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.bumptech.glide.Glide
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.ArtistProfileActivity
import dev.melodify.uranophilelab.activities.ListActivity
import dev.melodify.uranophilelab.activities.MusicOverviewActivity
import dev.melodify.uranophilelab.model.AlbumItem
import dev.melodify.uranophilelab.model.BasicDataRecord
import dev.melodify.uranophilelab.model.SearchListItem
import dev.melodify.uranophilelab.model.history.SongHistoryItem
import dev.melodify.uranophilelab.records.sharedpref.SavedLibraries
import dev.melodify.uranophilelab.utils.MusicPlayerManager
import dev.melodify.uranophilelab.utils.SharedPreferenceManager

class ActivitySearchListItemAdapter(private val data: MutableList<SearchListItem>) :
    RecyclerView.Adapter<ActivitySearchListItemAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView: TextView? = itemView.findViewById(R.id.title)
        val artistView: TextView? = itemView.findViewById(R.id.artist)
        val coverCard: MaterialCardView? = itemView.findViewById(R.id.coverCard)
        val coverImage: ImageView? = itemView.findViewById(R.id.coverImage)
        val moreIcon: ImageView? = itemView.findViewById(R.id.more)
        val shimmer: ShimmerFrameLayout? = itemView.findViewById(R.id.shimmer)

        init {
            titleView?.isSelected = true
            artistView?.isSelected = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = if (viewType == 0) R.layout.activity_list_song_item else R.layout.activity_list_shimmer
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            holder.shimmer?.startShimmer()
            return
        }

        val item = data[position]
        val parsedTitle = item.title()
        val parsedSubtitle = item.subtitle()

        val isCurrentPlaying = item.type == SearchListItem.Type.SONG && !item.id.isNullOrEmpty() && (item.id == MusicPlayerManager.MUSIC_ID)
        holder.titleView?.text = parsedTitle
        if (isCurrentPlaying) {
            holder.titleView?.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.spotify_green))
            holder.titleView?.setTypeface(null, Typeface.BOLD)
        } else {
            holder.titleView?.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_light))
            holder.titleView?.setTypeface(null, Typeface.NORMAL)
        }

        val typePrefix = when (item.type) {
            SearchListItem.Type.SONG -> "Song"
            SearchListItem.Type.ALBUM -> "Album"
            SearchListItem.Type.PLAYLIST -> "Playlist"
            SearchListItem.Type.ARTIST -> "Artist"
            else -> ""
        }
        val formattedSubtitle = if (typePrefix.isNotEmpty() && parsedSubtitle.isNotEmpty()) {
            "$typePrefix • $parsedSubtitle"
        } else if (typePrefix.isNotEmpty()) {
            typePrefix
        } else {
            parsedSubtitle
        }
        holder.artistView?.text = formattedSubtitle

        val coverCard = holder.coverCard
        if (coverCard != null) {
            val density = coverCard.context.resources.displayMetrics.density
            val radiusPx = if (item.type == SearchListItem.Type.ARTIST) (20f * density) else (4f * density)
            coverCard.radius = radiusPx
        }

        val favIcon = holder.itemView.findViewById<ImageView>(R.id.favorite_item_icon)
        val prefs = SharedPreferenceManager.getInstance(holder.itemView.context)

        if (item.type == SearchListItem.Type.SONG) {
            val isFav = !item.id.isNullOrEmpty() && prefs.isFavorite(item.id)
            favIcon?.visibility = if (isFav) View.VISIBLE else View.GONE
            favIcon?.setImageResource(R.drawable.favorite_24px)
        } else {
            favIcon?.visibility = View.GONE
        }

        val moreIcon = holder.moreIcon
        if (moreIcon != null) {
            when (item.type) {
                SearchListItem.Type.SONG -> {
                    moreIcon.visibility = View.VISIBLE
                    moreIcon.setOnClickListener { v ->
                        val popup = PopupMenu(v.context, v)
                        popup.menu.add("Play Next")
                        popup.menu.add("Add to Queue")
                        popup.setOnMenuItemClickListener { menuItem ->
                            when (menuItem.title) {
                                "Play Next" -> {
                                    MusicPlayerManager.playNext(item.id)
                                    Toast.makeText(v.context, "Song will play next", Toast.LENGTH_SHORT).show()
                                    true
                                }
                                "Add to Queue" -> {
                                    MusicPlayerManager.addToQueue(item.id)
                                    Toast.makeText(v.context, "Song added to queue", Toast.LENGTH_SHORT).show()
                                    true
                                }
                                else -> false
                            }
                        }
                        popup.show()
                    }
                }

                SearchListItem.Type.ALBUM, SearchListItem.Type.PLAYLIST -> {
                    moreIcon.visibility = View.VISIBLE
                    moreIcon.setOnClickListener { v ->
                        val popup = PopupMenu(v.context, v)
                        val isSaved = !item.id.isNullOrEmpty() && isLibrarySaved(item.id, prefs.savedLibrariesData)
                        popup.menu.add(if (isSaved) "Remove from Library" else "Add to Library")
                        popup.menu.add("Play")
                        popup.setOnMenuItemClickListener { menuItem ->
                            when (menuItem.title) {
                                "Add to Library", "Remove from Library" -> {
                                    if (!item.id.isNullOrEmpty()) {
                                        val isAlbum = item.type == SearchListItem.Type.ALBUM
                                        val newSavedState = toggleSavedLibrary(
                                            id = item.id,
                                            title = parsedTitle,
                                            subtitle = parsedSubtitle,
                                            coverUrl = item.coverImage ?: "",
                                            isAlbum = isAlbum,
                                            prefs = prefs
                                        )
                                        favIcon?.setImageResource(if (newSavedState) R.drawable.favorite_24px else R.drawable.favorite_outline_24px)
                                        Toast.makeText(
                                            v.context,
                                            if (newSavedState) "Added to Library" else "Removed from Library",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    true
                                }
                                "Play" -> {
                                    holder.itemView.performClick()
                                    true
                                }
                                else -> false
                            }
                        }
                        popup.show()
                    }
                }

                else -> {
                    moreIcon.visibility = View.GONE
                }
            }
        }

        val coverImageView = holder.coverImage
        if (coverImageView != null) {
            val rawImageUrl: String? = item.coverImage
            val imageUrl = if (!rawImageUrl.isNullOrEmpty() && rawImageUrl.contains("50x50")) {
                rawImageUrl.replace("50x50", "500x500")
            } else rawImageUrl ?: ""
            val isInvalid = imageUrl.isBlank() || imageUrl == "<shimmer>" || imageUrl.contains("default") || imageUrl.contains("artist-default")
            if (!isInvalid) {
                Glide.with(coverImageView.context).load(imageUrl)
                    .placeholder(R.drawable.headphone)
                    .error(R.drawable.headphone)
                    .fitCenter()
                    .centerCrop().into(coverImageView)
            } else {
                coverImageView.setImageResource(R.drawable.headphone)
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent()
            intent.putExtra("id", item.id)
            when (item.type) {
                SearchListItem.Type.SONG -> {
                    MusicPlayerManager.trackQueue?.clear()
                    var clickIndex = 0
                    var validIndex = 0
                    for (i in data.indices) {
                        val searchItem = data[i]
                        if (searchItem.type == SearchListItem.Type.SONG && !searchItem.id.isNullOrEmpty() && searchItem.id != "<shimmer>") {
                            MusicPlayerManager.trackQueue?.add(searchItem.id)
                            if (i == position) {
                                clickIndex = validIndex
                            }
                            validIndex++
                        }
                    }
                    MusicPlayerManager.track_position = clickIndex
                    intent.setClass(holder.itemView.context, MusicOverviewActivity::class.java)
                }

                SearchListItem.Type.ALBUM -> {
                    val albumItem =
                        AlbumItem(parsedTitle, parsedSubtitle, item.coverImage, item.id, "album")
                    intent.putExtra("data", Gson().toJson(albumItem))
                    intent.putExtra("type", "album")
                    intent.putExtra("id", item.id)
                    intent.setClass(holder.itemView.context, ListActivity::class.java)
                }

                SearchListItem.Type.PLAYLIST -> {
                    val albumItem =
                        AlbumItem(parsedTitle, parsedSubtitle, item.coverImage, item.id, "playlist")
                    intent.putExtra("data", Gson().toJson(albumItem))
                    intent.putExtra("type", "playlist")
                    intent.putExtra("id", item.id)
                    intent.setClass(holder.itemView.context, ListActivity::class.java)
                }

                SearchListItem.Type.ARTIST -> {
                    intent.setClass(holder.itemView.context, ArtistProfileActivity::class.java)
                    intent.putExtra(
                        "data",
                        Gson().toJson(BasicDataRecord(item.id, parsedTitle, "", item.coverImage))
                    )
                }

                else -> {}
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (data[position].id == "<shimmer>") 1 else 0
    }

    private fun isLibrarySaved(id: String?, savedLibraries: SavedLibraries?): Boolean {
        if (id.isNullOrBlank() || savedLibraries?.lists.isNullOrEmpty()) return false
        return savedLibraries?.lists?.any { it?.id == id } == true
    }

    private fun toggleSavedLibrary(
        id: String,
        title: String,
        subtitle: String,
        coverUrl: String,
        isAlbum: Boolean,
        prefs: SharedPreferenceManager
    ): Boolean {
        val saved = prefs.savedLibrariesData
        val list = saved?.lists
        if (list != null) {
            val index = list.indexOfFirst { it?.id == id }
            if (index != -1) {
                prefs.removeLibraryFromSavedLibraries(index)
                return false
            }
        }
        val library = SavedLibraries.Library(
            id,
            false,
            isAlbum,
            title,
            coverUrl,
            subtitle,
            ArrayList()
        )
        prefs.addLibraryToSavedLibraries(library)
        return true
    }
}
