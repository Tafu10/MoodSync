# Project Overview: MoodSync

## The Goal
MoodSync is a native Android application built in Kotlin. It is an intelligent multimedia adaptation and security system based on real-time facial analysis. The app interacts with the user by processing video feeds in real-time to unlock private sections and adapt multimedia content based on detected emotions.

## Core Features & Objectives
1. **Secure Access (Face Match):** A facial recognition module used as a biometric lock to unhide/unlock specific sections of the app (e.g., a private media gallery).
2. **Emotion Analysis:** Continuous scanning of the user's face to detect mood (Happiness, Sadness, Surprise) using Machine Learning.
3. **Multimedia Adaptation:** Based on the detected emotion, the app will:
   - Apply real-time visual graphical filters over the camera feed (e.g., warm colors for happiness, grayscale/cool colors for sadness).
   - Dynamically recommend and play audio playlists (integrating a visualizer and media playback).

## Tech Stack & Libraries
- **Language:** Kotlin (Strictly modern Kotlin).
- **UI Framework:** Jetpack Compose (Material Design 3). NO XML layouts.
- **Camera:** CameraX (Preview, ImageAnalysis use cases).
- **Machine Learning:** - Google ML Kit (for fast Face Detection and cropping).
  - TensorFlow Lite (for Emotion Classification on the cropped face).
- **Multimedia:** Media3 / ExoPlayer (for audio playback).
- **Backend/Storage:** Firebase (Auth, Firestore for playlist metadata, Storage for private gallery).
- **Architecture:** MVVM (Model-View-ViewModel) with Kotlin Coroutines and StateFlow for state management.

## Architecture & Data Flow
1. **Camera Input:** CameraX captures frames.
2. **Processing Split:** Frames are sent to both the Compose UI (`Preview`) and the ML Analyzer.
3. **ML Engine:** ML Kit detects the face bounding box -> crops the image -> TFLite model classifies the emotion.
4. **Decision Logic:** The `ViewModel` receives the emotion state and updates the UI state.
5. **Output:** Compose UI triggers the graphical filter overlay, and the Audio Player queues the relevant playlist.

## Strict Instructions for the AI Agent
When assisting with this project, you MUST adhere to the following rules:
1. **Compose First:** Always use Jetpack Compose for UI. Never generate XML unless strictly required for a legacy manifest configuration.
2. **Modern State Management:** Use `ViewModel`, `StateFlow`, and `collectAsState()` for passing data from the ML engine to the UI.
3. **Permissions:** Always ensure runtime permissions (Camera, Storage/Audio) are handled gracefully using Accompanist or the native Compose permissions API.
4. **Performance:** The CameraX `ImageAnalysis` analyzer must run on a background thread/executor to avoid blocking the Main (UI) thread. Drop frames if the ML model cannot keep up (`STRATEGY_KEEP_ONLY_LATEST`).
5. **Step-by-Step:** Do not write the entire application at once. Wait for the user to prompt you for the next specific step (e.g., "Implement CameraX setup", "Add ML Kit dependency").
6. **Context Awareness:** Before generating code, always check the current state of `build.gradle.kts` and `AndroidManifest.xml` to ensure dependencies and permissions match the code you are about to write.