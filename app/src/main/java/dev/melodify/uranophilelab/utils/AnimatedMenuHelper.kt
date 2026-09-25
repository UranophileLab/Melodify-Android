package dev.melodify.uranophilelab.utils

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.activities.AboutActivity
import dev.melodify.uranophilelab.activities.DownloadManagerActivity
import dev.melodify.uranophilelab.activities.FavoritesActivity
import dev.melodify.uranophilelab.activities.HistoryActivity
import dev.melodify.uranophilelab.activities.ListActivity
import dev.melodify.uranophilelab.activities.SavedLibrariesActivity
import dev.melodify.uranophilelab.activities.SettingsActivity
import dev.melodify.uranophilelab.model.AlbumItem
import dev.melodify.uranophilelab.model.history.SongHistoryItem
import dev.melodify.uranophilelab.records.sharedpref.SavedLibraries
import dev.melodify.uranophilelab.utils.customview.BottomSheetItemView

object AnimatedMenuHelper {

    /**
     * Cascading entrance animation for all children inside a container view.
     * Animates each menu element from left to right with a smooth delay.
     */
    fun animateMenuItems(container: ViewGroup) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            child.alpha = 0f
            child.translationX = -120f
            child.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(320)
                .setStartDelay((i * 60).toLong())
                .setInterpolator(DecelerateInterpolator(1.8f))
                .start()
        }
    }

    /**
     * Shows an animated bottom sheet options menu for a song item.
     */
    fun showAnimatedSongMenu(
        context: Context,
        songId: String,
        title: String,
        artist: String,
        coverUrl: String,
        albumId: String? = null
    ) {
        val bottomSheetDialog = BottomSheetDialog(context, R.style.MyBottomSheetDialogTheme)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.activity_list_more_info_bottom_sheet, null)

        val coverImage = dialogView.findViewById<ImageView>(R.id.coverImage)
        val albumTitle = dialogView.findViewById<TextView>(R.id.albumTitle)
        val albumSubTitle = dialogView.findViewById<TextView>(R.id.albumSubTitle)
        val mainContainer = dialogView.findViewById<LinearLayout>(R.id.main)

        albumTitle?.text = title
        albumSubTitle?.text = if (artist.isBlank()) "Song" else artist

        if (!coverUrl.isBlank() && coverUrl != "<shimmer>" && coverImage != null) {
            val cleanUrl = if (coverUrl.startsWith("http:")) coverUrl.replace("http:", "https:") else coverUrl
            Glide.with(coverImage.context)
                .load(cleanUrl.toUri())
                .placeholder(R.drawable.headphone)
                .error(R.drawable.headphone)
                .into(coverImage)
        }

        // Clear default extra items from layout if any
        val staticChildCount = 2 // bar layout and header linear layout
        while (mainContainer.childCount > staticChildCount) {
            mainContainer.removeViewAt(mainContainer.childCount - 1)
        }

        // 1. Play Next
        val playNextView = BottomSheetItemView(context, "Play Next", "", null)
        playNextView.iconImageView?.setImageResource(R.drawable.ic_queue_music)
        playNextView.setOnClickListener {
            bottomSheetDialog.dismiss()
            MusicPlayerManager.playNext(songId)
            Toast.makeText(context, "Playing next: $title", Toast.LENGTH_SHORT).show()
        }
        mainContainer.addView(playNextView)

        // 2. Add to Queue
        val addToQueueView = BottomSheetItemView(context, "Add to Queue", "", null)
        addToQueueView.iconImageView?.setImageResource(R.drawable.round_add_24)
        addToQueueView.setOnClickListener {
            bottomSheetDialog.dismiss()
            MusicPlayerManager.addToQueue(songId)
            Toast.makeText(context, "Added to queue: $title", Toast.LENGTH_SHORT).show()
        }
        mainContainer.addView(addToQueueView)

        // 3. Favorite / Like Toggle
        val prefs = SharedPreferenceManager.getInstance(context)
        val isFav = prefs.isFavorite(songId)
        val favTitle = if (isFav) "Remove from Favorites" else "Add to Favorites"
        val favIconRes = if (isFav) R.drawable.favorite_24px else R.drawable.favorite_outline_24px
        val favView = BottomSheetItemView(context, favTitle, "", null)
        favView.iconImageView?.setImageResource(favIconRes)
        favView.setOnClickListener {
            bottomSheetDialog.dismiss()
            val item = SongHistoryItem(
                id = songId,
                title = title,
                artist = artist,
                imageUrl = coverUrl
            )
            val newFavState = prefs.toggleFavorite(item)
            val msg = if (newFavState) "Added to Favorites" else "Removed from Favorites"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
        mainContainer.addView(favView)

        // 4. Go to Album (if albumId present)
        if (!albumId.isNullOrBlank()) {
            val albumView = BottomSheetItemView(context, "Go to Album", "", null)
            albumView.iconImageView?.setImageResource(R.drawable.baseline_album_24)
            albumView.setOnClickListener {
                bottomSheetDialog.dismiss()
                val albumItem = AlbumItem(title, artist, coverUrl, albumId, "album")
                val intent = Intent(context, ListActivity::class.java).apply {
                    putExtra("type", "album")
                    putExtra("id", albumId)
                    putExtra("data", Gson().toJson(albumItem))
                }
                context.startActivity(intent)
            }
            mainContainer.addView(albumView)
        }

        // 5. Share
        val shareView = BottomSheetItemView(context, "Share Song", "", null)
        shareView.iconImageView?.setImageResource(R.drawable.share_24px)
        shareView.setOnClickListener {
            bottomSheetDialog.dismiss()
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Listen to $title by $artist on Melodify!\nhttps://saavn.me/s/$songId")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share via"))
        }
        mainContainer.addView(shareView)

        bottomSheetDialog.setContentView(dialogView)

        bottomSheetDialog.setOnShowListener {
            animateMenuItems(mainContainer)
        }

        bottomSheetDialog.show()
    }

    /**
     * Shows an animated bottom sheet options menu for an Album / Playlist.
     */
    fun showAnimatedAlbumMenu(context: Context, albumItem: AlbumItem) {
        val bottomSheetDialog = BottomSheetDialog(context, R.style.MyBottomSheetDialogTheme)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.activity_list_more_info_bottom_sheet, null)

        val coverImage = dialogView.findViewById<ImageView>(R.id.coverImage)
        val albumTitle = dialogView.findViewById<TextView>(R.id.albumTitle)
        val albumSubTitle = dialogView.findViewById<TextView>(R.id.albumSubTitle)
        val mainContainer = dialogView.findViewById<LinearLayout>(R.id.main)

        val title = albumItem.albumTitle()
        val subtitle = albumItem.albumSubTitle()
        val coverUrl = albumItem.albumCover ?: ""

        albumTitle?.text = title
        albumSubTitle?.text = if (subtitle.isBlank()) (if (albumItem.type == "playlist") "Playlist" else "Album") else subtitle

        if (!coverUrl.isBlank() && coverUrl != "<shimmer>" && coverImage != null) {
            val cleanUrl = if (coverUrl.startsWith("http:")) coverUrl.replace("http:", "https:") else coverUrl
            Glide.with(coverImage.context)
                .load(cleanUrl.toUri())
                .placeholder(R.drawable.baseline_album_24)
                .error(R.drawable.baseline_album_24)
                .into(coverImage)
        }

        // Clear default extra items
        while (mainContainer.childCount > 2) {
            mainContainer.removeViewAt(mainContainer.childCount - 1)
        }

        // 1. Open Album / Playlist
        val openView = BottomSheetItemView(context, "Open " + if (albumItem.type == "playlist") "Playlist" else "Album", "", null)
        openView.iconImageView?.setImageResource(R.drawable.play_arrow_24px)
        openView.setOnClickListener {
            bottomSheetDialog.dismiss()
            val intent = Intent(context, ListActivity::class.java).apply {
                putExtra("data", Gson().toJson(albumItem))
                putExtra("id", albumItem.id)
                putExtra("type", if (albumItem.type == "playlist") "playlist" else "album")
            }
            context.startActivity(intent)
        }
        mainContainer.addView(openView)

        // 2. Add to / Remove from Library
        val prefs = SharedPreferenceManager.getInstance(context)
        val saved = prefs.savedLibrariesData
        val isSaved = saved?.lists?.any { it?.id == albumItem.id } == true
        val libTitle = if (isSaved) "Remove from Library" else "Add to Library"
        val libIcon = if (isSaved) R.drawable.round_close_24 else R.drawable.round_add_24
        val libraryView = BottomSheetItemView(context, libTitle, "", null)
        libraryView.iconImageView?.setImageResource(libIcon)
        libraryView.setOnClickListener {
            bottomSheetDialog.dismiss()
            if (!albumItem.id.isNullOrEmpty()) {
                val list = saved?.lists
                if (list != null) {
                    val index = list.indexOfFirst { it?.id == albumItem.id }
                    if (index != -1) {
                        prefs.removeLibraryFromSavedLibraries(index)
                        Toast.makeText(context, "Removed from Library", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                val isAlbum = albumItem.type != "playlist"
                val library = SavedLibraries.Library(
                    albumItem.id,
                    false,
                    isAlbum,
                    title,
                    coverUrl,
                    subtitle,
                    ArrayList()
                )
                prefs.addLibraryToSavedLibraries(library)
                Toast.makeText(context, "Added to Library", Toast.LENGTH_SHORT).show()
            }
        }
        mainContainer.addView(libraryView)

        // 3. Share
        val shareView = BottomSheetItemView(context, "Share", "", null)
        shareView.iconImageView?.setImageResource(R.drawable.share_24px)
        shareView.setOnClickListener {
            bottomSheetDialog.dismiss()
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Check out $title on Melodify!\nhttps://saavn.me/${albumItem.type}/${albumItem.id}")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share via"))
        }
        mainContainer.addView(shareView)

        bottomSheetDialog.setContentView(dialogView)

        bottomSheetDialog.setOnShowListener {
            animateMenuItems(mainContainer)
        }

        bottomSheetDialog.show()
    }

    /**
     * Shows an animated bottom sheet quick menu for Home Page Header 3-Dots Button.
     * Animates and slides in from left to right.
     */
    fun showAnimatedHomeMenu(context: Context) {
        val bottomSheetDialog = BottomSheetDialog(context, R.style.LeftDrawerBottomSheetDialogTheme)
        bottomSheetDialog.window?.setWindowAnimations(R.style.LeftToRightMenuAnimation)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.main_drawer_layout, null)

        val mainContainer = dialogView as? ViewGroup ?: return

        // Click actions
        dialogView.findViewById<View>(R.id.library)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            context.startActivity(Intent(context, SavedLibrariesActivity::class.java))
        }

        dialogView.findViewById<View>(R.id.favorites)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            context.startActivity(Intent(context, FavoritesActivity::class.java))
        }

        dialogView.findViewById<View>(R.id.history)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            context.startActivity(Intent(context, HistoryActivity::class.java))
        }

        dialogView.findViewById<View>(R.id.download_manager)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            context.startActivity(Intent(context, DownloadManagerActivity::class.java))
        }

        dialogView.findViewById<View>(R.id.settings)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }

        dialogView.findViewById<View>(R.id.updates)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            UpdateManager.checkForUpdates(context, isManualCheck = true)
        }

        dialogView.findViewById<View>(R.id.about)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            context.startActivity(Intent(context, AboutActivity::class.java))
        }

        bottomSheetDialog.setContentView(dialogView)

        bottomSheetDialog.setOnShowListener {
            animateMenuItems(mainContainer)
        }

        bottomSheetDialog.show()
    }
}
