package dev.melodify.uranophilelab.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dev.melodify.uranophilelab.BaseApplicationClass
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.adapters.ActivityListSongsItemAdapter
import dev.melodify.uranophilelab.adapters.ActivityMainAlbumItemAdapter
import dev.melodify.uranophilelab.adapters.ActivityMainArtistsItemAdapter
import dev.melodify.uranophilelab.adapters.ActivityMainPlaylistAdapter
import dev.melodify.uranophilelab.adapters.ActivityMainPopularSongs
import dev.melodify.uranophilelab.adapters.ActivityMainTrendingAdapter
import dev.melodify.uranophilelab.adapters.SavedLibrariesAdapter
import dev.melodify.uranophilelab.databinding.ActivityMainBinding
import dev.melodify.uranophilelab.databinding.RecommendationsBottomSheetBinding
import dev.melodify.uranophilelab.model.AlbumItem
import dev.melodify.uranophilelab.network.ApiManager
import dev.melodify.uranophilelab.network.NetworkChangeReceiver
import dev.melodify.uranophilelab.network.NetworkChangeReceiver.NetworkStatusListener
import dev.melodify.uranophilelab.network.utility.RequestNetwork
import dev.melodify.uranophilelab.records.AlbumsSearch
import dev.melodify.uranophilelab.records.ArtistsSearch
import dev.melodify.uranophilelab.records.PlaylistsSearch
import dev.melodify.uranophilelab.records.SongResponse
import dev.melodify.uranophilelab.records.SongSearch
import dev.melodify.uranophilelab.utils.AnimatedMenuHelper
import dev.melodify.uranophilelab.utils.MiniPlayerHelper
import dev.melodify.uranophilelab.utils.MusicPlayerManager
import dev.melodify.uranophilelab.utils.RecommendationManager
import dev.melodify.uranophilelab.utils.SharedPreferenceManager
import dev.melodify.uranophilelab.utils.UpdateManager
import dev.melodify.uranophilelab.utils.attachSnapHelper
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private var requestStoragePermission: ActivityResultLauncher<Array<String>>? = null
    private val TAG = "MainActivity"
    private var binding: ActivityMainBinding? = null
    private var baseApplicationClass: BaseApplicationClass? = null
    val songs: MutableList<AlbumItem?> = ArrayList<AlbumItem?>()
    val newReleaseSongs: MutableList<AlbumItem?> = ArrayList<AlbumItem?>()
    val artists: MutableList<ArtistsSearch.Data.Results?> = ArrayList<ArtistsSearch.Data.Results?>()
    val albums: MutableList<AlbumItem?> = ArrayList<AlbumItem?>()
    val playlists: MutableList<AlbumItem?> = ArrayList<AlbumItem?>()
    val recommendedSongs: MutableList<AlbumItem?> = ArrayList<AlbumItem?>()
    private var recommendationTitle: String = "Recommended For You"
    private var recommendationSubtitle: String = "Tailored track recommendations"

    var networkChangeReceiver: NetworkChangeReceiver =
        NetworkChangeReceiver(object : NetworkStatusListener {
            override fun onNetworkConnected() {
                if (songs.isEmpty() || newReleaseSongs.isEmpty() || artists.isEmpty() || albums.isEmpty() || playlists.isEmpty()) showData()
            }

            override fun onNetworkDisconnected() {
                if (songs.isEmpty() || newReleaseSongs.isEmpty() || artists.isEmpty() || albums.isEmpty() || playlists.isEmpty()) showOfflineData()
                val currentBinding = binding ?: return
                Snackbar.make(currentBinding.getRoot(), "No Internet Connection", Snackbar.LENGTH_LONG)
                    .show()
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflatedBinding = ActivityMainBinding.inflate(layoutInflater)
        binding = inflatedBinding
        setContentView(inflatedBinding.getRoot())

        baseApplicationClass = applicationContext as BaseApplicationClass?
        BaseApplicationClass.updateTheme(this)

        val initialToolbarPaddingTop = inflatedBinding.toolbar.paddingTop
        val initialBottomNavPaddingBottom = inflatedBinding.bottomNavigation?.paddingBottom ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(inflatedBinding.main) { _, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            inflatedBinding.toolbar.setPadding(
                inflatedBinding.toolbar.paddingLeft,
                initialToolbarPaddingTop + statusBarInset,
                inflatedBinding.toolbar.paddingRight,
                inflatedBinding.toolbar.paddingBottom
            )

            inflatedBinding.bottomNavigation?.let { bottomNav ->
                bottomNav.setPadding(
                    bottomNav.paddingLeft,
                    bottomNav.paddingTop,
                    bottomNav.paddingRight,
                    initialBottomNavPaddingBottom + navBarInset
                )
            }

            insets
        }

        setupGreeting()
        setupBottomNavigation()

        inflatedBinding.historyButton?.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        inflatedBinding.settingsButton?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        inflatedBinding.moreButton?.setOnClickListener {
            AnimatedMenuHelper.showAnimatedHomeMenu(this)
        }

        val openRecommendationsSheet = { showRecommendationsBottomSheet() }
        inflatedBinding.recommendationsSeeAll?.setOnClickListener { openRecommendationsSheet() }
        inflatedBinding.recommendationsTitle?.setOnClickListener { openRecommendationsSheet() }

        val span: Int = calculateNoOfColumns(this, 200f)
        inflatedBinding.playlistRecyclerView.layoutManager = GridLayoutManager(this, span)
        inflatedBinding.trendingRecyclerView?.layoutManager = GridLayoutManager(this, 2)

        inflatedBinding.recommendationsRecyclerView?.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        inflatedBinding.newReleasesRecyclerView?.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        inflatedBinding.popularSongsRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        inflatedBinding.popularAlbumsRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        inflatedBinding.savedRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Snap helpers — smooth magnetic scroll on all horizontal lists
        inflatedBinding.recommendationsRecyclerView?.attachSnapHelper()
        inflatedBinding.newReleasesRecyclerView?.attachSnapHelper()
        inflatedBinding.popularSongsRecyclerView.attachSnapHelper()
        inflatedBinding.popularAlbumsRecyclerView.attachSnapHelper()
        inflatedBinding.savedRecyclerView.attachSnapHelper()

        inflatedBinding.recommendationsRecyclerView?.let {
            OverScrollDecoratorHelper.setUpOverScroll(it, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)
        }
        inflatedBinding.newReleasesRecyclerView?.let {
            OverScrollDecoratorHelper.setUpOverScroll(it, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)
        }
        OverScrollDecoratorHelper.setUpOverScroll(
            inflatedBinding.popularSongsRecyclerView,
            OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL
        )
        OverScrollDecoratorHelper.setUpOverScroll(
            inflatedBinding.popularAlbumsRecyclerView,
            OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL
        )
        OverScrollDecoratorHelper.setUpOverScroll(
            inflatedBinding.savedRecyclerView,
            OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL
        )

        inflatedBinding.refreshLayout.setOnRefreshListener {
            showShimmerData()
            showData()
            binding?.refreshLayout?.isRefreshing = false
        }

        MiniPlayerHelper.initMiniPlayer(this)

        UpdateManager.checkForUpdates(this)

        showShimmerData()
        showData()

        showSavedLibrariesData()

        askNotificationPermission()

        requestStoragePermission =
            registerForActivityResult(RequestMultiplePermissions()) { result ->
                if (result.containsValue(false)) {
                    Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
                }
            }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {
        if (!checkIfStorageAccessAvailable()) {
            requestStoragePermission?.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun checkIfStorageAccessAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            true
        } else {
            (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED)
                    && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED)
        }
    }

    private fun showSavedLibrariesData() {
        val currentBinding = binding ?: return
        val prefs = SharedPreferenceManager.getInstance(this)
        prefs.syncLikedSongsLibrary()
        val savedLibraries = prefs.savedLibrariesData
        val hasItems = savedLibraries?.lists?.any { it != null } == true
        currentBinding.savedLibrariesSection.visibility =
            if (hasItems) View.VISIBLE else View.GONE
        if (savedLibraries != null) {
            currentBinding.savedRecyclerView.adapter = SavedLibrariesAdapter(
                savedLibraries.lists ?: mutableListOf()
            )
        }
    }



    override fun onResume() {
        super.onResume()
        NetworkChangeReceiver.registerReceiver(this, networkChangeReceiver)
        showSavedLibrariesData()
        binding?.bottomNavigation?.selectedItemId = R.id.nav_home
        MiniPlayerHelper.onActivityResume(this)
    }

    override fun onPause() {
        super.onPause()
        NetworkChangeReceiver.unregisterReceiver(this, networkChangeReceiver)
        MiniPlayerHelper.onActivityPause(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    private fun showData() {
        songs.clear()
        newReleaseSongs.clear()
        artists.clear()
        albums.clear()
        playlists.clear()

        RecommendationManager.getHomePersonalizedRecommendations(this) { title, subtitle, items ->
            val currentBinding = binding ?: return@getHomePersonalizedRecommendations
            recommendedSongs.clear()
            recommendedSongs.addAll(items)
            recommendationTitle = title
            recommendationSubtitle = subtitle

            currentBinding.recommendationsTitle?.text = title
            currentBinding.recommendationsSubtitle?.text = subtitle

            val recAdapter = ActivityMainPopularSongs(recommendedSongs)
            currentBinding.recommendationsRecyclerView?.adapter = recAdapter
            recAdapter.notifyDataSetChanged()
        }

        val songSeeds = listOf("2023", "2024", "2025", "2026", "Hits", "Latest", "Romantic", "Chill", "Lo-Fi", "Dance", "Hindi", "Sad", "Love", "Workout")
        val newReleaseSeeds = listOf("New Releases", "Global New Releases", "Latest Global Hits", "Latest Releases", "New Pop Hits", "2025 New Releases", "Global New", "Latest Songs", "New Hits")
        val artistSeeds = listOf("Arijit", "Taylor", "Drake", "BTS", "Ed Sheeran", "Shreya", "Justin", "Badshah")
        val albumSeeds = listOf("2023", "2024", "2025", "2026", "Hits", "Latest", "New", "Sad", "Lo-Fi", "Romantic", "Rock", "Pop", "Classic", "Party")
        val playlistSeeds = listOf("2023", "2024", "2025", "2026", "Hits", "Latest", "Trending", "Party", "Devotional", "Chill", "Love", "Sad", "Top", "Classic")

        val songQuery = songSeeds.random()
        val newReleaseQuery = newReleaseSeeds.random()
        val artistQuery = artistSeeds.random()
        val albumQuery = albumSeeds.random()
        val playlistQuery = playlistSeeds.random()

        val apiManager = ApiManager(this)

        apiManager.searchSongs(newReleaseQuery, 0, 15, object : RequestNetwork.RequestListener {
            override fun onResponse(
                tag: String?,
                response: String?,
                responseHeaders: HashMap<String?, Any?>?
            ) {
                val currentBinding = binding ?: return
                try {
                    val songSearch = Gson().fromJson(response, SongSearch::class.java)
                    Log.i(TAG, "onResponse New Releases: $response")
                    if (songSearch?.success == true) {
                        val resultsList = songSearch.data?.results ?: emptyList()
                        for (results in resultsList) {
                            if (results == null) continue
                            val imageList = results.image
                            val imageUrl =
                                if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
                            newReleaseSongs.add(
                                AlbumItem(
                                    results.name(), results.language + " " + results.year,
                                    imageUrl, results.id, "song"
                                )
                            )
                        }
                        val adapter = ActivityMainPopularSongs(newReleaseSongs)
                        currentBinding.newReleasesRecyclerView?.adapter = adapter
                        adapter.notifyDataSetChanged()
                        updateTrendingGrid()
                        BaseApplicationClass.sharedPreferenceManager?.homeNewReleasesRecommended = songSearch
                    } else {
                        showOfflineData()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onResponse new release song parse error: ", e)
                    showOfflineData()
                }
            }

            override fun onErrorResponse(tag: String?, message: String?) {
                showOfflineData()
            }
        })

        apiManager.searchSongs(songQuery, 0, 15, object : RequestNetwork.RequestListener {
            override fun onResponse(
                tag: String?,
                response: String?,
                responseHeaders: HashMap<String?, Any?>?
            ) {
                val currentBinding = binding ?: return
                try {
                    val songSearch = Gson().fromJson(response, SongSearch::class.java)
                    Log.i(TAG, "onResponse: $response")
                    if (songSearch?.success == true) {
                        val resultsList = songSearch.data?.results ?: emptyList()
                        for (results in resultsList) {
                            if (results == null) continue
                            val imageList = results.image
                            val imageUrl =
                                if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
                            songs.add(
                                AlbumItem(
                                    results.name(), results.language + " " + results.year,
                                    imageUrl, results.id, "song"
                                )
                            )
                        }
                        val adapter = ActivityMainPopularSongs(songs)
                        currentBinding.popularSongsRecyclerView.adapter = adapter
                        adapter.notifyDataSetChanged()
                        updateTrendingGrid()
                        BaseApplicationClass.sharedPreferenceManager?.homeSongsRecommended = songSearch
                    } else {
                        showOfflineData()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onResponse song parse error: ", e)
                    showOfflineData()
                }
            }

            override fun onErrorResponse(tag: String?, message: String?) {
                showOfflineData()
            }
        })

        apiManager.searchArtists(artistQuery, 0, 15, object : RequestNetwork.RequestListener {
            override fun onResponse(
                tag: String?,
                response: String?,
                responseHeaders: HashMap<String?, Any?>?
            ) {
                val currentBinding = binding ?: return
                try {
                    val artistSearch = Gson().fromJson(response, ArtistsSearch::class.java)
                    Log.i(TAG, "onResponse: $response")
                    if (artistSearch?.success == true) {
                        val resultsList = artistSearch.data?.results ?: emptyList()
                        for (results in resultsList) {
                            if (results == null) continue
                            artists.add(results)
                        }
                        val adapter = ActivityMainArtistsItemAdapter(artists)
                        currentBinding.popularArtistsRecyclerView?.adapter = adapter
                        adapter.notifyDataSetChanged()
                        BaseApplicationClass.sharedPreferenceManager?.homeArtistsRecommended = artistSearch
                    } else {
                        showOfflineData()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onResponse artist parse error: ", e)
                    showOfflineData()
                }
            }

            override fun onErrorResponse(tag: String?, message: String?) {
                showOfflineData()
            }
        })

        apiManager.searchAlbums(albumQuery, 0, 15, object : RequestNetwork.RequestListener {
            override fun onResponse(
                tag: String?,
                response: String?,
                responseHeaders: HashMap<String?, Any?>?
            ) {
                val currentBinding = binding ?: return
                try {
                    val albumsSearch = Gson().fromJson(response, AlbumsSearch::class.java)
                    Log.i(TAG, "onResponse: $response")
                    if (albumsSearch?.success == true) {
                        val resultsList = albumsSearch.data?.results ?: emptyList()
                        for (results in resultsList) {
                            if (results == null) continue
                            val imageList = results.image
                            val imageUrl =
                                if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
                            albums.add(
                                AlbumItem(
                                    results.name(), results.language + " " + results.year,
                                    imageUrl, results.id, "album"
                                )
                            )
                        }
                        val adapter = ActivityMainAlbumItemAdapter(albums)
                        currentBinding.popularAlbumsRecyclerView.adapter = adapter
                        adapter.notifyDataSetChanged()
                        updateTrendingGrid()
                        BaseApplicationClass.sharedPreferenceManager?.homeAlbumsRecommended = albumsSearch
                    } else {
                        showOfflineData()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onResponse album parse error: ", e)
                    showOfflineData()
                }
            }

            override fun onErrorResponse(tag: String?, message: String?) {
                showOfflineData()
            }
        })

        apiManager.searchPlaylists(
            playlistQuery, null, null,
            object : RequestNetwork.RequestListener {
                override fun onResponse(
                    tag: String?,
                    response: String?,
                    responseHeaders: HashMap<String?, Any?>?
                ) {
                    val currentBinding = binding ?: return
                    try {
                        val playlistsSearch = Gson().fromJson(response, PlaylistsSearch::class.java)
                        Log.i(TAG, "onResponse: $response")
                        if (playlistsSearch?.success == true) {
                            val resultsList = playlistsSearch.data?.results ?: emptyList()
                            for (results in resultsList) {
                                if (results == null) continue
                                val imageList = results.image
                                val imageUrl =
                                    if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
                                playlists.add(
                                    AlbumItem(
                                        results.name(), "",
                                        imageUrl, results.id, "playlist"
                                    )
                                )
                            }
                            val adapter = ActivityMainPlaylistAdapter(playlists)
                            currentBinding.playlistRecyclerView.adapter = adapter
                            adapter.notifyDataSetChanged()
                            BaseApplicationClass.sharedPreferenceManager?.homePlaylistRecommended = playlistsSearch
                        } else {
                            showOfflineData()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "onResponse playlist parse error: ", e)
                        showOfflineData()
                    }
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                    showOfflineData()
                }
            })
    }

    private fun showShimmerData() {
        val currentBinding = binding ?: return
        val dataShimmer: MutableList<AlbumItem?> = ArrayList()
        val artistsShimmer: MutableList<ArtistsSearch.Data.Results?> = ArrayList()
        for (i in 0..10) {
            dataShimmer.add(AlbumItem("<shimmer>", "<shimmer>", "<shimmer>", "<shimmer>"))
            artistsShimmer.add(
                ArtistsSearch.Data.Results(
                    "<shimmer>",
                    "<shimmer>",
                    "<shimmer>",
                    "<shimmer>",
                    "<shimmer>",
                    null
                )
            )
        }
        currentBinding.recommendationsRecyclerView?.adapter = ActivityMainAlbumItemAdapter(dataShimmer)
        currentBinding.newReleasesRecyclerView?.adapter = ActivityMainAlbumItemAdapter(dataShimmer)
        currentBinding.popularSongsRecyclerView.adapter = ActivityMainAlbumItemAdapter(dataShimmer)
        currentBinding.popularAlbumsRecyclerView.adapter = ActivityMainAlbumItemAdapter(dataShimmer)
        currentBinding.popularArtistsRecyclerView?.adapter = ActivityMainArtistsItemAdapter(artistsShimmer)
        currentBinding.playlistRecyclerView.adapter = ActivityMainPlaylistAdapter(dataShimmer)
    }

    private fun showOfflineData() {
        val currentBinding = binding ?: return
        val prefManager = BaseApplicationClass.sharedPreferenceManager ?: return
        val newReleasesSearch: SongSearch? = prefManager.homeNewReleasesRecommended
        if (newReleasesSearch?.success == true) {
            val resultsList = newReleasesSearch.data?.results ?: emptyList()
            for (results in resultsList) {
                if (results == null) continue
                val imageList = results.image
                val imageUrl =
                    if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
                newReleaseSongs.add(
                    AlbumItem(
                        results.name(), results.language + " " + results.year,
                        imageUrl, results.id, "song"
                    )
                )
            }
            val adapter = ActivityMainPopularSongs(newReleaseSongs)
            currentBinding.newReleasesRecyclerView?.adapter = adapter
            adapter.notifyDataSetChanged()
        }

        val songSearch: SongSearch? = prefManager.homeSongsRecommended
        if (songSearch?.success == true) {
            val resultsList = songSearch.data?.results ?: emptyList()
            for (results in resultsList) {
                if (results == null) continue
                val imageList = results.image
                val imageUrl =
                    if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
                songs.add(
                    AlbumItem(
                        results.name(), results.language + " " + results.year,
                        imageUrl, results.id
                    )
                )
            }
            val adapter = ActivityMainPopularSongs(songs)
            currentBinding.popularSongsRecyclerView.adapter = adapter
            adapter.notifyDataSetChanged()
        }

        val artistsSearch: ArtistsSearch? = prefManager.homeArtistsRecommended
        if (artistsSearch?.success == true) {
            val resultsList = artistsSearch.data?.results ?: emptyList()
            for (results in resultsList) {
                if (results == null) continue
                artists.add(results)
            }
            val adapter = ActivityMainArtistsItemAdapter(artists)
            currentBinding.popularArtistsRecyclerView?.adapter = adapter
            adapter.notifyDataSetChanged()
        }

        val albumsSearch: AlbumsSearch? = prefManager.homeAlbumsRecommended
        if (albumsSearch?.success == true) {
            val resultsList = albumsSearch.data?.results ?: emptyList()
            for (results in resultsList) {
                if (results == null) continue
                val imageList = results.image
                val imageUrl =
                    if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
                albums.add(
                    AlbumItem(
                        results.name(), results.language + " " + results.year,
                        imageUrl, results.id
                    )
                )
            }
            val adapter = ActivityMainAlbumItemAdapter(albums)
            currentBinding.popularAlbumsRecyclerView.adapter = adapter
            adapter.notifyDataSetChanged()
            updateTrendingGrid()
        }

        val playlistsSearch: PlaylistsSearch? = prefManager.homePlaylistRecommended
        if (playlistsSearch?.success == true) {
            val resultsList = playlistsSearch.data?.results ?: emptyList()
            for (results in resultsList) {
                if (results == null) continue
                val imageList = results.image
                val imageUrl =
                    if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
                playlists.add(
                    AlbumItem(
                        results.name(), "", imageUrl,
                        results.id
                    )
                )
            }
            val adapter = ActivityMainPlaylistAdapter(playlists)
            currentBinding.playlistRecyclerView.adapter = adapter
            adapter.notifyDataSetChanged()
        }
    }

    fun openSearch(view: View?) {
        startActivity(Intent(this, SearchActivity::class.java))
    }

    private val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { _: Boolean? -> }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        fun calculateNoOfColumns(
            context: Context,
            columnWidthDp: Float
        ): Int {
            val displayMetrics = context.resources.displayMetrics
            val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
            return (screenWidthDp / columnWidthDp + 0.5).toInt()
        }
    }

    private fun updateTrendingGrid() {
        val currentBinding = binding ?: return
        val trendingItems = mutableListOf<AlbumItem?>()
        val combined = ArrayList<AlbumItem?>()
        combined.addAll(albums.filterNotNull())
        combined.addAll(songs.filterNotNull())
        combined.addAll(newReleaseSongs.filterNotNull())
        combined.shuffle()
        for (item in combined) {
            if (item != null && item.id != "<shimmer>" && trendingItems.size < 6) {
                trendingItems.add(item)
            }
        }
        if (trendingItems.isNotEmpty()) {
            currentBinding.trendingRecyclerView?.adapter =
                ActivityMainTrendingAdapter(trendingItems)
        }
    }

    private fun setupBottomNavigation() {
        binding?.bottomNavigation?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding?.nestedScrollView?.smoothScrollTo(0, 0)
                    true
                }
                R.id.nav_search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                    true
                }
                R.id.nav_library -> {
                    startActivity(Intent(this, SavedLibrariesActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 4..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..22 -> "Good Evening"
            else -> "Good Night"
        }
        binding?.greetingText?.text = greeting
    }

    private fun showRecommendationsBottomSheet() {
        if (recommendedSongs.isEmpty()) return
        try {
            val bottomSheetDialog = BottomSheetDialog(this, R.style.MyBottomSheetDialogTheme)
            val sheetBinding = RecommendationsBottomSheetBinding.inflate(layoutInflater)

            sheetBinding.sheetTitle.text = recommendationTitle
            sheetBinding.sheetSubtitle.text = recommendationSubtitle

            sheetBinding.closeSheetBtn.setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            sheetBinding.playAllRecommended.setOnClickListener {
                val trackIds = recommendedSongs.mapNotNull { it?.id }
                if (trackIds.isNotEmpty()) {
                    MusicPlayerManager.trackQueue = trackIds.toMutableList()
                    MusicPlayerManager.track_position = 0
                    MusicPlayerManager.MUSIC_ID = trackIds[0]
                    MusicPlayerManager.playTrack()
                    startActivity(Intent(this, MusicOverviewActivity::class.java))
                    bottomSheetDialog.dismiss()
                }
            }

            sheetBinding.queueAllRecommended.setOnClickListener {
                val trackIds = recommendedSongs.mapNotNull { it?.id }
                val currentQueue = MusicPlayerManager.trackQueue ?: mutableListOf()
                var addedCount = 0
                trackIds.forEach { id ->
                    if (!currentQueue.contains(id)) {
                        currentQueue.add(id)
                        addedCount++
                    }
                }
                MusicPlayerManager.trackQueue = currentQueue
                Toast.makeText(this, "Added $addedCount songs to queue", Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }

            sheetBinding.recommendationsSheetRecyclerView.layoutManager = LinearLayoutManager(this)
            val adapter = ActivityListSongsItemAdapter(
                recommendedSongs.mapNotNull { item ->
                    if (item != null) {
                        SongResponse.Song(
                            id = item.id,
                            name = item.albumTitle,
                            type = "song",
                            year = null,
                            releaseDate = null,
                            duration = null,
                            label = null,
                            explicitContent = false,
                            playCount = null,
                            language = item.albumSubTitle,
                            hasLyrics = false,
                            lyricsId = null,
                            lyrics = null,
                            url = null,
                            copyright = null,
                            album = null,
                            artists = null,
                            image = mutableListOf(SongResponse.Image("high", item.albumCover)),
                            downloadUrl = null
                        )
                    } else null
                }.toMutableList()
            )
            sheetBinding.recommendationsSheetRecyclerView.adapter = adapter

            bottomSheetDialog.setContentView(sheetBinding.root)
            bottomSheetDialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing recommendations bottom sheet: ", e)
        }
    }


}
