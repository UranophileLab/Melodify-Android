package dev.melodify.uranophilelab.adapters

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.MusicOverviewActivity
import dev.melodify.uranophilelab.databinding.ActivityArtistProfileViewTopSongsItemBinding
import dev.melodify.uranophilelab.records.SongResponse.Song
import dev.melodify.uranophilelab.utils.MusicPlayerManager
import com.bumptech.glide.Glide
import androidx.core.net.toUri
import android.widget.Toast
import androidx.core.content.ContextCompat
import dev.melodify.uranophilelab.utils.AnimatedMenuHelper

class ActivityArtistProfileTopSongsAdapter(private val data: MutableList<Song?>) :
    RecyclerView.Adapter<ActivityArtistProfileTopSongsAdapter.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(
            parent.context,
            if (viewType == 1) R.layout.activity_artist_profile_view_top_songs_item else R.layout.artist_profile_view_top_songs_shimmer,
            null
        )
        _v.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == 0) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as ShimmerFrameLayout).startShimmer()
            return
        }

        val itemView = ActivityArtistProfileViewTopSongsItemBinding.bind(holder.itemView)

        val songData = data[position]
        val isCurrentPlaying = songData != null && !songData.id.isNullOrEmpty() && (songData.id == MusicPlayerManager.MUSIC_ID)

        itemView.position.text = (position + 1).toString()
        itemView.coverTitle.text = songData?.name()
        if (isCurrentPlaying) {
            itemView.coverTitle.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.spotify_green))
            itemView.coverTitle.setTypeface(null, Typeface.BOLD)
        } else {
            itemView.coverTitle.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_light))
            itemView.coverTitle.setTypeface(null, Typeface.NORMAL)
        }

        itemView.coverPlayed.text = String.format("%s | %s", data[position]!!.year, data[position]!!.label)
        val images = data[position]?.image
        val url = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        if (url.isNotEmpty() && url != "<shimmer>") {
            Glide.with(itemView.coverImage.context).load(url.toUri()).into(itemView.coverImage)
        }

        itemView.more.setOnClickListener { v ->
            val song = data[position] ?: return@setOnClickListener
            val songId = song.id ?: return@setOnClickListener
            val images = song.image
            val coverUrl = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
            val primaryArtist = song.artists?.primary?.firstOrNull()?.name() ?: song.label ?: ""
            AnimatedMenuHelper.showAnimatedSongMenu(
                context = v.context,
                songId = songId,
                title = song.name(),
                artist = primaryArtist,
                coverUrl = coverUrl,
                albumId = song.album?.id
            )
        }

        holder.itemView.setOnClickListener { view: View? ->
            MusicPlayerManager.trackQueue?.clear()
            var clickIndex = position
            var validIndex = 0
            for (i in data.indices) {
                val song = data[i]
                if (song != null && song.id != null && song.id != "<shimmer>") {
                    MusicPlayerManager.trackQueue?.add(song.id)
                    if (i == position) {
                        clickIndex = validIndex
                    }
                    validIndex++
                }
            }
            MusicPlayerManager.track_position = clickIndex
            view!!.context.startActivity(
                Intent(
                    view.context,
                    MusicOverviewActivity::class.java
                ).putExtra("id", data[position]!!.id)
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        if (data.get(position)!!.id == "<shimmer>") return 0
        else return 1
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
