package com.thesis.fixmyphysics;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.Word;
import com.google.protobuf.ByteString;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.provider.MediaStore;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import androidx.core.content.FileProvider;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity {

    Button picture_btn;
    ImageView display;
    String pathToFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        picture_btn = findViewById(R.id.capture_image);
        if (Build.VERSION.SDK_INT >=23){
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        picture_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchPictureTakerAction();
            }
        });
        display = findViewById(R.id.imageView4);

        Button temp = findViewById(R.id.temp_btn);
        temp.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v){
                try {
                    detectDocumentText(pathToFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("Message", "Here5");
            }
        });

    }

    //Camera
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bitmap imageBitmap = BitmapFactory.decodeFile(pathToFile);
            display.setImageBitmap(imageBitmap);
        }
    }
    private void dispatchPictureTakerAction(){
        Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePic.resolveActivity(getPackageManager()) !=null){
            File photoFile = null;
            photoFile = createImageFile();

            if (photoFile != null){
                pathToFile = photoFile.getAbsolutePath();
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this, "com.fixmyphysics.fileprovider", photoFile);
                Log.d("Test", "Worked");
                takePic.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePic, 1);
            }
        }
    }
    private File createImageFile() {
        String name = new SimpleDateFormat("yyyyMMdd__HHmmss").format(new Date());
        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(name,".jpg", storageDir);
        } catch (IOException e){
            Log.d("mylog", "Excep : " + e.toString());
        }
        return image;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void detectDocumentText(String filePath) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        Log.d("Message", "Here1");

        // Initialize client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests. After completing all of your requests, call
        // the "close" method on the client to safely clean up any remaining background resources.
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
                 Log.d("Message", "Here 2");
                 BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
                 List<AnnotateImageResponse> responses = response.getResponsesList();
                 client.close();
                 Log.d("Message", "Here 3");
                 for (AnnotateImageResponse res : responses) {
                 if (res.hasError()) {
                 //System.out.format("Error: %s%n", res.getError().getMessage());
                 Log.d("Message", "Error 1");
                 return;
                 }
                 Log.d("Message", "Here 4");
                 // For full list of available annotations, see http://g.co/cloud/vision/docs
                 TextAnnotation annotation = res.getFullTextAnnotation();
                 for (Page page : annotation.getPagesList()) {
                 String pageText = "";
                 for (Block block : page.getBlocksList()) {
                 String blockText = "";
                 for (Paragraph para : block.getParagraphsList()) {
                 String paraText = "";
                 for (Word word : para.getWordsList()) {
                 String wordText = "";
                 for (Symbol symbol : word.getSymbolsList()) {
                 wordText = wordText + symbol.getText();
                 System.out.format(
                 "Symbol text: %s (confidence: %f)%n",
                 symbol.getText(), symbol.getConfidence());
                 }
                 System.out.format(
                 "Word text: %s (confidence: %f)%n%n", wordText, word.getConfidence());
                 paraText = String.format("%s %s", paraText, wordText);
                 }
                 // Output Example using Paragraph:
                 System.out.println("%nParagraph: %n" + paraText);
                 System.out.format("Paragraph Confidence: %f%n", para.getConfidence());
                 blockText = blockText + paraText;
                 }
                 pageText = pageText + blockText;
                 }
                 }
                 System.out.println("%nComplete annotation:");
                 System.out.println(annotation.getText());
                 }
        }catch(Exception e){
            Log.d("Message", "Failed Here");
        }
    }
}
