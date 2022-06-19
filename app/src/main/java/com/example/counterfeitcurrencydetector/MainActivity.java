package com.example.counterfeitcurrencydetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.counterfeitcurrencydetector.ml.SrilankanCounterfeitCurrencyDetector;
import com.example.counterfeitcurrencydetector.ml.SrilankanCurrencyAmountDetector;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final int CAMERA_PERM_CODE = 1;
    public static final int CAMERA_REQUEST_CODE = 2;
    public static final int GALLERY_REQUEST_CODE = 3;
    TextToSpeech textToSpeech;
    ImageView currency;
    TextView currency_status, currency_amount;
    Button camera, gallery;
    int imgSize = 150;
    String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currency = (ImageView) findViewById(R.id.currency);
        currency_status = (TextView) findViewById(R.id.currency_status);
        currency_amount = (TextView) findViewById(R.id.currency_amount);
        camera = (Button) findViewById(R.id.camera);
        gallery = (Button) findViewById(R.id.gallery);
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i==TextToSpeech.SUCCESS){
                    int language = textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askCameraPermission();
            }
        });

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
            }
        });

    }

    private void askCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        }
        else {
            dispatchTakePictureIntent();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == CAMERA_PERM_CODE) {
            if(grantResults.length>0 && grantResults[1]==PackageManager.PERMISSION_GRANTED){
                dispatchTakePictureIntent();
            }
        }
        else {
            Toast.makeText(this, "Camera permission is required to open Camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void counterfeitDetect(Bitmap image){
        try {
            SrilankanCounterfeitCurrencyDetector model = SrilankanCounterfeitCurrencyDetector.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 150, 150, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imgSize * imgSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imgSize * imgSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getWidth());

            int pixelValue = 0;
            for(int i=0; i<imgSize; i++){
                for(int j=0; j<imgSize; j++){
                    int val = intValues[pixelValue++];
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 1));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            SrilankanCounterfeitCurrencyDetector.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidence = outputFeature0.getFloatArray();

            float maxConfidence = 0;
            int maxPos = 0;
            for(int i=0; i<confidence.length; i++) {
                if (confidence[i]>maxConfidence) {
                    maxConfidence = confidence[i];
                    maxPos = i;
                }
            }

            String[] classes = {"Counterfeit", "Real"};
            currency_status.setText(classes[maxPos]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    private void amountDetect(Bitmap image){
        try {
            SrilankanCurrencyAmountDetector model = SrilankanCurrencyAmountDetector.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 150, 150, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imgSize * imgSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imgSize * imgSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getWidth());

            int pixelValue = 0;
            for(int i=0; i<imgSize; i++){
                for(int j=0; j<imgSize; j++){
                    int val = intValues[pixelValue++];
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 1));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            SrilankanCurrencyAmountDetector.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidence = outputFeature0.getFloatArray();

            float maxConfidence = 0;
            int maxPos = 0;
            for(int i=0; i<confidence.length; i++) {
                if (confidence[i]>maxConfidence) {
                    maxConfidence = confidence[i];
                    maxPos = i;
                }
            }

            String[] classes = {"100 Rupees", "1000 Rupees", "20 Rupees", "50 Rupees", "500 Rupees", "5000 Rupees"};
            currency_amount.setText(classes[maxPos]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }

    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            // Error occurred while creating the File
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.example.android.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            File img = new File(currentPhotoPath);
            currency.setImageURI(Uri.fromFile(img));

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(img);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);

            Bitmap imgs = null;
            try {
               imgs = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            imgs = Bitmap.createScaledBitmap(imgs, imgSize, imgSize, false);
            counterfeitDetect(imgs);
            amountDetect(imgs);

            String text = currency_status.getText().toString() + " " + currency_amount.getText().toString();
            int speech = textToSpeech.speak(text, textToSpeech.QUEUE_FLUSH, null);
        }

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri imgUri = data.getData();
            Bitmap img = null;
            try {
                img = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            currency.setImageBitmap(img);
            //image.setImageURI(img);
            img = Bitmap.createScaledBitmap(img, imgSize, imgSize, false);
            counterfeitDetect(img);
            amountDetect(img);
            String text = currency_status.getText().toString() + " " + currency_amount.getText().toString();
            int speech = textToSpeech.speak(text, textToSpeech.QUEUE_FLUSH, null);

        }
    }

}