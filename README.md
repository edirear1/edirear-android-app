# Edirear Android App (Native Client)

This is the official native Android application for **Edirear**, engineered to function identically to the YouTube app & website ecosystem:
- **Shared Database & Storage**: Both this Android app and the Edirear website talk to the exact same backend server and database (`edirear.db`). Every video uploaded, comment posted, message sent, order made, or user profile registered on either the app or the website is instantly synchronized in real time.
- **Identical Theme & Pages**: Matches Edirear's signature dark aesthetic (`#000000` background with `#25d366` green accent), mobile bottom navigation bar, video stream player, reels/pearls, seller portal, live streams, and messenger.
- **Native Hardware Integration**:
  - File/Video Chooser: Select videos (MP4, WebM, MOV), photos, and custom thumbnails directly from your gallery or camera.
  - Full-screen Video Player with automatic landscape orientation.
  - Live Stream Camera & Microphone WebRTC bridges.
  - Swipe-to-Refresh & Offline connection recovery.
  - Native Android Download Manager for offline videos.
  - Cookie persistence for lifelong user sessions.

---

## 1. How Database Synchronization Works (Like YouTube)

```
        ┌─────────────────────────────────────────────────┐
        │                 Edirear Website                 │
        │             (Chrome / Safari / Edge)            │
        └────────────────────────┬────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Central Edirear Server                       │
│           (Flask Backend + edirear.db + Media Uploads)           │
│                     URL: https://edirear.com                    │
└────────────────────────────────┬────────────────────────────────┘
                                 ▲
                                 │
        ┌────────────────────────┴────────────────────────┐
        │                Edirear Android App              │
        │               (APK / Native Client)             │
        └─────────────────────────────────────────────────┘
```

When you upload a video on the Android App, it is uploaded directly to the central server's `static/uploads/` directory and stored in the server's `edirear.db`. Anyone visiting the website sees it immediately—just like when you upload a video on the YouTube phone app, it appears instantly on youtube.com!

---

## 2. Server Configuration

Open `app/src/main/java/com/edirear/app/Config.java`:

```java
// Change this to your live production domain or testing server
public static final String DEFAULT_URL = "https://edirear.com";
```
- **Live Server**: Set to your domain (e.g. `https://edirear.com` or your VPS/Railway/Render URL).
- **Local Wi-Fi Testing**: If running `python app.py` on your computer, set to `http://192.168.x.x:5000` (your PC's local IP).
- **Android Emulator**: Set to `http://10.0.2.2:5000`.

---

## 3. How to Get the APK File

### Option A: Free 1-Click Cloud Build (GitHub Actions - Recommended)
1. Push or upload this `edirear_android_app` folder to a GitHub repository (can be public or private).
2. Go to the **Actions** tab on your GitHub repository.
3. The build will run automatically in ~90 seconds.
4. Click on the completed workflow and download **`edirear-app-debug.apk`** from the **Artifacts** section!
5. Send the `.apk` file to your Android phone via WhatsApp, Telegram, Google Drive, or USB and tap to install!

### Option B: Build with Android Studio
1. Open **Android Studio**.
2. Click **Open** and select the `edirear_android_app` folder.
3. Wait for Gradle sync to complete.
4. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Android Studio will notify you: `APK(s) generated successfully`. Click **locate** to get `app-debug.apk`.

### Option C: Instant PWA WebAPK (Zero build needed!)
1. Open your Edirear website in Google Chrome or Samsung Internet on any Android phone.
2. Tap the **📲 Install App** button on the page or tap the browser menu (⋮) > **"Install App"** / **"Add to Home screen"**.
3. Android's OS will generate and install a native **WebAPK** onto your home screen and app drawer immediately!
