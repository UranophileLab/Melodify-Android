package dev.melodify.uranophilelab.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import dev.melodify.uranophilelab.network.utility.RequestNetwork
import dev.melodify.uranophilelab.network.utility.RequestNetworkController

class ApiManager(context: Context?) {
    private val requestNetwork: RequestNetwork = RequestNetwork(context)

    fun globalSearch(text: String?, listener: RequestNetwork.RequestListener?) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["query"] = text
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SEARCH_URL,
            "",
            listener
        )
    }

    fun searchSongs(
        query: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener
    ) {
        val directUrl = "https://www.jiosaavn.com/api.php"
        val queryMap = HashMap<String?, Any?>()
        queryMap["__call"] = "search.getResults"
        queryMap["_format"] = "json"
        queryMap["_marker"] = "0"
        queryMap["api_version"] = "4"
        queryMap["ctx"] = "web6dot0"
        queryMap["q"] = query
        if (page != null) queryMap["p"] = page
        if (limit != null) queryMap["n"] = limit

        val directRequestNetwork = RequestNetwork(requestNetwork.appContext)
        directRequestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        directRequestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            directUrl,
            "",
            object : RequestNetwork.RequestListener {
                override fun onResponse(
                    tag: String?,
                    response: String?,
                    responseHeaders: HashMap<String?, Any?>?
                ) {
                    val parsed = parseJioSaavnSearchResults(response)
                    if (!parsed.isNullOrEmpty()) {
                        listener.onResponse(tag, parsed, responseHeaders)
                    } else {
                        fallbackSearchSongs(query, page, limit, listener)
                    }
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                    fallbackSearchSongs(query, page, limit, listener)
                }
            }
        )
    }

    private fun fallbackSearchSongs(
        query: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["query"] = query
        if (page != null) queryMap["page"] = page
        if (limit != null) queryMap["limit"] = limit
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SEARCH_URL + SONGS,
            "",
            listener
        )
    }

    fun searchAlbums(
        query: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["query"] = query
        if (page != null) queryMap["page"] = page
        if (limit != null) queryMap["limit"] = limit
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SEARCH_URL + ALBUMS,
            "",
            listener
        )
    }

    fun searchArtists(
        query: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["query"] = query
        if (page != null) queryMap["page"] = page
        if (limit != null) queryMap["limit"] = limit
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SEARCH_URL + ARTISTS,
            "",
            listener
        )
    }

    fun searchPlaylists(
        query: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["query"] = query
        if (page != null) queryMap["page"] = page
        if (limit != null) queryMap["limit"] = limit
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SEARCH_URL + PLAYLISTS,
            "",
            listener
        )
    }

    fun retrieveSongsByIds(ids: String, listener: RequestNetwork.RequestListener?) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["ids"] = ids
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SONGS_URL,
            "",
            listener
        )
    }

    fun retrieveSongByLink(link: String, listener: RequestNetwork.RequestListener?) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["link"] = link
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SONGS_URL,
            "",
            listener
        )
    }

    fun retrieveSongById(id: String, lyrics: Boolean?, listener: RequestNetwork.RequestListener?) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["lyrics"] = lyrics
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            "$SONGS_URL/$id",
            "",
            listener
        )
    }

    fun retrieveLyricsById(id: String, listener: RequestNetwork.RequestListener?) {
        requestNetwork.setParams(HashMap(), RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET, "$SONGS_URL/$id/lyrics", "",
            listener
        )
    }

    fun retrieveSongSuggestions(
        id: String,
        limit: Int?,
        listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["limit"] = limit
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET, "$SONGS_URL/$id/suggestions", "",
            listener
        )
    }

    fun retrieveAlbumById(id: String, listener: RequestNetwork.RequestListener?) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["id"] = id
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            ALBUMS_URL,
            "",
            listener
        )
    }

    fun retrieveAlbumByLink(link: String, listener: RequestNetwork.RequestListener?) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["link"] = link
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            ALBUMS_URL,
            "",
            listener
        )
    }

    fun retrieveArtistsById(
        id: String, page: Int?, songCount: Int?, albumCount: Int?,
        sortBy: String?, sortOrder: String?, listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["id"] = id

        if (page != null) queryMap["page"] = page
        if (songCount != null) queryMap["songCount"] = songCount
        if (albumCount != null) queryMap["albumCount"] = albumCount
        if (sortBy != null) queryMap["sortBy"] = sortBy
        if (sortOrder != null) queryMap["sortOrder"] = sortOrder

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            ARTISTS_URL,
            "",
            listener
        )
    }

    fun retrieveArtistsByLink(
        link: String, page: Int?, songCount: Int?, albumCount: Int?,
        sortBy: String?, sortOrder: String?, listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["link"] = link

        if (page != null) queryMap["page"] = page
        if (songCount != null) queryMap["songCount"] = songCount
        if (albumCount != null) queryMap["albumCount"] = albumCount
        if (sortBy != null) queryMap["sortBy"] = sortBy

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            ARTISTS_URL,
            "",
            listener
        )
    }

    fun retrieveArtistById(
        id: String, page: Int?, songCount: Int?, albumCount: Int?,
        sortBy: SortBy?, sortOrder: SortOrder?, listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()

        if (page != null) queryMap["page"] = page
        if (songCount != null) queryMap["songCount"] = songCount
        if (albumCount != null) queryMap["albumCount"] = albumCount
        if (sortBy != null) queryMap["sortBy"] = sortBy.name
        if (sortOrder != null) queryMap["sortOrder"] = sortOrder.name

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            "$ARTISTS_URL/$id",
            "",
            listener
        )
    }

    fun retrieveArtistSongs(
        id: String, page: Int?, sortBy: SortBy?, sortOrder: SortOrder?,
        listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        if (page != null) queryMap["page"] = page
        if (sortBy != null) queryMap["sortBy"] = sortBy.name
        if (sortOrder != null) queryMap["sortOrder"] = sortOrder.name
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET, "$ARTISTS_URL/$id/songs", "",
            listener
        )
    }

    fun retrieveArtistAlbums(
        id: String, page: Int?, sortBy: SortBy?, sortOrder: SortOrder?,
        listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        if (page != null) queryMap["page"] = page
        if (sortBy != null) queryMap["sortBy"] = sortBy.name
        if (sortOrder != null) queryMap["sortOrder"] = sortOrder.name

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET, "$ARTISTS_URL/$id/albums", "",
            listener
        )
    }

    fun retrievePlaylistById(
        id: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener?
    ) {
        val directUrl = "https://www.jiosaavn.com/api.php"
        val queryMap = HashMap<String?, Any?>()
        queryMap["__call"] = "playlist.getDetails"
        queryMap["_format"] = "json"
        queryMap["_marker"] = "0"
        queryMap["api_version"] = "4"
        queryMap["ctx"] = "web6dot0"
        queryMap["listid"] = id
        if (page != null) queryMap["p"] = page
        if (limit != null) queryMap["n"] = limit

        val directRequestNetwork = RequestNetwork(requestNetwork.appContext)
        directRequestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        directRequestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            directUrl,
            "",
            object : RequestNetwork.RequestListener {
                override fun onResponse(
                    tag: String?,
                    response: String?,
                    responseHeaders: HashMap<String?, Any?>?
                ) {
                    val parsed = parseJioSaavnPlaylistResponse(response)
                    if (!parsed.isNullOrEmpty()) {
                        listener?.onResponse(tag, parsed, responseHeaders)
                    } else {
                        fallbackRetrievePlaylistById(id, page, limit, listener)
                    }
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                    fallbackRetrievePlaylistById(id, page, limit, listener)
                }
            }
        )
    }

    private fun fallbackRetrievePlaylistById(
        id: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["id"] = id
        if (page != null) queryMap["page"] = page
        if (limit != null) queryMap["limit"] = limit

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            PLAYLISTS_URL,
            "",
            listener
        )
    }

    fun retrievePlaylistByLink(
        link: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener?
    ) {
        val token = link.trimEnd('/').substringAfterLast('/')
        if (token.isNotBlank()) {
            val directUrl = "https://www.jiosaavn.com/api.php"
            val queryMap = HashMap<String?, Any?>()
            queryMap["__call"] = "webapi.get"
            queryMap["token"] = token
            queryMap["type"] = "playlist"
            queryMap["_format"] = "json"
            queryMap["_marker"] = "0"
            queryMap["api_version"] = "4"
            queryMap["ctx"] = "web6dot0"

            val directRequestNetwork = RequestNetwork(requestNetwork.appContext)
            directRequestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
            directRequestNetwork.startRequestNetwork(
                RequestNetworkController.GET,
                directUrl,
                "",
                object : RequestNetwork.RequestListener {
                    override fun onResponse(
                        tag: String?,
                        response: String?,
                        responseHeaders: HashMap<String?, Any?>?
                    ) {
                        try {
                            val root = JsonParser.parseString(response).asJsonObject
                            val listId = if (root.has("id") && !root.get("id").isJsonNull) root.get("id").asString else null
                            if (!listId.isNullOrEmpty()) {
                                retrievePlaylistById(listId, page, limit, listener)
                                return
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        fallbackRetrievePlaylistByLink(link, page, limit, listener)
                    }

                    override fun onErrorResponse(tag: String?, message: String?) {
                        fallbackRetrievePlaylistByLink(link, page, limit, listener)
                    }
                }
            )
        } else {
            fallbackRetrievePlaylistByLink(link, page, limit, listener)
        }
    }

    private fun fallbackRetrievePlaylistByLink(
        link: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["link"] = link
        if (page != null) queryMap["page"] = page
        if (limit != null) queryMap["limit"] = limit

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            PLAYLISTS_URL,
            "",
            listener
        )
    }

    fun retrieveArtistById(artistId: String, requestListener: RequestNetwork.RequestListener?) {
        retrieveArtistById(artistId, null, null, null, null, null, requestListener)
    }

    fun retrieveArtistSongs(
        artistId: String, page: Int, sortBy: SortBy?, sortOrder: SortOrder?,
        requestListener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["page"] = if (page == -1) 0 else page
        if (sortBy != null) queryMap["sortBy"] = sortBy.name
        if (sortOrder != null) queryMap["sortOrder"] = sortOrder.name

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            "$ARTISTS_URL/$artistId/songs", "", requestListener
        )
    }

    fun retrieveArtistSongs(artistId: String, requestListener: RequestNetwork.RequestListener?) {
        retrieveArtistSongs(artistId, 0, null, null, requestListener)
    }

    fun retrieveArtistAlbums(
        artistId: String, page: Int, sortBy: SortBy?, sortOrder: SortOrder?,
        requestListener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap["page"] = if (page == -1) 0 else page
        if (sortBy != null) queryMap["sortBy"] = sortBy.name
        if (sortOrder != null) queryMap["sortOrder"] = sortOrder.name

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            "$ARTISTS_URL/$artistId/albums", "", requestListener
        )
    }

    fun retrieveArtistAlbums(
        artistId: String,
        page: Int,
        requestListener: RequestNetwork.RequestListener?
    ) {
        retrieveArtistAlbums(artistId, page, null, null, requestListener)
    }

    enum class SortBy {
        popularity,
        latest,
        alphabetical
    }

    enum class SortOrder {
        asc,
        desc
    }

    companion object {
        private const val BASE_URL = "https://jiosaavn-api.uranophilelab.workers.dev/api/"
        private val SEARCH_URL: String = BASE_URL + "search"
        private const val SONGS = "/songs"
        private const val ALBUMS = "/albums"
        private const val ARTISTS = "/artists"
        private const val PLAYLISTS = "/playlists"
        private val SONGS_URL: String = BASE_URL + "songs"
        private val ALBUMS_URL: String = BASE_URL + "albums"
        private val ARTISTS_URL: String = BASE_URL + "artists"
        private val PLAYLISTS_URL: String = BASE_URL + "playlists"
        
        private const val DEFAULT_LANGUAGES = "hindi,english,punjabi,tamil,telugu,marathi,gujarati,bengali,kannada,bhojpuri,malayalam,urdu,haryanvi,rajasthani,odia,assamese"

        fun parseJioSaavnSearchResults(responseJson: String?): String? {
            if (responseJson.isNullOrEmpty()) return null
            try {
                val root = JsonParser.parseString(responseJson).asJsonObject
                val resultsArray = root.getAsJsonArray("results") ?: return null
                if (resultsArray.size() == 0) return null
                val total = root.get("total")?.asInt ?: resultsArray.size()
                val start = root.get("start")?.asInt ?: 1

                val convertedResults = mutableListOf<Map<String, Any?>>()

                for (elem in resultsArray) {
                    if (!elem.isJsonObject) continue
                    val obj = elem.asJsonObject
                    val id = obj.get("id")?.asString ?: continue
                    val title = obj.get("title")?.asString ?: obj.get("name")?.asString ?: ""
                    val subtitle = obj.get("subtitle")?.asString ?: ""
                    val year = obj.get("year")?.asString ?: ""
                    val language = obj.get("language")?.asString ?: ""
                    val rawImage = obj.get("image")?.asString ?: ""

                    val moreInfo = if (obj.has("more_info") && obj.get("more_info").isJsonObject) obj.getAsJsonObject("more_info") else null
                    val albumName = moreInfo?.get("album")?.asString ?: ""
                    val albumId = moreInfo?.get("album_id")?.asString ?: ""
                    val label = moreInfo?.get("label")?.asString ?: ""
                    val music = moreInfo?.get("music")?.asString ?: ""
                    val singers = moreInfo?.get("singers")?.asString ?: ""

                    val primaryArtistName = when {
                        singers.isNotBlank() -> singers
                        music.isNotBlank() -> music
                        subtitle.contains("-") -> subtitle.split("-")[0].trim()
                        subtitle.isNotBlank() -> subtitle
                        else -> ""
                    }

                    val image50 = if (rawImage.contains("150x150")) rawImage.replace("150x150", "50x50") else rawImage
                    val image150 = rawImage
                    val image500 = if (rawImage.contains("150x150")) rawImage.replace("150x150", "500x500") else rawImage

                    val songMap = mapOf(
                        "id" to id,
                        "name" to title,
                        "type" to "song",
                        "year" to year,
                        "language" to language,
                        "label" to label,
                        "explicitContent" to false,
                        "hasLyrics" to false,
                        "album" to mapOf(
                            "id" to albumId,
                            "name" to albumName,
                            "url" to ""
                        ),
                        "artists" to mapOf(
                            "primary" to listOf(
                                mapOf(
                                    "id" to "",
                                    "name" to primaryArtistName,
                                    "role" to "primary_artists",
                                    "type" to "artist",
                                    "url" to ""
                                )
                            )
                        ),
                        "image" to listOf(
                            mapOf("quality" to "50x50", "url" to image50),
                            mapOf("quality" to "150x150", "url" to image150),
                            mapOf("quality" to "500x500", "url" to image500)
                        )
                    )
                    convertedResults.add(songMap)
                }

                if (convertedResults.isEmpty()) return null

                val convertedMap = mapOf(
                    "success" to true,
                    "data" to mapOf(
                        "total" to total,
                        "start" to start,
                        "results" to convertedResults
                    )
                )

                return Gson().toJson(convertedMap)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        fun parseJioSaavnPlaylistResponse(responseJson: String?): String? {
            if (responseJson.isNullOrEmpty()) return null
            try {
                val root = JsonParser.parseString(responseJson).asJsonObject
                val listArray = if (root.has("list") && root.get("list").isJsonArray) {
                    root.getAsJsonArray("list")
                } else if (root.has("songs") && root.get("songs").isJsonArray) {
                    root.getAsJsonArray("songs")
                } else null

                if (listArray == null || listArray.size() == 0) return null

                val id = if (root.has("id") && !root.get("id").isJsonNull) root.get("id").asString else if (root.has("listid") && !root.get("listid").isJsonNull) root.get("listid").asString else ""
                val title = if (root.has("title") && !root.get("title").isJsonNull) root.get("title").asString else if (root.has("listname") && !root.get("listname").isJsonNull) root.get("listname").asString else ""
                val description = if (root.has("header_desc") && !root.get("header_desc").isJsonNull) root.get("header_desc").asString else if (root.has("subtitle") && !root.get("subtitle").isJsonNull) root.get("subtitle").asString else ""
                val permaUrl = if (root.has("perma_url") && !root.get("perma_url").isJsonNull) root.get("perma_url").asString else ""
                val rawImage = if (root.has("image") && !root.get("image").isJsonNull) root.get("image").asString else ""
                val language = if (root.has("language") && !root.get("language").isJsonNull) root.get("language").asString else ""
                val yearInt = try { root.get("year")?.asInt ?: 0 } catch (_: Exception) { 0 }
                val playCountInt = try { root.get("play_count")?.asInt ?: 0 } catch (_: Exception) { 0 }

                val image50 = if (rawImage.contains("150x150")) rawImage.replace("150x150", "50x50") else rawImage
                val image150 = rawImage
                val image500 = if (rawImage.contains("150x150")) rawImage.replace("150x150", "500x500") else rawImage

                val imagesList = listOf(
                    mapOf("quality" to "50x50", "url" to image50),
                    mapOf("quality" to "150x150", "url" to image150),
                    mapOf("quality" to "500x500", "url" to image500)
                )

                val convertedSongs = mutableListOf<Map<String, Any?>>()

                for (elem in listArray) {
                    if (!elem.isJsonObject) continue
                    val obj = elem.asJsonObject
                    val songId = if (obj.has("id") && !obj.get("id").isJsonNull) obj.get("id").asString else continue
                    val songTitle = if (obj.has("title") && !obj.get("title").isJsonNull) obj.get("title").asString else if (obj.has("song") && !obj.get("song").isJsonNull) obj.get("song").asString else ""
                    val songImage = if (obj.has("image") && !obj.get("image").isJsonNull) obj.get("image").asString else ""
                    val sImg50 = if (songImage.contains("150x150")) songImage.replace("150x150", "50x50") else songImage
                    val sImg150 = songImage
                    val sImg500 = if (songImage.contains("150x150")) songImage.replace("150x150", "500x500") else songImage

                    val songYear = if (obj.has("year") && !obj.get("year").isJsonNull) obj.get("year").asString else ""
                    val songLanguage = if (obj.has("language") && !obj.get("language").isJsonNull) obj.get("language").asString else ""
                    val songPermaUrl = if (obj.has("perma_url") && !obj.get("perma_url").isJsonNull) obj.get("perma_url").asString else ""
                    val songPlayCount = try { obj.get("play_count")?.asInt ?: 0 } catch (e: Exception) { 0 }

                    val moreInfo = if (obj.has("more_info") && obj.get("more_info").isJsonObject) obj.getAsJsonObject("more_info") else null
                    val albumName = if (moreInfo != null && moreInfo.has("album") && !moreInfo.get("album").isJsonNull) moreInfo.get("album").asString else ""
                    val albumId = if (moreInfo != null && moreInfo.has("album_id") && !moreInfo.get("album_id").isJsonNull) moreInfo.get("album_id").asString else ""
                    val albumUrl = if (moreInfo != null && moreInfo.has("album_url") && !moreInfo.get("album_url").isJsonNull) moreInfo.get("album_url").asString else ""
                    val label = if (moreInfo != null && moreInfo.has("label") && !moreInfo.get("label").isJsonNull) moreInfo.get("label").asString else ""
                    val releaseDate = if (moreInfo != null && moreInfo.has("release_date") && !moreInfo.get("release_date").isJsonNull) moreInfo.get("release_date").asString else ""
                    val copyright = if (moreInfo != null && moreInfo.has("copyright_text") && !moreInfo.get("copyright_text").isJsonNull) moreInfo.get("copyright_text").asString else ""
                    val durationStr = if (moreInfo != null && moreInfo.has("duration") && !moreInfo.get("duration").isJsonNull) moreInfo.get("duration").asString else "0"
                    val durationVal = durationStr.toDoubleOrNull() ?: 0.0

                    val primaryArtistsList = mutableListOf<Map<String, Any?>>()
                    if (moreInfo != null && moreInfo.has("artistMap") && moreInfo.get("artistMap").isJsonObject) {
                        val artistMap = moreInfo.getAsJsonObject("artistMap")
                        if (artistMap.has("primary_artists") && artistMap.get("primary_artists").isJsonArray) {
                            for (pa in artistMap.getAsJsonArray("primary_artists")) {
                                if (!pa.isJsonObject) continue
                                val paObj = pa.asJsonObject
                                val paId = if (paObj.has("id") && !paObj.get("id").isJsonNull) paObj.get("id").asString else ""
                                val paName = if (paObj.has("name") && !paObj.get("name").isJsonNull) paObj.get("name").asString else ""
                                val paRole = if (paObj.has("role") && !paObj.get("role").isJsonNull) paObj.get("role").asString else "primary_artists"
                                val paType = if (paObj.has("type") && !paObj.get("type").isJsonNull) paObj.get("type").asString else "artist"
                                val paUrl = if (paObj.has("perma_url") && !paObj.get("perma_url").isJsonNull) paObj.get("perma_url").asString else ""
                                primaryArtistsList.add(
                                    mapOf(
                                        "id" to paId,
                                        "name" to paName,
                                        "role" to paRole,
                                        "type" to paType,
                                        "image" to emptyList<Any>(),
                                        "url" to paUrl
                                    )
                                )
                            }
                        }
                    }

                    if (primaryArtistsList.isEmpty()) {
                        val singers = if (obj.has("subtitle") && !obj.get("subtitle").isJsonNull) obj.get("subtitle").asString else if (moreInfo != null && moreInfo.has("singers") && !moreInfo.get("singers").isJsonNull) moreInfo.get("singers").asString else ""
                        primaryArtistsList.add(
                            mapOf(
                                "id" to "",
                                "name" to singers,
                                "role" to "primary_artists",
                                "type" to "artist",
                                "image" to emptyList<Any>(),
                                "url" to ""
                            )
                        )
                    }

                    val songMap = mapOf(
                        "id" to songId,
                        "name" to songTitle,
                        "type" to "song",
                        "year" to songYear,
                        "releaseDate" to releaseDate,
                        "duration" to durationVal,
                        "label" to label,
                        "explicitContent" to false,
                        "playCount" to songPlayCount,
                        "language" to songLanguage,
                        "hasLyrics" to false,
                        "lyricsId" to null,
                        "lyrics" to null,
                        "url" to songPermaUrl,
                        "copyright" to copyright,
                        "album" to mapOf(
                            "id" to albumId,
                            "name" to albumName,
                            "url" to albumUrl
                        ),
                        "artists" to mapOf(
                            "primary" to primaryArtistsList,
                            "featured" to emptyList<Any>(),
                            "all" to primaryArtistsList
                        ),
                        "image" to listOf(
                            mapOf("quality" to "50x50", "url" to sImg50),
                            mapOf("quality" to "150x150", "url" to sImg150),
                            mapOf("quality" to "500x500", "url" to sImg500)
                        ),
                        "downloadUrl" to emptyList<Any>()
                    )

                    convertedSongs.add(songMap)
                }

                if (convertedSongs.isEmpty()) return null

                val convertedMap = mapOf(
                    "success" to true,
                    "data" to mapOf(
                        "id" to id,
                        "name" to title,
                        "url" to permaUrl,
                        "description" to description,
                        "type" to "playlist",
                        "year" to yearInt,
                        "playCount" to playCountInt,
                        "songCount" to convertedSongs.size,
                        "language" to language,
                        "explicitContent" to false,
                        "artists" to emptyList<Any>(),
                        "image" to imagesList,
                        "songs" to convertedSongs
                    )
                )

                return Gson().toJson(convertedMap)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }
}
