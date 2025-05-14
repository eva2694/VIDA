# VIDA – Visual Interpretation and Detection Assistant

VIDA is an offline-capable Android application providing real-time assistance to blind and visually impaired users through advanced computer vision and machine learning techniques. The application performs object detection, depth estimation, text recognition, and scene detection entirely on-device, ensuring data privacy, low latency, and independence from internet connectivity.

## Features

### Object Detection
- Utilizes a custom-trained **YOLOv8n** model.
- Supports detection of **196 object classes**.
- Trained on a combination of **Open Images V4** and **Roboflow datasets**.
- Exported to **TensorFlow Lite (TFLite)** format for mobile inference.

### Depth Estimation
- Implements **MiDaS v2.1 small-lite** model for monocular depth estimation.
- Provides relative distance estimations to enhance spatial awareness.
- Runs efficiently on-device without requiring stereo cameras.

### Scene Detection
- Employs a **ResNet-18 model trained on the Places365 dataset**.
- Detects and classifies the user’s environment context (e.g., kitchen, street).
- Model is pre-trained and optimized for mobile deployment.

### Text Recognition (OCR)
- Integrated **Google ML Kit Text Recognition API**.
- Supports real-time recognition of printed text.
- Provides support for both Slovenian and English languages.

### Text-to-Speech (TTS)
- Utilizes the Android **Text-to-Speech (TTS) API** for audio feedback.
- Fully bilingual: **Slovenian** and **English** support.
- Adjustable speech rate and voice preferences.

### Offline Functionality
- All AI models run locally on the device.
- No internet connection required for core functionality.
- Enhances user privacy and ensures fast response times.

## Technical Stack

- **Android (Kotlin, Jetpack Compose, MVVM architecture)**
- **TensorFlow Lite** (YOLOv8n, MiDaS, ResNet-18)
- **Google ML Kit OCR**
- **CameraX API** (real-time image capture)
- **Android TTS API**
- **DataStore Preferences** (for storing user settings)

## Architecture Overview

- **Camera input** is captured using **CameraX**.
- Images are processed through:
  - **YOLOv8n TFLite model** for object detection.
  - **MiDaS TFLite model** for depth estimation.
  - **ResNet-18 Places365** for scene detection.
  - **ML Kit OCR** for text recognition.
- **Non-Maximum Suppression (NMS)** is applied post-detection to refine results.
- Results are presented via **auditory feedback** using the **TTS API**.
- **User settings** (language, TTS speed, theme) are managed through **DataStore**.

## Typical Use Cases

- Navigating unfamiliar environments through object and scene detection.
- Reading labels, documents, and signage via OCR and TTS.
- Enhancing spatial orientation with depth estimation.
- Offering a fully private and offline solution for daily assistance.

## Resources

- [Places365 Scene Classification Dataset](https://github.com/CSAILVision/places365)
- [Open Images Dataset V7 – Ultralytics Documentation](https://docs.ultralytics.com/datasets/detect/open-images-v7/#open-images-v7-pretrained-models)
- [MiDaS v2.1 Small-Lite Model – TensorFlow Lite](https://www.kaggle.com/models/intel/midas/tfLite/v2-1-small-lite/1?tfhub-redirect=true)
- [Open Images Dataset](https://storage.googleapis.com/openimages/web/index.html)
- [Roboflow Crosswalk Dataset](https://universe.roboflow.com/tom-lai-8bp7n/crosswalk-xlzhu)
- [Roboflow Elevator Status Dataset](https://universe.roboflow.com/elevator-0iq4p/elevator-status)
- [Roboflow Arrows Dataset](https://universe.roboflow.com/icons-wrs49/arrows-vnwx8)
