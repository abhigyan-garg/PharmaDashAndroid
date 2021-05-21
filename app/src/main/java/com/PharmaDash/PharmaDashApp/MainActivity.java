package com.PharmaDash.PharmaDashApp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private Button mDriver, mCustomer;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    private DatabaseReference userRef;
    private ValueEventListener userRefListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    String userId = user.getUid();
                    userRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId);
                    userRefListener =  userRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                Intent intent = new Intent(MainActivity.this, DriverMapActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }
                            else{
                                Intent intent = new Intent(MainActivity.this, CustomerMapActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                }
                else{
                    setContentView(R.layout.activity_main);

                    mDriver = (Button) findViewById(R.id.driver);
                    mCustomer = (Button) findViewById(R.id.customer);

                    mDriver.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(MainActivity.this, DriverLoginActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        }
                    });

                    mCustomer.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(MainActivity.this, CustomerLoginActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        }
                    });
                }
            }
        };
        super.onCreate(savedInstanceState);


        startService(new Intent(MainActivity.this, onAppKilled.class));
    }
    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }
    @Override
    protected void onStop() {
        super.onStop();
        if(userRefListener!=null){
            userRef.removeEventListener(userRefListener);
        }
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }
}

