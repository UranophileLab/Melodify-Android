package dev.melodify.uranophilelab.activities

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.adapters.ActivityListSongsItemAdapter
import dev.melodify.uranophilelab.adapters.UserCreatedSongsListAdapter
import dev.melodify.uranophilelab.databinding.ActivityListBinding
import dev.melodify.uranophilelab.databinding.ActivityListMoreInfoBottomSheetBinding
import dev.melodify.uranophilelab.databinding.UserCreatedListActivityMoreBottomSheetBinding
import dev.melodify.uranophilelab.model.AlbumItem
import dev.melodify.uranophilelab.model.BasicDataRecord
import dev.melodify.uranophilelab.model.history.AlbumHistoryItem
import dev.melodify.uranophilelab.network.ApiManager
import dev.melodify.uranophilelab.network.utility.RequestNetwork
import dev.melodify.uranophilelab.records.AlbumSearch
import dev.melodify.uranophilelab.records.PlaylistSearch
import dev.melodify.uranophilelab.records.SongResponse
import dev.melodify.uranophilelab.records.SongResponse.Song
import dev.melodify.uranophilelab.records.sharedpref.SavedLibraries
import dev.melodify.uranophilelab.records.sharedpref.SavedLibraries.Library
import dev.melodify.uranophilelab.utils.MiniPlayerHelper
import dev.melodify.uranophilelab.utils.MusicPlayerManager
import dev.melodify.uranophilelab.utils.SharedPreferenceManager
import dev.melodify.uranophilelab.utils.attachSnapHelper
import dev.melodify.uranophilelab.utils.customview.BottomSheetItemView
import com.bumptech.glide.Glide
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.palette.graphics.Palette
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dev.melodify.uranophilelab.utils.TrackDownloader

class ListActivity : AppCompatActivity() {
    var binding: ActivityListBinding? = null

    private val trackQueue: MutableList<String?> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())
        MiniPlayerHelper.initMiniPlayer(this)

        binding!!.recyclerView.setLayoutManager(LinearLayoutManager(this))
        binding!!.recyclerView.attachSnapHelper()
        binding?.addMoreSongs?.visibility = View.GONE

        Log.i("ListActivity", "onCreate: reached ListActivity")

        showShimmerData()

        val playAction = View.OnClickListener {
            val validQueue = trackQueue.filterNotNull().filter { it.isNotBlank() && it != "<shimmer>" }
            if (validQueue.isEmpty()) {
                Toast.makeText(this@ListActivity, "Loading songs...", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            val player = MusicPlayerManager.player
            val currentMusicId = MusicPlayerManager.MUSIC_ID
            val isAlbumPlaying = !currentMusicId.isNullOrBlank() && validQueue.contains(currentMusicId)

            if (isAlbumPlaying && player != null) {
                MusicPlayerManager.togglePlayPause()
                updatePlayerControlsState()
            } else {
                MusicPlayerManager.trackQueue = ArrayList(validQueue)
                MusicPlayerManager.track_position = 0
                Log.i(TAG, "trackQueueSet: ${MusicPlayerManager.trackQueue}")
                startActivity(
                    Intent(this@ListActivity, MusicOverviewActivity::class.java).putExtra(
                        "id",
                        validQueue[0]
                    )
                )
            }
        }

        binding?.playAllBtn?.setOnClickListener(playAction)
        binding?.playAllCard?.setOnClickListener(playAction)

        binding?.shuffleBtn?.setOnClickListener {
            val validQueue = trackQueue.filterNotNull().filter { it.isNotBlank() && it != "<shimmer>" }
            if (validQueue.isEmpty()) {
                Toast.makeText(this@ListActivity, "Loading songs...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val player = MusicPlayerManager.player
            if (player != null) {
                val newShuffleState = !player.shuffleModeEnabled
                player.shuffleModeEnabled = newShuffleState
                if (newShuffleState) {
                    val shuffledQueue = ArrayList(validQueue).apply { shuffle() }
                    MusicPlayerManager.trackQueue = shuffledQueue
                    MusicPlayerManager.track_position = 0
                    if (player.isPlaying) {
                        Toast.makeText(this@ListActivity, "Shuffle enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        startActivity(
                            Intent(this@ListActivity, MusicOverviewActivity::class.java).putExtra(
                                "id",
                                shuffledQueue[0]
                            )
                        )
                    }
                } else {
                    MusicPlayerManager.trackQueue = ArrayList(validQueue)
                    Toast.makeText(this@ListActivity, "Shuffle disabled", Toast.LENGTH_SHORT).show()
                }
                updatePlayerControlsState()
            } else {
                val shuffledQueue = ArrayList(validQueue).apply { shuffle() }
                MusicPlayerManager.trackQueue = shuffledQueue
                MusicPlayerManager.track_position = 0
                startActivity(
                    Intent(this@ListActivity, MusicOverviewActivity::class.java).putExtra(
                        "id",
                        shuffledQueue[0]
                    )
                )
            }
        }

        val shareAction = View.OnClickListener {
            val title = binding?.albumTitle?.text?.toString() ?: ""
            val subTitle = binding?.albumSubTitle?.text?.toString() ?: ""
            val shareText = "Check out $title ($subTitle) on Melodify!"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "Share"))
        }
        binding?.shareBtn?.setOnClickListener(shareAction)
        binding?.shareIcon?.setOnClickListener(shareAction)

        binding?.downloadBtn?.setOnClickListener {
            val validQueue = trackQueue.filterNotNull().filter { it.isNotBlank() && it != "<shimmer>" }
            if (validQueue.isEmpty()) {
                Toast.makeText(this@ListActivity, "Loading songs...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this@ListActivity, "Starting download for ${validQueue.size} tracks...", Toast.LENGTH_SHORT).show()
            val idsString = validQueue.joinToString(",")
            ApiManager(this@ListActivity).retrieveSongsByIds(idsString, object : RequestNetwork.RequestListener {
                override fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String?, Any?>?) {
                    try {
                        val songResponse = Gson().fromJson(response, SongResponse::class.java)
                        if (songResponse.success && !songResponse.data.isNullOrEmpty()) {
                            var downloadedCount = 0
                            val totalCount = songResponse.data.size
                            for (song in songResponse.data) {
                                if (song != null) {
                                    TrackDownloader.downloadAndEmbedMetadata(
                                        this@ListActivity, song, object : TrackDownloader.TrackDownloadListener {
                                            override fun onStarted() {}
                                            override fun onFinished() {
                                                downloadedCount++
                                                if (downloadedCount == totalCount) {
                                                    Toast.makeText(this@ListActivity, "Downloaded $totalCount tracks!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            override fun onError(errorMessage: String?) {}
                                        }
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Download error", e)
                    }
                }
                override fun onErrorResponse(tag: String?, message: String?) {}
            })
        }
        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.getInstance(this@ListActivity)

        binding!!.addToLibrary.setOnClickListener(View.OnClickListener {
            if (albumItem == null) return@OnClickListener
            if (isAlbumInLibrary(albumItem!!, sharedPreferenceManager.savedLibrariesData)) {
                MaterialAlertDialogBuilder(this@ListActivity)
                    .setTitle("Are you sure?")
                    .setMessage("Do you want to remove this album from your library?")
                    .setPositiveButton(
                        "Yes",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            val index = getAlbumIndexInLibrary(
                                albumItem!!,
                                sharedPreferenceManager.savedLibrariesData
                            )
                            if (index == -1) return@OnClickListener
                            sharedPreferenceManager.removeLibraryFromSavedLibraries(index)
                            Snackbar.make(
                                binding!!.getRoot(),
                                "Removed from Library",
                                Snackbar.LENGTH_SHORT
                            ).show()
                            updateAlbumInLibraryStatus()
                            finish()
                        })
                    .setNegativeButton(
                        "No"
                    ) { _: DialogInterface?, _: Int -> }
                    .show()
            } else {
                val library = Library(
                    albumItem!!.id,
                    false,
                    isAlbum,
                    binding!!.albumTitle.text.toString(),
                    albumItem!!.albumCover,
                    binding!!.albumSubTitle.text.toString(),
                    ArrayList()
                )
                sharedPreferenceManager.addLibraryToSavedLibraries(library)
                Snackbar.make(binding!!.getRoot(), "Added to Library", Snackbar.LENGTH_SHORT).show()
            }
            updateAlbumInLibraryStatus()
        })

        binding?.addMoreSongs?.setOnClickListener {
            startActivity(Intent(this@ListActivity, SearchActivity::class.java))
        }

        binding!!.moreIcon.setOnClickListener { onMoreIconClicked() }

        showData()
    }

    fun updatePlayerControlsState() {
        val b = binding ?: return
        val player = MusicPlayerManager.player
        val isPlaying = player != null && player.isPlaying
        val currentMusicId = MusicPlayerManager.MUSIC_ID
        val isAlbumPlaying = !currentMusicId.isNullOrBlank() && trackQueue.contains(currentMusicId)

        val btn = b.playAllBtn
        if (btn is ImageView) {
            btn.setImageResource(if (isAlbumPlaying && isPlaying) R.drawable.baseline_pause_24 else R.drawable.play_arrow_24px)
        } else if (btn is Button) {
            btn.text = if (isAlbumPlaying && isPlaying) "Pause" else "Play"
        }

        val isShuffle = player?.shuffleModeEnabled ?: false
        val tintColor = if (isShuffle) Color.parseColor("#1DB954") else Color.WHITE
        b.shuffleBtn?.imageTintList = ColorStateList.valueOf(tintColor)
    }

    override fun onResume() {
        super.onResume()
        if (intent.extras != null && intent.extras!!
                .getBoolean("createdByUser", false)
        ) {
            onUserCreatedFetch()
        }
        updatePlayerControlsState()
        binding?.recyclerView?.adapter?.notifyDataSetChanged()
        MiniPlayerHelper.onActivityResume(this)
    }

    override fun onPause() {
        super.onPause()
        MiniPlayerHelper.onActivityPause(this)
    }

    private fun onMoreIconClicked() {
        if (albumItem == null) return

        if (isUserCreated) {
            onMoreIconClickedUserCreated()
            return
        }

        val bottomSheetDialog =
            BottomSheetDialog(this@ListActivity, R.style.MyBottomSheetDialogTheme)
        val _binding = ActivityListMoreInfoBottomSheetBinding.inflate(layoutInflater)

        _binding.albumTitle.text = binding!!.albumTitle.text.toString()
        _binding.albumSubTitle.text = binding!!.albumSubTitle.text.toString()
        Glide.with(_binding.coverImage.context).load(albumItem!!.albumCover?.toUri()).into(_binding.coverImage)

        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.getInstance(this@ListActivity)
        val savedLibraries: SavedLibraries? = sharedPreferenceManager.savedLibrariesData
        if (savedLibraries == null || savedLibraries.lists == null) {
            _binding.addToLibrary.titleTextView?.text = "Add to library"
            _binding.addToLibrary.iconImageView?.setImageResource(R.drawable.round_add_24)
        } else {
            if (isAlbumInLibrary(albumItem!!, savedLibraries)) {
                _binding.addToLibrary.titleTextView?.text = "Remove from library"
                _binding.addToLibrary.iconImageView?.setImageResource(R.drawable.round_close_24)
            } else {
                _binding.addToLibrary.titleTextView?.text = "Add to library"
                _binding.addToLibrary.iconImageView?.setImageResource(R.drawable.round_add_24)
            }
        }
        _binding.addToLibrary.setOnClickListener {
            bottomSheetDialog.dismiss()
            binding!!.addToLibrary.performClick()
        }

        for (artist in artistData) {
            try {
                val imgUrl = artist.image ?: ""
                val bottomSheetItemView =
                    BottomSheetItemView(this@ListActivity, artist.name, imgUrl, artist.id)
                bottomSheetItemView.setFocusable(true)
                bottomSheetItemView.isClickable = true
                bottomSheetItemView.setOnClickListener {
                    Log.i("ListActivity", "BottomSheetItemView: onCLicked!")
                    startActivity(
                        Intent(this@ListActivity, ArtistProfileActivity::class.java)
                            .putExtra(
                                "data", Gson().toJson(
                                    BasicDataRecord(artist.id, artist.name, "", imgUrl)
                                )
                            )
                    )
                }
                _binding.main.addView(bottomSheetItemView)
            } catch (e: Exception) {
                Log.e("ListActivity", "BottomSheetDialog: ", e)
            }
        }

        bottomSheetDialog.setContentView(_binding.getRoot())
        bottomSheetDialog.create()
        bottomSheetDialog.show()
    }

    private fun onMoreIconClickedUserCreated() {
        val bottomSheetDialog =
            BottomSheetDialog(this@ListActivity, R.style.MyBottomSheetDialogTheme)
        val _binding = UserCreatedListActivityMoreBottomSheetBinding.inflate(layoutInflater)

        _binding.albumTitle.text = binding!!.albumTitle.text.toString()
        _binding.albumSubTitle.text = binding!!.albumSubTitle.text.toString()
        Glide.with(_binding.coverImage.context).load(albumItem!!.albumCover?.toUri()).into(_binding.coverImage)

        _binding.removeLibrary.setOnClickListener {
            bottomSheetDialog.dismiss()
            binding!!.addToLibrary.performClick()
        }

        bottomSheetDialog.setContentView(_binding.getRoot())
        bottomSheetDialog.create()
        bottomSheetDialog.show()
    }

    private fun updateAlbumInLibraryStatus() {
        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.getInstance(this@ListActivity)
        if (sharedPreferenceManager.savedLibrariesData == null) binding!!.addToLibrary.setImageResource(
            R.drawable.round_add_24
        )
        else {
            val savedLibraries = sharedPreferenceManager.savedLibrariesData
            binding!!.addToLibrary.setImageResource(
                if (savedLibraries != null && isAlbumInLibrary(
                        albumItem!!,
                        savedLibraries
                    )
                ) R.drawable.round_done_24 else R.drawable.round_add_24
            )
        }
    }

    @SuppressLint("NewApi")
    private fun isAlbumInLibrary(albumItem: AlbumItem, savedLibraries: SavedLibraries?): Boolean {
        if (savedLibraries == null || savedLibraries.lists == null) {
            return false
        }
        Log.i("ListActivity", "isAlbumInLibrary: $savedLibraries")
        if (savedLibraries.lists.isEmpty()) return false
        val lists = savedLibraries.lists
        return lists.stream()
            .anyMatch { library: Library? -> library?.id == albumItem.id }
    }

    @SuppressLint("NewApi")
    private fun getAlbumIndexInLibrary(albumItem: AlbumItem, savedLibraries: SavedLibraries?): Int {
        if (savedLibraries == null || savedLibraries.lists == null) {
            return -1
        }
        Log.i("ListActivity", "getAlbumIndexInLibrary: $savedLibraries")
        if (savedLibraries.lists.isEmpty()) return -1
        var index = -1
        val lists = savedLibraries.lists
        for (library in lists) {
            if (library?.id == albumItem.id) {
                index = lists.indexOf(library)
                break
            }
        }
        return index
    }

    private fun showShimmerData() {
        val data: MutableList<Song?> = ArrayList()
        for (i in 0..10) {
            data.add(
                Song(
                    "<shimmer>",
                    "",
                    "",
                    "",
                    "",
                    0.0,
                    "",
                    false,
                    0,
                    "",
                    false,
                    "",
                    null,
                    "",
                    "",
                    null,
                    null, null, null
                )
            )
        }
        binding!!.recyclerView.setAdapter(
            ActivityListSongsItemAdapter(
                data.filterNotNull().toMutableList()
            )
        )
    }

    private var albumItem: AlbumItem? = null
    private var isAlbum = false

    private var currentLoadedImageUrl: String? = null

    private fun loadArtworkAndApplyDynamicTheme(imageUrl: String?) {
        val b = binding ?: return
        if (imageUrl.isNullOrBlank()) return
        if (imageUrl == currentLoadedImageUrl) return
        currentLoadedImageUrl = imageUrl

        Glide.with(this)
            .asBitmap()
            .load(imageUrl.toUri())
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    if (isFinishing || isDestroyed || binding == null) return
                    b.albumCover.setImageBitmap(resource)

                    Palette.from(resource).generate { palette ->
                        if (isFinishing || isDestroyed || binding == null || palette == null) return@generate

                        val vibrant = palette.getVibrantColor(0)
                        val darkVibrant = palette.getDarkVibrantColor(0)
                        val muted = palette.getMutedColor(0)
                        val darkMuted = palette.getDarkMutedColor(0)
                        val dominant = palette.getDominantColor(0)

                        val primaryColor = when {
                            vibrant != 0 -> vibrant
                            darkVibrant != 0 -> darkVibrant
                            muted != 0 -> muted
                            darkMuted != 0 -> darkMuted
                            dominant != 0 -> dominant
                            else -> 0xFF2A150D.toInt()
                        }

                        val topColor = adjustColorDarkness(primaryColor, 0.65f)
                        val centerColor = adjustColorDarkness(primaryColor, 0.20f)
                        val bottomColor = Color.parseColor("#121212")

                        val dynamicGradient = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(topColor, centerColor, bottomColor)
                        )

                        val parentGroup = b.root as? ViewGroup
                        if (parentGroup != null) {
                            val changeBounds = ChangeBounds().apply { duration = 400 }
                            TransitionManager.beginDelayedTransition(parentGroup, changeBounds)
                        }
                        b.main.background = dynamicGradient
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun adjustColorDarkness(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    private fun showData() {
        if (intent.extras == null) return
        albumItem = Gson().fromJson(
            intent.extras!!.getString("data"),
            AlbumItem::class.java
        )
        updateAlbumInLibraryStatus()
        if (albumItem != null) {
            binding!!.albumTitle.text = albumItem!!.albumTitle()
            binding!!.albumSubTitle.text = albumItem!!.albumSubTitle()
            if (albumItem!!.albumCover?.isNotBlank() == true) loadArtworkAndApplyDynamicTheme(albumItem!!.albumCover)
        }

        val apiManager = ApiManager(this)
        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.getInstance(this)
        val intentId = intent.extras!!.getString("id", "")

        if (intent.extras!!.getBoolean("createdByUser", false)) {
            onUserCreatedFetch()
            return
        }

        val checkIfUrlData =
            (intentId.startsWith("http") || intentId.startsWith("www")) && intentId.contains("jiosaavn.com")
        if (intent.extras!!.getString("type", "") == "album") {
            isAlbum = true
            if (albumItem != null) {
                val cached = sharedPreferenceManager.getAlbumResponseById(albumItem!!.id)
                if (cached != null) {
                    onAlbumFetched(cached)
                    return
                }
            }
            val requestListener: RequestNetwork.RequestListener =
                object : RequestNetwork.RequestListener {
                    override fun onResponse(
                        tag: String?,
                        response: String?,
                        responseHeaders: HashMap<String?, Any?>?
                    ) {
                        val albumSearch =
                            Gson().fromJson(response, AlbumSearch::class.java)
                        Log.i("ListActivity", "onResponse: $albumSearch")
                        if (albumSearch.success) {
                            val data = albumSearch.data ?: return
                            sharedPreferenceManager.setAlbumResponseById(
                                data.id ?: "",
                                albumSearch
                            )
                            onAlbumFetched(albumSearch)
                        }
                    }

                    override fun onErrorResponse(tag: String?, message: String?) {
                        Log.e("ListActivity", "onErrorResponse: $message")
                        Toast.makeText(
                            this@ListActivity,
                            "Failed to fetch Album",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            if (checkIfUrlData) {
                apiManager.retrieveAlbumByLink(intentId, requestListener)
            } else {
                apiManager.retrieveAlbumById(albumItem!!.id ?: "", requestListener)
            }
            return
        }

        val responseListener: RequestNetwork.RequestListener =
            object : RequestNetwork.RequestListener {
                override fun onResponse(
                    tag: String?,
                    response: String?,
                    responseHeaders: HashMap<String?, Any?>?
                ) {
                    Log.i("API_RESPONSE", "onResponse: $response")
                    val playlistSearch =
                        Gson().fromJson(response, PlaylistSearch::class.java)
                    if (playlistSearch.success) {
                        val data = playlistSearch.data ?: return
                        sharedPreferenceManager.setPlaylistResponseById(
                            data.id ?: "",
                            playlistSearch
                        )
                        onPlaylistFetched(playlistSearch)
                    }
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                }
            }
        if (checkIfUrlData) {
            apiManager.retrievePlaylistByLink(intentId, null, null, responseListener)
        } else {
            val cached = sharedPreferenceManager.getPlaylistResponseById(albumItem!!.id ?: "")
            if (cached != null && !cached.data?.songs.isNullOrEmpty()) {
                onPlaylistFetched(cached)
            }
            apiManager.retrievePlaylistById(albumItem!!.id ?: "", null, null, responseListener)
        }
    }

    private var isUserCreated = false

    private fun onUserCreatedFetch() {
        isUserCreated = true

        binding!!.shareIcon.visibility = View.INVISIBLE
        // binding.moreIcon.setVisibility(View.INVISIBLE);
        binding!!.addToLibrary.visibility = View.INVISIBLE
        binding?.addMoreSongs?.visibility = View.VISIBLE

        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.getInstance(this)
        val savedLibraries = sharedPreferenceManager.savedLibrariesData
        if (savedLibraries == null || savedLibraries.lists.isNullOrEmpty()) finish()
        var library: Library? = null
        if (savedLibraries != null) for (l in savedLibraries.lists ?: emptyList()) {
            if (l?.id == albumItem!!.id) {
                library = l
                break
            }
        }
        if (library == null) finish()
        if (library != null) {
            binding!!.albumTitle.text = library.name
            binding!!.albumSubTitle.text = library.description
            if (library.image?.isNotBlank() == true) loadArtworkAndApplyDynamicTheme(library.image)

            val songs = library.songs ?: mutableListOf()
            binding!!.recyclerView.setAdapter(
                UserCreatedSongsListAdapter(
                    songs.filterNotNull().toMutableList()
                )
            )
            trackQueue.clear()
            for (song in songs) {
                if (song != null && !song.id.isNullOrBlank()) trackQueue.add(song.id)
            }
        }
    }

    private fun onAlbumFetched(albumSearch: AlbumSearch) {
        val data = albumSearch.data ?: return

        binding!!.albumTitle.text = data.name()
        binding!!.albumSubTitle.text = data.description()
        val imageList = data.image
        val coverUrl = if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else albumItem?.albumCover ?: ""
        if (coverUrl.isNotEmpty()) {
            loadArtworkAndApplyDynamicTheme(coverUrl)
        }
        SharedPreferenceManager.getInstance(this).addAlbumToHistory(
            AlbumHistoryItem(
                id = data.id ?: albumItem?.id,
                title = data.name(),
                subtitle = data.description(),
                imageUrl = coverUrl
            )
        )
        val songs = data.songs ?: mutableListOf()
        binding!!.recyclerView.setAdapter(
            ActivityListSongsItemAdapter(
                songs.filterNotNull().toMutableList()
            )
        )
        trackQueue.clear()
        for (song in songs) {
            if (song != null && !song.id.isNullOrBlank()) trackQueue.add(song.id)
        }

        // ((ApplicationClass)getApplicationContext()).setTrackQueue(trackQueue);
        binding!!.shareIcon.setOnClickListener(View.OnClickListener {
            if (data.url.isNullOrBlank()) return@OnClickListener
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, data.url)
            sendIntent.type = "text/plain"
            startActivity(sendIntent)
        })

        for (artist in data.artist?.all ?: emptyList()) {
            if (artist == null) continue
            val aImgList = artist.image
            artistData.add(
                ArtistData(
                    artist.name(), artist.id,
                    if (!aImgList.isNullOrEmpty())
                        aImgList[aImgList.size - 1]?.url ?: ""
                    else
                        ""
                )
            )
        }
    }

    private fun onPlaylistFetched(playlistSearch: PlaylistSearch) {
        val data = playlistSearch.data ?: return

        binding!!.albumTitle.text = data.name()
        binding!!.albumSubTitle.text = data.description()
        val imageList = data.image
        val coverUrl = if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else albumItem?.albumCover ?: ""
        if (coverUrl.isNotEmpty()) {
            loadArtworkAndApplyDynamicTheme(coverUrl)
        }
        val songs = data.songs ?: mutableListOf()
        binding!!.recyclerView.setAdapter(
            ActivityListSongsItemAdapter(
                songs.filterNotNull().toMutableList()
            )
        )
        trackQueue.clear()
        for (song in songs) {
            if (song != null && !song.id.isNullOrBlank()) trackQueue.add(song.id)
        }

        // ((ApplicationClass)getApplicationContext()).setTrackQueue(trackQueue);
        binding!!.shareIcon.setOnClickListener(View.OnClickListener {
            if (data.url.isNullOrBlank()) return@OnClickListener
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, data.url)
            sendIntent.type = "text/plain"
            startActivity(sendIntent)
        })

        for (artist in data.artists ?: emptyList()) {
            if (artist == null) continue
            val aImgList = artist.image
            artistData.add(
                ArtistData(
                    artist.name(), artist.id,
                    if (!aImgList.isNullOrEmpty())
                        aImgList[aImgList.size - 1]?.url
                            ?: "https://i.pinimg.com/564x/1d/04/a8/1d04a87b8e6cf2c3829c7af2eccf6813.jpg"
                    else
                        "https://i.pinimg.com/564x/1d/04/a8/1d04a87b8e6cf2c3829c7af2eccf6813.jpg"
                )
            )
        }
    }

    private val artistData: MutableList<ArtistData> = ArrayList<ArtistData>()

    fun backPress(view: View?) {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }


    internal data class ArtistData(
        val name: String?,
        val id: String?,
        val image: String?
    )

    companion object {
        private const val TAG = "ListActivity"
    }
}
