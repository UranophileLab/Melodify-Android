package dev.melodify.uranophilelab.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.ListActivity
import dev.melodify.uranophilelab.activities.MusicOverviewActivity
import dev.melodify.uranophilelab.model.AlbumItem
import dev.melodify.uranophilelab.utils.AnimatedMenuHelper

import dev.melodify.uranophilelab.utils.MusicPlayerManager

class ActivityMainTrendingAdapter(private val data: List<AlbumItem?>) :
    RecyclerView.Adapter<ActivityMainTrendingAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView? = itemView.findViewById(R.id.trending_title)
        val cover: ImageView? = itemView.findViewById(R.id.trending_cover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_main_trending_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position] ?: return

        holder.title?.text = item.albumTitle() ?: ""

        val imageUrl = item.albumCover
        if (!imageUrl.isNullOrBlank() && imageUrl != "<shimmer>") {
            holder.cover?.let {
                Glide.with(it.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.baseline_album_24)
                    .centerCrop()
                    .into(it)
            }
        } else {
            holder.cover?.setImageResource(R.drawable.baseline_album_24)
        }

        holder.itemView.findViewById<View?>(R.id.more)?.setOnClickListener { v ->
            if (item.type == "song") {
                AnimatedMenuHelper.showAnimatedSongMenu(
                    context = v.context,
                    songId = item.id ?: "",
                    title = item.albumTitle() ?: "",
                    artist = item.albumSubTitle() ?: "",
                    coverUrl = item.albumCover ?: ""
                )
            } else {
                AnimatedMenuHelper.showAnimatedAlbumMenu(
                    context = v.context,
                    albumItem = item
                )
            }
        }

        holder.itemView.setOnClickListener { view ->
            val context = view.context
            if (!item.id.isNullOrBlank()) {
                if (item.type == "song") {
                    MusicPlayerManager.trackQueue = arrayListOf(item.id)
                    MusicPlayerManager.track_position = 0
                    val intent = Intent(context, MusicOverviewActivity::class.java).apply {
                        putExtra("id", item.id)
                    }
                    context.startActivity(intent)
                } else {
                    val intent = Intent(context, ListActivity::class.java).apply {
                        putExtra("data", Gson().toJson(item))
                        putExtra("id", item.id)
                        putExtra("type", if (item.type == "playlist") "playlist" else "album")
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}
