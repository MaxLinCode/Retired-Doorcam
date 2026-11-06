# Retired Doorcam

A native Android camera prototype for an old phone, currently being developed for a Samsung Galaxy S8 viewing apartment stairs through a window. Person detection and face matching run on the phone. Optional alerts upload recorded clips to Telegram.

## Clone and run on a Mac

Install Android Studio with support for Android Gradle Plugin 9.1, Android SDK Platform 36, and Java 21. The Gradle wrapper is included; no separate Gradle installation is needed.

```bash
git clone https://github.com/MaxLinCode/Retired-Doorcam.git
cd Retired-Doorcam
```

Open this directory in Android Studio and let Gradle sync. Select an installed Java 21 JDK under **Settings → Build, Execution, Deployment → Build Tools → Gradle** if needed. Android Studio creates the machine-specific `local.properties` file; keep it out of Git. The project supports Android 7.0 (API 24) and newer.

On the S8, enable Developer options by tapping **Build number** seven times under **About phone → Software information**, then enable **USB debugging**. Connect with a USB data cable and accept the debugging prompt. Select the phone in Android Studio and click **Run** to install the app and view logs in Logcat.

For command-line use, install platform tools with Homebrew:

```bash
brew install --cask android-platform-tools
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:assembleDebug
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s MainActivity AndroidRuntime
```

The APK contains the local models and native libraries and is about 120 MB. Installing with `-r` preserves app data when the signing key matches. The cloud debug APK and a Mac build may use different debug keys: if Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstalling the old app allows installation but deletes its settings and enrolled faces. Record settings and retain enrollment photos before doing so.

## Current features

- CameraX rear-camera preview with person boxes, using YOLO26.
- Gallery photo enrollment and FaceNet identity matching, with ML Kit face detection.
- Basic person tracking and separate visitor alert decisions.
- Independent unknown-visitor and recognized-person notification switches.
- Telegram video delivery, a **Send Telegram test** button, and visible delivery/error status.
- Adjustable camera resolution and analysis FPS, defaulting to 480p and 3 FPS.
- Local clips in the app-specific external `Movies/Doorcam` directory.

## Setup and testing

1. Grant camera permission.
2. For Telegram, create a bot with `@BotFather`. Paste its token in **Bot Token** and your own numeric ID from `@userinfobot` in **Chat ID**. Open your created bot's chat from that same account and tap **Start**. Enable Telegram alerts and press **Send Telegram test**. Error messages identify token, chat-access, timeout, and network problems. Never share the token or commit it.
3. Add clear photos in **Upload Images**. Add the exact enrolled names to **Safe Identities** in Settings to use them as recognized visitors.
4. Start monitoring and keep that screen open. Walk through the view as an unknown person, a known person, and both together. Leave the view empty for about 15 seconds between visits.
5. Repeat through the actual window in daylight and at night. Check recognition, alert delivery, clip usefulness, reflections, and phone temperature.

Captions and upload results appear on the monitoring screen. The Telegram test sends a text message independently of detection and recording, helping distinguish connection issues from camera logic.

## Prototype limitations

- **Carried-object and delivery detection are not implemented.** Their settings are reserved for future model integration and do not activate a detector.
- The stairway region field is saved but does not filter detections yet.
- Direction detection is an experimental upward-image-motion heuristic; there is no direction calibration yet.
- Tracking can change IDs after occlusion; recognition is not yet confirmed across multiple frames.
- Clips use a small sampled frame buffer preceding the alert, rather than recording the full visit. Only the first alert in a frame is included in its clip caption.
- Background monitoring, event-history UI, automatic storage cleanup, and upload retries are not implemented. Returning from a backgrounded monitoring screen may require restarting monitoring.
- Thermal headroom display requires Android 11; it is unavailable on older stock S8 software.

Treat this version as a development prototype. On-device inference does not prevent recognition errors. Face embeddings are stored in app-private storage; Telegram receives clips when enabled. Android backup behavior follows the application's configured backup rules.

## Managed cloud builds

The cloud environment requires Java proxy options and its system certificate store. Use:

```bash
bash build-cloud.sh
```

This script uses the managed `/workspace` Java, Android SDK, and Gradle cache, and translates the inherited HTTPS proxy into Java settings. On a Mac, use Android Studio or `./gradlew` directly. No cloud proxy settings are stored in the project's Gradle configuration.

## License

MIT; see [LICENSE](LICENSE).
