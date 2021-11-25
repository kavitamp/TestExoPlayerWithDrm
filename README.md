# Overview:
This project helps to play the Widevine DRM-protected video in Android ExoPlayer. Check [Quick Guide: Integrating Widevine DRM In ExoPlayer For Android](https://blog.kiprosh.com/widevine-drm-setup-in-android-exoplayer/) blog for more details.

To play the DRM-protected video in ExoPlayer, you will require the below URLs from the backend setup:
1. Manifest URL (Media file URL)
2. Widevine license URL
3. JWT token (A way of authorizing users who make a decryption key request)

Update `WIDEVINE_URL`, `URL` & `jwtToken` fields in `PlayerActivity` with required data.

## DRM setup in Android ExoPlayer:

1. Add ExoPlayer dependency in `build.gradle`:

    ```java
    implementation 'com.google.android.exoplayer:exoplayer:2.10.1'
    ```

2. Add PlayerView in the layout:

    ```java
    <com.google.android.exoplayer2.ui.PlayerView
       android:id="@+id/player_view"
       android:layout_width="match_parent"
       android:layout_height="match_parent" />
    ```
   
3. ExoPlayer supports different streaming methods, including MPEG-DASH, HLS, SmoothStreaming. As I'm performing DASH implementation in the project, hence I need to use `DashMediaSource` class and pass the `manifestURL`.

    ```java
    MediaSourceFactory mediaSrcFactory = new DashMediaSource.Factory(this::newDefaultHttpDataSource);
    MediaSource targetMediaSource = mediaSrcFactory.createMediaSource(manifestURL);
    ```

4. Before creating a media source we need to instantiate `HttpMediaDrmCallback` by adding the `widevineURL` in its constructor.
`HttpMediaDrmCallback` helps to perform the Widevine license exchange.

    ```java
    HttpMediaDrmCallback mediaDrmCallback = new HttpMediaDrmCallback(widevineURL, new DefaultHttpDataSourceFactory(userAgentString));
    DrmSessionManager drmSessionManager = new DefaultDrmSessionManager.Builder().build(mediaDrmCallback);
    mediaSrcFactory.setDrmSessionManager(drmSessionManager);
    ```

5. The license server needs an authentication token before issuing the license. Hence while requesting a DRM license, we need to append the token authorization header in the request. Here I will be sending the `jwtToken` in the request with the help of `HttpMediaDrmCallback` instance.

    ```java
    mediaDrmCallback.setKeyRequestProperty("Authorization", "Bearer="+jwtToken);
    ```

That's it!
Run the code and watch the DRM-protected video in the native ExoPlayer.
