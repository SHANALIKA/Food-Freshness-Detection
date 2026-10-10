package com.example.foodfreshness;


import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class PreviewActivity extends AppCompatActivity {


    ImageView imagePreview;
    Button btnAnalyze;

    Bitmap selectedBitmap;

    FoodClassifier classifier;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preview);



        imagePreview = findViewById(R.id.imagePreview);
        btnAnalyze = findViewById(R.id.btnAnalyze);



        // Load AI Model

        try {

            classifier = new FoodClassifier(this);

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Model loading failed",
                    Toast.LENGTH_SHORT
            ).show();

        }





        // Receive Gallery Image

        String imageUri =
                getIntent().getStringExtra("imageUri");



        if(imageUri != null) {

            try {

                selectedBitmap =
                        MediaStore.Images.Media.getBitmap(
                                getContentResolver(),
                                Uri.parse(imageUri)
                        );


                imagePreview.setImageBitmap(
                        selectedBitmap
                );


            } catch (IOException e) {

                e.printStackTrace();

            }

        }







        // Receive Camera Image

        Bitmap cameraImage = null;


        if(android.os.Build.VERSION.SDK_INT >= 33) {


            cameraImage =
                    getIntent().getParcelableExtra(
                            "cameraImage",
                            Bitmap.class
                    );


        } else {


            cameraImage =
                    getIntent().getParcelableExtra(
                            "cameraImage"
                    );

        }




        if(cameraImage != null) {

            selectedBitmap = cameraImage;

            imagePreview.setImageBitmap(
                    selectedBitmap
            );

        }







        // Analyze Button

        btnAnalyze.setOnClickListener(v -> {



            if(selectedBitmap == null) {


                Toast.makeText(
                        this,
                        "Please select image first",
                        Toast.LENGTH_SHORT
                ).show();


                return;

            }




            if(classifier == null) {


                Toast.makeText(
                        this,
                        "AI Model unavailable",
                        Toast.LENGTH_SHORT
                ).show();


                return;

            }





            String prediction =
                    classifier.classify(selectedBitmap);





            Intent intent =
                    new Intent(
                            PreviewActivity.this,
                            ResultActivity.class
                    );




            // Send Prediction

            intent.putExtra(
                    "prediction",
                    prediction
            );





            // Save Image Path

            String imagePath =
                    saveBitmap(selectedBitmap);



            intent.putExtra(
                    "imagePath",
                    imagePath
            );



            startActivity(intent);



        });



    }





    // Save bitmap to cache

    private String saveBitmap(Bitmap bitmap) {


        try {


            File file =
                    new File(
                            getCacheDir(),
                            "fruit_scan.jpg"
                    );


            FileOutputStream fos =
                    new FileOutputStream(file);



            bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    fos
            );


            fos.flush();
            fos.close();



            return file.getAbsolutePath();



        } catch(Exception e) {


            e.printStackTrace();

            return null;

        }

    }


}