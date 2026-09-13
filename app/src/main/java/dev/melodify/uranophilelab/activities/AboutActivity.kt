package dev.melodify.uranophilelab.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dev.melodify.uranophilelab.BuildConfig
import dev.melodify.uranophilelab.databinding.ActivityAboutBinding
import dev.melodify.uranophilelab.utils.UpdateUtil

class AboutActivity : AppCompatActivity() {
    var binding: ActivityAboutBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        setSupportActionBar(binding!!.toolbar)
        binding!!.toolbar.setNavigationOnClickListener { _: android.view.View? -> finish() }

        binding!!.versionTxt.titleTextView?.text = BuildConfig.VERSION_NAME
        binding!!.versionTxt.setOnClickListener {
            Toast.makeText(this@AboutActivity, "Checking for updates...", Toast.LENGTH_SHORT).show()
            UpdateUtil.checkForUpdates(this@AboutActivity, true)
        }
    }
}
