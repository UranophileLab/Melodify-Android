package dev.melodify.uranophilelab.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dev.melodify.uranophilelab.BuildConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/UranophileLab/Melodify-Android/releases/latest"

    data class GitHubRelease(
        @SerializedName("tag_name") val tagName: String?,
        @SerializedName("name") val name: String?,
        @SerializedName("body") val body: String?,
        @SerializedName("html_url") val htmlUrl: String?,
        @SerializedName("assets") val assets: List<Asset>?
    ) {
        data class Asset(
            @SerializedName("name") val name: String?,
            @SerializedName("browser_download_url") val browserDownloadUrl: String?
        )
    }

    fun checkForUpdates(context: Context, isManualCheck: Boolean = false) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(GITHUB_LATEST_RELEASE_URL)
            .addHeader("User-Agent", "Melodify-Android-App")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to check for updates: ${e.message}")
                if (isManualCheck) {
                    Handler(Looper.getMainLooper()).post {
                        showAlertDialog(context, "Check for Updates", "Unable to check for updates. Please check your internet connection.")
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to check for updates: HTTP ${response.code}")
                    if (isManualCheck) {
                        Handler(Looper.getMainLooper()).post {
                            showAlertDialog(context, "Check for Updates", "Unable to check for updates (Server error: ${response.code}).")
                        }
                    }
                    return
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrBlank()) {
                    if (isManualCheck) {
                        Handler(Looper.getMainLooper()).post {
                            showAlertDialog(context, "Check for Updates", "Received empty response from update server.")
                        }
                    }
                    return
                }

                try {
                    val release = Gson().fromJson(responseBody, GitHubRelease::class.java)
                    val latestVersion = release?.tagName?.replace("v", "", ignoreCase = true)?.trim() ?: ""
                    if (latestVersion.isBlank()) {
                        if (isManualCheck) {
                            Handler(Looper.getMainLooper()).post {
                                showAlertDialog(context, "Check for Updates", "Could not determine latest version.")
                            }
                        }
                        return
                    }

                    val currentVersion = BuildConfig.VERSION_NAME.replace("v", "", ignoreCase = true).trim()

                    if (isNewerVersion(currentVersion, latestVersion)) {
                        val downloadUrl = release?.assets?.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }?.browserDownloadUrl
                            ?: release?.htmlUrl
                            ?: "https://github.com/UranophileLab/Melodify-Android/releases"

                        val releaseNotes = if (!release?.body.isNullOrBlank()) {
                            "\n\nWhat's New:\n${release?.body}"
                        } else ""

                        Handler(Looper.getMainLooper()).post {
                            showUpdateDialog(
                                context,
                                latestVersion,
                                releaseNotes,
                                downloadUrl
                            )
                        }
                    } else if (isManualCheck) {
                        Handler(Looper.getMainLooper()).post {
                            showAlertDialog(
                                context,
                                "Up to Date 🎉",
                                "You are using the latest version of Melodify (v$currentVersion)."
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing GitHub release response", e)
                    if (isManualCheck) {
                        Handler(Looper.getMainLooper()).post {
                            showAlertDialog(context, "Check for Updates", "Error parsing update information.")
                        }
                    }
                }
            }
        })
    }

    private fun showAlertDialog(context: Context, title: String, message: String) {
        if (context is Activity && (context.isFinishing || context.isDestroyed)) return
        try {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing dialog: ${e.message}")
        }
    }

    private fun showUpdateDialog(
        context: Context,
        newVersion: String,
        notes: String,
        downloadUrl: String
    ) {
        if (context is Activity && (context.isFinishing || context.isDestroyed)) return
        try {
            AlertDialog.Builder(context)
                .setTitle("New Update Available! 🚀")
                .setMessage("Version $newVersion is now available! Would you like to update now?$notes")
                .setCancelable(true)
                .setPositiveButton("Update Now") { _, _ ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error opening download URL: ${e.message}")
                    }
                }
                .setNegativeButton("Later", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing update dialog: ${e.message}")
        }
    }

    fun isNewerVersion(currentVersion: String, latestVersion: String): Boolean {
        if (latestVersion.isBlank()) return false
        try {
            val currentClean = currentVersion.replace(Regex("^[vV]"), "").substringBefore("-").substringBefore("+")
            val latestClean = latestVersion.replace(Regex("^[vV]"), "").substringBefore("-").substringBefore("+")

            val currentParts = currentClean.split(".").mapNotNull { it.toIntOrNull() }
            val latestParts = latestClean.split(".").mapNotNull { it.toIntOrNull() }

            val maxParts = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxParts) {
                val currentPart = currentParts.getOrElse(i) { 0 }
                val latestPart = latestParts.getOrElse(i) { 0 }
                if (latestPart > currentPart) return true
                if (latestPart < currentPart) return false
            }
        } catch (e: Exception) {
            return latestVersion != currentVersion
        }
        return false
    }
}
