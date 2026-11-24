package dev.melodify.uranophilelab.adapters

import android.content.Intent
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.MusicOverviewActivity
import dev.melodify.uranophilelab.databinding.ActivityListSongItemBinding
import dev.melodify.uranophilelab.records.sharedpref.SavedLibraries.Library
import dev.melodify.uranophilelab.utils.HapticUtil
import dev.melodify.uranophilelab.utils.MusicPlayerManager
import com.bumptech.glide.Glide
import androidx.core.net.toUri

class UserCreatedSongsListAdapter(private val data: MutableList<Library.Songs?>) :
    RecyclerView.Adapter<UserCreatedSongsListAdapter.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(parent.context, R.layout.activity_list_song_item, null)
        _v.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(_v)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val songItem = data[position]
        holder.binding.title.text = songItem?.title

        val isCurrentPlaying = songItem != null && !songItem.id.isNullOrEmpty() && (songItem.id == MusicPlayerManager.MUSIC_ID)
        if (isCurrentPlaying) {
            holder.binding.title.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.spotify_green))
            holder.binding.title.setTypeface(null, Typeface.BOLD)
        } else {
            holder.binding.title.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_light))
            holder.binding.title.setTypeface(null, Typeface.NORMAL)
        }

        val eqView = holder.binding.playingEqualizer
        val isPlaying = isCurrentPlaying && (MusicPlayerManager.player?.isPlaying == true)
        eqView.visibility = if (isCurrentPlaying) View.VISIBLE else View.GONE
        eqView.isAnimating = isPlaying

        holder.binding.artist.text = songItem?.description
        val imageUrl = songItem?.image
        if (imageUrl?.isNotBlank() == true && imageUrl != "<shimmer>") {
            Glide.with(holder.binding.coverImage.context)
                .load(imageUrl.toUri())
                .placeholder(R.drawable.headphone)
                .error(R.drawable.headphone)
                .into(holder.binding.coverImage)
        } else {
            Glide.with(holder.binding.coverImage.context).clear(holder.binding.coverImage)
            holder.binding.coverImage.setImageResource(R.drawable.headphone)
        }

        // Wire up the 3-dots menu
        val moreIcon = holder.itemView.findViewById<ImageView>(R.id.more)
        moreIcon?.setOnClickListener { v ->
            HapticUtil.tap(v)
            val popup = androidx.appcompat.widget.PopupMenu(v.context, v)
            popup.menu.add("Play Next")
            popup.menu.add("Add to Queue")
            popup.setOnMenuItemClickListener { menuItem ->
                val songId = data.getOrNull(holder.bindingAdapterPosition)?.id
                when (menuItem.title) {
                    "Play Next" -> {
                        if (!songId.isNullOrEmpty()) {
                            MusicPlayerManager.playNext(songId)
                            Toast.makeText(v.context, "Song will play next", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    "Add to Queue" -> {
                        if (!songId.isNullOrEmpty()) {
                            MusicPlayerManager.addToQueue(songId)
                            Toast.makeText(v.context, "Song added to queue", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        holder.itemView.setOnClickListener { view: View? ->
            HapticUtil.tap(view)
            val currentPos = holder.bindingAdapterPosition
            val targetSong = data.getOrNull(currentPos)
            val targetId = targetSong?.id ?: return@setOnClickListener

            // Build full queue from list and set correct position
            if (MusicPlayerManager.trackQueue == null) {
                MusicPlayerManager.trackQueue = ArrayList()
            }
            MusicPlayerManager.trackQueue?.clear()
            var clickIndex = 0
            var validIndex = 0
            for (i in data.indices) {
                val song = data[i]
                if (song != null && !song.id.isNullOrEmpty()) {
                    MusicPlayerManager.trackQueue?.add(song.id)
                    if (i == currentPos) clickIndex = validIndex
                    validIndex++
                }
            }
            MusicPlayerManager.track_position = clickIndex
            val ctx = view?.context ?: holder.itemView.context
            ctx.startActivity(
                Intent(ctx, MusicOverviewActivity::class.java)
                    .putExtra("id", targetId)
            )
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: ActivityListSongItemBinding = ActivityListSongItemBinding.bind(itemView)
    }
}
