package com.example.myapplication

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.example.myapplication.databinding.ActivityPlayerBinding
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import com.google.gson.Gson


class PlayerActivity : Activity(), Player.Listener, AdEvent.AdEventListener, AdErrorEvent.AdErrorListener {

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var binding : ActivityPlayerBinding
    private lateinit var trackSelector: DefaultTrackSelector

    private var adsLoader: ImaAdsLoader? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adsLoader = ImaAdsLoader.Builder(this)
            .setAdEventListener(this)
            .setAdErrorListener(this)
            .build()

        binding.qualityBtn.setOnClickListener { showDialog() }

    }


    private fun initializePlayer() {

        val mediaDataSourceFactory: DataSource.Factory =
            DefaultDataSource.Factory(this)

        val drmConfig: MediaItem.DrmConfiguration =
            MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                .setLicenseUri(LICENCE_URL)
                .build()

        val ads: MediaItem.AdsConfiguration =
            MediaItem.AdsConfiguration.Builder(Uri.parse(ADS_URL))
                .build()

        val mediaItem: MediaItem = MediaItem.Builder()
            .setUri(Uri.parse(STREAM_URL_MPD))
            .setDrmConfiguration(drmConfig)
            .setAdsConfiguration(ads)
            .build()

        val mediaSourceFactory: MediaSourceFactory =
            DefaultMediaSourceFactory(mediaDataSourceFactory)
                .setAdsLoaderProvider { adsLoader }
                .setAdViewProvider(binding.playerView)

        val adaptiveTrackSelection = AdaptiveTrackSelection.Factory()
        trackSelector = DefaultTrackSelector(this, adaptiveTrackSelection)

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build()

        exoPlayer.addMediaItem(mediaItem)
        exoPlayer.addListener(this)

        val mEventLogger = EventLogger(trackSelector)
        exoPlayer.addAnalyticsListener(mEventLogger)

        exoPlayer.playWhenReady = true
        binding.playerView.player = exoPlayer
        adsLoader?.setPlayer(exoPlayer)
        binding.playerView.requestFocus()
    }


    private fun showDialog() {
        val trackSelector = TrackSelectionDialogBuilder(
            this,
            "Select Track",
            trackSelector,
            0
        ).build()
        trackSelector.show()
    }

    override fun onAdEvent(p0: AdEvent?) {
        Log.e("AD_EVENT", p0.toString())
        Log.e("AD_EVENT_TITLE", p0?.ad?.title.toString())
        Log.e("AD_POD_POS", p0?.ad?.adPodInfo?.adPosition.toString())
        Log.e("AD_POD_TOTAL", p0?.ad?.adPodInfo?.totalAds.toString())
    }

    override fun onAdError(p0: AdErrorEvent?) {
        Log.e("AD_ERROR", Gson().toJson(p0))
    }

    override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
        when (playbackState) {
            Player.STATE_IDLE -> {
                println("IDLE")
            }
            Player.STATE_BUFFERING -> {
                println("BUFFERING")
                binding.progressBar.isVisible = true
            }
            Player.STATE_READY -> {
                println("READY")
                binding.progressBar.isGone = true
            }
            Player.STATE_ENDED -> {
                println("ENDED")
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        Log.e("PLAYBACK_ERROR_MESSAGE", error.toString())
        Log.e("PLAYBACK_ERROR_CAUSE", error.cause.toString())
        Log.e("PLAYBACK_ERROR_CNAME", error.errorCodeName)
        Log.e("PLAYBACK_ERROR_CODE", error.errorCode.toString())
        Log.e("ERROR_STACKTRACE", error.stackTraceToString())
        Log.e("ERROR_SUPP_EXCEPTIONS", error.suppressedExceptions.toString())
    }

    private fun releasePlayer() {
        exoPlayer.release()
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) initializePlayer()
    }

    public override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23) initializePlayer()
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) releasePlayer()
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        adsLoader?.release()
        Log.e("ADS_DESTROYED", "X")
    }

    companion object {
        const val STREAM_URL_MPD = "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd"
        const val LICENCE_URL = "https://proxy.uat.widevine.com/proxy?provider=widevine_test"
        const val ADS_URL = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpremidpostlongpod&cmsid=496&vid=short_tencue&correlator="
    }
}