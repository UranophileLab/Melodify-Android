package dev.melodify.uranophilelab.adapters

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import com.bumptech.glide.Glide
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.ListActivity
import dev.melodify.uranophilelab.adapters.ActivityMainAlbumItemAdapter.ActivityMainAlbumItemAdapterViewHolder
import dev.melodify.uranophilelab.model.AlbumItem
import dev.melodify.uranophilelab.utils.AnimatedMenuHelper

class ActivityMainAlbumItemAdapter(private val data: MutableList<AlbumItem?>) :
    RecyclerView.Adapter<ActivityMainAlbumItemAdapterViewHolder?>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ActivityMainAlbumItemAdapterViewHolder {
        val _v = View.inflate(
            parent.context,
            if (viewType == 0) R.layout.activity_main_songs_item else R.layout.songs_item_shimmer,
            null
        )
        _v.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ActivityMainAlbumItemAdapterViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ActivityMainAlbumItemAdapterViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as? ShimmerFrameLayout)?.startShimmer()
            return
        }

        val item = if (position in 0 until data.size) data[position] else null
        if (item == null) return

        holder.itemView.findViewById<TextView?>(R.id.albumTitle)?.text = item.albumTitle()
        holder.itemView.findViewById<TextView?>(R.id.albumSubTitle)?.text = item.albumSubTitle()

        holder.itemView.findViewById<View?>(R.id.albumTitle)?.isSelected = true
        holder.itemView.findViewById<View?>(R.id.albumSubTitle)?.isSelected = true

        val coverImage = holder.itemView.findViewById<ImageView?>(R.id.coverImage)
        val coverUrl: String? = item.albumCover
        if (!coverUrl.isNullOrEmpty() && coverUrl != "<shimmer>" && coverImage != null) {
            Glide.with(coverImage.context).load(Uri.parse(coverUrl)).into(coverImage)
        }

        holder.itemView.findViewById<View?>(R.id.more)?.setOnClickListener { v ->
            AnimatedMenuHelper.showAnimatedAlbumMenu(
                context = v.context,
                albumItem = item
            )
        }

        holder.itemView.setOnClickListener { v: View? ->
            val albumItem = if (position in 0 until data.size) data[position] else null
            if (albumItem == null) return@setOnClickListener
            v?.context?.startActivity(
                Intent(v.context, ListActivity::class.java)
                    .putExtra("data", Gson().toJson(albumItem))
                    .putExtra("type", "album")
                    .putExtra("id", albumItem.id)
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        val item = if (position in 0 until data.size) data[position] else null
        return if (item?.albumTitle() == "<shimmer>") 1 else 0
    }

    class ActivityMainAlbumItemAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
