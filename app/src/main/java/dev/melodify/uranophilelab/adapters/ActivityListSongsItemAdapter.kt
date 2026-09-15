package dev.melodify.uranophilelab.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.squareup.picasso.Picasso
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.MusicOverviewActivity
import dev.melodify.uranophilelab.records.SongResponse.Song
import dev.melodify.uranophilelab.utils.MusicPlayerManager

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

        holder.titleView?.text = song.name()

        val artistsNames = StringBuilder()
        val artistsList = song.artists?.all.orEmpty()
        for (a in artistsList) {
            val name = a?.name()
            if (!name.isNullOrEmpty() && !artistsNames.contains(name)) {
                if (artistsNames.isNotEmpty()) artistsNames.append(", ")
                artistsNames.append(name)
            }
        }
        holder.artistView?.text = artistsNames.toString()

        val images = song.image
        val imgUrl = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        val coverImage = holder.coverImage
        if (coverImage != null) {
            if (imgUrl.isNotEmpty()) {
                Picasso.get()
                    .load(imgUrl)
                    .placeholder(R.drawable.headphone)
                    .fit()
                    .centerCrop()
                    .into(coverImage)
            } else {
                coverImage.setImageResource(R.drawable.headphone)
            }
        }

        val moreIcon = holder.moreIcon
        if (moreIcon != null) {
            moreIcon.setOnClickListener { v ->
                val popup = PopupMenu(v.context, v)
                popup.menu.add("Play Next")
                popup.menu.add("Add to Queue")
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        "Play Next" -> {
                            MusicPlayerManager.playNext(song.id)
                            Toast.makeText(v.context, "Song will play next", Toast.LENGTH_SHORT).show()
                            true
                        }
                        "Add to Queue" -> {
                            MusicPlayerManager.addToQueue(song.id)
                            Toast.makeText(v.context, "Song added to queue", Toast.LENGTH_SHORT).show()
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
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
