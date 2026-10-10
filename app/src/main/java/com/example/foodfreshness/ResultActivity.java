package com.example.foodfreshness;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;



public class ResultActivity extends AppCompatActivity {


    ImageView resultImage;

    TextView txtPrediction;
    TextView txtConfidence;
    TextView txtRecommendation;

    Button btnSave;



    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_result);




        resultImage = findViewById(R.id.resultImage);

        txtPrediction = findViewById(R.id.txtPrediction);

        txtConfidence = findViewById(R.id.txtConfidence);

        txtRecommendation = findViewById(R.id.txtRecommendation);

        btnSave = findViewById(R.id.btnSave);







        // Receive Image Path

        String imagePath =
                getIntent().getStringExtra("imagePath");



        if(imagePath != null){


            Bitmap bitmap =
                    BitmapFactory.decodeFile(imagePath);



            if(bitmap != null){

                resultImage.setImageBitmap(bitmap);

            }


        }








        // Receive Prediction

        String prediction =
                getIntent().getStringExtra("prediction");



        if(prediction != null){


            txtPrediction.setText(
                    prediction
            );


        }else{


            txtPrediction.setText(
                    "Unknown"
            );

        }







        // Confidence

        String confidenceText = "Confidence : --";
        if(prediction != null){
            int open = prediction.lastIndexOf('(');
            int close = prediction.lastIndexOf(')');
            if(open >= 0 && close > open){
                confidenceText = "Confidence : " +
                        prediction.substring(open + 1, close);
            }
        }
        txtConfidence.setText(confidenceText);








        // Recommendation

        String lower = prediction == null ? "" : prediction.toLowerCase();

        if(lower.contains("rotten")){

            txtRecommendation.setText(
                    "\u274C Not recommended for eating"
            );

        }else if(lower.contains("fresh")){

            txtRecommendation.setText(
                    "\u2705 Looks fresh"
            );

        }else{

            txtRecommendation.setText(
                    "\u2753 Not sure - try a clearer photo"
            );

        }

        // Save Button

        btnSave.setOnClickListener(v -> {


            // Add save history later


        });



    }


}