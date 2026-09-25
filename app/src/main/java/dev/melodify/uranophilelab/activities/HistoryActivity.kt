package dev.melodify.uranophilelab.activities

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.melodify.uranophilelab.BaseApplicationClass
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.adapters.AlbumHistoryAdapter
import dev.melodify.uranophilelab.adapters.SongHistoryAdapter
import dev.melodify.uranophilelab.databinding.ActivityHistoryBinding
import dev.melodify.uranophilelab.model.history.AlbumHistoryItem
import dev.melodify.uranophilelab.model.history.SongHistoryItem
import dev.melodify.uranophilelab.utils.MiniPlayerHelper
import dev.melodify.uranophilelab.utils.MusicPlayerManager
import dev.melodify.uranophilelab.utils.SharedPreferenceManager
import dev.melodify.uranophilelab.utils.attachSnapHelper

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private var songHistoryList: MutableList<SongHistoryItem> = mutableListOf()
    private var albumHistoryList: MutableList<AlbumHistoryItem> = mutableListOf()

    private var songAdapter: SongHistoryAdapter? = null
    private var albumAdapter: AlbumHistoryAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BaseApplicationClass.updateTheme(this)
        MiniPlayerHelper.initMiniPlayer(this)

        binding.songsRecycler.layoutManager = LinearLayoutManager(this)
        binding.albumsRecycler.layoutManager = LinearLayoutManager(this)

        setupChipFilter()

        binding.clearHistoryBtn.setOnClickListener {
            showClearHistoryDialog()
        }

        loadHistoryData()
    }

    override fun onResume() {
        super.onResume()
        MiniPlayerHelper.onActivityResume(this)
        loadHistoryData()
    }

    override fun onPause() {
        super.onPause()
        MiniPlayerHelper.onActivityPause(this)
    }

    fun backPress(view: View?) {
        finish()
    }

    private fun loadHistoryData() {
        val prefs = SharedPreferenceManager.getInstance(this)

        val curId = MusicPlayerManager.MUSIC_ID
        val curTitle = MusicPlayerManager.MUSIC_TITLE
        if (!curId.isNullOrBlank() && !curTitle.isNullOrBlank() && !curTitle.equals("loading...", true)) {
            val rawArtist = MusicPlayerManager.MUSIC_DESCRIPTION?.split("|")?.firstOrNull()?.trim() ?: ""
            val artistName = if (rawArtist.contains("plays", ignoreCase = true)) "" else rawArtist
            prefs.addSongToHistory(
                SongHistoryItem(
                    id = curId,
                    title = curTitle,
                    artist = artistName,
                    imageUrl = MusicPlayerManager.IMAGE_URL
                )
            )
        }

        val newSongs = prefs.songHistory
        val newAlbums = prefs.albumHistory

        Log.d("HistoryDebug", "loadHistoryData -> newSongs: ${newSongs.size}, newAlbums: ${newAlbums.size}")

        songHistoryList.clear()
        songHistoryList.addAll(newSongs)

        albumHistoryList.clear()
        albumHistoryList.addAll(newAlbums)

        if (songAdapter == null) {
            songAdapter = SongHistoryAdapter(songHistoryList) {
                updateEmptyState()
            }
            binding.songsRecycler.adapter = songAdapter
        } else {
            songAdapter?.updateData(songHistoryList)
        }

        if (albumAdapter == null) {
            albumAdapter = AlbumHistoryAdapter(albumHistoryList) {
                updateEmptyState()
            }
            binding.albumsRecycler.adapter = albumAdapter
        } else {
            albumAdapter?.updateData(albumHistoryList)
        }

        updateEmptyState()
    }

    private fun updateEmptyState() {
        val hasSongs = songHistoryList.isNotEmpty()
        val hasAlbums = albumHistoryList.isNotEmpty()

        val checkedChipId = binding.chipGroup.checkedChipId
        if (checkedChipId == View.NO_ID) {
            binding.chipAll.isChecked = true
        }

        val effectiveChipId = if (checkedChipId == View.NO_ID) R.id.chip_all else checkedChipId

        val showSongs = (effectiveChipId == R.id.chip_all || effectiveChipId == R.id.chip_songs) && hasSongs
        val showAlbums = (effectiveChipId == R.id.chip_all || effectiveChipId == R.id.chip_albums) && hasAlbums

        binding.songSection.visibility = if (showSongs) View.VISIBLE else View.GONE
        binding.albumSection.visibility = if (showAlbums) View.VISIBLE else View.GONE

        val isFilterEmpty = when (effectiveChipId) {
            R.id.chip_songs -> !hasSongs
            R.id.chip_albums -> !hasAlbums
            else -> !hasSongs && !hasAlbums
        }

        if (isFilterEmpty) {
            binding.nestedScroll.visibility = View.GONE
            binding.emptyHistoryTv.visibility = View.VISIBLE
        } else {
            binding.nestedScroll.visibility = View.VISIBLE
            binding.emptyHistoryTv.visibility = View.GONE
        }
    }

    private fun setupChipFilter() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                binding.chipAll.isChecked = true
            }
            updateEmptyState()
        }
    }

    private fun showClearHistoryDialog() {
        val options = arrayOf("Clear Song History", "Clear Album History", "Clear All History")
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear History")
            .setItems(options) { _: DialogInterface, which: Int ->
                val prefs = SharedPreferenceManager.getInstance(this)
                when (which) {
                    0 -> {
                        prefs.clearSongHistory()
                        songHistoryList.clear()
                        songAdapter?.notifyDataSetChanged()
                        Toast.makeText(this, "Song history cleared", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        prefs.clearAlbumHistory()
                        albumHistoryList.clear()
                        albumAdapter?.notifyDataSetChanged()
                        Toast.makeText(this, "Album history cleared", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        prefs.clearAllHistory()
                        songHistoryList.clear()
                        albumHistoryList.clear()
                        songAdapter?.notifyDataSetChanged()
                        albumAdapter?.notifyDataSetChanged()
                        Toast.makeText(this, "All history cleared", Toast.LENGTH_SHORT).show()
                    }
                }
                updateEmptyState()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
