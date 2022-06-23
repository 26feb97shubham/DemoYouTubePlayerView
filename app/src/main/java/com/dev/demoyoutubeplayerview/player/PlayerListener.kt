package com.dev.demoyoutubeplayerview.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.display.DisplayManager
import android.media.audiofx.LoudnessEnhancer
import com.dev.demoyoutubeplayerview.PlayerActivity
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.apiAccess
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.displayListener
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.displayManager
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.errorToShow
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.frameRendered
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.isScrubbing
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.mActivity
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.mContext
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.mPrefs
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.notifyAudioSessionUpdate
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.play
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.playbackFinished
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.playerView
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.setEndControlsVisible
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.setSelectedTracks
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.showError
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.timeBar
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.updateButtonRotation
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.updateLoading
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.updatePictureInPictureActions
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.updateSubtitleViewMargin
import com.dev.demoyoutubeplayerview.PlayerActivity.Companion.videoLoading
import com.dev.demoyoutubeplayerview.R
import com.dev.demoyoutubeplayerview.dtpv.Utils
import com.google.android.exoplayer2.*
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit

class PlayerListener(private val activity: PlayerActivity) : Player.Listener {
    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        if (PlayerActivity.loudnessEnhancer != null) {
            PlayerActivity.loudnessEnhancer!!.release()
        }
        try {
            PlayerActivity.loudnessEnhancer = LoudnessEnhancer(audioSessionId)
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
        notifyAudioSessionUpdate(true)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        playerView?.setKeepScreenOn(isPlaying)
        if (Utils.isPiPSupported(activity)) {
            if (isPlaying) {
                updatePictureInPictureActions(
                    R.drawable.ic_pause_24dp,
                    R.string.exo_controls_pause_description,
                    PlayerActivity.CONTROL_TYPE_PAUSE,
                    PlayerActivity.REQUEST_PAUSE
                )
            } else {
                updatePictureInPictureActions(
                    R.drawable.ic_play_arrow_24dp,
                    R.string.exo_controls_play_description,
                    PlayerActivity.CONTROL_TYPE_PLAY,
                    PlayerActivity.REQUEST_PLAY
                )
            }
        }
        if (!isScrubbing) {
            if (isPlaying) {
                if (PlayerActivity.shortControllerTimeout) {
                    playerView?.setControllerShowTimeoutMs(PlayerActivity.CONTROLLER_TIMEOUT / 3)
                    PlayerActivity.shortControllerTimeout = false
                    PlayerActivity.restoreControllerTimeout = true
                } else {
                    playerView?.setControllerShowTimeoutMs(PlayerActivity.CONTROLLER_TIMEOUT)
                }
            } else {
                playerView?.setControllerShowTimeoutMs(-1)
            }
        }
        if (!isPlaying) {
            PlayerActivity.locked = false
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onPlaybackStateChanged(state: Int) {
        var isNearEnd = false
        val duration: Long = PlayerActivity.player?.duration!!
        if (duration != C.TIME_UNSET) {
            val position: Long = PlayerActivity.player?.currentPosition!!
            if (position + 4000 >= duration) {
                isNearEnd = true
            } else {
                // Last chapter is probably "Credits" chapter
                val chapters: Int = PlayerActivity.chapterStarts.size
                if (chapters > 1) {
                    val lastChapter: Long = PlayerActivity.chapterStarts.get(chapters - 1)
                    if (duration - lastChapter < duration / 10 && position > lastChapter) {
                        isNearEnd = true
                    }
                }
            }
        }
        setEndControlsVisible(PlayerActivity.haveMedia && (state == Player.STATE_ENDED || isNearEnd))
        if (state == Player.STATE_READY) {
            frameRendered = true
            if (videoLoading) {
                videoLoading = false
                if (mPrefs?.orientation === Utils.Orientation.UNSPECIFIED) {
                    mPrefs?.orientation = Utils.getNextOrientation(mPrefs?.orientation)
                    Utils.setOrientation(mActivity!!, mPrefs?.orientation)
                }
                val format: Format? = PlayerActivity.player?.getVideoFormat()
                if (format != null) {
                    if (mPrefs?.orientation === Utils.Orientation.VIDEO) {
                        if (Utils.isPortrait(format)) {
                            mActivity?.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)
                        } else {
                            mActivity?.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
                        }
                        updateButtonRotation()
                    }
                    updateSubtitleViewMargin(format)
                }
                if (duration != C.TIME_UNSET && duration > TimeUnit.MINUTES.toMillis(20)) {
                    timeBar?.setKeyTimeIncrement(TimeUnit.MINUTES.toMillis(1))
                } else {
                    timeBar?.setKeyCountIncrement(20)
                }
                var switched = false
                if (mPrefs?.frameRateMatching == true) {
                    if (play) {
                        if (displayManager == null) {
                            displayManager =
                                mActivity!!.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                        }
                        if (displayListener == null) {
                            displayListener = object : DisplayManager.DisplayListener {
                                override fun onDisplayAdded(displayId: Int) {}
                                override fun onDisplayRemoved(displayId: Int) {}
                                override fun onDisplayChanged(displayId: Int) {
                                    if (play) {
                                        play = false
                                        displayManager?.unregisterDisplayListener(this)
                                        if (PlayerActivity.player != null) {
                                            PlayerActivity.player?.play()
                                        }
                                        if (playerView != null) {
                                            playerView?.hideController()
                                        }
                                    }
                                }
                            }
                        }
                        displayManager!!.registerDisplayListener(displayListener, null)
                    }
                    switched = mPrefs!!.mediaUri?.let { Utils.switchFrameRate(mContext!!, it, play) } == true
                }
                if (!switched) {
                    if (displayManager != null) {
                        displayManager!!.unregisterDisplayListener(displayListener)
                    }
                    if (play) {
                        play = false
                        PlayerActivity.player!!.play()
                        playerView!!.hideController()
                    }
                }
                updateLoading(false)
                if (mPrefs!!.speed <= 0.99f || mPrefs!!.speed >= 1.01f) {
                    PlayerActivity.player!!.setPlaybackSpeed(mPrefs!!.speed)
                }
                if (!apiAccess) {
                    setSelectedTracks(mPrefs!!.subtitleTrackId!!, mPrefs!!.audioTrackId)
                }
            }
        } else if (state == Player.STATE_ENDED) {
            playbackFinished = true
            if (apiAccess) {
                mContext!!.finish()
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        updateLoading(false)
        if (error is ExoPlaybackException) {
            val exoPlaybackException = error
            if (PlayerActivity.controllerVisible && PlayerActivity.controllerVisibleFully) {
                showError(exoPlaybackException)
            } else {
                errorToShow = exoPlaybackException
            }
        }
    }
}