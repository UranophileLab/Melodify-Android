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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dev.melodify.uranophilelab.BaseApplicationClass
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.adapters.ActivityMainAlbumItemAdapter
import dev.melodify.uranophilelab.adapters.ActivityMainArtistsItemAdapter
import dev.melodify.uranophilelab.adapters.ActivityMainPlaylistAdapter
import dev.melodify.uranophilelab.adapters.ActivityMainPopularSongs
import dev.melodify.uranophilelab.adapters.SavedLibrariesAdapter
import dev.melodify.uranophilelab.databinding.ActivityMainBinding
import dev.melodify.uranophilelab.model.AlbumItem
import dev.melodify.uranophilelab.network.ApiManager
import dev.melodify.uranophilelab.network.NetworkChangeReceiver
import dev.melodify.uranophilelab.network.NetworkChangeReceiver.NetworkStatusListener
import dev.melodify.uranophilelab.network.utility.RequestNetwork
import dev.melodify.uranophilelab.records.AlbumsSearch
import dev.melodify.uranophilelab.records.ArtistsSearch
import dev.melodify.uranophilelab.records.PlaylistsSearch
import dev.melodify.uranophilelab.records.SongSearch
import dev.melodify.uranophilelab.utils.MiniPlayerHelper
import dev.melodify.uranophilelab.utils.MusicPlayerManager
import dev.melodify.uranophilelab.utils.SharedPreferenceManager
import dev.melodify.uranophilelab.utils.attachSnapHelper
import com.yarolegovich.slidingrootnav.SlidingRootNav
import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder
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
    val artists: MutableList<ArtistsSearch.Data.Results?> = ArrayList<ArtistsSearch.Data.Results?>()
    val albums: MutableList<AlbumItem?> = ArrayList<AlbumItem?>()
    val playlists: MutableList<AlbumItem?> = ArrayList<AlbumItem?>()

    var networkChangeReceiver: NetworkChangeReceiver =
        NetworkChangeReceiver(object : NetworkStatusListener {
            override fun onNetworkConnected() {
                if (songs.isEmpty() || artists.isEmpty() || albums.isEmpty() || playlists.isEmpty()) showData()
            }

            override fun onNetworkDisconnected() {
                if (songs.isEmpty() || artists.isEmpty() || albums.isEmpty() || playlists.isEmpty()) showOfflineData()
                val currentBinding = binding ?: return
                Snackbar.make(currentBinding.getRoot(), "No Internet Connection", Snackbar.LENGTH_LONG)
                    .show()
            }
        })

    private var slidingRootNavBuilder: SlidingRootNav? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflatedBinding = ActivityMainBinding.inflate(layoutInflater)
        binding = inflatedBinding
        setContentView(inflatedBinding.getRoot())

        baseApplicationClass = applicationContext as BaseApplicationClass?
        BaseApplicationClass.updateTheme(this)

        slidingRootNavBuilder = SlidingRootNavBuilder(this)
            .withMenuLayout(R.layout.main_drawer_layout)
            .withContentClickableWhenMenuOpened(false)
            .withDragDistance(250)
            .inject()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (slidingRootNavBuilder?.isMenuOpened == true) {
                    slidingRootNavBuilder?.closeMenu()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        // Set version text in the drawer layout
        updateVersionTextInDrawer()

        onDrawerItemsClicked()

        setupGreeting()
        setupCategoryFilterChips()

        inflatedBinding.profileIcon.setOnClickListener {
            slidingRootNavBuilder?.openMenu(true)
        }

        val span: Int = calculateNoOfColumns(this, 200f)
        inflatedBinding.playlistRecyclerView.layoutManager = GridLayoutManager(this, span)

        inflatedBinding.popularSongsRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        inflatedBinding.popularArtistsRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        inflatedBinding.popularAlbumsRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        inflatedBinding.savedRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Snap helpers — smooth magnetic scroll on all horizontal lists
        inflatedBinding.popularSongsRecyclerView.attachSnapHelper()
        inflatedBinding.popularArtistsRecyclerView.attachSnapHelper()
        inflatedBinding.popularAlbumsRecyclerView.attachSnapHelper()
        inflatedBinding.savedRecyclerView.attachSnapHelper()

        OverScrollDecoratorHelper.setUpOverScroll(
            inflatedBinding.popularSongsRecyclerView,
            OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL
        )
        OverScrollDecoratorHelper.setUpOverScroll(
            inflatedBinding.popularArtistsRecyclerView,
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
        val savedLibraries = SharedPreferenceManager.getInstance(this).savedLibrariesData
        currentBinding.savedLibrariesSection.visibility =
            if (savedLibraries != null && !(savedLibraries.lists?.isEmpty() ?: true)) View.VISIBLE else View.GONE
        if (savedLibraries != null) {
            currentBinding.savedRecyclerView.adapter = SavedLibrariesAdapter(
                savedLibraries.lists ?: mutableListOf()
            )
        }
    }

    private fun onDrawerItemsClicked() {
        val layout = slidingRootNavBuilder?.layout ?: return
        layout.findViewById<View>(R.id.settings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            slidingRootNavBuilder?.closeMenu()
        }

        layout.findViewById<View>(R.id.logo)?.setOnClickListener {
            slidingRootNavBuilder?.closeMenu()
        }

        layout.findViewById<View>(R.id.library)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, SavedLibrariesActivity::class.java))
            slidingRootNavBuilder?.closeMenu()
        }

        layout.findViewById<View>(R.id.favorites)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, FavoritesActivity::class.java))
            slidingRootNavBuilder?.closeMenu()
        }

        layout.findViewById<View>(R.id.history)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, HistoryActivity::class.java))
            slidingRootNavBuilder?.closeMenu()
        }

        layout.findViewById<View>(R.id.about)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, AboutActivity::class.java))
            slidingRootNavBuilder?.closeMenu()
        }

        layout.findViewById<View>(R.id.download_manager)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, DownloadManagerActivity::class.java))
            slidingRootNavBuilder?.closeMenu()
        }
    }

    /**
     * Updates the version text in the navigation drawer with the app's current version
     */
    private fun updateVersionTextInDrawer() {
        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            val drawerLayout: View? = slidingRootNavBuilder?.layout
            if (drawerLayout != null) {
                val versionTextView = drawerLayout.findViewById<View?>(R.id.versionTxt)
                if (versionTextView is TextView) {
                    versionTextView.text = "version $versionName"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app version: " + e.message)
        }
    }

    override fun onResume() {
        super.onResume()
        NetworkChangeReceiver.registerReceiver(this, networkChangeReceiver)
        showSavedLibrariesData()
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
        artists.clear()
        albums.clear()
        playlists.clear()

        val songSeeds = listOf("2023", "2024", "2025", "2026", "Hits", "Latest", "Romantic", "Chill", "Lo-Fi", "Dance", "Hindi", "Sad", "Love", "Workout")
        val artistSeeds = listOf("Arijit", "Taylor", "Drake", "BTS", "Ed Sheeran", "Shreya", "Justin", "Badshah")
        val albumSeeds = listOf("2023", "2024", "2025", "2026", "Hits", "Latest", "New", "Sad", "Lo-Fi", "Romantic", "Rock", "Pop", "Classic", "Party")
        val playlistSeeds = listOf("2023", "2024", "2025", "2026", "Hits", "Latest", "Trending", "Party", "Devotional", "Chill", "Love", "Sad", "Top", "Classic")

        val songQuery = songSeeds.random()
        val artistQuery = artistSeeds.random()
        val albumQuery = albumSeeds.random()
        val playlistQuery = playlistSeeds.random()

        val apiManager = ApiManager(this)

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
                                    imageUrl, results.id
                                )
                            )
                        }
                        val adapter = ActivityMainPopularSongs(songs)
                        currentBinding.popularSongsRecyclerView.adapter = adapter
                        adapter.notifyDataSetChanged()
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
                        currentBinding.popularArtistsRecyclerView.adapter = adapter
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
                                    imageUrl, results.id
                                )
                            )
                        }
                        val adapter = ActivityMainAlbumItemAdapter(albums)
                        currentBinding.popularAlbumsRecyclerView.adapter = adapter
                        adapter.notifyDataSetChanged()
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
                                        imageUrl, results.id
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
        currentBinding.popularSongsRecyclerView.adapter = ActivityMainAlbumItemAdapter(dataShimmer)
        currentBinding.popularAlbumsRecyclerView.adapter = ActivityMainAlbumItemAdapter(dataShimmer)
        currentBinding.popularArtistsRecyclerView.adapter = ActivityMainArtistsItemAdapter(artistsShimmer)
        currentBinding.playlistRecyclerView.adapter = ActivityMainPlaylistAdapter(dataShimmer)
    }

    private fun showOfflineData() {
        val currentBinding = binding ?: return
        val prefManager = BaseApplicationClass.sharedPreferenceManager ?: return
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
            currentBinding.popularArtistsRecyclerView.adapter = adapter
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

    private fun setupCategoryFilterChips() {
        binding?.categoryChipGroup?.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_all
            when (checkedId) {
                R.id.chip_songs -> {
                    binding?.popularSongsSection?.visibility = View.VISIBLE
                    binding?.popularArtistsSection?.visibility = View.GONE
                    binding?.popularAlbumsSection?.visibility = View.GONE
                    binding?.savedLibrariesSection?.visibility = View.GONE
                    binding?.popularPlaylistsSection?.visibility = View.GONE
                }
                R.id.chip_albums -> {
                    binding?.popularSongsSection?.visibility = View.GONE
                    binding?.popularArtistsSection?.visibility = View.GONE
                    binding?.popularAlbumsSection?.visibility = View.VISIBLE
                    binding?.savedLibrariesSection?.visibility = View.GONE
                    binding?.popularPlaylistsSection?.visibility = View.GONE
                }
                R.id.chip_artists -> {
                    binding?.popularSongsSection?.visibility = View.GONE
                    binding?.popularArtistsSection?.visibility = View.VISIBLE
                    binding?.popularAlbumsSection?.visibility = View.GONE
                    binding?.savedLibrariesSection?.visibility = View.GONE
                    binding?.popularPlaylistsSection?.visibility = View.GONE
                }
                R.id.chip_playlists -> {
                    binding?.popularSongsSection?.visibility = View.GONE
                    binding?.popularArtistsSection?.visibility = View.GONE
                    binding?.popularAlbumsSection?.visibility = View.GONE
                    binding?.savedLibrariesSection?.visibility = View.GONE
                    binding?.popularPlaylistsSection?.visibility = View.VISIBLE
                }
                else -> { // R.id.chip_all
                    binding?.popularSongsSection?.visibility = View.VISIBLE
                    binding?.popularArtistsSection?.visibility = View.VISIBLE
                    binding?.popularAlbumsSection?.visibility = View.VISIBLE
                    binding?.savedLibrariesSection?.visibility = View.VISIBLE
                    binding?.popularPlaylistsSection?.visibility = View.VISIBLE
                }
            }
        }
    }
}
