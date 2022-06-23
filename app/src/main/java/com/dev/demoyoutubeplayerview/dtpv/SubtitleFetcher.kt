package com.dev.demoyoutubeplayerview.dtpv

import android.net.Uri
import android.widget.Toast
import com.dev.demoyoutubeplayerview.BuildConfig
import com.dev.demoyoutubeplayerview.PlayerActivity
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.mPrefs
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.CountDownLatch

internal class SubtitleFetcher(activity: PlayerActivity, urls: List<Uri>) {
    private val activity: PlayerActivity
    private var countDownLatch: CountDownLatch? = null
    private val urls: List<Uri>
    private var subtitleUri: Uri? = null
    private val foundUrls: MutableList<Uri>
    fun start() {
        Thread(label@ Runnable {
            var client: OkHttpClient = OkHttpClient.Builder() //.callTimeout(15, TimeUnit.SECONDS)
                .build()
            val callback: Callback = object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    countDownLatch!!.countDown()
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    val url =
                        Uri.parse(response.request.url.toString())
                    Utils.log(response.code.toString() + ": " + url)
                    if (response.isSuccessful) {
                        foundUrls.add(url)
                    }
                    response.close()
                    countDownLatch!!.countDown()
                }
            }
            countDownLatch = CountDownLatch(urls.size)
            for (url in urls) {
                // Total Commander 3.24 / LAN plugin 3.20 does not support HTTP HEAD
                //Request request = new Request.Builder().url(url.toString()).head().build();
                if (url.toString().toHttpUrlOrNull() == null) {
                    countDownLatch!!.countDown()
                    continue
                }
                val request: Request = Request.Builder().url(url.toString()).build()
                client.newCall(request).enqueue(callback)
            }
            try {
                countDownLatch!!.await()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            for (url in urls) {
                if (foundUrls.contains(url)) {
                    subtitleUri = url
                    break
                }
            }
            if (subtitleUri == null) {
                return@Runnable
            }
            Utils.log(subtitleUri.toString())

            // ProtocolException when reusing client:
            // java.net.ProtocolException: Unexpected status line: 1
            client = OkHttpClient.Builder() //.callTimeout(15, TimeUnit.SECONDS)
                .build()
            val request: Request = Request.Builder().url(subtitleUri.toString()).build()
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body
                    if (responseBody == null || responseBody.contentLength() > 2000000) {
                        return@Runnable
                    }
                    val inputStream = responseBody.byteStream()
                    val convertedSubtitleUri: Uri =
                        SubtitleUtils.convertInputStreamToUTF(activity, subtitleUri, inputStream)
                            ?: return@Runnable
                    activity.runOnUiThread {
                        mPrefs?.updateSubtitle(convertedSubtitleUri)
                        if (PlayerActivity.player != null) {
                            var mediaItem: MediaItem =
                                PlayerActivity.player?.getCurrentMediaItem()!!
                            if (mediaItem != null) {
                                val subtitle: SubtitleConfiguration =
                                    SubtitleUtils.buildSubtitle(
                                        activity,
                                        convertedSubtitleUri,
                                        null,
                                        true
                                    )
                                mediaItem =
                                    mediaItem.buildUpon()
                                        .setSubtitleConfigurations(listOf(subtitle))
                                        .build()
                                PlayerActivity.player!!.setMediaItem(mediaItem, false)
                                if (BuildConfig.DEBUG) {
                                    Toast.makeText(
                                        activity,
                                        "Subtitle found",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Utils.log(e.toString())
                e.printStackTrace()
            }
        }).start()
    }

    init {
        this.activity = activity
        this.urls = urls
        foundUrls = ArrayList()
    }
}
