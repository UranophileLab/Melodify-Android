# Melodify

<div align="center">

[![GitHub Release](https://img.shields.io/github/v/release/UranophileLab/Melodify-Android?style=flat-square&color=F43F5E)](https://github.com/UranophileLab/Melodify-Android/releases)
[![Stars](https://img.shields.io/github/stars/UranophileLab/Melodify-Android?style=flat-square&color=F43F5E)](https://github.com/UranophileLab/Melodify-Android/stargazers)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-brightgreen?style=flat-square)](https://developer.android.com)

**A modern, lightweight, privacy-focused Android music streaming app.**  
Enjoy high-fidelity, ad-free music with an authentic liquid glass interface, customizable themes, and zero tracking.

</div>

---

## Highlights

- **100% Ad-Free & Privacy-First**: Zero ads, no account registration required, no telemetry, and zero user data tracking.
- **Auris Liquid Glass UI**: Translucent floating stadium navigation capsule with traveling specular active pill indicator and smooth spring physics.
- **Vibrant Themes & Dark Mode**: Modern Coral Red & OLED Deep Dark theme support with seamless Light, Dark, or System mode switching.
- **10-Band Built-in Equalizer**: Custom 10-band interactive Equalizer featuring real-time EQ response curves (`EqCurveView`), custom band sliders, and built-in audio presets (Bass Boost, Vocal Booster, Acoustic, Rock, Pop, and more).
- **Variable Playback Speed**: Fine-tune listening speed from 0.5x up to 2.0x in real time.
- **Hi-Res Audio Streaming & Downloads**: Customizable streaming and download bitrates from Lossless (24-bit / 96 kHz Hi-Res) and 320kbps down to 12kbps data-saving mode.
- **Multi-Format Downloads & ID3 Tagging**: Download tracks in MP3, M4A, or OGG format, with embedded high-resolution album artwork and full ID3 metadata tags powered by JAudioTagger.
- **Dual-Source Synchronized Lyrics**: Real-time LRC synchronized scrolling lyrics powered by JioSaavn and LRCLIB integrations with live line tracking and tap-to-toggle lyrics view.
- **Smart Recommendation Engine**: Intelligent recommendation algorithm that queues similar, non-repeating songs based on your current track and listening history.
- **Floating Mini-Player & Smart Queue**: Persistent stadium mini-player with live playback progress, animated audio wave equalizer bars, album art swipe gestures, and drag-and-drop queue reordering.
- **In-Playlist & Global Search**: Instant global search across songs, albums, artists, playlists, and lyrics with quick search chips and live real-time filtering inside albums and playlists.
- **Library Management & JSON Backup**: Organize favorite songs, albums, and custom playlists, with complete JSON **Export & Import** backup and restore capability.
- **Sleep Timer**: Built-in sleep timer with presets (15 to 60 minutes), custom duration input, or "End of Track" auto-pause.
- **Interactive Home Screen Widget**: Control playback and view real-time album artwork directly from your Android home screen.
- **Deep Link Integration**: Seamlessly open JioSaavn and Melodify song, album, artist, and playlist links directly in the app.
- **In-App Auto Updates**: Built-in update checker to automatically notify and update the app directly from GitHub releases.

---

## Getting Started

### Prerequisites

* [Android Studio](https://developer.android.com/studio) (Ladybug / Meerkat or later recommended)
* Java Development Kit (JDK 17+)
* Android SDK with Build Tools (Target SDK 36, Compile SDK 37)
* An Android device or emulator running Android 7.0 (API level 24) or higher

### Building from Source

1. **Clone the repository:**
   ```bash
   git clone https://github.com/UranophileLab/Melodify-Android.git
   cd Melodify-Android
   ```

2. **Open the project in Android Studio:**
   - Select **File > Open...** and choose the `Melodify-Android` folder.
   - Allow Gradle to sync dependencies.

3. **Build and install:**
   - To build a debug APK via CLI:
     ```bash
     ./gradlew assembleDebug
     ```
   - The output APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.
   - Or click **Run** (`Shift + F10`) in Android Studio to deploy directly to your connected device or emulator.

---

## Built With

*   [Kotlin](https://kotlinlang.org/) & [Java 17](https://openjdk.org/projects/jdk/17/) - Modern, expressive language for Android development.
*   [Jetpack Media3 (ExoPlayer)](https://developer.android.com/media/media3/exoplayer) - Next-generation media playback architecture.
*   [Material Components 3](https://m3.material.io/) - Material You design system and UI components.
*   [Ktor Client](https://ktor.io/) & [OkHttp](https://square.github.io/okhttp/) - High-performance asynchronous HTTP networking.
*   [Glide](https://github.com/bumptech/glide) - Fast and efficient image loading and disk caching.
*   [Room Database](https://developer.android.com/training/data-storage/room) - Robust SQLite object mapping for offline libraries, history, and favorites.
*   [JAudioTagger](https://github.com/RouHim/jaudiotagger) - Full ID3 metadata and cover art tagging for audio files.
*   [FFmpegKit](https://github.com/arthenica/ffmpeg-kit) - Audio processing, transcode support, and stream decoding.
*   [JioSaavn API](https://github.com/sumitkolhe/jiosaavn-api) & [LRCLIB](https://lrclib.net/) - High-fidelity audio streams and synchronized LRC lyrics.
*   [Shimmer](https://github.com/facebookarchive/shimmer-android) - Smooth skeleton loading animations.
*   [Overscroll-Decor](https://github.com/EverythingMe/overscroll-decor) - Natural iOS-style overscroll physics.
*   [SSP](https://github.com/intuit/ssp) & [SDP](https://github.com/intuit/sdp) - Scalable dimension units for responsive UI across all screen sizes.

---

## Special Thanks

- Special thanks to [Yug Dabgar](https://github.com/yugdabgar21) for UI/UX design and contributions.
- Featured on [PlayBook](https://github.com/Goodwy/PlayBook).

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
