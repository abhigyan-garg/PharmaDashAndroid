package com.PharmaDash.PharmaDashApp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {

    private String rideId, customerId, driverId, userDriverOrCustomer, medicine;

    private TextView deliveryPriceView, medicinePriceView, totalPriceView, rideDistance, rideDate, userName, userPhone;

    private ImageView userImage;

    private RatingBar mRatingBar;

    private Button mPay;

    private DatabaseReference historyRideInfoDb;

    private LatLng destinationLatLng, pickupLatLng;
    private String distance;
    private double deliveryPrice;
    private double medicinePrice;
    private double driverPayout;
    private double totalPrice;
    private Boolean customerPaid = false;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        startService(intent);

        userDriverOrCustomer = getIntent().getStringExtra("customerOrDriver");

        polylines = new ArrayList<>();

        rideId = getIntent().getExtras().getString("rideId");

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        deliveryPriceView = (TextView) findViewById(R.id.deliveryPrice);
        medicinePriceView = (TextView) findViewById(R.id.medicinePrice);
        totalPriceView = (TextView) findViewById(R.id.totalPrice);
        rideDistance = (TextView) findViewById(R.id.rideDistance);
        rideDate = (TextView) findViewById(R.id.rideDate);
        userName = (TextView) findViewById(R.id.userName);
        userPhone = (TextView) findViewById(R.id.userPhone);
        userImage = (ImageView) findViewById(R.id.userImage);
        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);

        mPay = findViewById(R.id.pay);


        historyRideInfoDb = FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
        getRideInformation();
    }

    private void getRideInformation() {
        historyRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot child:dataSnapshot.getChildren()){
                        if(child.getKey().equals("customer")){
                            customerId = child.getValue().toString();
                        }
                        if(child.getKey().equals("driver")){
                            driverId = child.getValue().toString();
                        }
                        if(child.getKey().equals("timestamp")){
                            rideDate.setText(getDate(Double.valueOf(child.getValue().toString())));
                        }
                        if(child.getKey().equals("rating")){
                            mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));
                            if(Integer.valueOf(child.getValue().toString())!=0)
                                mRatingBar.setIsIndicator(true);
                        }
                        if(child.getKey().equals("customerPaid")){
                            customerPaid = true;
                        }
                        if(child.getKey().equals("medicine"))
                            medicine = child.getValue().toString();
                        if(child.getKey().equals("distance")){
                            distance=child.getValue().toString();
                            rideDistance.setText(Math.round(Double.valueOf(distance)*100.0)/100.0 +" km");

                        }

                        if (child.getKey().equals("price")) {
                            deliveryPrice = Math.round(100.0*Double.valueOf(child.getValue().toString()))/100.0;
                        }
                        if (child.getKey().equals("amount")) {
                            medicinePrice = Math.round(100.0*Double.valueOf(child.getValue().toString()))/100.0;
                        }

                        if (child.getKey().equals("driverPayout")) {
                            driverPayout = Math.round(100.0*Double.valueOf(child.getValue().toString()))/100.0;
                        }


                        if(child.getKey().equals("location")){
                            pickupLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()), Double.valueOf(child.child("from").child("lng").getValue().toString()));
                            destinationLatLng = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()), Double.valueOf(child.child("to").child("lng").getValue().toString()));
                            if(destinationLatLng!=new LatLng(0,0)){
                                getRouteToMarker();
                            }
                        }
                    }
                    medicinePriceView.setText(medicine + ": $ " + medicinePrice);
                    if(userDriverOrCustomer.equals("Drivers"))
                    {
                        deliveryPriceView.setText("Earnings: $" + driverPayout);
                        totalPriceView.setText("Total Payout: $" + Math.round(100.0*(medicinePrice + driverPayout))/100.0);
                        getUserInformation("Customers", customerId );
                    }
                    else if(userDriverOrCustomer.equals("Customers"))
                    {
                        deliveryPriceView.setText("Delivery Fee: $" + deliveryPrice);
                        totalPriceView.setText("Total Price: $" + Math.round(100.0*(medicinePrice + deliveryPrice))/100.0);
                        totalPrice = Math.round(100.0*(medicinePrice + deliveryPrice))/100.0;
                        getUserInformation("Drivers", driverId);
                        displayCustomerRelatedObjects();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void displayCustomerRelatedObjects() {
        mRatingBar.setVisibility(View.VISIBLE);
        mPay.setVisibility(View.VISIBLE);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                historyRideInfoDb.child("rating").setValue(v);
                DatabaseReference mDriverRatingDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("rating");
                mDriverRatingDb.child(rideId).setValue(v);
                DatabaseReference rideRatingReference = FirebaseDatabase.getInstance().getReference().child("history").child(rideId).child("rating");
                rideRatingReference.setValue(v);
            }
        });
        if(customerPaid){
            mPay.setEnabled(false);
        }
        else{
            mPay.setEnabled(true);
        }
        mPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                payPalPayment();
            }
        });
    }

    private int PAYPAL_REQUEST_CODE = 1;
    private static PayPalConfiguration config = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(PayPalConfig.PAYPAL_CLIENT_ID);
    private void payPalPayment() {
        PayPalPayment payment = new PayPalPayment(new BigDecimal(totalPrice), "USD", "PharmaDash delivery", PayPalPayment.PAYMENT_INTENT_SALE);
        Intent intent = new Intent(this, PaymentActivity.class);

        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

        startActivityForResult(intent, PAYPAL_REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PAYPAL_REQUEST_CODE){

            if(resultCode == Activity.RESULT_OK){
                PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if(confirm!=null){
                    try{
                        JSONObject jsonObj = new JSONObject(confirm.toJSONObject().toString());

                        String paymentResponse = jsonObj.getJSONObject("response").getString("state");
                        if(paymentResponse.equals("approved")){
                            Toast.makeText(getApplicationContext(), "Payment Successful", Toast.LENGTH_LONG).show();
                            historyRideInfoDb.child("customerPaid").setValue(true);
                            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId).child("history").child(rideId);
                            userRef.setValue(true);
                            mPay.setEnabled(false);
                        }
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }
            else{
                Toast.makeText(getApplicationContext(), "Payment Unsuccessful", Toast.LENGTH_LONG).show();
            }

        }
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }

    private void getUserInformation(String otherUserDriverOrCustomer, String otherUserId) {
        DatabaseReference mOtherUserDB = FirebaseDatabase.getInstance().getReference().child("Users").child(otherUserDriverOrCustomer).child(otherUserId);
        mOtherUserDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        userName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null){
                        userPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(userImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getRouteToMarker() {
        Routing routing = new Routing.Builder()
                .key("AIzaSyB_bcy42zRFG3pWKFBdGBN-JM1aHo1h-dw")
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(pickupLatLng, destinationLatLng)
                .build();
        routing.execute();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;
    }
    private String getDate(Double timestamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp.longValue()*1000);
        String date = DateFormat.format("dd-MM-yyyy hh:mm a", cal).toString();
        return date;
    }

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickupLatLng);
        builder.include(destinationLatLng);
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width*0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        mMap.animateCamera(cameraUpdate);

        mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("pickup location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_medicine)));
        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("destination").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));

        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

        }
    }


    @Override
    public void onRoutingCancelled() {

    }
    private void ereasePolylines(){
        for(Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }

}
