package dev.melodify.uranophilelab.activities

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.melodify.uranophilelab.BaseApplicationClass
import dev.melodify.uranophilelab.adapters.SongHistoryAdapter
import dev.melodify.uranophilelab.databinding.ActivityFavoritesBinding
import dev.melodify.uranophilelab.model.history.SongHistoryItem
import dev.melodify.uranophilelab.utils.MiniPlayerHelper
import dev.melodify.uranophilelab.utils.SharedPreferenceManager
import dev.melodify.uranophilelab.utils.attachSnapHelper

class FavoritesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoritesBinding
    private var favoriteList: MutableList<SongHistoryItem> = mutableListOf()
    private var adapter: SongHistoryAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BaseApplicationClass.updateTheme(this)
        MiniPlayerHelper.initMiniPlayer(this)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.attachSnapHelper()

        binding.clearFavoritesBtn.setOnClickListener {
            showClearConfirmationDialog()
        }

        loadFavoritesData()
    }

    override fun onResume() {
        super.onResume()
        MiniPlayerHelper.onActivityResume(this)
        loadFavoritesData()
    }

    override fun onPause() {
        super.onPause()
        MiniPlayerHelper.onActivityPause(this)
    }

    fun backPress(view: View?) {
        finish()
    }

    private fun loadFavoritesData() {
        val prefs = SharedPreferenceManager.getInstance(this)
        favoriteList = prefs.favoriteSongs.toMutableList()

        adapter = SongHistoryAdapter(favoriteList) {
            prefs.favoriteSongs = favoriteList
            updateEmptyState()
        }
        binding.recyclerView.adapter = adapter

        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (favoriteList.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyFavoritesTv.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyFavoritesTv.visibility = View.GONE
        }
    }

    private fun showClearConfirmationDialog() {
        if (favoriteList.isEmpty()) {
            Toast.makeText(this, "Favorites is already empty", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Clear Favorites")
            .setMessage("Are you sure you want to remove all songs from your favorites?")
            .setPositiveButton("Clear") { _: DialogInterface, _: Int ->
                val prefs = SharedPreferenceManager.getInstance(this)
                prefs.clearFavoriteSongs()
                favoriteList.clear()
                adapter?.notifyDataSetChanged()
                updateEmptyState()
                Toast.makeText(this, "Favorites cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
