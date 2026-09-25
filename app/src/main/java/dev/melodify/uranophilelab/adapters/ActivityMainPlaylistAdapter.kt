package dev.melodify.uranophilelab.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import com.bumptech.glide.Glide
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.ListActivity
import dev.melodify.uranophilelab.adapters.ActivityMainPlaylistAdapter.PlaylistAdapterViewHolder
import dev.melodify.uranophilelab.model.AlbumItem

class ActivityMainPlaylistAdapter(private val data: MutableList<AlbumItem?>) :
    RecyclerView.Adapter<PlaylistAdapterViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistAdapterViewHolder {
        val _v = LayoutInflater.from(parent.context).inflate(
            if (viewType == 0) R.layout.activity_main_playlist_item else R.layout.main_playlist_item_shimmer,
            null,
            false
        )
        _v.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return PlaylistAdapterViewHolder(_v)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: PlaylistAdapterViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as? ShimmerFrameLayout)?.startShimmer()
            return
        }

        val item = data[position] ?: return

        holder.itemView.findViewById<TextView?>(R.id.title)?.text = item.albumTitle()
        val imageView = holder.itemView.findViewById<ImageView?>(R.id.imageView)
        val coverUrl: String? = item.albumCover
        if (coverUrl != null && coverUrl.isNotEmpty() && coverUrl != "<shimmer>" && imageView != null) {
            Glide.with(imageView.context).load(Uri.parse(coverUrl)).into(imageView)
        }

        holder.itemView.setOnClickListener { v: View? ->
            val playlistItem = data[position] ?: return@setOnClickListener
            v?.context?.startActivity(
                Intent(v.context, ListActivity::class.java).putExtra(
                    "data",
                    Gson().toJson(playlistItem)
                )
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = data[position] ?: return 0
        return if (item.id == "<shimmer>") 1 else 0
    }

    class PlaylistAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
