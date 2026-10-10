"""
Food Freshness Detection - model training script
=================================================
Makes a new model.tflite + labels.txt for the Android app.

HOW TO RUN (Google Colab, free GPU):
  1. Open https://colab.research.google.com  ->  New notebook
  2. Runtime -> Change runtime type -> T4 GPU
  3. Upload the project's  dataset  folder to Colab  (left side Files panel).
     Photos can be loose files named like  "rotten apple 3.jpg"
     OR sorted into folders (better, see below).
  4. Upload this file, then run in a cell:
         !python train_model.py
  5. Download  model.tflite  and  labels.txt  from the  output/  folder and put
     both into  app/src/main/assets/  (replace the old ones).

PHOTOS: this matters more than anything else in the script.
  The old dataset has only ~25 photos. Use at least 100 photos per class (600
  total). Take them with your own phone, on different backgrounds and lighting,
  so the model learns what your camera sees. Sort them like this:

      dataset/
        fresh_apple/     rotten_apple/
        fresh_banana/    rotten_banana/
        fresh_orange/    rotten_orange/

  If the folders are missing, the script sorts loose files by their file name
  (it looks for the words apple / banana / orange and rotten).
"""

import os
import re
import shutil
from pathlib import Path

import numpy as np
import tensorflow as tf
from PIL import Image

# ----------------------------------------------------------------- settings
DATASET_DIR = "dataset"        # your photos
WORK_DIR = "data_sorted"       # script copies sorted photos here
OUTPUT_DIR = "output"          # model.tflite + labels.txt end up here
IMG_SIZE = 224                 # MobileNetV2 size. The app reads this from the model.
BATCH = 16
EPOCHS_HEAD = 25               # train the new top layer
EPOCHS_FINE = 15               # then fine-tune the pretrained layers
SEED = 42

# The ORDER here must match labels.txt in the app. Do not change it.
CLASSES = [
    "fresh_apple", "fresh_banana", "fresh_orange",
    "rotten_apple", "rotten_banana", "rotten_orange",
]
LABEL_NAMES = [
    "Fresh Apple", "Fresh Banana", "Fresh Orange",
    "Rotten Apple", "Rotten Banana", "Rotten Orange",
]
IMAGE_EXT = (".jpg", ".jpeg", ".png", ".bmp", ".webp")


# ------------------------------------------------------- 1. organise photos
def class_from_filename(name: str):
    n = name.lower()
    if "apple" in n:
        fruit = "apple"
    elif "banana" in n:
        fruit = "banana"
    elif re.search(r"or+ange", n):          # also matches the "orrange" typo
        fruit = "orange"
    else:
        return None
    return ("rotten_" if "rotten" in n else "fresh_") + fruit


def organise():
    if os.path.exists(WORK_DIR):
        shutil.rmtree(WORK_DIR)
    for c in CLASSES:
        os.makedirs(os.path.join(WORK_DIR, c))

    count = 0
    for path in Path(DATASET_DIR).rglob("*"):
        if not path.is_file() or path.suffix.lower() not in IMAGE_EXT:
            if path.is_file() and path.name.lower() != "desktop.ini":
                print("  skipped (unsupported file):", path.name)
            continue
        # Use the folder name if photos are already sorted, else the file name
        cls = path.parent.name if path.parent.name in CLASSES \
            else class_from_filename(path.name)
        if cls is None:
            print("  skipped (cannot tell the class):", path.name)
            continue
        try:
            Image.open(path).verify()           # skip broken images
        except Exception:
            print("  skipped (broken image):", path.name)
            continue
        shutil.copy(path, os.path.join(WORK_DIR, cls, f"{count:05d}{path.suffix.lower()}"))
        count += 1

    print("\nPhotos per class:")
    for c in CLASSES:
        n = len(os.listdir(os.path.join(WORK_DIR, c)))
        warn = "   <-- too few, add more photos" if n < 100 else ""
        print(f"  {c:15s} {n:4d}{warn}")
    return count


# ------------------------------------------------------------ 2. load data
def load_data(total):
    kwargs = dict(
        class_names=CLASSES,               # fixes the class order
        label_mode="categorical",
        image_size=(IMG_SIZE, IMG_SIZE),
        crop_to_aspect_ratio=True,         # centre crop, same as the app
        batch_size=BATCH,
        seed=SEED,
    )
    # With a tiny dataset a validation split is meaningless, so only split when
    # there are enough photos. Otherwise test the finished model on NEW photos.
    if total >= 150:
        train = tf.keras.utils.image_dataset_from_directory(
            WORK_DIR, validation_split=0.2, subset="training", **kwargs)
        val = tf.keras.utils.image_dataset_from_directory(
            WORK_DIR, validation_split=0.2, subset="validation", **kwargs)
    else:
        print("\nWARNING: fewer than 150 photos - training on ALL of them with "
              "no validation set. Accuracy numbers below are NOT reliable.\n")
        train = tf.keras.utils.image_dataset_from_directory(WORK_DIR, **kwargs)
        val = None

    # Scale pixels to 0..1. This is exactly what the Android app sends.
    norm = lambda x, y: (x / 255.0, y)
    train = train.map(norm).cache().shuffle(1000, seed=SEED).prefetch(tf.data.AUTOTUNE)
    if val is not None:
        val = val.map(norm).cache().prefetch(tf.data.AUTOTUNE)
    return train, val


# ------------------------------------------------------------- 3. the model
def build_model():
    # Augmentation is only active during training. Colour is NOT changed
    # (hue shifts would turn a fresh banana into something else).
    augment = tf.keras.Sequential([
        tf.keras.layers.RandomFlip("horizontal_and_vertical"),
        tf.keras.layers.RandomRotation(0.15),
        tf.keras.layers.RandomZoom((-0.25, 0.1)),
        tf.keras.layers.RandomTranslation(0.1, 0.1),
        tf.keras.layers.RandomBrightness(0.2, value_range=(0.0, 1.0)),
        tf.keras.layers.RandomContrast(0.2),
    ], name="augment")

    base = tf.keras.applications.MobileNetV2(
        input_shape=(IMG_SIZE, IMG_SIZE, 3), include_top=False, weights="imagenet")
    base.trainable = False

    inputs = tf.keras.Input(shape=(IMG_SIZE, IMG_SIZE, 3))     # 0..1 from the app
    x = augment(inputs)
    x = tf.keras.layers.Rescaling(2.0, offset=-1.0)(x)          # 0..1 -> -1..1 (MobileNetV2)
    x = base(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.3)(x)
    outputs = tf.keras.layers.Dense(len(CLASSES), activation="softmax")(x)
    return tf.keras.Model(inputs, outputs), base


def train(model, base, train_ds, val_ds):
    cb = [tf.keras.callbacks.EarlyStopping(
        monitor="val_accuracy" if val_ds is not None else "accuracy",
        patience=8, restore_best_weights=True)]

    model.compile(optimizer=tf.keras.optimizers.Adam(1e-3),
                  loss="categorical_crossentropy", metrics=["accuracy"])
    model.fit(train_ds, validation_data=val_ds, epochs=EPOCHS_HEAD, callbacks=cb)

    # Fine-tune the last part of MobileNetV2 with a very small learning rate
    base.trainable = True
    for layer in base.layers[:-30]:
        layer.trainable = False
    model.compile(optimizer=tf.keras.optimizers.Adam(1e-5),
                  loss="categorical_crossentropy", metrics=["accuracy"])
    model.fit(train_ds, validation_data=val_ds, epochs=EPOCHS_FINE, callbacks=cb)


# ------------------------------------------------------------- 4. export
def export(model):
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]   # smaller file, input stays float32
    tflite_model = converter.convert()
    with open(os.path.join(OUTPUT_DIR, "model.tflite"), "wb") as f:
        f.write(tflite_model)
    with open(os.path.join(OUTPUT_DIR, "labels.txt"), "w") as f:
        f.write("\n".join(LABEL_NAMES))
    print(f"\nSaved {OUTPUT_DIR}/model.tflite ({len(tflite_model)/1e6:.1f} MB) and labels.txt")


def check_tflite():
    """Run the exported model on every training photo, the same way the app does."""
    interp = tf.lite.Interpreter(model_path=os.path.join(OUTPUT_DIR, "model.tflite"))
    interp.allocate_tensors()
    inp, out = interp.get_input_details()[0], interp.get_output_details()[0]
    ok = total = 0
    for ci, c in enumerate(CLASSES):
        for f in os.listdir(os.path.join(WORK_DIR, c)):
            im = Image.open(os.path.join(WORK_DIR, c, f)).convert("RGB")
            s = min(im.size)
            l, t = (im.width - s) // 2, (im.height - s) // 2
            im = im.crop((l, t, l + s, t + s)).resize((IMG_SIZE, IMG_SIZE), Image.BILINEAR)
            x = (np.asarray(im, dtype=np.float32) / 255.0)[None]
            interp.set_tensor(inp["index"], x)
            interp.invoke()
            ok += int(np.argmax(interp.get_tensor(out["index"])) == ci)
            total += 1
    print(f"TFLite check on the training photos: {ok}/{total} correct")
    print("(These photos were used for training. Real accuracy = test with NEW photos.)")


if __name__ == "__main__":
    tf.keras.utils.set_random_seed(SEED)
    n = organise()
    if n == 0:
        raise SystemExit("No photos found. Put them in the 'dataset' folder.")
    train_ds, val_ds = load_data(n)
    model, base = build_model()
    train(model, base, train_ds, val_ds)
    export(model)
    check_tflite()
