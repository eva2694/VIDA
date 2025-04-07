# Pomocnik

## Description
Pomocnik is an Android application developed to assist visually impaired users with navigation, orientation, and reading. It supports both Slovenian and English, and leverages on-device machine learning and text-to-speech (TTS) technologies for real-time assistance.

## Features
- Real-time object detection using YOLOv8
- Depth estimation with MiDaS for spatial awareness
- Text recognition via Google ML Kit OCR
- Text-to-speech support (Google TTS recommended, especially for Slovenian)
- Dual language support: Slovenian and English
- Configurable reading speed and dark mode preference
   
## Installation

1. Open the project in Android Studio.
2. Make sure the following requirements are met:
   - Android SDK (API level 31 or above)
   - Gradle 8 or higher
   - Permissions for Camera and Internet granted on the device
3. Connect an Android device (or use an emulator) and run the app.

## Usage

- Grant all requested permissions on first launch.
- The app opens in portrait mode and begins in the splash screen.
- Navigate through screens such as assist, read, or explore with voice feedback and guidance.
- Change TTS language, voice speed, or UI theme in the settings screen.

## Note for Samsung Phone Users

Samsung TTS does not support Slovenian. Please switch to Google TTS:

1. Go to **Settings > General Management > Text-to-speech output**
2. Select **Google Text-to-speech Engine**

## License

This project is licensed under the University of Ljubljana (UL).
