package com.dev.demoyoutubeplayerview.dtpv

import android.net.Uri
import com.google.android.exoplayer2.util.Util
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.ArrayList

class SubtitleFinder(activity: PlayerActivity, uri: Uri) {
    private val activity: PlayerActivity
    private val baseUri: Uri
    private var path: String?
    private val urls: MutableList<Uri>
    private fun addLanguage(lang: String, suffix: String) {
        urls.add(buildUri("$lang.$suffix"))
        urls.add(buildUri(Util.normalizeLanguageCode(lang) + "." + suffix))
    }

    private fun buildUri(suffix: String): Uri {
        val newPath = "$path.$suffix"
        return baseUri.buildUpon().path(newPath).build()
    }

    fun start() {
        // Prevent IllegalArgumentException in okhttp3.Request.Builder
        if (baseUri.toString().toHttpUrlOrNull() == null) {
            return
        }
        for (suffix in arrayOf("srt", "ssa", "ass")) {
            urls.add(buildUri(suffix))
            for (language in Utils.deviceLanguages) {
                addLanguage(language, suffix)
            }
        }
        urls.add(buildUri("vtt"))
        val subtitleFetcher = SubtitleFetcher(activity, urls)
        subtitleFetcher.start()
    }

    companion object {
        fun isUriCompatible(uri: Uri): Boolean {
            val pth = uri.path
            return if (pth != null) {
                pth.lastIndexOf('.') > -1
            } else false
        }
    }

    init {
        this.activity = activity
        path = uri.path
        path = path!!.substring(0, path!!.lastIndexOf('.'))
        baseUri = uri
        urls = ArrayList()
    }
}
