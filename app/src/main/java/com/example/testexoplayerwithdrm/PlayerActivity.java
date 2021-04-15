package com.example.testexoplayerwithdrm;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.MimeTypes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

public class PlayerActivity extends AppCompatActivity {

    String WIDEVINE_URL = "";
    PlayerView playerView;
    SubtitleView subtitleView;
    SimpleExoPlayer player;
    DefaultTrackSelector trackSelector;
    String URL = "";
    String jwtToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playerView = findViewById(R.id.player_view);
        playerView.setKeepContentOnPlayerReset(true);

        subtitleView = playerView.getSubtitleView();
        handleIntent(getIntent());
        startContentFromURL();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (player != null) {
            player.stop(true);
        }
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String appLinkAction = intent.getAction();
        Uri appLinkData = intent.getData();
        if (Intent.ACTION_VIEW.equals(appLinkAction) && appLinkData != null) {
            try {
                this.startContent();
            } catch (Exception ex) {
                Log.e("UI-EVENTS", "Button click error", ex);
            }
        }
    }

    public void startContentFromURL() {
        try {
            this.startContent();
        } catch (Exception ex) {
            Log.e("UI-EVENTS", "Button click error", ex);
        }
    }
    private void startContent() throws Exception {

        if (player != null) {
            player.stop(true);
        }

        Uri videoUrl = Uri.parse(URL);
        String protocolType = "DASH";

        TrackSelection.Factory trackSelectionFactory;
        trackSelectionFactory = new AdaptiveTrackSelection.Factory();

        trackSelector = new DefaultTrackSelector(/* context= */ this, trackSelectionFactory);
        player = new SimpleExoPlayer.Builder(this).setTrackSelector(trackSelector).build();
        player.addListener(eventListener);
        player.setPlayWhenReady(true);

        player.addAnalyticsListener(new EventLogger(trackSelector));

        playerView.setPlayer(player);
        playerView.setKeepScreenOn(true);
        player.prepare(buildMediaSource(videoUrl, protocolType), false, false);

        player.addTextOutput((List<Cue> cues) -> {
            if (subtitleView != null) {
                subtitleView.onCues(cues);
            }
        });
    }

    private MediaSource buildMediaSource(Uri uri, String protocolType) throws Exception {
        MediaSourceFactory mediaSrcFactory = instantiateMediaSourceFactoryByStreamingProtocol(protocolType);
        applyDrmConfigurationToMediaSourceFactory(mediaSrcFactory);
        MediaSource targetMediaSource = mediaSrcFactory.createMediaSource(uri);
        return targetMediaSource;
    }

    private void applyDrmConfigurationToMediaSourceFactory(MediaSourceFactory mediaSrcFactory) {
        DrmSessionManager drmSessionManager = null;
        HttpMediaDrmCallback mediaDrmCallback = createMediaDrmCallback(WIDEVINE_URL);
        mediaDrmCallback.setKeyRequestProperty("Authorization", "Bearer="+jwtToken);

        drmSessionManager = new DefaultDrmSessionManager.Builder().build(mediaDrmCallback);
        mediaSrcFactory.setDrmSessionManager(drmSessionManager);
    }

    private HttpMediaDrmCallback createMediaDrmCallback(String licenseUrl) {
        return new HttpMediaDrmCallback(licenseUrl, new DefaultHttpDataSourceFactory("Player-Test"));
    }

    private MediaSourceFactory instantiateMediaSourceFactoryByStreamingProtocol(String protocolType) throws Exception {
        MediaSourceFactory factory;
        switch (protocolType) {
            case "HLS":
                factory = new HlsMediaSource.Factory(this::newDefaultHttpDataSource);
                break;
            case "DASH":
                factory = new DashMediaSource.Factory(this::newDefaultHttpDataSource);
                break;
            default:
                throw new Exception(String.format("Invalid streaming protocol: '%s'", protocolType));
        }

        return factory;
    }

    final Player.EventListener eventListener = new Player.EventListener() {
        public void log(String type, String message) {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
            String time = dateFormat.format(Calendar.getInstance().getTime());
            Log.d("TestingSetup","-->"+time + "[" + type + "]" + message);
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            log("ERROR", error.getMessage());
        }

        @Override
        public void onTimelineChanged(Timeline timeline, int reason) {
            log("INFO", "Timeline: " + timeline + " Reason: " + reason);
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            log("INFO", "IsPlaying :" + isPlaying);
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            log("INFO", "Loading: " + isLoading);
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            log("INFO", "PlaybackParameters: " + playbackParameters);
        }

        @Override
        public void onPlaybackSuppressionReasonChanged(int playbackSuppressionReason) {
            log("INFO", "PlaybackSuppressionReason: " + playbackSuppressionReason);
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            log("INFO", "PlayerState: " + playbackState + " PlayWhenReady: " + playWhenReady);
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            log("INFO", "PositionDiscontinuity: " + reason);
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            log("INFO", "RepeatMode: " + repeatMode);
        }

        @Override
        public void onSeekProcessed() {
            log("INFO", "SeekProcessed");
        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            log("INFO", "ShuffleModeEnabled: " + shuffleModeEnabled);
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            log("INFO", "TrackGroups: " + trackGroups + " TrackSelections: " + trackSelections);
        }
    };

    private HttpDataSource newDefaultHttpDataSource() {
        HttpDataSource dataSource = new DefaultHttpDataSource("Player-Test");
        dataSource.setRequestProperty("Authorization", "Bearer="+jwtToken);
        return dataSource;
    }

}