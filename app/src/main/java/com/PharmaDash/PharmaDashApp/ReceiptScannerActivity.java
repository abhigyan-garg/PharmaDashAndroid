package com.PharmaDash.PharmaDashApp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ReceiptScannerActivity extends AppCompatActivity {

    private Button mConfirm, mScan;
    private EditText mTotalAmount;
    private ImageView mReceiptImage;

    String currentPhotoPath;
    private Uri resultUri;
    private double mAmount;

    private DatabaseReference mDeliveryDatabaseReference;

    private String customerID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_scanner);

        customerID=getIntent().getStringExtra("EXTRA_SESSION_ID");
        mDeliveryDatabaseReference= FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerID);

        mConfirm = (Button) findViewById(R.id.confirm);
        mScan = (Button) findViewById(R.id.scan);
        mTotalAmount = (EditText) findViewById(R.id.totalAmount);
        mReceiptImage = (ImageView) findViewById(R.id.receiptImage);


        mScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                        } catch (IOException ex) {
                            Toast.makeText(getApplicationContext(), "An error occurred while creating the image",Toast.LENGTH_LONG).show();
                        }
                        if (photoFile != null) {
                            resultUri = FileProvider.getUriForFile(getApplicationContext(),
                                    "com.mydomain.fileprovider",
                                    photoFile);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, resultUri);
                            startActivityForResult(takePictureIntent, 1);
                        }
                    }
                }
                else{
                    checkCameraPermission();
                }
            }
        });

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    saveReceiptInformation();
                }
                catch(NumberFormatException e)
                {
                    Toast.makeText(getApplicationContext(), "Please enter the total amount on the receipt",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveReceiptInformation() throws NumberFormatException {

        if(resultUri != null)
        {
            mAmount = Math.round(Double.valueOf(mTotalAmount.getText().toString())*100.0)/100.0;
            mDeliveryDatabaseReference.child("amount").setValue(mAmount);
            mDeliveryDatabaseReference.child("status").setValue(2);

            final String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("receipts").child(userID);
            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Bitmap rotatedBitmap = null;
            try{
                ExifInterface exif = new ExifInterface(getApplication().getContentResolver().openInputStream(resultUri));
                Matrix matrix = new Matrix();
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.preRotate(270);
                        rotatedBitmap = Bitmap.createBitmap(bitmap,0,0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.preRotate(180);
                        rotatedBitmap = Bitmap.createBitmap(bitmap,0,0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.preRotate(90);
                        rotatedBitmap = Bitmap.createBitmap(bitmap,0,0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        break;
                    default:
                        rotatedBitmap = bitmap;
                        break;
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if(rotatedBitmap!=null){
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            }
            byte[] data = baos.toByteArray();
            UploadTask uploadTask = filePath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(), "Error while uploading receipt",Toast.LENGTH_LONG).show();
                    return;
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Map newImage = new HashMap();
                            newImage.put("receiptImageUrl", uri.toString());
                            mDeliveryDatabaseReference.updateChildren(newImage);
                            Intent returnIntent = new Intent();
                            setResult(Activity.RESULT_OK, returnIntent);
                            finish();
                            return;
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "Error while uploading receipt",Toast.LENGTH_LONG).show();
                            return;
                        }
                    });


                }
            });
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Please scan the receipt",Toast.LENGTH_LONG).show();
        }
    }

    private void checkCameraPermission(){
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)){
                new AlertDialog.Builder(this)
                        .setTitle("Give Permission")
                        .setMessage("Please grant the app permission to use your camera")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(ReceiptScannerActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
                            }
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(ReceiptScannerActivity.this, new String[]{Manifest.permission.CAMERA}, 1);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:{
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(getApplicationContext(), "Permission Granted",Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "Please provide the permission",Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1&&resultCode== Activity.RESULT_OK){
                mReceiptImage.setImageURI(resultUri);
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
