# Face Details Tracker (Blink & Smile Counter with Background Floating Overlay)

An Android application that uses the front camera to analyze facial expressions and landmarks in real-time, counting:
- **Total Eye Blinks** (detecting transition from Open $\rightarrow$ Closed $\rightarrow$ Open with debouncing)
- **Total Smiles** (detecting smile probability transitions)
- **Eye Openness Percentage & Smile Probability Percentage**
- **Background Mode**: Allows you to use other applications (WhatsApp, YouTube, Chrome, Games) while continuing face detection via an Android **Foreground Service** and a **Draggable Floating Window Overlay**.

---

## Technical Stack
- **Language**: Kotlin
- **Camera API**: Jetpack CameraX (`Preview`, `ImageAnalysis`)
- **Computer Vision & ML**: Google ML Kit Face Detection (`CLASSIFICATION_MODE_ALL`)
- **Background Tracking**: Android `LifecycleService` with `TYPE_APPLICATION_OVERLAY` (`SYSTEM_ALERT_WINDOW`) and `FOREGROUND_SERVICE_CAMERA`.

---

## Why Floating Window Overlay for Background Tracking?
In modern Android versions (Android 10, 11, 12, 13, and 14+), the Android operating system enforces strict security and privacy restrictions that prevent apps from accessing the camera completely silently in the background when no UI is present. 

To overcome this while adhering to Android platform guidelines:
- We use a **Floating Bubble Window** (`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`).
- This renders a compact, draggable overlay on top of any other app you are using.
- It displays a mini front-camera preview, live Blink and Smile counters, a minimize/expand toggle, and drag controls.
- The front camera keeps capturing and analyzing frames seamlessly in real time!

---

## How to Build the APK

### Option 1: Using Android Studio (Recommended)
1. Open **Android Studio**.
2. Click **File > Open** and select the `c:\work\Sankar\FaceTrackerApp` folder.
3. Wait for Gradle sync to complete.
4. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)** in the top menu.
5. Once built, Android Studio will display a notification saying **APK(s) generated successfully**. Click **locate** to find `app-debug.apk`.
6. Transfer `app-debug.apk` to your phone via USB or WhatsApp/Google Drive and tap to install!

### Option 2: Using Command Line / Gradle
From the project folder, run:
```bash
./gradlew assembleDebug
```
The output APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Phone Permissions Setup
When opening the app for the first time:
1. **Camera Permission**: Tap **Allow** when prompted.
2. **Display over other apps (Overlay)**: Tap **Start Background Tracking**. Android settings will open: find **Face Details Tracker** and toggle **Allow display over other apps** to ON.
3. Return to the app and press **Start Background Tracking**. The floating tracker will appear. You can now press the Home button and use any other app while the tracker continues in the background!
