package com.PharmaDash.PharmaDashApp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.PharmaDash.PharmaDashApp.historyRecyclerView.HistoryAdapter;
import com.PharmaDash.PharmaDashApp.historyRecyclerView.HistoryObject;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class HistoryActivity extends AppCompatActivity {
    private String customerOrDriver, userId;

    private RecyclerView mHistoryRecyclerView;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;
    private TextView mBalance;
    private Double Balance = 0.0;

    private Button mPayout;

    private EditText mPayoutEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        customerOrDriver = getIntent().getExtras().getString("customerOrDriver");

        mBalance = findViewById(R.id.balance);
        mPayout = findViewById(R.id.payout);
        mPayoutEmail = findViewById(R.id.payoutEmail);

        mHistoryRecyclerView = (RecyclerView) findViewById(R.id.historyRecyclerView);
        mHistoryRecyclerView.setNestedScrollingEnabled(false);
        mHistoryRecyclerView.setHasFixedSize(true);
        mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        mHistoryRecyclerView.setLayoutManager(mHistoryLayoutManager);
        mHistoryAdapter=new HistoryAdapter(getDataSetHistory(), HistoryActivity.this, customerOrDriver);
        mHistoryRecyclerView.setAdapter(mHistoryAdapter);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUserHistoryIds();
        if(customerOrDriver.equals("Drivers")){
            mBalance.setVisibility(View.VISIBLE);
            mPayout.setVisibility(View.VISIBLE);
            mPayoutEmail.setVisibility(View.VISIBLE);
        }

        mPayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Balance == 0) {
                    Snackbar.make(findViewById(R.id.layout), "You need a balance greater than $0.00 to payout", Snackbar.LENGTH_LONG).show();
                }
                else if(String.valueOf(mPayoutEmail.getText()).equals("")){
                    Snackbar.make(findViewById(R.id.layout), "Please enter an email", Snackbar.LENGTH_LONG).show();
                }
                else {
                    payoutRequest();
                }

            }
        });

    }


    private void getUserHistoryIds() {
        DatabaseReference userHistoryDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(customerOrDriver).child(userId).child("history");
        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot history : dataSnapshot.getChildren()){
                            FetchRideInformation(history.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void FetchRideInformation(String rideKey) {
        DatabaseReference historyDatabase = FirebaseDatabase.getInstance().getReference().child("history").child(rideKey);
        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String rideId = dataSnapshot.getKey();
                    Double timestamp = 0.0;
                    if(dataSnapshot.child("timestamp").getValue()!=null){
                        timestamp = Double.valueOf(dataSnapshot.child("timestamp").getValue().toString());
                    }

                    HistoryObject obj = new HistoryObject(rideId, getDate(timestamp));
                    resultsHistory.add(0,obj);
                    mHistoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        if(customerOrDriver.equals("Drivers")) {
            DatabaseReference driverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("balance");
            driverDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Balance = Double.valueOf(dataSnapshot.getValue().toString());
                        mBalance.setText("Balance: $" + Math.round(Balance * 100.0) / 100.0);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private String getDate(Double timestamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp.longValue()*1000);
        String date = DateFormat.format("dd-MM-yyyy hh:mm a", cal).toString();
        return date;
    }

    private ArrayList resultsHistory = new ArrayList<HistoryObject>();

    private ArrayList<HistoryObject> getDataSetHistory(){
        return resultsHistory;
    }

    public static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    ProgressDialog progress;
    private void payoutRequest() {
        progress = new ProgressDialog(this);
        progress.setTitle("Processing your payout");
        progress.setMessage("Please wait");
        progress.setCancelable(false);
        progress.show();

        final OkHttpClient client = new OkHttpClient();
        JSONObject postData = new JSONObject();

        try {
            postData.put("uid", FirebaseAuth.getInstance().getUid());
            postData.put("email", mPayoutEmail.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MEDIA_TYPE, postData.toString());

        final Request request = new Request.Builder()
                .url("https://us-central1-uberapp-a89d9.cloudfunctions.net/payout")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("cache-control", "no-cache")
                .addHeader("Authorization", "Your Token")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                progress.dismiss();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                int responseCode = response.code();
                if(response.isSuccessful()){
                    switch (responseCode){
                        case 200:
                            Snackbar.make(findViewById(R.id.layout), "Payout Successful", Snackbar.LENGTH_LONG).show();
                            Balance = 0.0;
                            mBalance.setText("Balance: $0.00");
                            break;
                        case 500:
                            Snackbar.make(findViewById(R.id.layout), "Error: Could not complete payout", Snackbar.LENGTH_LONG).show();
                            break;
                        default:
                            Snackbar.make(findViewById(R.id.layout), "Error: Could not complete payout", Snackbar.LENGTH_LONG).show();
                            break;
                    }
                }
                progress.dismiss();
            }
        });


    }

}
