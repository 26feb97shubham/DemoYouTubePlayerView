package com.dev.demoyoutubeplayerview

import android.content.pm.PackageManager.FEATURE_EXPANDED_PICTURE_IN_PICTURE
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.*
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.DocumentsContract
import android.provider.Settings
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Base64
import android.util.Rational
import android.view.SurfaceView
import android.view.View
import android.view.WindowInsetsController
import android.view.accessibility.CaptioningManager
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.documentfile.provider.DocumentFile
import com.arthenica.ffmpegkit.Packages.getPackageName
import com.dev.demoyoutubeplayerview.dtpv.*
import com.dev.demoyoutubeplayerview.dtpv.Utils
import com.dev.demoyoutubeplayerview.dtpv.youtube.YouTubeOverlay
import com.dev.demoyoutubeplayerview.player.PlayerListener
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory
import com.google.android.exoplayer2.extractor.ts.TsExtractor
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import com.google.android.exoplayer2.ui.*
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.lang.reflect.InvocationTargetException
import java.util.ArrayList
import java.util.HashMap
import kotlin.math.roundToInt


class PlayerActivity : Activity() {
    companion object{
         val playerListener: PlayerListener? = null
         val mReceiver: BroadcastReceiver? = null
         val mAudioManager: AudioManager? = null
         var mediaSession: MediaSessionCompat? = null

         var trackSelector: DefaultTrackSelector? = null
        var loudnessEnhancer: LoudnessEnhancer? = null

        var playerView: CustomStyledPlayerView? = null
        var player: ExoPlayer? = null
         val youTubeOverlay: YouTubeOverlay? = null

         val mPictureInPictureParamsBuilder: PictureInPictureParams.Builder? = null

        var mPrefs: Prefs? = null
        var mBrightnessControl: BrightnessControl? = null
        var haveMedia = false
         var videoLoading = false
        var controllerVisible = false
        var controllerVisibleFully = false
        var snackbar: Snackbar? = null
         var errorToShow: ExoPlaybackException? = null
        var boostLevel = 0
         var isScaling = false
         var isScaleStarting = false
         var scaleFactor = 1.0f

         val REQUEST_CHOOSER_VIDEO = 1
         val REQUEST_CHOOSER_SUBTITLE = 2
         val REQUEST_CHOOSER_SCOPE_DIR = 10
         val REQUEST_CHOOSER_VIDEO_MEDIASTORE = 20
         val REQUEST_CHOOSER_SUBTITLE_MEDIASTORE = 21
         val REQUEST_SETTINGS = 100
         val REQUEST_SYSTEM_CAPTIONS = 200
        val CONTROLLER_TIMEOUT = 3500
         val ACTION_MEDIA_CONTROL = "media_control"
         val EXTRA_CONTROL_TYPE = "control_type"
         val REQUEST_PLAY = 1
         val REQUEST_PAUSE = 2
         val CONTROL_TYPE_PLAY = 1
         val CONTROL_TYPE_PAUSE = 2

         val coordinatorLayout: CoordinatorLayout? = null
         val titleView: TextView? = null
         val buttonOpen: ImageButton? = null
         val buttonPiP: ImageButton? = null
         val buttonAspectRatio: ImageButton? = null
         val buttonRotation: ImageButton? = null
         val exoSettings: ImageButton? = null
         val exoPlayPause: ImageButton? = null
         val loadingProgressBar: ProgressBar? = null
         val controlView: StyledPlayerControlView? = null
         val timeBar: CustomDefaultTimeBar? = null

         var restoreOrientationLock = false
         var restorePlayState = false
         val restorePlayStateAllowed = false
         var play = false
         var subtitlesScale = 0f
         val isScrubbing = false
         var scrubbingNoticeable = false
         val scrubbingStart: Long = 0
        var frameRendered = false
         val alive = false
        var focusPlay = false
         var nextUri: Uri? = null
         var isTvBox = false
        var locked = false
         var nextUriThread: Thread? = null
        var frameRateSwitchThread: Thread? = null
        var chaptersThread: Thread? = null
         val lastScrubbingPosition: Long = 0
        lateinit var chapterStarts: LongArray

        var restoreControllerTimeout = false
        var shortControllerTimeout = false
        var mContext : PlayerActivity?=null
        var mActivity : Activity?=null

        val rationalLimitWide = Rational(239, 100)
        val rationalLimitTall = Rational(100, 239)

        val API_POSITION = "position"
        val API_DURATION = "duration"
        val API_RETURN_RESULT = "return_result"
        val API_SUBS = "subs"
        val API_SUBS_ENABLE = "subs.enable"
        val API_SUBS_NAME = "subs.name"
        val API_TITLE = "title"
        val API_END_BY = "end_by"
        var apiAccess = false
        var apiTitle: String? = null
        var apiSubs: ArrayList<SubtitleConfiguration> = ArrayList()
        var intentReturnResult = false
        var playbackFinished = false

        var displayManager: DisplayManager? = null
        var displayListener: DisplayManager.DisplayListener? = null
        var subtitleFinder: SubtitleFinder? = null

        fun notifyAudioSessionUpdate(active: Boolean) {
            val intent =
                Intent(if (active) AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION else AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player!!.audioSessionId)
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName())
            if (active) {
                intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MOVIE)
            }
            try {
                mContext!!.sendBroadcast(intent)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        @TargetApi(26)
        fun updatePictureInPictureActions(
            iconId: Int,
            resTitle: Int,
            controlType: Int,
            requestCode: Int
        ): Boolean {
            try {
                val actions = ArrayList<RemoteAction>()
                val intent = PendingIntent.getBroadcast(
                    mContext,
                    requestCode,
                    Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlType),
                    PendingIntent.FLAG_IMMUTABLE
                )
                val icon = Icon.createWithResource(mContext, iconId)
                val title = mContext!!.getString(resTitle)
                actions.add(RemoteAction(icon, title, title, intent))
                (mPictureInPictureParamsBuilder as PictureInPictureParams.Builder).setActions(actions)
                mContext!!.setPictureInPictureParams(mPictureInPictureParamsBuilder.build())
                return true
            } catch (e: IllegalStateException) {
                // On Samsung devices with Talkback active:
                // Caused by: java.lang.IllegalStateException: setPictureInPictureParams: Device doesn't support picture-in-picture mode.
                e.printStackTrace()
            }
            return false
        }


        fun updateLoading(enableLoading: Boolean) {
            if (enableLoading) {
                exoPlayPause!!.visibility = View.GONE
                loadingProgressBar!!.visibility = View.VISIBLE
            } else {
                loadingProgressBar!!.visibility = View.GONE
                exoPlayPause!!.visibility = View.VISIBLE
                if (focusPlay) {
                    focusPlay = false
                    exoPlayPause.requestFocus()
                }
            }
        }

        fun showError(error: ExoPlaybackException) {
            val errorGeneral = error.localizedMessage
            val errorDetailed: String
            errorDetailed = when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> error.sourceException.localizedMessage
                ExoPlaybackException.TYPE_RENDERER -> error.rendererException.localizedMessage
                ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException.localizedMessage
                ExoPlaybackException.TYPE_REMOTE -> errorGeneral
                else -> errorGeneral
            }
            showSnack(errorGeneral, errorDetailed)
        }

        fun showSnack(textPrimary: String?, textSecondary: String?) {
            snackbar = Snackbar.make(coordinatorLayout!!, textPrimary!!, Snackbar.LENGTH_LONG)
            if (textSecondary != null) {
                snackbar!!.setAction(R.string.error_details) { v ->
                    val builder =
                        AlertDialog.Builder(mContext)
                    builder.setMessage(textSecondary)
                    builder.setPositiveButton(
                        android.R.string.ok
                    ) { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() }
                    val dialog = builder.create()
                    dialog.show()
                }
            }
            snackbar!!.setAnchorView(R.id.exo_bottom_bar)
            snackbar!!.show()
        }

        fun setSelectedTracks(subtitleId: String, audioId: String?) {
            if ("#none" == subtitleId) {
                if (trackSelector == null) {
                    return
                }
                trackSelector?.setParameters(
                    trackSelector?.buildUponParameters()!!.setDisabledTextTrackSelectionFlags(C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_FORCED)
                )
            }
            val subtitleGroup = getTrackGroupFromFormatId(C.TRACK_TYPE_TEXT, subtitleId)
            val audioGroup = getTrackGroupFromFormatId(C.TRACK_TYPE_AUDIO, audioId)
            val overridesBuilder = TrackSelectionParameters.Builder(mContext!!)
            var trackSelectionOverride: TrackSelectionOverride? = null
            val tracks: MutableList<Int> = ArrayList()
            tracks.add(0)
            if (subtitleGroup != null) {
                trackSelectionOverride = TrackSelectionOverride(subtitleGroup, tracks)
                overridesBuilder.addOverride(trackSelectionOverride)
            }
            if (audioGroup != null) {
                trackSelectionOverride = TrackSelectionOverride(audioGroup, tracks)
                overridesBuilder.addOverride(trackSelectionOverride)
            }
            if (player != null) {
                val trackSelectionParametersBuilder = player!!.trackSelectionParameters.buildUpon()
                if (trackSelectionOverride != null) {
                    trackSelectionParametersBuilder.setOverrideForType(trackSelectionOverride)
                }
                player!!.trackSelectionParameters = trackSelectionParametersBuilder.build()
            }
        }

        private fun getTrackGroupFromFormatId(trackType: Int, id: String?): TrackGroup? {
            if (id == null && trackType == C.TRACK_TYPE_AUDIO || player == null) {
                return null
            }
            for (group in player!!.currentTracks.groups) {
                if (group.type == trackType) {
                    val trackGroup = group.mediaTrackGroup
                    val format = trackGroup.getFormat(0)
                    if (id == format.id) {
                        return trackGroup
                    }
                }
            }
            return null
        }


        fun setEndControlsVisible(visible: Boolean) {
            val deleteVisible = if (visible && haveMedia && Utils.isDeletable(
                    mContext!!,
                    mPrefs!!.mediaUri!!
                )
            ) View.VISIBLE else View.INVISIBLE
            val nextVisible =
                if (visible && haveMedia && (nextUri != null || mPrefs!!.askScope && !isTvBox)) View.VISIBLE else View.INVISIBLE
            mContext!!.findViewById<View>(R.id.delete).visibility = deleteVisible
            mContext!!.findViewById<View>(R.id.next).visibility = nextVisible
        }

        fun updateButtonRotation() {
            val portrait = mContext!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            var auto = false
            try {
                auto =
                    Settings.System.getInt(mContext!!.contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 1
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }
            if (mPrefs!!.orientation === Utils.Orientation.VIDEO) {
                if (auto) {
                    buttonRotation!!.setImageResource(R.drawable.ic_screen_lock_rotation_24dp)
                } else if (portrait) {
                    buttonRotation!!.setImageResource(R.drawable.ic_screen_lock_portrait_24dp)
                } else {
                    buttonRotation!!.setImageResource(R.drawable.ic_screen_lock_landscape_24dp)
                }
            } else {
                if (auto) {
                    buttonRotation!!.setImageResource(R.drawable.ic_screen_rotation_24dp)
                } else if (portrait) {
                    buttonRotation!!.setImageResource(R.drawable.ic_screen_portrait_24dp)
                } else {
                    buttonRotation!!.setImageResource(R.drawable.ic_screen_landscape_24dp)
                }
            }
        }

        fun updateSubtitleViewMargin() {
            if (player == null) {
                return
            }
            updateSubtitleViewMargin(player!!.videoFormat)
        }
        fun updateSubtitleViewMargin(format: Format?) {
            if (format == null) {
                return
            }
            val aspectVideo = Utils.getRational(format)
            val metrics = mContext!!.resources.displayMetrics
            val aspectDisplay = Rational(metrics.widthPixels, metrics.heightPixels)
            var marginHorizontal = 0
            val marginVertical = 0
            if (mContext!!.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (aspectDisplay.toFloat() > aspectVideo.toFloat()) {
                    // Left & right bars
                    val videoWidth =
                        metrics.heightPixels / aspectVideo.denominator * aspectVideo.numerator
                    marginHorizontal = (metrics.widthPixels - videoWidth) / 2
                }
            }
            Utils.setViewParams(
                playerView!!.subtitleView!!, 0, 0, 0, 0,
                marginHorizontal, marginVertical, marginHorizontal, marginVertical
            )
        }

    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        //Initializing the pref class
        mPrefs = Prefs(this)
        mContext = this
        mActivity = this
        //Setting the orientation
        Utils.setOrientation(this, mPrefs?.orientation)

        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT == 28 && Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) &&
            (Build.DEVICE.equals("oneday", ignoreCase = true) || Build.DEVICE.equals(
                "once",
                ignoreCase = true
            ))
        ) {
            setContentView(R.layout.activity_player_textureview)
        } else {
            setContentView(R.layout.activity_main)
        }

        if (Build.VERSION.SDK_INT >= 31) {
            val window = window
            if (window != null) {
                window.setDecorFitsSystemWindows(false)
                val windowInsetsController = window.insetsController
                if (windowInsetsController != null) {
                    // On Android 12 BEHAVIOR_DEFAULT allows system gestures without visible system bars
                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_DEFAULT
                }
            }
        }

        isTvBox = Utils.isTvBox(this)
        if (isTvBox){
            setDefaultNightMode(MODE_NIGHT_YES)
        }

        val launchIntent = intent
        val action = launchIntent.action
        val type = launchIntent.type

        if ("com.dev.demoyoutubeplayerview.action.SHORTCUT_VIDEOS" == action) {
            openFile(Utils.moviesFolderUri!!)
        } else if (Intent.ACTION_SEND == action && "text/plain" == type) {
            val text = launchIntent.getStringExtra(Intent.EXTRA_TEXT)
            if (text != null) {
                val parsedUri = Uri.parse(text)
                if (parsedUri.isAbsolute) {
                    mPrefs!!.updateMedia(this, parsedUri, null)
                    focusPlay = true
                }
            }
        } else if (launchIntent.data != null) {
            resetApiAccess()
            val uri = launchIntent.data
            if (SubtitleUtils.isSubtitle(uri, type)) {
                handleSubtitles(uri!!)
            } else {
                val bundle = launchIntent.extras
                if (bundle != null) {
                    apiAccess =
                        (bundle.containsKey(API_POSITION) || bundle.containsKey(API_RETURN_RESULT) || bundle.containsKey(
                            API_TITLE
                        )
                                || bundle.containsKey(API_SUBS) || bundle.containsKey(
                            API_SUBS_ENABLE
                        ))
                    if (apiAccess) {
                        mPrefs!!.setPersistent(false)
                    }
                    apiTitle = bundle.getString(API_TITLE)
                }
                mPrefs!!.updateMedia(this, uri, type)
                if (bundle != null) {
                    var defaultSub: Uri? = null
                    val subsEnable = bundle.getParcelableArray(API_SUBS_ENABLE)
                    if (subsEnable != null && subsEnable.size > 0) {
                        defaultSub = subsEnable[0] as Uri
                    }
                    val subs = bundle.getParcelableArray(API_SUBS)
                    val subsName = bundle.getStringArray(API_SUBS_NAME)
                    if (subs != null && subs.size > 0) {
                        for (i in subs.indices) {
                            val sub = subs[i] as Uri
                            var name: String? = null
                            if (subsName != null && subsName.size > i) {
                                name = subsName[i]
                            }
                            apiSubs.add(
                                SubtitleUtils.buildSubtitle(
                                    this,
                                    sub,
                                    name,
                                    sub == defaultSub
                                )
                            )
                        }
                    }
                }
                if (apiSubs.isEmpty()) {
                    searchSubtitles()
                }
                if (bundle != null) {
                    intentReturnResult = bundle.getBoolean(API_RETURN_RESULT)
                    if (bundle.containsKey(API_POSITION)) {
                        mPrefs!!.updatePosition(bundle.getInt(API_POSITION).toLong())
                    }
                }
            }
            focusPlay = true
        }
    }


    //Open File Method
    private fun openFile(pickerInitialUri: Uri?) {
        var pickerInitialUri: Uri? = pickerInitialUri
        val targetSdkVersion = applicationContext.applicationInfo.targetSdkVersion
        if (isTvBox && Build.VERSION.SDK_INT >= 30 && targetSdkVersion >= 30 && mPrefs!!.fileAccess.equals(
                "auto"
            ) || mPrefs!!.fileAccess.equals("mediastore")
        ) {
            val intent = Intent(
                this,
                MediaStoreChooserActivity::class.java
            )
            startActivityForResult(intent, REQUEST_CHOOSER_VIDEO_MEDIASTORE)
        } else if (isTvBox && mPrefs!!.fileAccess.equals("auto") || mPrefs!!.fileAccess.equals("legacy")) {
            Utils.alternativeChooser(this, pickerInitialUri, true)
        } else {
            enableRotation()
            if (pickerInitialUri == null || Utils.isSupportedNetworkUri(pickerInitialUri) || !Utils.fileExists(
                    this,
                    pickerInitialUri
                )
            ) {
                pickerInitialUri = Utils.moviesFolderUri
            }
            val intent: Intent = createBaseFileIntent(Intent.ACTION_OPEN_DOCUMENT, pickerInitialUri)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "video/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, Utils.supportedMimeTypesVideo)
            if (Build.VERSION.SDK_INT < 30) {
                val systemComponentName = Utils.getSystemComponent(this, intent)
                if (systemComponentName != null) {
                    intent.component = systemComponentName
                }
            }
            safelyStartActivityForResult(intent, REQUEST_CHOOSER_VIDEO)
        }
    }


    private fun enableRotation() {
        try {
            if (Settings.System.getInt(
                    contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION
                ) == 0
            ) {
                Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                restoreOrientationLock = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    private fun loadSubtitleFile(pickerInitialUri: Uri?) {
        Toast.makeText(this@PlayerActivity, R.string.open_subtitles, Toast.LENGTH_SHORT).show()
        val targetSdkVersion = applicationContext.applicationInfo.targetSdkVersion
        if (isTvBox && Build.VERSION.SDK_INT >= 30 && targetSdkVersion >= 30 && mPrefs!!.fileAccess.equals(
                "auto"
            ) || mPrefs!!.fileAccess.equals("mediastore")
        ) {
            val intent = Intent(
                this,
                MediaStoreChooserActivity::class.java
            )
            intent.putExtra(MediaStoreChooserActivity.SUBTITLES, true)
            startActivityForResult(intent, REQUEST_CHOOSER_SUBTITLE_MEDIASTORE)
        } else if (isTvBox && mPrefs!!.fileAccess.equals("auto") || mPrefs!!.fileAccess.equals("legacy")) {
            Utils.alternativeChooser(this, pickerInitialUri, false)
        } else {
            enableRotation()
            val intent = createBaseFileIntent(Intent.ACTION_OPEN_DOCUMENT, pickerInitialUri)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            val supportedMimeTypes = arrayOf(
                MimeTypes.APPLICATION_SUBRIP,
                MimeTypes.TEXT_SSA,
                MimeTypes.TEXT_VTT,
                MimeTypes.APPLICATION_TTML,
                "text/*",
                "application/octet-stream"
            )
            intent.putExtra(Intent.EXTRA_MIME_TYPES, supportedMimeTypes)
            if (Build.VERSION.SDK_INT < 30) {
                val systemComponentName = Utils.getSystemComponent(this, intent)
                if (systemComponentName != null) {
                    intent.component = systemComponentName
                }
            }
            safelyStartActivityForResult(intent, REQUEST_CHOOSER_SUBTITLE)
        }
    }

    private fun requestDirectoryAccess() {
        enableRotation()
        val intent =
            createBaseFileIntent(Intent.ACTION_OPEN_DOCUMENT_TREE, Utils.moviesFolderUri)
        safelyStartActivityForResult(intent, REQUEST_CHOOSER_SCOPE_DIR)
    }

    private fun createBaseFileIntent(action: String, initialUri: Uri?): Intent {
        val intent = Intent(action)

        // http://stackoverflow.com/a/31334967/1615876
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
        if (Build.VERSION.SDK_INT >= 26 && initialUri != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        }
        return intent
    }

    fun safelyStartActivityForResult(intent: Intent, code: Int) {
        if (intent.resolveActivity(packageManager) == null) showSnack(
            getText(R.string.error_files_missing).toString(),
            intent.toString()
        ) else startActivityForResult(intent, code)
    }





    private fun hasOverrideType(trackType: Int): Boolean {
        val trackSelectionParameters = player!!.trackSelectionParameters
        for (override in trackSelectionParameters.overrides.values) {
            if (override.type == trackType) return true
        }
        return false
    }

    fun getSelectedTrack(trackType: Int): String? {
        if (player == null) {
            return null
        }
        val tracks = player!!.currentTracks

        // Disabled (e.g. selected subtitle "None" - different than default)
        if (!tracks.isTypeSelected(trackType)) {
            return "#none"
        }

        // Audio track set to "Auto"
        if (trackType == C.TRACK_TYPE_AUDIO) {
            if (!hasOverrideType(C.TRACK_TYPE_AUDIO)) {
                return null
            }
        }
        for (group in tracks.groups) {
            if (group.isSelected && group.type == trackType) {
                val format = group.mediaTrackGroup.getFormat(0)
                return format.id
            }
        }
        return null
    }

    fun setSubtitleTextSize() {
        setSubtitleTextSize(resources.configuration.orientation)
    }

    fun setSubtitleTextSize(orientation: Int) {
        // Tweak text size as fraction size doesn't work well in portrait
        val subtitleView = playerView!!.subtitleView
        if (subtitleView != null) {
            val size: Float
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                size = SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitlesScale
            } else {
                val metrics = resources.displayMetrics
                var ratio = metrics.heightPixels.toFloat() / metrics.widthPixels.toFloat()
                if (ratio < 1) ratio = 1 / ratio
                size = SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitlesScale / ratio
            }
            subtitleView.setFractionalTextSize(size)
        }
    }



    // Set margins to fix PGS aspect as subtitle view is outside of content frame


    fun setSubtitleTextSizePiP() {
        val subtitleView = playerView!!.subtitleView
        subtitleView?.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 2)
    }



    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun isInPip(): Boolean {
        return if (!Utils.isPiPSupported(this)) false else isInPictureInPictureMode
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isInPip()) {
            setSubtitleTextSize(newConfig.orientation)
        }
        updateSubtitleViewMargin()
        updateButtonRotation()
    }





    fun reportScrubbing(position: Long) {
        val diff = position - scrubbingStart
        if (Math.abs(diff) > 1000) {
            scrubbingNoticeable = true
        }
        if (scrubbingNoticeable) {
            playerView!!.clearIcon()
            playerView!!.setCustomErrorMessage(Utils.formatMilisSign(diff))
        }
        if (frameRendered) {
            frameRendered = false
            player!!.seekTo(position)
        }
    }

    fun updateSubtitleStyle(context: Context?) {
        val captioningManager = getSystemService(CAPTIONING_SERVICE) as CaptioningManager
        val subtitleView = playerView!!.subtitleView
        val isTablet = Utils.isTablet(context!!)
        subtitlesScale =
            SubtitleUtils.normalizeFontScale(captioningManager.fontScale, isTvBox || isTablet)
        if (subtitleView != null) {
            val userStyle = captioningManager.userStyle
            val userStyleCompat = CaptionStyleCompat.createFromCaptionStyle(userStyle)
            val captionStyle = CaptionStyleCompat(
                if (userStyle.hasForegroundColor()) userStyleCompat.foregroundColor else Color.WHITE,
                if (userStyle.hasBackgroundColor()) userStyleCompat.backgroundColor else Color.TRANSPARENT,
                if (userStyle.hasWindowColor()) userStyleCompat.windowColor else Color.TRANSPARENT,
                if (userStyle.hasEdgeType()) userStyleCompat.edgeType else CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                if (userStyle.hasEdgeColor()) userStyleCompat.edgeColor else Color.BLACK,
                if (userStyleCompat.typeface != null) userStyleCompat.typeface else Typeface.DEFAULT_BOLD
            )
            subtitleView.setStyle(captionStyle)
            if (captioningManager.isEnabled) {
                // Do not apply embedded style as currently the only supported color style is PrimaryColour
                // https://github.com/google/ExoPlayer/issues/8435#issuecomment-762449001
                // This may result in poorly visible text (depending on user's selected edgeColor)
                // The same can happen with style provided using setStyle but enabling CaptioningManager should be a way to change the behavior
                subtitleView.setApplyEmbeddedStyles(false)
            } else {
                subtitleView.setApplyEmbeddedStyles(true)
            }
            subtitleView.setBottomPaddingFraction(SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION * 2f / 3f)
        }
        setSubtitleTextSize()
    }

    fun searchSubtitles() {
        if (mPrefs!!.mediaUri == null) return
        if (Utils.isSupportedNetworkUri(mPrefs!!.mediaUri!!) && Utils.isProgressiveContainerUri(
                mPrefs!!.mediaUri!!
            )
        ) {
            SubtitleUtils.clearCache(this)
            if (SubtitleFinder.isUriCompatible(mPrefs!!.mediaUri!!)) {
                subtitleFinder = SubtitleFinder(this@PlayerActivity, mPrefs!!.mediaUri!!)
                subtitleFinder!!.start()
            }
            return
        }
        if (mPrefs!!.scopeUri != null || isTvBox) {
            var video: DocumentFile? = null
            var videoRaw: File? = null
            val scheme = mPrefs!!.mediaUri!!.scheme
            if (mPrefs!!.scopeUri != null) {
                video =
                    if ("com.android.externalstorage.documents" == mPrefs!!.mediaUri!!.host || "org.courville.nova.provider" == mPrefs!!.mediaUri!!.host) {
                        // Fast search based on path in uri
                        SubtitleUtils.findUriInScope(this, mPrefs!!.scopeUri!!, mPrefs!!.mediaUri!!)
                    } else {
                        // Slow search based on matching metadata, no path in uri
                        // Provider "com.android.providers.media.documents" when using "Videos" tab in file picker
                        val fileScope = DocumentFile.fromTreeUri(this, mPrefs!!.scopeUri!!)
                        val fileMedia = DocumentFile.fromSingleUri(
                            this,
                            mPrefs!!.mediaUri!!
                        )
                        SubtitleUtils.findDocInScope(fileScope, fileMedia)
                    }
            } else if (ContentResolver.SCHEME_FILE == scheme) {
                videoRaw = File(mPrefs!!.mediaUri!!.schemeSpecificPart)
                video = DocumentFile.fromFile(videoRaw)
            }
            if (video != null) {
                var subtitle: DocumentFile? = null
                if (mPrefs!!.scopeUri != null) {
                    subtitle = SubtitleUtils.findSubtitle(video)
                } else if (ContentResolver.SCHEME_FILE == scheme) {
                    val parentRaw = videoRaw!!.parentFile
                    val dir = DocumentFile.fromFile(parentRaw)
                    subtitle = SubtitleUtils.findSubtitle(video, dir)
                }
                if (subtitle != null) {
                    handleSubtitles(subtitle.uri)
                }
            }
        }
    }

    fun findNext(): Uri? {
        // TODO: Unify with searchSubtitles()
        if (mPrefs!!.scopeUri != null || isTvBox) {
            var video: DocumentFile? = null
            var videoRaw: File? = null
            if (!isTvBox && mPrefs!!.scopeUri != null) {
                video = if ("com.android.externalstorage.documents" == mPrefs!!.mediaUri!!.host) {
                    // Fast search based on path in uri
                    SubtitleUtils.findUriInScope(this, mPrefs!!.scopeUri!!, mPrefs!!.mediaUri!!)
                } else {
                    // Slow search based on matching metadata, no path in uri
                    // Provider "com.android.providers.media.documents" when using "Videos" tab in file picker
                    val fileScope = DocumentFile.fromTreeUri(this, mPrefs!!.scopeUri!!)
                    val fileMedia = DocumentFile.fromSingleUri(
                        this,
                        mPrefs!!.mediaUri!!
                    )
                    SubtitleUtils.findDocInScope(fileScope, fileMedia)
                }
            } else if (isTvBox) {
                videoRaw = File(mPrefs!!.mediaUri!!.schemeSpecificPart)
                video = DocumentFile.fromFile(videoRaw)
            }
            if (video != null) {
                val next: DocumentFile?
                next = if (!isTvBox) {
                    SubtitleUtils.findNext(video)
                } else {
                    val parentRaw = videoRaw!!.parentFile
                    val dir = DocumentFile.fromFile(parentRaw)
                    SubtitleUtils.findNext(video, dir)
                }
                if (next != null) {
                    return next.uri
                }
            }
        }
        return null
    }

    fun askForScope(loadSubtitlesOnCancel: Boolean, skipToNextOnCancel: Boolean) {
        val builder = AlertDialog.Builder(this@PlayerActivity)
        builder.setMessage(
            String.format(
                getString(R.string.request_scope),
                getString(R.string.app_name)
            )
        )
        builder.setPositiveButton(
            android.R.string.ok
        ) { dialogInterface: DialogInterface?, i: Int -> requestDirectoryAccess() }
        builder.setNegativeButton(
            android.R.string.cancel
        ) { dialog: DialogInterface?, which: Int ->
            mPrefs!!.markScopeAsked()
            if (loadSubtitlesOnCancel) {
                loadSubtitleFile(mPrefs!!.mediaUri)
            }
            if (skipToNextOnCancel) {
                nextUri = findNext()
                if (nextUri != null) {
                    skipToNext()
                }
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    fun resetHideCallbacks() {
        if (haveMedia && player != null && player!!.isPlaying) {
            // Keep controller UI visible - alternative to resetHideCallbacks()
            playerView!!.controllerShowTimeoutMs = CONTROLLER_TIMEOUT
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onUserLeaveHint() {
        if (mPrefs != null && mPrefs!!.autoPiP && player != null && player!!.isPlaying && Utils.isPiPSupported(
                this
            )
        ) enterPiP() else super.onUserLeaveHint()
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun enterPiP() {
        val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        if (AppOpsManager.MODE_ALLOWED != appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE, Process.myUid(),
                packageName
            )
        ) {
            val intent = Intent(
                "android.settings.PICTURE_IN_PICTURE_SETTINGS", Uri.fromParts(
                    "package",
                    packageName, null
                )
            )
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
            return
        }
        if (player == null) {
            return
        }
        playerView!!.controllerAutoShow = false
        playerView!!.hideController()
        val format = player!!.videoFormat
        if (format != null) {
            // https://github.com/google/ExoPlayer/issues/8611
            // TODO: Test/disable on Android 11+
            val videoSurfaceView = playerView!!.videoSurfaceView
            if (videoSurfaceView is SurfaceView) {
                videoSurfaceView.holder.setFixedSize(format.width, format.height)
            }
            var rational = Utils.getRational(format)
            if (Build.VERSION.SDK_INT >= 33 &&
                packageManager.hasSystemFeature(FEATURE_EXPANDED_PICTURE_IN_PICTURE) &&
                (rational.toFloat() > rationalLimitWide.toFloat() || rational.toFloat() < rationalLimitTall.toFloat())
            ) {
                mPictureInPictureParamsBuilder?.setAspectRatio(rational)

            }
            if (rational.toFloat() > rationalLimitWide.toFloat()) rational =
                rationalLimitWide else if (rational.toFloat() < rationalLimitTall.toFloat()) rational =
                rationalLimitTall
            (mPictureInPictureParamsBuilder as PictureInPictureParams.Builder).setAspectRatio(
                rational
            )
        }
        enterPictureInPictureMode((mPictureInPictureParamsBuilder as PictureInPictureParams.Builder).build())
    }



    fun askDeleteMedia() {
        val builder = AlertDialog.Builder(this@PlayerActivity)
        builder.setMessage(getString(R.string.delete_query))
        builder.setPositiveButton(R.string.delete_confirmation) { dialogInterface, i ->
            releasePlayer()
            deleteMedia()
            if (nextUri == null) {
                haveMedia = false
                setEndControlsVisible(false)
                playerView!!.controllerShowTimeoutMs = -1
            } else {
                skipToNext()
            }
        }
        builder.setNegativeButton(
            android.R.string.cancel
        ) { dialog: DialogInterface?, which: Int -> }
        val dialog = builder.create()
        dialog.show()
    }

    fun deleteMedia() {
        try {
            if (ContentResolver.SCHEME_CONTENT == mPrefs!!.mediaUri!!.scheme) {
                DocumentsContract.deleteDocument(
                    contentResolver,
                    mPrefs!!.mediaUri!!
                )
            } else if (ContentResolver.SCHEME_FILE == mPrefs!!.mediaUri!!.scheme) {
                val file = File(mPrefs!!.mediaUri!!.schemeSpecificPart)
                if (file.canWrite()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dispatchPlayPause() {
        if (player == null) return
        val state = player!!.playbackState
        val methodName: String
        if (state == Player.STATE_IDLE || state == Player.STATE_ENDED || !player!!.playWhenReady) {
            methodName = "dispatchPlay"
            shortControllerTimeout = true
        } else {
            methodName = "dispatchPause"
        }
        try {
            val method =
                StyledPlayerControlView::class.java.getDeclaredMethod(
                    methodName,
                    Player::class.java
                )
            method.isAccessible = true
            method.invoke(controlView, player as Player?)
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
    }

    fun skipToNext() {
        if (nextUri != null) {
            releasePlayer()
            mPrefs!!.updateMedia(this, nextUri, null)
            searchSubtitles()
            initializePlayer()
        }
    }

    fun notifyAudioSessionUpdate(active: Boolean) {
        val intent =
            Intent(if (active) AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION else AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player!!.audioSessionId)
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        if (active) {
            intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MOVIE)
        }
        try {
            sendBroadcast(intent)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun updateButtons(enable: Boolean) {
        if (buttonPiP != null) {
            Utils.setButtonEnabled(this, buttonPiP, enable)
        }
        Utils.setButtonEnabled(this, buttonAspectRatio!!, enable)
        if (isTvBox) {
            Utils.setButtonEnabled(this, exoSettings!!, true)
        } else {
            Utils.setButtonEnabled(this, exoSettings!!, enable)
        }
    }

    private fun scaleStart() {
        isScaling = true
        if (playerView!!.resizeMode !== AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            playerView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
        scaleFactor = playerView!!.videoSurfaceView!!.scaleX
        playerView!!.removeCallbacks(playerView!!.textClearRunnable)
        playerView!!.clearIcon()
        playerView!!.setCustomErrorMessage((scaleFactor*100).roundToInt().toString() + "%")
        playerView!!.hideController()
        isScaleStarting = true
    }

    private fun scale(up: Boolean) {
        if (up) {
            scaleFactor += 0.01f
        } else {
            scaleFactor -= 0.01f
        }
        scaleFactor = Utils.normalizeScaleFactor(scaleFactor, playerView!!.scaleFit)
        playerView!!.setScale(scaleFactor)

        playerView!!.setCustomErrorMessage((scaleFactor*100).roundToInt().toString() + "%")
    }

    private fun scaleEnd() {
        isScaling = false
        playerView!!.postDelayed(playerView!!.textClearRunnable, 200)
        if (!player!!.isPlaying) {
            playerView!!.showController()
        }
        if (Math.abs(playerView?.scaleFit?.minus(scaleFactor)!!) < 0.01 / 2) {
            playerView!!.setScale(1f)
            playerView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        updatebuttonAspectRatioIcon()
    }

    private fun updatebuttonAspectRatioIcon() {
        if (playerView!!.resizeMode === AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            buttonAspectRatio!!.setImageResource(R.drawable.ic_fit_screen_24dp)
        } else {
            buttonAspectRatio!!.setImageResource(R.drawable.ic_aspect_ratio_24dp)
        }
    }



    fun releasePlayer() {
        releasePlayer(true)
    }
    private fun releasePlayer(save: Boolean) {
        if (save) {
            savePlayer()
        }
        if (player != null) {
            notifyAudioSessionUpdate(false)
            mediaSession!!.isActive = false
            mediaSession?.release()
            if (player!!.isPlaying && restorePlayStateAllowed) {
                restorePlayState = true
            }
            player!!.removeListener(playerListener!!)
            player!!.clearMediaItems()
            player!!.release()
            player = null
        }
        titleView!!.visibility = View.GONE
        updateButtons(false)
    }

    private fun savePlayer() {
        if (player != null) {
            mPrefs!!.updateBrightness(mBrightnessControl!!.currentBrightnessLevel)
            mPrefs!!.updateOrientation()
            if (haveMedia) {
                // Prevent overwriting temporarily inaccessible media position
                if (player!!.isCurrentMediaItemSeekable) {
                    mPrefs!!.updatePosition(player!!.currentPosition)
                }
                mPrefs!!.updateMeta(
                    getSelectedTrack(C.TRACK_TYPE_AUDIO),
                    getSelectedTrack(C.TRACK_TYPE_TEXT),
                    playerView!!.resizeMode,
                    playerView!!.videoSurfaceView!!.scaleX,
                    player!!.playbackParameters.speed
                )
            }
        }
    }

    fun initializePlayer() {
        val isNetworkUri = mPrefs!!.mediaUri != null && Utils.isSupportedNetworkUri(
            mPrefs!!.mediaUri!!
        )
        haveMedia = mPrefs!!.mediaUri != null && (Utils.fileExists(
            this,
            mPrefs!!.mediaUri!!
        ) || isNetworkUri)
        if (player != null) {
            player!!.removeListener(playerListener!!)
            player!!.clearMediaItems()
            player!!.release()
            player = null
        }
        trackSelector = DefaultTrackSelector(this)
        if (mPrefs!!.tunneling) {
            trackSelector?.setParameters(
                trackSelector?.buildUponParameters()!!.setTunnelingEnabled(true)
            )
        }
        trackSelector?.setParameters(
            trackSelector?.buildUponParameters()!!.setPreferredAudioLanguages(Utils.deviceLanguages.toString())
        )
        // https://github.com/google/ExoPlayer/issues/8571
        val extractorsFactory = DefaultExtractorsFactory()
            .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
            .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)
        @SuppressLint("WrongConstant") val renderersFactory: RenderersFactory =
            DefaultRenderersFactory(this)
                .setExtensionRendererMode(mPrefs!!.decoderPriority)
        val playerBuilder = ExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector!!)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractorsFactory))
        if (haveMedia && isNetworkUri) {
            if (mPrefs!!.mediaUri!!.scheme!!.toLowerCase().startsWith("http")) {
                val headers = HashMap<String, String>()
                val userInfo = mPrefs!!.mediaUri!!.userInfo
                if (userInfo != null && userInfo.length > 0 && userInfo.contains(":")) {
                    headers["Authorization"] =
                        "Basic " + Base64.encodeToString(userInfo.toByteArray(), Base64.NO_WRAP)
                    val defaultHttpDataSourceFactory = DefaultHttpDataSource.Factory()
                    defaultHttpDataSourceFactory.setDefaultRequestProperties(headers)
                    playerBuilder.setMediaSourceFactory(
                        DefaultMediaSourceFactory(
                            defaultHttpDataSourceFactory,
                            extractorsFactory
                        )
                    )
                }
            }
        }
        player = playerBuilder.build()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        player!!.setAudioAttributes(audioAttributes, true)
        if (mPrefs!!.skipSilence) {
            player!!.skipSilenceEnabled = true
        }
        youTubeOverlay!!.player(player)
        playerView!!.player = player
        mediaSession = MediaSessionCompat(this, getString(R.string.app_name))
        val mediaSessionConnector = MediaSessionConnector(mediaSession!!)
        mediaSessionConnector.setPlayer(player)

        mediaSessionConnector.setMediaMetadataProvider { player: Player? ->
            if (mPrefs!!.mediaUri == null){
                return@setMediaMetadataProvider null as MediaMetadataCompat
            }
            val title = Utils.getFileName(this@PlayerActivity, mPrefs!!.mediaUri!!)
            if (title == null) return@setMediaMetadataProvider null as MediaMetadataCompat else return@setMediaMetadataProvider MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .build()
        }
        playerView!!.controllerShowTimeoutMs = -1
        locked = false
        chapterStarts = LongArray(0)
        if (haveMedia) {
            if (isNetworkUri) {
                timeBar!!.setBufferedColor(DefaultTimeBar.DEFAULT_BUFFERED_COLOR)
            } else {
                // https://github.com/google/ExoPlayer/issues/5765
                timeBar!!.setBufferedColor(0x33FFFFFF)
            }
            playerView!!.resizeMode = mPrefs!!.resizeMode
            if (mPrefs!!.resizeMode === AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                playerView!!.setScale(mPrefs!!.scale)
            } else {
                playerView!!.setScale(1f)
            }
            updatebuttonAspectRatioIcon()
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(mPrefs!!.mediaUri)
                .setMimeType(mPrefs!!.mediaType)
            if (apiAccess && apiSubs.size > 0) {
                mediaItemBuilder.setSubtitleConfigurations(apiSubs)
            } else if (mPrefs!!.subtitleUri != null && Utils.fileExists(
                    this,
                    mPrefs!!.subtitleUri!!
                )
            ) {
                val subtitle = SubtitleUtils.buildSubtitle(
                    this,
                    mPrefs!!.subtitleUri!!, null, true
                )
                mediaItemBuilder.setSubtitleConfigurations(listOf(subtitle))
            }
            player?.setMediaItem(mediaItemBuilder.build(), mPrefs?.position!!)
            if (loudnessEnhancer != null) {
                loudnessEnhancer!!.release()
            }
            try {
                loudnessEnhancer = LoudnessEnhancer(player!!.audioSessionId)
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
            notifyAudioSessionUpdate(true)
            videoLoading = true
            updateLoading(true)
            if (mPrefs?.position === 0L || apiAccess) {
                play = true
            }
            if (apiTitle != null) {
                titleView!!.text = apiTitle
            } else {
                titleView!!.text = Utils.getFileName(this, mPrefs!!.mediaUri!!)
            }
            titleView.visibility = View.VISIBLE
            updateButtons(true)
            (playerView as DoubleTapPlayerView).isDoubleTapEnabled = true
            if (!apiAccess) {
                if (nextUriThread != null) {
                    nextUriThread?.interrupt()
                }
                nextUri = null
                nextUriThread = Thread {
                    val uri = findNext()
                    if (!Thread.currentThread().isInterrupted) {
                        nextUri = uri
                    }
                }
                nextUriThread?.start()
            }
            Utils.markChapters(this, mPrefs!!.mediaUri!!, controlView!!)
            player!!.setHandleAudioBecomingNoisy(!isTvBox)
            mediaSession?.setActive(true)
        } else {
            playerView!!.showController()
        }
        player!!.addListener(playerListener!!)
        player!!.prepare()
        if (restorePlayState) {
            restorePlayState = false
            playerView!!.showController()
            playerView!!.controllerShowTimeoutMs = CONTROLLER_TIMEOUT
            player!!.playWhenReady = true
        }
    }

    fun resetApiAccess() {
        apiAccess = false
        apiTitle = null
        apiSubs.clear()
        mPrefs!!.setPersistent(true)
    }

    private fun handleSubtitles(uri: Uri) {
        // Convert subtitles to UTF-8 if necessary
        var uri: Uri? = uri
        SubtitleUtils.clearCache(this)
        uri = SubtitleUtils.convertToUTF(this, uri!!)
        mPrefs!!.updateSubtitle(uri)
    }
}