package com.example.foodfreshness;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;


public class MainActivity extends AppCompatActivity {


    private CardView cardCamera;
    private CardView cardGallery;
    private CardView cardTips;

    private BottomNavigationView bottomNavigation;


    private Uri imageUri;



    // Gallery Launcher

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {


                        if(result.getResultCode() == RESULT_OK
                                && result.getData() != null){


                            imageUri = result.getData().getData();


                            openPreview(imageUri);


                        }


                    }
            );






    // Camera Launcher

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {


                        if(result.getResultCode() == RESULT_OK){


                            if(imageUri != null){


                                openPreview(imageUri);


                            }


                        }


                    }
            );







    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);



        cardCamera = findViewById(R.id.cardCamera);
        cardGallery = findViewById(R.id.cardGallery);
        cardTips = findViewById(R.id.cardTips);


        bottomNavigation = findViewById(R.id.bottomNavigation);







        // CAMERA BUTTON


        cardCamera.setOnClickListener(v -> {


            if(ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED){



                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.CAMERA
                        },
                        100
                );



            }
            else{


                openCamera();


            }



        });







        // GALLERY BUTTON


        cardGallery.setOnClickListener(v -> {



            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );


            galleryLauncher.launch(intent);



        });







        // TIPS BUTTON


        cardTips.setOnClickListener(v -> {


            Toast.makeText(
                    this,
                    "Fresh food has good colour, firm texture and fresh smell.",
                    Toast.LENGTH_LONG
            ).show();



        });







        // BOTTOM NAVIGATION


        bottomNavigation.setOnItemSelectedListener(item -> {


            int id = item.getItemId();



            if(id == R.id.nav_home){


                return true;


            }
            else if(id == R.id.nav_history){


                Toast.makeText(
                        this,
                        "History Selected",
                        Toast.LENGTH_SHORT
                ).show();


                return true;


            }
            else if(id == R.id.nav_profile){


                Toast.makeText(
                        this,
                        "Profile Selected",
                        Toast.LENGTH_SHORT
                ).show();


                return true;


            }



            return false;


        });



    }









    // OPEN CAMERA


    private void openCamera(){



        ContentValues values = new ContentValues();


        values.put(
                MediaStore.Images.Media.TITLE,
                "Food_Image"
        );


        values.put(
                MediaStore.Images.Media.DESCRIPTION,
                "Captured Food Image"
        );



        imageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );





        Intent cameraIntent = new Intent(
                MediaStore.ACTION_IMAGE_CAPTURE
        );



        cameraIntent.putExtra(
                MediaStore.EXTRA_OUTPUT,
                imageUri
        );




        if(cameraIntent.resolveActivity(getPackageManager()) != null){


            cameraLauncher.launch(cameraIntent);


        }
        else{


            Toast.makeText(
                    this,
                    "Camera not available",
                    Toast.LENGTH_SHORT
            ).show();


        }



    }









    // OPEN PREVIEW ACTIVITY


    private void openPreview(Uri uri){



        Intent intent = new Intent(
                MainActivity.this,
                PreviewActivity.class
        );



        intent.putExtra(
                "imageUri",
                uri.toString()
        );



        startActivity(intent);



    }









    // CAMERA PERMISSION RESULT


    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ){



        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );



        if(requestCode == 100){



            if(grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED){



                openCamera();



            }
            else{


                Toast.makeText(
                        this,
                        "Camera permission required",
                        Toast.LENGTH_SHORT
                ).show();



            }


        }



    }



}