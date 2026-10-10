# Food Freshness Detection

An Android app that tells you whether a piece of fruit is **fresh or rotten**. Take a photo with the camera or pick one from the gallery, and an on-device TensorFlow Lite model classifies it.

## Features

- Splash screen, then the main screen
- Capture an image with the camera or choose one from the gallery
- Preview screen before analysing
- On-device classification (no internet needed) using `model.tflite`
- Result screen showing the predicted label and the real confidence percentage
- Says "Not sure" when the model confidence is below 60%, instead of guessing
- Bottom navigation menu (Home, History, Statistics, Profile). History and Profile are placeholders for now.

## Supported classes

Listed in `app/src/main/assets/labels.txt`. The order must match the model output.

| Index | Label |
|-------|-------|
| 0 | Fresh Apple |
| 1 | Fresh Banana |
| 2 | Fresh Orange |
| 3 | Rotten Apple |
| 4 | Rotten Banana |
| 5 | Rotten Orange |

## Tech stack

- Language: Java (activities) and Kotlin (Compose theme files, tests)
- Build: Gradle (Kotlin DSL), Android Gradle Plugin
- ML: TensorFlow Lite 2.14.0 and TFLite Support 0.4.4
- UI: XML layouts and Material Components
- `minSdk` 24, `targetSdk` and `compileSdk` 35
- Model input: RGB image, float values 0 to 1. The app reads the size (56, 224 ...) from the model itself. Output: 6 class scores.

## App flow

```
SplashActivity -> MainActivity -> PreviewActivity -> ResultActivity
                  (camera/gallery)  (show image)      (label + confidence)
```

`FoodClassifier.java` loads `model.tflite` and `labels.txt` from assets and runs the prediction.

---

## Project structure (what is required)

These are the files and folders that must stay in the project. Without them the app will not build or run.

```
FoodFreshnessDetection/
├── app/
│   ├── build.gradle.kts            # REQUIRED - app module config and dependencies
│   ├── proguard-rules.pro          # REQUIRED - referenced by the release build
│   ├── .gitignore
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # REQUIRED - activities and permissions
│       │   ├── assets/
│       │   │   ├── model.tflite    # REQUIRED - the trained model
│       │   │   └── labels.txt      # REQUIRED - class names, same order as model
│       │   ├── java/com/example/foodfreshness/
│       │   │   ├── SplashActivity.java
│       │   │   ├── MainActivity.java
│       │   │   ├── PreviewActivity.java
│       │   │   ├── ResultActivity.java
│       │   │   ├── FoodClassifier.java
│       │   │   └── ui/theme/       # Color.kt, Theme.kt, Type.kt
│       │   └── res/
│       │       ├── drawable/       # logo, fruit image, button/card shapes
│       │       ├── layout/         # activity_*.xml screens
│       │       ├── menu/           # bottom_menu.xml
│       │       ├── mipmap-*/       # launcher icons
│       │       ├── values/         # colors, strings, themes
│       │       └── xml/            # backup_rules, data_extraction_rules
│       ├── test/                   # unit tests (optional but harmless)
│       └── androidTest/            # instrumented tests (optional but harmless)
├── gradle/
│   ├── libs.versions.toml          # REQUIRED - dependency versions
│   └── wrapper/                    # REQUIRED - gradle-wrapper.jar and .properties
├── build.gradle.kts                # REQUIRED - root build file
├── settings.gradle.kts             # REQUIRED - project name and modules
├── gradle.properties               # REQUIRED - Gradle settings
├── gradlew                         # REQUIRED - Gradle wrapper script (Linux/Mac)
├── gradlew.bat                     # REQUIRED - Gradle wrapper script (Windows)
├── .gitignore                      # recommended
├── README.md                       # this file
├── training/
│   └── train_model.py              # script that makes a new model.tflite (run in Google Colab)
└── dataset/                        # photos used by train_model.py (not used by the app itself)
```

## What to remove or not upload

These are generated automatically or are specific to your machine. They make the project large (the zip was about 85 MB, mostly build output) and are rebuilt when you open the project.

| Folder / file | Why remove it |
|---|---|
| `app/build/` | Generated build output, including the APK and intermediate files. This is the biggest part of the zip. |
| `build/` (root) | Generated build output |
| `.gradle/` | Gradle cache, rebuilt automatically |
| `.kotlin/` | Kotlin session cache |
| `.idea/` | Android Studio settings. If you want to keep any, keep only the small config files. Never keep `.idea/caches/` or `workspace.xml`. |
| `local.properties` | Contains your local SDK path, which differs on every computer. Android Studio recreates it. |
| `*.iml` | IDE module files |
| `dataset/desktop.ini` | Windows system file (already removed) |

> Tip: `.gitignore` already ignores most of these. When zipping, delete them first or exclude them.

## Improving the model (important)

The model that came with the project is a very small CNN (56 x 56 input). When tested on the 25 photos in `dataset/` it got only about 17 of 25 right, and it confidently mixed up fruits (for example a fresh apple labelled "Fresh Banana" at 100%). The app code was not the problem. The model needs to be retrained with more photos.

1. **Collect photos.** At least 100 per class (600 total), taken with your own phone, with different backgrounds and lighting. Sort them like this:

   ```
   dataset/
     fresh_apple/    rotten_apple/
     fresh_banana/   rotten_banana/
     fresh_orange/   rotten_orange/
   ```

   Loose files named like `rotten apple 3.jpg` also work (the script sorts them by name), but folders are safer.
2. **Train in Google Colab** (free). Choose Runtime > Change runtime type > T4 GPU, upload the `dataset` folder and `training/train_model.py`, then run `!python train_model.py`.
3. **Replace the model.** Download `output/model.tflite` and `output/labels.txt` and copy both into `app/src/main/assets/`. No code changes are needed. The app reads the input size from the model.
4. **Test with NEW photos** that were not used for training. Accuracy on training photos means nothing.

The class order is fixed: Fresh Apple, Fresh Banana, Fresh Orange, Rotten Apple, Rotten Banana, Rotten Orange. It must match `labels.txt`.

If the app crashes with a "Didn't find op" error after changing the model, raise the `tensorflow-lite` version in `app/build.gradle.kts`.

## Code fixes made

- `ResultActivity`: the confidence was hard-coded as "98.50%" for every photo. It now shows the real value.
- `ResultActivity`: anything not labelled "rotten" used to show "Safe to eat". It now shows "Looks fresh", "Not recommended for eating" or "Not sure - try a clearer photo".
- `FoodClassifier`: the photo is centre-cropped (no stretching) and shrunk in steps, input size is read from the model, and low-confidence results show "Not sure".

## Build and run

1. Install Android Studio and open the `FoodFreshnessDetection` folder.
2. Let Gradle sync finish. `local.properties` is created for you.
3. Connect a device or start an emulator (Android 7.0 / API 24 or higher).
4. Press **Run**.

Or from the command line:

```bash
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/`.

## Permissions

- `CAMERA` for taking photos (camera hardware is optional)
- `READ_MEDIA_IMAGES` for the gallery on Android 13+
- `READ_EXTERNAL_STORAGE` for the gallery on Android 12 and below

## Known limitations and future work

- Only apples, bananas and oranges are supported.
- A "Looks fresh" result is only a guess from a photo. Do not rely on it for food safety.
- The current model is weak (see "Improving the model"). Accuracy depends on retraining with more photos.
- History and Profile screens are not implemented yet. The code has a note to add history saving later.
"# Food-Freshness-Detection" 
