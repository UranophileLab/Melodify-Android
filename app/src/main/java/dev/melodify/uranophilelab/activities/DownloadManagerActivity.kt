package dev.melodify.uranophilelab.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import dev.melodify.uranophilelab.adapters.ActivityDownloadManagerListAdapter
import dev.melodify.uranophilelab.databinding.ActivityDownloadManagerBinding
import dev.melodify.uranophilelab.utils.TrackDownloader
import dev.melodify.uranophilelab.utils.attachSnapHelper

class DownloadManagerActivity : AppCompatActivity() {
    private var binding: ActivityDownloadManagerBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadManagerBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        binding!!.recyclerView.setLayoutManager(LinearLayoutManager(this))
        binding!!.recyclerView.attachSnapHelper()
        val tracks = TrackDownloader.getDownloadedTracks(this)
        binding!!.recyclerView.setAdapter(
            ActivityDownloadManagerListAdapter(
                tracks.filterNotNull().toMutableList()
            )
        )
    }

    fun backPress(view: View?) {
        finish()
    }
}
