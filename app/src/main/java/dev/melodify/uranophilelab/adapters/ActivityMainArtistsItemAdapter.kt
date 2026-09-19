package dev.melodify.uranophilelab.adapters

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.ArtistProfileActivity
import dev.melodify.uranophilelab.adapters.ActivityMainArtistsItemAdapter.ActivityMainArtistsItemAdapterViewHolder
import dev.melodify.uranophilelab.model.BasicDataRecord
import dev.melodify.uranophilelab.records.ArtistsSearch

class ActivityMainArtistsItemAdapter(private val data: MutableList<ArtistsSearch.Data.Results?>) :
    RecyclerView.Adapter<ActivityMainArtistsItemAdapterViewHolder?>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ActivityMainArtistsItemAdapterViewHolder {
        val _v = View.inflate(
            parent.context,
            if (viewType == 0) R.layout.activity_main_artists_item else R.layout.artists_item_shimmer,
            null
        )
        _v.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ActivityMainArtistsItemAdapterViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ActivityMainArtistsItemAdapterViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as? ShimmerFrameLayout)?.startShimmer()
            return
        }

        val item = if (position in 0 until data.size) data[position] else null
        if (item == null) return

        holder.itemView.findViewById<View?>(R.id.artist_name)?.isSelected = true
        holder.itemView.findViewById<TextView?>(R.id.artist_name)?.text = item.name()
        val imageView = holder.itemView.findViewById<ImageView?>(R.id.artist_img)
        val images = item.image
        val rawUrl = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        val url = if (rawUrl.contains("50x50")) rawUrl.replace("50x50", "500x500") else rawUrl
        val isInvalid = url.isBlank() || url.contains("default") || url.contains("artist-default")
        if (imageView != null) {
            if (!isInvalid) {
                Picasso.get()
                    .load(url.toUri())
                    .placeholder(R.drawable.headphone)
                    .error(R.drawable.headphone)
                    .fit()
                    .centerCrop()
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.headphone)
            }
        }

        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            val artistItem = if (position in 0 until data.size) data[position] else null ?: return@OnClickListener
            if (artistItem == null) return@OnClickListener
            val itemImages = artistItem.image
            val itemUrl = if (itemImages.isNullOrEmpty()) "" else itemImages[itemImages.size - 1]?.url ?: ""
            v?.context?.startActivity(
                Intent(v.context, ArtistProfileActivity::class.java)
                    .putExtra(
                        "data",
                        Gson().toJson(
                            BasicDataRecord(
                                artistItem.id,
                                artistItem.name(),
                                "",
                                itemUrl
                            )
                        )
                    )
            )
        })
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        val item = if (position in 0 until data.size) data[position] else null
        return if (item?.id == "<shimmer>") 1 else 0
    }

    class ActivityMainArtistsItemAdapterViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView)
}
