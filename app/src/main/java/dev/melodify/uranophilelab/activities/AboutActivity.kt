package dev.melodify.uranophilelab.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.melodify.uranophilelab.BuildConfig
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.databinding.ActivityAboutBinding
import dev.melodify.uranophilelab.utils.UpdateManager

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
            UpdateManager.checkForUpdates(this@AboutActivity, isManualCheck = true)
        }

        binding!!.licenseTxt.setOnClickListener {
            val licenseText = try {
                assets.open("LICENSE").bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                getString(R.string.app_license)
            }
            MaterialAlertDialogBuilder(this@AboutActivity)
                .setTitle("License")
                .setMessage(licenseText)
                .setPositiveButton("Close", null)
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
