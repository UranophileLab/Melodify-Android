package dev.melodify.uranophilelab.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.melodify.uranophilelab.adapters.ActivityDownloadManagerListAdapter
import dev.melodify.uranophilelab.databinding.ActivityDownloadManagerBinding
import dev.melodify.uranophilelab.utils.TrackDownloader
import dev.melodify.uranophilelab.utils.attachSnapHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDownloadManagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadManagerBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.attachSnapHelper()

        lifecycleScope.launch(Dispatchers.IO) {
            val tracks = TrackDownloader.getDownloadedTracks(this@DownloadManagerActivity)
                .filterNotNull()
                .toMutableList()
            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    binding.recyclerView.adapter = ActivityDownloadManagerListAdapter(tracks)
                }
            }
        }
    }

    fun backPress(view: View?) {
        finish()
    }
}
