package com.dev.demoyoutubeplayerview

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.preference.PreferenceManager
import com.dev.demoyoutubeplayerview.dtpv.SubtitleUtils
import com.dev.demoyoutubeplayerview.dtpv.Utils
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.Exception
import java.util.LinkedHashMap

class Prefs(val mContext: Context) {
    val mSharedPreferences: SharedPreferences
    var mediaUri: Uri? = null
    var subtitleUri: Uri? = null
    var scopeUri: Uri? = null
    var mediaType: String? = null
    var resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    var orientation: Utils.Orientation = Utils.Orientation.UNSPECIFIED
    var scale = 1f
    var speed = 1f
    var subtitleTrackId: String? = null
    var audioTrackId: String? = null
    var brightness = -1
    var firstRun = true
    var askScope = true
    var autoPiP = false
    var tunneling = false
    var skipSilence = false
    var frameRateMatching = false
    var repeatToggle = false
    var fileAccess: String? = "auto"
    var decoderPriority = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
    private var positions: LinkedHashMap<String, String>? = null
    var persistentMode = true
    var nonPersitentPosition = -1L
    private fun loadSavedPreferences() {
        if (mSharedPreferences.contains(PREF_KEY_MEDIA_URI)) mediaUri = Uri.parse(
            mSharedPreferences.getString(
                PREF_KEY_MEDIA_URI, null
            )
        )
        if (mSharedPreferences.contains(PREF_KEY_MEDIA_TYPE)) mediaType =
            mSharedPreferences.getString(
                PREF_KEY_MEDIA_TYPE, null
            )
        brightness = mSharedPreferences.getInt(PREF_KEY_BRIGHTNESS, brightness)
        firstRun = mSharedPreferences.getBoolean(PREF_KEY_FIRST_RUN, firstRun)
        if (mSharedPreferences.contains(PREF_KEY_SUBTITLE_URI)) subtitleUri = Uri.parse(
            mSharedPreferences.getString(
                PREF_KEY_SUBTITLE_URI, null
            )
        )
        if (mSharedPreferences.contains(PREF_KEY_AUDIO_TRACK_ID)) audioTrackId =
            mSharedPreferences.getString(
                PREF_KEY_AUDIO_TRACK_ID, audioTrackId
            )
        if (mSharedPreferences.contains(PREF_KEY_SUBTITLE_TRACK_ID)) subtitleTrackId =
            mSharedPreferences.getString(
                PREF_KEY_SUBTITLE_TRACK_ID, subtitleTrackId
            )
        if (mSharedPreferences.contains(PREF_KEY_RESIZE_MODE)) resizeMode =
            mSharedPreferences.getInt(
                PREF_KEY_RESIZE_MODE, resizeMode
            )
        orientation = Utils.Orientation.values()
            .get(mSharedPreferences.getInt(PREF_KEY_ORIENTATION, orientation.value))
        scale = mSharedPreferences.getFloat(PREF_KEY_SCALE, scale)
        if (mSharedPreferences.contains(PREF_KEY_SCOPE_URI)) scopeUri = Uri.parse(
            mSharedPreferences.getString(
                PREF_KEY_SCOPE_URI, null
            )
        )
        askScope = mSharedPreferences.getBoolean(PREF_KEY_ASK_SCOPE, askScope)
        speed = mSharedPreferences.getFloat(PREF_KEY_SPEED, speed)
        loadUserPreferences()
    }

    fun loadUserPreferences() {
        autoPiP = mSharedPreferences.getBoolean(PREF_KEY_AUTO_PIP, autoPiP)
        tunneling = mSharedPreferences.getBoolean(PREF_KEY_TUNNELING, tunneling)
        skipSilence = mSharedPreferences.getBoolean(PREF_KEY_SKIP_SILENCE, skipSilence)
        frameRateMatching =
            mSharedPreferences.getBoolean(PREF_KEY_FRAMERATE_MATCHING, frameRateMatching)
        repeatToggle = mSharedPreferences.getBoolean(PREF_KEY_REPEAT_TOGGLE, repeatToggle)
        fileAccess = mSharedPreferences.getString(PREF_KEY_FILE_ACCESS, fileAccess)
        decoderPriority =
            mSharedPreferences.getString(PREF_KEY_DECODER_PRIORITY, decoderPriority.toString())!!
                .toInt()
    }

    fun updateMedia(context: Context, uri: Uri?, type: String?) {
        mediaUri = uri
        mediaType = type
        updateSubtitle(null)
        updateMeta(null, null, AspectRatioFrameLayout.RESIZE_MODE_FIT, 1f, 1f)
        if (mediaType != null && mediaType!!.endsWith("/*")) {
            mediaType = null
        }
        if (mediaType == null) {
            if (ContentResolver.SCHEME_CONTENT == mediaUri!!.scheme) {
                mediaType = context.contentResolver.getType(mediaUri!!)
            }
        }
        if (persistentMode) {
            val sharedPreferencesEditor = mSharedPreferences.edit()
            if (mediaUri == null) sharedPreferencesEditor.remove(PREF_KEY_MEDIA_URI) else sharedPreferencesEditor.putString(
                PREF_KEY_MEDIA_URI, mediaUri.toString()
            )
            if (mediaType == null) sharedPreferencesEditor.remove(PREF_KEY_MEDIA_TYPE) else sharedPreferencesEditor.putString(
                PREF_KEY_MEDIA_TYPE, mediaType
            )
            sharedPreferencesEditor.commit()
        }
    }

    fun updateSubtitle(uri: Uri?) {
        subtitleUri = uri
        subtitleTrackId = null
        if (persistentMode) {
            val sharedPreferencesEditor = mSharedPreferences.edit()
            if (uri == null) sharedPreferencesEditor.remove(PREF_KEY_SUBTITLE_URI) else sharedPreferencesEditor.putString(
                PREF_KEY_SUBTITLE_URI, uri.toString()
            )
            sharedPreferencesEditor.remove(PREF_KEY_SUBTITLE_TRACK_ID)
            sharedPreferencesEditor.commit()
        }
    }

    fun updatePosition(position: Long) {
        if (mediaUri == null) return
        while (positions!!.size > 100) positions!!.remove(positions!!.keys.toTypedArray()[0])
        if (persistentMode) {
            positions!![mediaUri.toString()] = position.toString()
            savePositions()
        } else {
            nonPersitentPosition = position
        }
    }

    fun updateBrightness(brightness: Int) {
        if (brightness >= -1) {
            this.brightness = brightness
            val sharedPreferencesEditor = mSharedPreferences.edit()
            sharedPreferencesEditor.putInt(PREF_KEY_BRIGHTNESS, brightness)
            sharedPreferencesEditor.commit()
        }
    }

    fun markFirstRun() {
        firstRun = false
        val sharedPreferencesEditor = mSharedPreferences.edit()
        sharedPreferencesEditor.putBoolean(PREF_KEY_FIRST_RUN, false)
        sharedPreferencesEditor.commit()
    }

    fun markScopeAsked() {
        askScope = false
        val sharedPreferencesEditor = mSharedPreferences.edit()
        sharedPreferencesEditor.putBoolean(PREF_KEY_ASK_SCOPE, false)
        sharedPreferencesEditor.commit()
    }

    private fun savePositions() {
        try {
            val fos = mContext.openFileOutput("positions", Context.MODE_PRIVATE)
            val os = ObjectOutputStream(fos)
            os.writeObject(positions)
            os.close()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadPositions() {
        try {
            val fis = mContext.openFileInput("positions")
            val `is` = ObjectInputStream(fis)
            positions = `is`.readObject() as LinkedHashMap<String, String>
            `is`.close()
            fis.close()
        } catch (e: Exception) {
            e.printStackTrace()
            positions = LinkedHashMap<String, String>(10)
        }
    }

    // Return position for uri from limited scope (loaded after using Next action)
    val position: Long
        get() {
            if (!persistentMode) {
                return nonPersitentPosition
            }
            val `val` = positions!![mediaUri.toString()]
            if (`val` != null) return `val` as Long

            // Return position for uri from limited scope (loaded after using Next action)
            if (ContentResolver.SCHEME_CONTENT == mediaUri!!.scheme) {
                val searchPath: String = SubtitleUtils.getTrailPathFromUri(mediaUri!!)!!
                if (searchPath == null || searchPath.length < 1) return 0L
                val keySet: Set<String> = positions!!.keys
                val keys: Array<Any> = keySet.toTypedArray()
                for (i in keys.size downTo 1) {
                    val key = keys[i - 1] as String
                    val uri = Uri.parse(key)
                    if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
                        val keyPath: String = SubtitleUtils.getTrailPathFromUri(uri)!!
                        if (searchPath == keyPath) {
                            return positions!![key] as Long
                        }
                    }
                }
            }
            return 0L
        }

    fun updateOrientation() {
        val sharedPreferencesEditor = mSharedPreferences.edit()
        sharedPreferencesEditor.putInt(PREF_KEY_ORIENTATION, orientation.value)
        sharedPreferencesEditor.commit()
    }

    fun updateMeta(
        audioTrackId: String?,
        subtitleTrackId: String?,
        resizeMode: Int,
        scale: Float,
        speed: Float
    ) {
        this.audioTrackId = audioTrackId
        this.subtitleTrackId = subtitleTrackId
        this.resizeMode = resizeMode
        this.scale = scale
        this.speed = speed
        if (persistentMode) {
            val sharedPreferencesEditor = mSharedPreferences.edit()
            if (audioTrackId == null) sharedPreferencesEditor.remove(PREF_KEY_AUDIO_TRACK_ID) else sharedPreferencesEditor.putString(
                PREF_KEY_AUDIO_TRACK_ID, audioTrackId
            )
            if (subtitleTrackId == null) sharedPreferencesEditor.remove(PREF_KEY_SUBTITLE_TRACK_ID) else sharedPreferencesEditor.putString(
                PREF_KEY_SUBTITLE_TRACK_ID, subtitleTrackId
            )
            sharedPreferencesEditor.putInt(PREF_KEY_RESIZE_MODE, resizeMode)
            sharedPreferencesEditor.putFloat(PREF_KEY_SCALE, scale)
            sharedPreferencesEditor.putFloat(PREF_KEY_SPEED, speed)
            sharedPreferencesEditor.commit()
        }
    }

    fun updateScope(uri: Uri?) {
        scopeUri = uri
        val sharedPreferencesEditor = mSharedPreferences.edit()
        if (uri == null) sharedPreferencesEditor.remove(PREF_KEY_SCOPE_URI) else sharedPreferencesEditor.putString(
            PREF_KEY_SCOPE_URI, uri.toString()
        )
        sharedPreferencesEditor.commit()
    }

    fun setPersistent(persistentMode: Boolean) {
        this.persistentMode = persistentMode
    }

    companion object {
        // Previously used
        // private static final String PREF_KEY_AUDIO_TRACK = "audioTrack";
        // private static final String PREF_KEY_AUDIO_TRACK_FFMPEG = "audioTrackFfmpeg";
        // private static final String PREF_KEY_SUBTITLE_TRACK = "subtitleTrack";
        private const val PREF_KEY_MEDIA_URI = "mediaUri"
        private const val PREF_KEY_MEDIA_TYPE = "mediaType"
        private const val PREF_KEY_BRIGHTNESS = "brightness"
        private const val PREF_KEY_FIRST_RUN = "firstRun"
        private const val PREF_KEY_SUBTITLE_URI = "subtitleUri"
        private const val PREF_KEY_AUDIO_TRACK_ID = "audioTrackId"
        private const val PREF_KEY_SUBTITLE_TRACK_ID = "subtitleTrackId"
        private const val PREF_KEY_RESIZE_MODE = "resizeMode"
        private const val PREF_KEY_ORIENTATION = "orientation"
        private const val PREF_KEY_SCALE = "scale"
        private const val PREF_KEY_SCOPE_URI = "scopeUri"
        private const val PREF_KEY_ASK_SCOPE = "askScope"
        private const val PREF_KEY_AUTO_PIP = "autoPiP"
        private const val PREF_KEY_TUNNELING = "tunneling"
        private const val PREF_KEY_SKIP_SILENCE = "skipSilence"
        private const val PREF_KEY_FRAMERATE_MATCHING = "frameRateMatching"
        private const val PREF_KEY_REPEAT_TOGGLE = "repeatToggle"
        private const val PREF_KEY_SPEED = "speed"
        private const val PREF_KEY_FILE_ACCESS = "fileAccess"
        private const val PREF_KEY_DECODER_PRIORITY = "decoderPriority"
    }

    init {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            mContext
        )
        loadSavedPreferences()
        loadPositions()
    }
}