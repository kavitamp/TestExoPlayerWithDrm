# Overview:
This project helps to play the Widevine DRM-protected video in Android ExoPlayer.
To play the DRM-protected video in ExoPlayer, you will require the below URLs from the backend setup:
1. Manifest URL (Media file URL)
2. Widevine license URL
3. JWT token (A way of authorizing users who make a decryption key request)

Update `WIDEVINE_URL`, `URL` & `jwtToken` fields in `PlayerActivity` with required data.
