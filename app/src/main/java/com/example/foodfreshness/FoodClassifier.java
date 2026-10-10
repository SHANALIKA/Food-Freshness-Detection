package com.example.foodfreshness;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;


public class FoodClassifier {

    // Below this confidence the app says "Not sure" instead of guessing
    private static final float MIN_CONFIDENCE = 0.60f;

    private final Interpreter interpreter;
    private final List<String> labels;

    // Read from the model itself, so a new model with a different
    // input size (56, 128, 224 ...) works without changing the code
    private final int inputWidth;
    private final int inputHeight;
    private final int numClasses;


    public FoodClassifier(Context context) throws IOException {

        interpreter = new Interpreter(loadModelFile(context));

        labels = loadLabels(context);

        int[] inputShape = interpreter.getInputTensor(0).shape();   // [1, H, W, 3]
        inputHeight = inputShape[1];
        inputWidth = inputShape[2];

        int[] outputShape = interpreter.getOutputTensor(0).shape(); // [1, N]
        numClasses = outputShape[1];
    }


    private ByteBuffer loadModelFile(Context context) throws IOException {

        AssetFileDescriptor fileDescriptor =
                context.getAssets().openFd("model.tflite");

        FileInputStream inputStream =
                new FileInputStream(fileDescriptor.getFileDescriptor());

        FileChannel fileChannel = inputStream.getChannel();

        return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.getStartOffset(),
                fileDescriptor.getDeclaredLength()
        );
    }


    private List<String> loadLabels(Context context) throws IOException {

        List<String> list = new ArrayList<>();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("labels.txt"))
        );

        String line;

        while ((line = reader.readLine()) != null) {

            line = line.trim();

            if (!line.isEmpty()) {
                list.add(line);
            }
        }

        reader.close();

        return list;
    }


    /**
     * Crop the centre square of the photo, then shrink it to the model size.
     * Shrinking is done in halving steps. A single huge jump (for example a
     * 4000 px camera photo down to 56 px) throws away most pixels and gives a
     * noisy image that the model has never seen.
     */
    private Bitmap prepare(Bitmap source) {

        Bitmap bmp = source;

        // Hardware bitmaps cannot be read pixel by pixel
        if (bmp.getConfig() == Bitmap.Config.HARDWARE) {
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, false);
        }

        // Centre square crop (no stretching of the fruit)
        int side = Math.min(bmp.getWidth(), bmp.getHeight());
        int x = (bmp.getWidth() - side) / 2;
        int y = (bmp.getHeight() - side) / 2;
        bmp = Bitmap.createBitmap(bmp, x, y, side, side);

        // Halve until we are close to the target size
        int target = Math.max(inputWidth, inputHeight);

        while (bmp.getWidth() / 2 >= target) {
            bmp = Bitmap.createScaledBitmap(
                    bmp, bmp.getWidth() / 2, bmp.getHeight() / 2, true);
        }

        return Bitmap.createScaledBitmap(bmp, inputWidth, inputHeight, true);
    }


    public String classify(Bitmap original) {

        Bitmap bitmap = prepare(original);

        ByteBuffer input = ByteBuffer.allocateDirect(
                4 * inputWidth * inputHeight * 3);

        input.order(ByteOrder.nativeOrder());

        int[] pixels = new int[inputWidth * inputHeight];

        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        // RGB, values 0..1 (same as the training script)
        for (int pixel : pixels) {

            input.putFloat(((pixel >> 16) & 0xFF) / 255f);
            input.putFloat(((pixel >> 8) & 0xFF) / 255f);
            input.putFloat((pixel & 0xFF) / 255f);
        }

        input.rewind();

        float[][] output = new float[1][numClasses];

        interpreter.run(input, output);

        int maxIndex = 0;
        float max = output[0][0];

        for (int i = 1; i < numClasses; i++) {

            if (output[0][i] > max) {
                max = output[0][i];
                maxIndex = i;
            }
        }

        String confidence = String.format(java.util.Locale.US, "%.2f", max * 100);

        if (max < MIN_CONFIDENCE || maxIndex >= labels.size()) {
            return "Not sure (" + confidence + "%)";
        }

        return labels.get(maxIndex) + " (" + confidence + "%)";
    }

}
