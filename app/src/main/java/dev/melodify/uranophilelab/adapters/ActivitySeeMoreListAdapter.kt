package dev.melodify.uranophilelab.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.MusicOverviewActivity
import dev.melodify.uranophilelab.records.SongResponse.Song
import dev.melodify.uranophilelab.utils.MusicPlayerManager

class ActivitySeeMoreListAdapter : RecyclerView.Adapter<ActivitySeeMoreListAdapter.ViewHolder> {
    private val data: MutableList<Song?>?

    constructor(data: MutableList<Song?>?) {
        this.data = data
    }

    constructor() {
        this.data = ArrayList<Song?>()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImage: ImageView? = itemView.findViewById(R.id.coverImage)
        val coverTitle: TextView? = itemView.findViewById(R.id.coverTitle)
        val coverPlayed: TextView? = itemView.findViewById(R.id.coverPlayed)
        val positionTextView: TextView? = itemView.findViewById(R.id.position)
        val moreIcon: ImageView? = itemView.findViewById(R.id.more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = if (viewType == 1) R.layout.activity_artist_profile_view_top_songs_item else R.layout.progress_bar_layout
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == 0) {
            return
        }

        if (data == null || position >= data.size) return
        val song = data[position] ?: return

        holder.positionTextView?.text = (position + 1).toString()
        holder.coverTitle?.text = song.name()
        holder.coverPlayed?.text = String.format("%s | %s", song.year ?: "", song.label ?: "")
        val images = song.image
        val url = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        val coverImageView = holder.coverImage
        if (coverImageView != null) {
            if (url.isNotEmpty() && url != "<shimmer>") {
                Glide.with(coverImageView.context).load(url)
                    .placeholder(R.drawable.headphone)
                    .fitCenter()
                    .centerCrop().into(coverImageView)
            } else {
                coverImageView.setImageResource(R.drawable.headphone)
            }
        }

        holder.itemView.setOnClickListener { view ->
            MusicPlayerManager.trackQueue?.clear()
            var clickIndex = position
            var validIndex = 0
            if (data != null) {
                for (i in data.indices) {
                    val s = data[i]
                    if (s?.id != null && s.id != "<shimmer>") {
                        MusicPlayerManager.trackQueue?.add(s.id)
                        if (i == position) {
                            clickIndex = validIndex
                        }
                        validIndex++
                    }
                }
            }
            MusicPlayerManager.track_position = clickIndex
            view.context.startActivity(
                Intent(view.context, MusicOverviewActivity::class.java).putExtra("id", song.id)
            )
        }
    }

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }

    override fun getItemViewType(position: Int): Int {
        return ITEM
    }

    fun add(da: Song?) {
        data?.add(da)
        notifyItemInserted((data?.size ?: 1) - 1)
    }

    fun addAll(moveResults: MutableList<Song?>) {
        for (result in moveResults) {
            add(result)
        }
    }

    enum class Mode {
        TOP_SONGS,
        TOP_ALBUMS,
        TOP_SINGLES
    }

    companion object {
        private const val LOADING = 0
        private const val ITEM = 1
    }
}
