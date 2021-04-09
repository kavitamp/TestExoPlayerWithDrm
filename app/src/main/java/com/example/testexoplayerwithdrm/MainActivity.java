package com.example.testexoplayerwithdrm;

import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private PlayerView playerView;
    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private String url = "";
    private String widevineDrmLicenseUrl = "";
    private UUID widevineDrmSchemeUuid;
    DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playerView = findViewById(R.id.playerView);
        initializePlayer();
    }

    private void initializePlayer() {
        widevineDrmSchemeUuid = Util.getDrmUuid(C.WIDEVINE_UUID.toString());
        drmSessionManager = drmSessionManager(widevineDrmSchemeUuid, widevineDrmLicenseUrl, true);
        if (player == null) {
            trackSelector = new DefaultTrackSelector();
            trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSize(200, 200));
            player = ExoPlayerFactory.newSimpleInstance(getApplicationContext(), trackSelector, new DefaultLoadControl(), drmSessionManager);
            playerView.setPlayer(player);
            player.setPlayWhenReady(true);
        }
        DashMediaSource dashMediaSource = setDashMediaSource(Uri.parse(url));
        player.prepare(dashMediaSource, true, false);
    }

    private DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager(UUID uuid, String licenseUrl, boolean multiSession) {
        HttpDataSource.Factory licenseDataSourceFactory = new DefaultHttpDataSourceFactory(Util.getUserAgent(this, getPackageName()));
        HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory);
        FrameworkMediaDrm mediaDrm = null;
        try {
            mediaDrm = FrameworkMediaDrm.newInstance(uuid);
        } catch (UnsupportedDrmException e) {
            e.printStackTrace();
        }
        return new DefaultDrmSessionManager(uuid, mediaDrm, drmCallback, null, multiSession);
    }

    private DashMediaSource setDashMediaSource(Uri uri) {
        String userAgent = "ExoPlayer-Drm";
        DashChunkSource.Factory dashChunkSourceFactory = new DefaultDashChunkSource.Factory(new DefaultHttpDataSourceFactory("userAgent", new DefaultBandwidthMeter()));
        DefaultHttpDataSourceFactory manifestDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent);
        return new DashMediaSource.Factory(dashChunkSourceFactory, manifestDataSourceFactory).createMediaSource(uri);
    }
}