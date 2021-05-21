package com.PharmaDash.PharmaDashApp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomerSettingsActivity extends AppCompatActivity {

    private EditText mNameField, mPhoneField;

    private Button mBack, mConfirm;

    private ImageView mProfileImage;

    private FirebaseAuth mAuth;
    private DatabaseReference mCustomerDatabase;

    private String userID;

    private String mName;
    private String mPhone;

    private String mProfileImageUrl;

    private Uri resultUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);

        mNameField=(EditText) findViewById(R.id.name);
        mPhoneField=(EditText) findViewById(R.id.phone);

        mConfirm=(Button) findViewById(R.id.confirm);
        mBack=(Button) findViewById(R.id.back);

        mProfileImage = (ImageView)  findViewById(R.id.profileImage);

        mAuth = FirebaseAuth.getInstance();
        userID=mAuth.getCurrentUser().getUid();
        mCustomerDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userID);

        getUserInfo();

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveUserInformation();
            }
        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                return;
            }
        });
    }


    private void getUserInfo(){
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        mName=map.get("name").toString();
                        mNameField.setText(mName);
                    }
                    if(map.get("phone")!=null){
                        mPhone=map.get("phone").toString();
                        mPhoneField.setText(mPhone);
                    }
                    if(map.get("profileImageUrl")!=null){
                        mProfileImageUrl=map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileImageUrl).into(mProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void saveUserInformation() {
        mName=mNameField.getText().toString();
        mPhone=mPhoneField.getText().toString();
        if(mName.equals(""))
            Toast.makeText(getApplicationContext(), "Please enter your name",Toast.LENGTH_LONG).show();
        else if(mPhone.equals(""))
            Toast.makeText(getApplicationContext(), "Please enter your phone number",Toast.LENGTH_LONG).show();
        else
        {
            Map userInfo= new HashMap();
            userInfo.put("name", mName);
            userInfo.put("phone", mPhone);
            mCustomerDatabase.updateChildren(userInfo);

            if(resultUri!=null){
                final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userID);
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
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
                }
                byte[] data = baos.toByteArray();
                UploadTask uploadTask = filePath.putBytes(data);

                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        finish();
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
                                newImage.put("profileImageUrl", uri.toString());
                                mCustomerDatabase.updateChildren(newImage);

                                finish();
                                return;
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                finish();
                                return;
                            }
                        });


                    }
                });
            }
            else {
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1&&resultCode== Activity.RESULT_OK){
            final Uri imageUri =data.getData();
            resultUri=imageUri;
            mProfileImage.setImageURI(resultUri);
        }
    }
}
