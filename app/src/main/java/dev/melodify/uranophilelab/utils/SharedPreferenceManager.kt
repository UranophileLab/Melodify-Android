package dev.melodify.uranophilelab.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import com.google.gson.Gson
import dev.melodify.uranophilelab.records.AlbumSearch
import dev.melodify.uranophilelab.records.AlbumsSearch
import dev.melodify.uranophilelab.records.ArtistSearch
import dev.melodify.uranophilelab.records.ArtistsSearch
import dev.melodify.uranophilelab.records.GlobalSearch
import dev.melodify.uranophilelab.records.PlaylistSearch
import dev.melodify.uranophilelab.records.PlaylistsSearch
import dev.melodify.uranophilelab.records.SongResponse
import dev.melodify.uranophilelab.records.SongSearch
import dev.melodify.uranophilelab.records.sharedpref.SavedLibraries
import dev.melodify.uranophilelab.records.sharedpref.SavedLibraries.Library
import dev.melodify.uranophilelab.model.history.SongHistoryItem
import dev.melodify.uranophilelab.model.history.AlbumHistoryItem
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import android.util.LruCache
import java.util.concurrent.Executors

@Entity(tableName = "key_value")
class KeyValue(
    @PrimaryKey var key: String,
    @ColumnInfo(name = "json") var json: String?,
    @ColumnInfo(name = "last_updated") var lastUpdated: Long
)

@Dao
interface KeyValueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(kv: KeyValue)

    @Query("SELECT json FROM key_value WHERE `key` = :key LIMIT 1")
    fun getJson(key: String?): String?

    @Query("SELECT 1 FROM key_value WHERE `key` = :key")
    fun exists(key: String?): Boolean

    @Query("DELETE FROM key_value WHERE `key` = :key")
    fun deleteByKey(key: String?)

    @Query("SELECT * FROM key_value")
    fun getAll(): List<KeyValue>
}

@Database(entities = [KeyValue::class], version = 1, exportSchema = false)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun keyValueDao(): KeyValueDao
}

/**
 * Drop-in replacement for your old SharedPreferenceManager that uses Room.
 * 
 * Notes:
 * - This keeps the same public API-method names so you can replace the file with minimal changes.
 * - For compatibility this version allows main-thread DB queries (allowMainThreadQueries).
 * This is convenient for drop-in replacement but not recommended for long-term (remove it later
 * and call write/read methods on background threads).
 * - Run migrateFromOldPrefs(context, onComplete) once (e.g. in Application.onCreate) to move
 * existing entries from SharedPreferences named "cache" into Room. After verifying, call
 * clearOldPrefsAsync(context, onComplete) to free space.
 */
class SharedPreferenceManager private constructor(context: Context) {
    // IMPORTANT: allowMainThreadQueries is enabled here for drop-in sync compatibility.
    // Recommended: remove allowMainThreadQueries() and perform DB operations off the UI thread.
    private val db: CacheDatabase = databaseBuilder<CacheDatabase>(
        context.applicationContext,
        CacheDatabase::class.java,
        "saavn_cache.db"
    )
        .allowMainThreadQueries()
        .build()
    private val dao: KeyValueDao = db.keyValueDao()
    private val gson: Gson = Gson()

    val sharedPreferences: SharedPreferences?
        // keep a small in-memory SharedPreferences reference only used during migration/clear
        get() =// we keep this method to preserve your API; it's no longer the primary store
            null

    // ---------- Internal helpers to map keys ----------
    private fun keyForSearch(query: String): String {
        return "search://$query"
    }

    private fun keyForArtistData(artistId: String): String {
        return "artistData://$artistId"
    }

    private fun now(): Long {
        return System.currentTimeMillis()
    }

    private val ioExecutor = Executors.newSingleThreadExecutor()

    @Volatile private var cachedSongHistory: MutableList<SongHistoryItem>? = null
    @Volatile private var cachedAlbumHistory: MutableList<AlbumHistoryItem>? = null
    @Volatile private var cachedPlaylistHistory: MutableList<AlbumHistoryItem>? = null
    @Volatile private var cachedSavedLibraries: SavedLibraries? = null
    @Volatile private var cachedFavoriteSongs: MutableList<SongHistoryItem>? = null

    private val songResponseCache = LruCache<String, SongResponse>(50)
    private val albumResponseCache = LruCache<String, AlbumSearch>(20)
    private val playlistResponseCache = LruCache<String, PlaylistSearch>(20)
    private val artistDataCache = LruCache<String, ArtistSearch>(20)

    // ---------- Generic put/get/remove helpers (asynchronous disk writes) ----------
    internal fun putJson(key: String, json: String?) {
        ioExecutor.execute {
            try {
                dao.upsert(KeyValue(key, json, now()))
            } catch (e: Exception) {
                Log.e("SharedPreferenceManager", "Error writing key $key", e)
            }
        }
    }

    internal fun getJson(key: String?): String? {
        if (key.isNullOrEmpty()) return null
        return try {
            dao.getJson(key)
        } catch (e: Exception) {
            Log.e("SharedPreferenceManager", "Error reading key $key from Room DB", e)
            null
        }
    }

    private fun containsKey(key: String?): Boolean {
        if (key.isNullOrEmpty()) return false
        return try {
            dao.exists(key)
        } catch (e: Exception) {
            Log.e("SharedPreferenceManager", "Error checking existence of key $key", e)
            false
        }
    }

    private fun removeKey(key: String?) {
        ioExecutor.execute {
            try {
                dao.deleteByKey(key)
            } catch (e: Exception) {
                Log.e("SharedPreferenceManager", "Error deleting key $key", e)
            }
        }
    }

    var homeSongsRecommended: SongSearch?
        get() {
            val json = getJson("home_songs_recommended")
            return if (json.isNullOrEmpty()) null else gson.fromJson(
                json,
                SongSearch::class.java
            )
        }
        // ---------- Methods mapped from your old SharedPreferenceManager ----------
        set(songSearch) {
            putJson("home_songs_recommended", gson.toJson(songSearch))
        }

    var homeArtistsRecommended: ArtistsSearch?
        get() {
            val json = getJson("home_artists_recommended")
            return if (json.isNullOrEmpty()) null else gson.fromJson(
                json,
                ArtistsSearch::class.java
            )
        }
        // home artists recommended
        set(artistsRecommended) {
            putJson("home_artists_recommended", gson.toJson(artistsRecommended))
        }

    var homeAlbumsRecommended: AlbumsSearch?
        get() {
            val json = getJson("home_albums_recommended")
            return if (json.isNullOrEmpty()) null else gson.fromJson(
                json,
                AlbumsSearch::class.java
            )
        }
        // home albums recommended
        set(albumsSearch) {
            putJson("home_albums_recommended", gson.toJson(albumsSearch))
        }

    var homePlaylistRecommended: PlaylistsSearch?
        get() {
            val json = getJson("home_playlists_recommended")
            return if (json.isNullOrEmpty()) null else gson.fromJson(
                json,
                PlaylistsSearch::class.java
            )
        }
        // home playlists recommended
        set(playlistsSearch) {
            putJson("home_playlists_recommended", gson.toJson(playlistsSearch))
        }

    var homeNewReleasesRecommended: SongSearch?
        get() {
            val json = getJson("home_new_releases_recommended")
            return if (json.isNullOrEmpty()) null else gson.fromJson(
                json,
                SongSearch::class.java
            )
        }
        // home new releases recommended
        set(newReleases) {
            putJson("home_new_releases_recommended", gson.toJson(newReleases))
        }

    // song response by id
    fun setSongResponseById(id: String?, songSearch: SongResponse?) {
        if (id == null) return
        if (songSearch != null) songResponseCache.put(id, songSearch) else songResponseCache.remove(id)
        putJson(id, gson.toJson(songSearch))
    }

    fun getSongResponseById(id: String?): SongResponse? {
        if (id == null) return null
        songResponseCache.get(id)?.let { return it }
        val json = getJson(id)
        if (json.isNullOrEmpty()) return null
        val result = try { gson.fromJson(json, SongResponse::class.java) } catch (e: Exception) { null }
        if (result != null) songResponseCache.put(id, result)
        return result
    }

    fun isSongResponseById(id: String?): Boolean {
        if (id == null) return false
        if (songResponseCache.get(id) != null) return true
        return containsKey(id)
    }

    // album response by id
    fun setAlbumResponseById(id: String?, albumSearch: AlbumSearch?) {
        if (id == null) return
        if (albumSearch != null) albumResponseCache.put(id, albumSearch) else albumResponseCache.remove(id)
        putJson(id, gson.toJson(albumSearch))
    }

    fun getAlbumResponseById(id: String?): AlbumSearch? {
        if (id == null) return null
        albumResponseCache.get(id)?.let { return it }
        val json = getJson(id)
        if (json.isNullOrEmpty()) return null
        val result = try { gson.fromJson(json, AlbumSearch::class.java) } catch (e: Exception) { null }
        if (result != null) albumResponseCache.put(id, result)
        return result
    }

    // playlist response by id
    fun setPlaylistResponseById(id: String?, playlistSearch: PlaylistSearch?) {
        if (id == null) return
        if (playlistSearch != null) playlistResponseCache.put(id, playlistSearch) else playlistResponseCache.remove(id)
        putJson(id, gson.toJson(playlistSearch))
    }

    fun getPlaylistResponseById(id: String?): PlaylistSearch? {
        if (id == null) return null
        playlistResponseCache.get(id)?.let { return it }
        val json = getJson(id)
        if (json.isNullOrEmpty()) return null
        val result = try { gson.fromJson(json, PlaylistSearch::class.java) } catch (e: Exception) { null }
        if (result != null) playlistResponseCache.put(id, result)
        return result
    }

    var trackQuality: String?
        get() {
            val json = getJson("track_quality")
            if (json.isNullOrEmpty()) return "320kbps"
            // stored as JSON string
            return try {
                gson.fromJson(json, String::class.java)
            } catch (e: Exception) {
                "320kbps"
            }
        }
        // track quality
        set(string) {
            var string = string
            if (string == null) string = ""
            putJson("track_quality", gson.toJson(string))
        }

    var eqEnabled: Boolean
        get() {
            val json = getJson("eq_enabled")
            if (json.isNullOrEmpty()) return false
            return try {
                gson.fromJson(json, Boolean::class.java) ?: false
            } catch (e: Exception) {
                false
            }
        }
        set(value) {
            putJson("eq_enabled", gson.toJson(value))
        }

    var eqPresetId: String?
        get() {
            val json = getJson("eq_preset_id")
            if (json.isNullOrEmpty()) return "flat"
            return try {
                gson.fromJson(json, String::class.java) ?: "flat"
            } catch (e: Exception) {
                "flat"
            }
        }
        set(value) {
            putJson("eq_preset_id", gson.toJson(value ?: "custom"))
        }

    var eqGains: FloatArray
        get() {
            val json = getJson("eq_gains")
            if (json.isNullOrEmpty()) return FloatArray(10) { 0f }
            return try {
                gson.fromJson(json, FloatArray::class.java) ?: FloatArray(10) { 0f }
            } catch (e: Exception) {
                FloatArray(10) { 0f }
            }
        }
        set(value) {
            putJson("eq_gains", gson.toJson(value))
        }

    var savedLibrariesData: SavedLibraries?
        get() {
            cachedSavedLibraries?.let { return it }
            val json = getJson("saved_libraries")
            if (json.isNullOrEmpty()) return null
            val result = try {
                gson.fromJson(
                    json,
                    SavedLibraries::class.java
                )
            } catch (e: Exception) {
                null
            }
            cachedSavedLibraries = result
            return cachedSavedLibraries
        }
        // saved libraries (full object)
        set(savedLibraries) {
            cachedSavedLibraries = savedLibraries
            putJson("saved_libraries", gson.toJson(savedLibraries))
        }

    // add library to saved libraries (keeps same semantics as original)
    fun addLibraryToSavedLibraries(library: Library?) {
        if (library == null) return
        val savedLibraries = this.savedLibrariesData ?: SavedLibraries(ArrayList())
        val list = ArrayList(savedLibraries.lists ?: ArrayList())
        list.add(library)
        this.savedLibrariesData = SavedLibraries(list)
    }

    fun removeLibraryFromSavedLibraries(index: Int) {
        val savedLibraries = this.savedLibrariesData ?: return
        val list = savedLibraries.lists ?: return
        if (index < 0 || index >= list.size) return
        list.removeAt(index)
        this.savedLibrariesData = SavedLibraries(list)
    }

    // saved library by id (stored under the id key)
    fun setSavedLibraryDataById(id: String?, library: Library?) {
        if (id == null || library == null) return
        putJson(id, gson.toJson(library))
    }

    fun getSavedLibraryDataById(id: String?): Library? {
        if (id == null) return null
        val json = getJson(id)
        if (json.isNullOrEmpty()) return null
        return try {
            gson.fromJson(json, Library::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // search cache
    fun setSearchResultCache(query: String?, searchResult: GlobalSearch?) {
        if (query == null) return
        putJson(keyForSearch(query), gson.toJson(searchResult))
    }

    fun getSearchResult(query: String?): GlobalSearch? {
        if (query == null) return null
        val json = getJson(keyForSearch(query))
        return if (json.isNullOrEmpty()) null else gson.fromJson(
            json,
            GlobalSearch::class.java
        )
    }

    // artist data
    fun setArtistData(artistID: String?, artistSearch: ArtistSearch?) {
        if (artistID == null) return
        if (artistSearch != null) artistDataCache.put(artistID, artistSearch) else artistDataCache.remove(artistID)
        putJson(keyForArtistData(artistID), gson.toJson(artistSearch))
    }

    fun getArtistData(artistId: String?): ArtistSearch? {
        if (artistId == null) return null
        artistDataCache.get(artistId)?.let { return it }
        val json = getJson(keyForArtistData(artistId))
        if (json.isNullOrEmpty()) return null
        val result = try { gson.fromJson(json, ArtistSearch::class.java) } catch (e: Exception) { null }
        if (result != null) artistDataCache.put(artistId, result)
        return result
    }

    // ---------- Song & Album History ----------
    var songHistory: List<SongHistoryItem>
        get() {
            cachedSongHistory?.let { return ArrayList(it) }
            val json = getJson("song_history")
            if (json.isNullOrEmpty()) {
                cachedSongHistory = mutableListOf()
                return ArrayList(cachedSongHistory!!)
            }
            val type = object : TypeToken<List<SongHistoryItem>>() {}.type
            val result = try {
                gson.fromJson<List<SongHistoryItem>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            cachedSongHistory = result.toMutableList()
            return ArrayList(cachedSongHistory!!)
        }
        set(list) {
            cachedSongHistory = list.toMutableList()
            putJson("song_history", gson.toJson(list))
        }

    fun addSongToHistory(item: SongHistoryItem) {
        if (item.id.isNullOrBlank()) return
        val cleanTitle = TextParserUtil.parseHtmlText(item.title)
        if (cleanTitle.isBlank() || cleanTitle.equals("loading...", ignoreCase = true)) return
        val cleanArtist = TextParserUtil.parseHtmlText(item.artist)

        val current = songHistory.toMutableList()
        val existing = current.firstOrNull { it.id == item.id || (!it.title.isNullOrBlank() && it.title == cleanTitle) }
        val finalArtist = if (cleanArtist.isNotBlank()) cleanArtist else (existing?.artist ?: "")
        val cleanItem = item.copy(title = cleanTitle, artist = finalArtist)

        current.removeAll { it.id == item.id || (!it.title.isNullOrBlank() && it.title == cleanTitle) }
        current.add(0, cleanItem)
        songHistory = if (current.size > 100) current.subList(0, 100) else current
    }

    fun clearSongHistory() {
        cachedSongHistory = mutableListOf()
        putJson("song_history", gson.toJson(emptyList<SongHistoryItem>()))
    }

    var albumHistory: List<AlbumHistoryItem>
        get() {
            cachedAlbumHistory?.let { return ArrayList(it) }
            val json = getJson("album_history")
            if (json.isNullOrEmpty()) {
                cachedAlbumHistory = mutableListOf()
                return ArrayList(cachedAlbumHistory!!)
            }
            val type = object : TypeToken<List<AlbumHistoryItem>>() {}.type
            val result = try {
                gson.fromJson<List<AlbumHistoryItem>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            cachedAlbumHistory = result.toMutableList()
            return ArrayList(cachedAlbumHistory!!)
        }
        set(list) {
            cachedAlbumHistory = list.toMutableList()
            putJson("album_history", gson.toJson(list))
        }

    fun addAlbumToHistory(item: AlbumHistoryItem) {
        if (item.id.isNullOrBlank()) return
        val cleanTitle = TextParserUtil.parseHtmlText(item.title)
        if (cleanTitle.isBlank() || cleanTitle.equals("loading...", ignoreCase = true)) return
        val cleanSubtitle = TextParserUtil.parseHtmlText(item.subtitle)

        val current = albumHistory.toMutableList()
        val existing = current.firstOrNull { it.id == item.id || (!it.title.isNullOrBlank() && it.title == cleanTitle) }
        val finalSubtitle = if (cleanSubtitle.isNotBlank()) cleanSubtitle else (existing?.subtitle ?: "")
        val cleanItem = item.copy(title = cleanTitle, subtitle = finalSubtitle)

        current.removeAll { it.id == item.id || (!it.title.isNullOrBlank() && it.title == cleanTitle) }
        current.add(0, cleanItem)
        albumHistory = if (current.size > 100) current.subList(0, 100) else current
    }

    fun clearAlbumHistory() {
        cachedAlbumHistory = mutableListOf()
        putJson("album_history", gson.toJson(emptyList<AlbumHistoryItem>()))
    }

    var playlistHistory: List<AlbumHistoryItem>
        get() {
            cachedPlaylistHistory?.let { return ArrayList(it) }
            val json = getJson("playlist_history")
            if (json.isNullOrEmpty()) {
                cachedPlaylistHistory = mutableListOf()
                return ArrayList(cachedPlaylistHistory!!)
            }
            val type = object : TypeToken<List<AlbumHistoryItem>>() {}.type
            val result = try {
                gson.fromJson<List<AlbumHistoryItem>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            cachedPlaylistHistory = result.toMutableList()
            return ArrayList(cachedPlaylistHistory!!)
        }
        set(list) {
            cachedPlaylistHistory = list.toMutableList()
            putJson("playlist_history", gson.toJson(list))
        }

    fun addPlaylistToHistory(item: AlbumHistoryItem) {
        if (item.id.isNullOrBlank()) return
        val cleanTitle = TextParserUtil.parseHtmlText(item.title)
        if (cleanTitle.isBlank() || cleanTitle.equals("loading...", ignoreCase = true)) return
        val cleanSubtitle = TextParserUtil.parseHtmlText(item.subtitle)

        val current = playlistHistory.toMutableList()
        val existing = current.firstOrNull { it.id == item.id || (!it.title.isNullOrBlank() && it.title == cleanTitle) }
        val finalSubtitle = if (cleanSubtitle.isNotBlank()) cleanSubtitle else (existing?.subtitle ?: "")
        val cleanItem = item.copy(title = cleanTitle, subtitle = finalSubtitle)

        current.removeAll { it.id == item.id || (!it.title.isNullOrBlank() && it.title == cleanTitle) }
        current.add(0, cleanItem)
        playlistHistory = if (current.size > 100) current.subList(0, 100) else current
    }

    fun clearPlaylistHistory() {
        cachedPlaylistHistory = mutableListOf()
        putJson("playlist_history", gson.toJson(emptyList<AlbumHistoryItem>()))
    }

    fun clearAllHistory() {
        clearSongHistory()
        clearAlbumHistory()
        clearPlaylistHistory()
    }

    // ---------- Favorites ----------
    var favoriteSongs: List<SongHistoryItem>
        get() {
            cachedFavoriteSongs?.let { return ArrayList(it) }
            val json = getJson("favorite_songs")
            if (json.isNullOrEmpty()) {
                cachedFavoriteSongs = mutableListOf()
                return ArrayList(cachedFavoriteSongs!!)
            }
            val type = object : TypeToken<List<SongHistoryItem>>() {}.type
            val result = try {
                gson.fromJson<List<SongHistoryItem>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            cachedFavoriteSongs = result.toMutableList()
            return ArrayList(cachedFavoriteSongs!!)
        }
        set(list) {
            cachedFavoriteSongs = list.toMutableList()
            putJson("favorite_songs", gson.toJson(list))
        }

    fun isFavorite(songId: String?): Boolean {
        if (songId.isNullOrBlank()) return false
        val cleanId = songId.trim()
        return favoriteSongs.any { !it.id.isNullOrBlank() && it.id.trim() == cleanId }
    }

    fun addFavorite(song: SongHistoryItem) {
        if (song.id.isNullOrBlank()) return
        val cleanId = song.id.trim()
        val current = favoriteSongs.toMutableList()
        current.removeAll { !it.id.isNullOrBlank() && it.id.trim() == cleanId }
        current.add(0, song)
        favoriteSongs = current
        syncLikedSongsLibrary()
    }

    fun removeFavorite(songId: String?) {
        if (songId.isNullOrBlank()) return
        val cleanId = songId.trim()
        val current = favoriteSongs.toMutableList()
        current.removeAll { !it.id.isNullOrBlank() && it.id.trim() == cleanId }
        favoriteSongs = current
        syncLikedSongsLibrary()
    }

    fun toggleFavorite(song: SongHistoryItem): Boolean {
        val isFav = isFavorite(song.id)
        if (isFav) {
            removeFavorite(song.id)
            return false
        } else {
            addFavorite(song)
            return true
        }
    }

    fun clearFavoriteSongs() {
        cachedFavoriteSongs = mutableListOf()
        putJson("favorite_songs", gson.toJson(emptyList<SongHistoryItem>()))
        syncLikedSongsLibrary()
    }

    fun syncLikedSongsLibrary() {
        val favs = favoriteSongs
        val saved = savedLibrariesData ?: SavedLibraries(ArrayList())
        val list: MutableList<Library?> = ArrayList(saved.lists ?: ArrayList())

        val existingIndex = list.indexOfFirst { it?.id == "liked_songs" }

        if (favs.isNotEmpty()) {
            val songsList: MutableList<Library.Songs?> = ArrayList()
            for (item in favs) {
                songsList.add(
                    Library.Songs(
                        id = item.id,
                        title = item.title,
                        description = item.artist,
                        image = item.imageUrl
                    )
                )
            }

            val likedLibrary = Library(
                id = "liked_songs",
                isCreatedByUser = true,
                isAlbum = false,
                name = "Liked Songs",
                image = favs.firstOrNull { !it.imageUrl.isNullOrBlank() }?.imageUrl ?: "",
                description = "${favs.size} ${if (favs.size == 1) "song" else "songs"}",
                songs = songsList
            )

            if (existingIndex >= 0) {
                list[existingIndex] = likedLibrary
            } else {
                list.add(0, likedLibrary)
            }
            this.savedLibrariesData = SavedLibraries(list)
        } else {
            if (existingIndex >= 0) {
                list.removeAt(existingIndex)
                this.savedLibrariesData = SavedLibraries(list)
            }
        }
    }

    fun getStreamQuality(): String {
        return getJson("stream_quality") ?: "auto"
    }

    fun setStreamQuality(quality: String) {
        putJson("stream_quality", quality)
    }

    fun getPreferredProvider(): String {
        return getJson("preferred_provider") ?: "saavn"
    }

    fun setPreferredProvider(provider: String) {
        putJson("preferred_provider", provider)
    }

    // ---------- Migration helpers for one-time migration ----------
    /**
     * Migrate all entries from the old SharedPreferences named "cache" into Room.
     * This runs synchronously (it can be called from Application.onCreate). It iterates all keys
     * in the old prefs and upserts their value (strings/primitives) into Room under the same key.
     *
     * You can pass a Runnable for onComplete which will run after migration (on the caller thread).
     * Recommended: call this once in Application.onCreate and then verify data in Room.
     */
    fun migrateFromOldPrefs(context: Context, onComplete: Runnable?) {
        val prefs = context.applicationContext
            .getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE)
        val all = prefs.all
        now()
        if (all == null) {
            onComplete?.run()
            return
        }
        for (entry in all.entries) {
            val key: String = entry.key ?: continue
            val value: Any = entry.value ?: continue
            val json: String? = value as? String ?: gson.toJson(value)
            putJson(key, json)
        }
        onComplete?.run()
    }

    /**
     * Clear the old SharedPreferences (async-friendly, but uses allowMainThreadQueries for this file).
     * Call this only after you verified migration succeeded and Room contains your data.
     */
    fun clearOldPrefsAsync(context: Context, onComplete: Runnable?) {
        val prefs = context.applicationContext
            .getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { clear() }
        onComplete?.run()
    }

    companion object {
        private var instance: SharedPreferenceManager? = null
        private const val OLD_PREFS_NAME = "cache"

        fun getInstance(context: Context): SharedPreferenceManager {
            if (instance == null) {
                synchronized(SharedPreferenceManager::class.java) {
                    if (instance == null) {
                        instance = SharedPreferenceManager(context.applicationContext)
                    }
                }
            }
            return instance!!
        }
    }
}
