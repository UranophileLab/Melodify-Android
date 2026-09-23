package dev.melodify.uranophilelab.adapters

import android.content.Intent
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.bumptech.glide.Glide
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.MusicOverviewActivity
import dev.melodify.uranophilelab.model.AlbumItem
import dev.melodify.uranophilelab.utils.MusicPlayerManager

class ActivityMainPopularSongs(private val data: MutableList<AlbumItem?>) :
    RecyclerView.Adapter<ActivityMainPopularSongs.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(
            parent.context,
            if (viewType == 0) R.layout.activity_main_songs_item else R.layout.songs_item_shimmer,
            null
        )
        _v.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as? ShimmerFrameLayout)?.startShimmer()
            return
        }

        val item = if (position in 0 until data.size) data[position] else return
        if (item == null) return

        holder.itemView.findViewById<TextView?>(R.id.albumTitle)?.text = item.albumTitle()
        holder.itemView.findViewById<TextView?>(R.id.albumSubTitle)?.text = item.albumSubTitle()

        holder.itemView.findViewById<View?>(R.id.albumTitle)?.isSelected = true
        holder.itemView.findViewById<View?>(R.id.albumSubTitle)?.isSelected = true

        val coverImage = holder.itemView.findViewById<ImageView?>(R.id.coverImage)
        val coverUrl = item.albumCover
        if (!coverUrl.isNullOrEmpty() && coverImage != null) {
            Glide.with(coverImage.context).load(coverUrl.toUri()).into(coverImage)
        }

        holder.itemView.setOnClickListener { v: View? ->
            MusicPlayerManager.trackQueue?.clear()
            Log.d(
                "AdapterDebug",
                "Click at pos: " + position + ". Populating queue with " + data.size + " items."
            )
            for (i in data.indices) {
                val songItem = data[i] ?: continue
                val id = songItem.id
                Log.d(
                    "AdapterDebug",
                    "Queue[" + i + "]: " + id + " - " + songItem.albumTitle()
                )
                MusicPlayerManager.trackQueue?.add(id)
            }
            MusicPlayerManager.track_position = position
            v?.context?.startActivity(
                Intent(v.context, MusicOverviewActivity::class.java).putExtra(
                    "id",
                    item.id
                )
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        val item = if (position in 0 until data.size) data[position] else return 0
        return if (item?.albumTitle() == "<shimmer>") 1 else 0
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
