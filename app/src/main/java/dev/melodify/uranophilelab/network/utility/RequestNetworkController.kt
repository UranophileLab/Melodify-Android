package dev.melodify.uranophilelab.network.utility

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class RequestNetworkController {
    protected var client: OkHttpClient? = null

    private fun getOkHttpClient(): OkHttpClient {
        if (client == null) {
            client = OkHttpClient.Builder()
                .connectTimeout(SOCKET_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                .writeTimeout(READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                .build()
        }
        return client!!
    }

    fun execute(
        requestNetwork: RequestNetwork, method: String, url: String, tag: String?,
        requestListener: RequestNetwork.RequestListener
    ) {
        val reqBuilder = Request.Builder()
        val headerBuilder = Headers.Builder()

        val headers = requestNetwork.headers
        if (headers != null && !headers.isEmpty()) {
            for (key in headers.keys) {
                if (key != null) {
                    val value = headers[key]
                    if (value != null) {
                        headerBuilder.add(key, value.toString())
                    }
                }
            }
        }

        try {
            if (requestNetwork.requestType == REQUEST_PARAM) {
                if (method == GET) {
                    val httpBuilder: HttpUrl.Builder?

                    try {
                        httpBuilder = url.toHttpUrl().newBuilder()
                    } catch (ne: NullPointerException) {
                        throw NullPointerException("unexpected url: " + url)
                    }

                    val params = requestNetwork.params
                    if (params != null && !params.isEmpty()) {
                        for (key in params.keys) {
                            if (key != null) {
                                val value = params[key]
                                if (value != null) {
                                    httpBuilder.addQueryParameter(key, value.toString())
                                }
                            }
                        }
                    }

                    reqBuilder.url(httpBuilder.build()).headers(headerBuilder.build()).get()
                } else {
                    val formBuilder = FormBody.Builder()
                    val params = requestNetwork.params
                    if (params != null && !params.isEmpty()) {
                        for (key in params.keys) {
                            if (key != null) {
                                val value = params[key]
                                if (value != null) {
                                    formBuilder.add(key, value.toString())
                                }
                            }
                        }
                    }

                    val reqBody: RequestBody = formBuilder.build()

                    reqBuilder.url(url).headers(headerBuilder.build()).method(method, reqBody)
                }
            } else {
                val reqBody = RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    Gson().toJson(requestNetwork.params)
                )

                if (method == GET) {
                    reqBuilder.url(url).headers(headerBuilder.build()).get()
                } else {
                    reqBuilder.url(url).headers(headerBuilder.build()).method(method, reqBody)
                }
            }

            val req = reqBuilder.build()

            getOkHttpClient().newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Handler(Looper.getMainLooper()).post(Runnable {
                        requestListener.onErrorResponse(tag, e.message)
                    })
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()?.trim { it <= ' ' }
                    Handler(Looper.getMainLooper()).post(Runnable {
                        try {
                            val b = response.headers
                            val map = HashMap<String?, Any?>()
                            for (s in b.names()) {
                                map.put(s, if (b.get(s) != null) b.get(s) else "null")
                            }
                            requestListener.onResponse(tag, responseBody, map)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    })
                }
            })
        } catch (e: Exception) {
            requestListener.onErrorResponse(tag, e.message)
        }
    }

    companion object {
        const val GET: String = "GET"
        const val POST: String = "POST"
        const val PUT: String = "PUT"
        const val DELETE: String = "DELETE"

        const val REQUEST_PARAM: Int = 0
        const val REQUEST_BODY: Int = 1

        private const val SOCKET_TIMEOUT = 15000
        private const val READ_TIMEOUT = 25000

        private var mInstance: RequestNetworkController? = null

        @get:Synchronized
        val instance: RequestNetworkController
            get() {
                if (mInstance == null) {
                    mInstance = RequestNetworkController()
                }
                return mInstance!!
            }
    }
}
