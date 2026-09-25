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
import com.bumptech.glide.Glide
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.MusicOverviewActivity
import dev.melodify.uranophilelab.model.history.SongHistoryItem
import dev.melodify.uranophilelab.records.SongResponse.Song
import dev.melodify.uranophilelab.utils.AnimatedMenuHelper
import dev.melodify.uranophilelab.utils.MusicPlayerManager
import dev.melodify.uranophilelab.utils.SharedPreferenceManager

class ActivityListSongsItemAdapter(private val data: MutableList<Song>) :
    RecyclerView.Adapter<ActivityListSongsItemAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView: TextView? = itemView.findViewById(R.id.title)
        val artistView: TextView? = itemView.findViewById(R.id.artist)
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

        val song = data[position]

        val isCurrentPlaying = !song.id.isNullOrEmpty() && song.id == MusicPlayerManager.MUSIC_ID
        holder.titleView?.text = song.name()
        if (isCurrentPlaying) {
            holder.titleView?.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.spotify_green))
            holder.titleView?.setTypeface(null, Typeface.BOLD)
        } else {
            holder.titleView?.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_light))
            holder.titleView?.setTypeface(null, Typeface.NORMAL)
        }

        val artistsNames = StringBuilder()
        val artistsList = song.artists?.all.orEmpty()
        for (a in artistsList) {
            val name = a?.name()
            if (!name.isNullOrEmpty() && !artistsNames.contains(name)) {
                if (artistsNames.isNotEmpty()) artistsNames.append(", ")
                artistsNames.append(name)
            }
        }
        val artistText = artistsNames.toString()
        holder.artistView?.text = if (artistText.isNotEmpty()) "• $artistText" else ""

        val favIcon: ImageView? = holder.itemView.findViewById(R.id.favorite_item_icon)
        val prefs = SharedPreferenceManager.getInstance(holder.itemView.context)
        val songId = song.id
        if (favIcon != null) {
            val isFav = !songId.isNullOrEmpty() && prefs.isFavorite(songId)
            favIcon.visibility = if (isFav) View.VISIBLE else View.GONE
            favIcon.setImageResource(R.drawable.favorite_24px)
            favIcon.setOnClickListener {
                if (songId.isNullOrEmpty()) return@setOnClickListener
                val primaryArtist = if (artistsList.isNotEmpty()) artistsList[0]?.name() ?: "" else ""
                val images = song.image
                val imgUrl = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
                val item = SongHistoryItem(
                    id = songId,
                    title = song.name(),
                    artist = primaryArtist,
                    imageUrl = imgUrl
                )
                val newFavState = prefs.toggleFavorite(item)
                favIcon.visibility = if (newFavState) View.VISIBLE else View.GONE
            }
        }

        val images = song.image
        val imgUrl = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        val coverImage = holder.coverImage
        if (coverImage != null) {
            if (imgUrl.isNotEmpty() && imgUrl != "<shimmer>") {
                Glide.with(coverImage.context).load(imgUrl)
                    .placeholder(R.drawable.headphone)
                    .fitCenter()
                    .centerCrop().into(coverImage)
            } else {
                coverImage.setImageResource(R.drawable.headphone)
            }
        }

        val moreIcon = holder.moreIcon
        if (moreIcon != null) {
            moreIcon.setOnClickListener { v ->
                val songId = song.id ?: return@setOnClickListener
                val primaryArtist = if (artistsList.isNotEmpty()) artistsList[0]?.name() ?: "" else ""
                AnimatedMenuHelper.showAnimatedSongMenu(
                    context = v.context,
                    songId = songId,
                    title = song.name(),
                    artist = primaryArtist,
                    coverUrl = imgUrl,
                    albumId = song.album?.id
                )
            }
        }

        holder.itemView.setOnClickListener { view: View ->
            MusicPlayerManager.trackQueue?.clear()
            var clickIndex = 0
            var validIndex = 0
            val adapterPos = holder.bindingAdapterPosition
            for (i in data.indices) {
                val id = data[i].id
                if (id != null && id != "<shimmer>") {
                    MusicPlayerManager.trackQueue?.add(id)
                    if (i == adapterPos) {
                        clickIndex = validIndex
                    }
                    validIndex++
                }
            }
            MusicPlayerManager.track_position = clickIndex
            holder.itemView.context.startActivity(
                Intent(view.context, MusicOverviewActivity::class.java).putExtra(
                    "id",
                    song.id
                )
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (data[position].id == "<shimmer>") 1 else 0
    }
}
