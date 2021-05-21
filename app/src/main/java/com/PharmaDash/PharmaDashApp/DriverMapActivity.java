package com.PharmaDash.PharmaDashApp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, RoutingListener {

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationClient;

    private Button mLogout, mSettings, mrideStatus, mHistory, mDirections;

    private Switch mWorkingSwitch;


    private String customerId = "", pickup;
    private LatLng pickupLatLng, dropoffLatLng;

    private Boolean isLoggingOut=false;

    private LinearLayout mCustomerInfo;

    private ImageView mCustomerProfileImage;

    private TextView mCustomerName, mCustomerPhone, mCustomerPickup, mCustomerMedicine;

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        polylines = new ArrayList<>();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        mCustomerInfo=(LinearLayout) findViewById(R.id.customerInfo);

        mCustomerProfileImage=(ImageView) findViewById(R.id.customerProfileImage);

        mCustomerName = (TextView) findViewById(R.id.customerName);
        mCustomerPhone= (TextView) findViewById(R.id.customerPhone);
        mCustomerPickup= (TextView) findViewById(R.id.customerPickup);
        mCustomerMedicine = (TextView) findViewById(R.id.customerMedicine);

        mWorkingSwitch = (Switch) findViewById(R.id.workingSwitch);
        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.child("name").getValue() == null || dataSnapshot.child("phone").getValue() == null || dataSnapshot.child("profileImageUrl").getValue() == null|| dataSnapshot.child("car").getValue() == null)
                            {
                                Toast.makeText(getApplicationContext(), "You must fill out the information in the settings menu to work",Toast.LENGTH_LONG).show();
                                mWorkingSwitch.setChecked(false);
                            }
                            else {
                                connectDriver();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
                else{
                    if(!customerId.equals("")){
                        DatabaseReference statusRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("status");
                        statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.getValue()!=null){
                                    String status = dataSnapshot.getValue().toString();
                                    if(status.equals("0")){
                                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                        DatabaseReference customerRequestRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId);
                                        customerRequestRef.removeValue();
                                        DatabaseReference endRideRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
                                        endRideRef.removeValue();
                                        Toast.makeText(getApplicationContext(), "Ride Ended",Toast.LENGTH_LONG).show();
                                        disconnectDriver();
                                    }
                                    else {
                                        Toast.makeText(getApplicationContext(), "You cannot stop working until you complete the ride",Toast.LENGTH_LONG).show();
                                        mWorkingSwitch.setChecked(true);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                    else {
                        disconnectDriver();
                    }
                }
            }
        });

        mLogout=(Button)findViewById(R.id.logout);
        mSettings=(Button) findViewById(R.id.settings);
        mrideStatus=(Button) findViewById(R.id.rideStatus);
        mHistory=(Button) findViewById(R.id.history);
        mDirections = (Button) findViewById(R.id.directions);

        mrideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final DatabaseReference goToDropoffRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("status");
                goToDropoffRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            switch(dataSnapshot.getValue().toString())
                            {
                                case "0":
                                    Toast.makeText(getApplicationContext(), "Please move closer to the pharmacy until you are within 75 metres",Toast.LENGTH_LONG).show();
                                    break;
                                case "1":
                                    Intent intent=new Intent(DriverMapActivity.this, ReceiptScannerActivity.class);
                                    intent.putExtra("EXTRA_SESSION_ID", customerId);
                                    startActivityForResult(intent, 1);
                                    break;
                                case "2":
                                    Toast.makeText(getApplicationContext(), "Please move closer to the dropoff until you are within 75 metres",Toast.LENGTH_LONG).show();
                                    break;
                                case "3":
                                    goToDropoffRef.setValue(4);
                                    break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!customerId.equals("")){
                    DatabaseReference statusRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("status");
                    statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.getValue()!=null){
                                String status = dataSnapshot.getValue().toString();
                                if(status.equals("0")){
                                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    DatabaseReference customerRequestRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId);
                                    customerRequestRef.removeValue();
                                    DatabaseReference endRideRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
                                    endRideRef.removeValue();
                                    Toast.makeText(getApplicationContext(), "Ride Ended",Toast.LENGTH_LONG).show();
                                    isLoggingOut = true;
                                    disconnectDriver();
                                    FirebaseAuth.getInstance().signOut();
                                    Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                    return;
                                }
                                else {
                                    Toast.makeText(getApplicationContext(), "You cannot log out until you complete the ride",Toast.LENGTH_LONG).show();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
                else {
                    isLoggingOut = true;
                    disconnectDriver();
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        });
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(DriverMapActivity.this, DriverSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });
        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DriverMapActivity.this, HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Drivers");
                startActivity(intent);
                return;
            }
        });
        getAssignedCustomer();
    }

    private void getAssignedCustomer(){
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    customerId = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerDropoff();
                    getAssignedCustomerInfo();
                }
                else{
                    endRide();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    Marker dropoffMarker;
    Marker pickupMarker;

    private void getAssignedCustomerPickupLocation(){
        DatabaseReference assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId);
        assignedCustomerPickupLocationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&&!customerId.equals("")){
                    Map<String, Object> map = (HashMap<String, Object>) dataSnapshot.getValue();

                    if(map.get("pickup")!=null){
                        pickup=map.get("pickup").toString();
                        mCustomerPickup.setText("Pharmacy: "+ pickup);
                    }
                    else{
                        mCustomerPickup.setText("Pharmacy: --");
                    }

                    if(map.get("medicine")!=null){
                        mCustomerMedicine.setText("Medicine: "+ map.get("medicine").toString());
                    }
                    else{
                        mCustomerMedicine.setText("Medicine: --");
                    }

                    double locationLat=0;
                    double locationLng = 0;
                    if(map.get("pickupLat")!=null){
                        locationLat=Double.parseDouble(map.get("pickupLat").toString());
                    }
                    if(map.get("pickupLng")!=null){
                        locationLng=Double.parseDouble(map.get("pickupLng").toString());
                    }
                    pickupLatLng = new LatLng(locationLat, locationLng);
                    if(map.get("status").toString().equals("0")||map.get("status").toString().equals("1"))
                    {
                        pickupMarker=mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_medicine)));
                        if(!mWorkingSwitch.isChecked()){
                            connectDriver();
                            mWorkingSwitch.setChecked(true);
                        }
                        getRouteToMarker(pickupLatLng);
                    }
                    else if(map.get("status").toString().equals("2")||map.get("status").toString().equals("3")) {
                        pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_medicine)));
                        goToDropoff();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerDropoff(){
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("l");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();

                    Double dropoffLat=0.0;
                    Double dropoffLng=0.0;
                    if(map.get(0)!=null){
                        dropoffLat=Double.valueOf(map.get(0).toString());
                    }
                    if(map.get(1)!=null) {
                        dropoffLng = Double.valueOf(map.get(1).toString());
                        dropoffLatLng = new LatLng(dropoffLat, dropoffLng);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void goToDropoff()
    {
        ereasePolylines();
        if(!mWorkingSwitch.isChecked()){
            connectDriver();
            mWorkingSwitch.setChecked(true);
        }
        if(dropoffLatLng.latitude!=0.0&&dropoffLatLng.longitude!=0.0){
            dropoffMarker=mMap.addMarker(new MarkerOptions().position(dropoffLatLng).title("Dropoff").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));
            if(!mWorkingSwitch.isChecked()){
                connectDriver();
                mWorkingSwitch.setChecked(true);
            }
            getRouteToMarker(dropoffLatLng);
        }
        mrideStatus.setText("I dropped off the medicine");
    }

    private void getRouteToMarker(final LatLng dropoffLatLng) {
        if(mLastLocation==null){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
                checkLocationPermission();
            else {
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    mLastLocation = location;
                                    getRouteToMarker(dropoffLatLng);
                                }
                            }
                        });
            }
        }
        else if(dropoffLatLng != null && mLastLocation !=null) {
            Routing routing = new Routing.Builder()
                    .key("AIzaSyB_bcy42zRFG3pWKFBdGBN-JM1aHo1h-dw")
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), dropoffLatLng)
                    .build();
            routing.execute();
            mDirections.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Uri intentUri = Uri.parse("google.navigation:q=" + dropoffLatLng.latitude+","+dropoffLatLng.longitude+"&avoid=tf");
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, intentUri);
                    startActivity(mapIntent);
                }
            });
            mDirections.setVisibility(View.VISIBLE);

        }
    }


    private void getAssignedCustomerInfo(){
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        mCustomerName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null){
                        mCustomerPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomerProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void endRide(){
        mrideStatus.setText("I picked up the medicine");
        ereasePolylines();
        if(FirebaseAuth.getInstance().getCurrentUser()!=null){
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
            driverRef.removeValue();


            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
            GeoFire geoFire = new GeoFire(ref);
            geoFire.removeLocation(customerId);
            customerId="";

            if(dropoffMarker!=null){
                dropoffMarker.remove();
            }
            if(pickupMarker!=null){
                pickupMarker.remove();
            }
            mCustomerInfo.setVisibility(View.GONE);
            mCustomerName.setText("");
            mCustomerPhone.setText("");
            mCustomerPickup.setText("Pharmacy: --");
            mCustomerMedicine.setText("Medicine: --");
            mCustomerProfileImage.setImageResource(R.mipmap.ic_default_user);
        }

    }


    private Long getCurrentTimestamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){

            }
            else{
                checkLocationPermission();
            }
        }
    }

    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location : locationResult.getLocations()){
                mLastLocation = location;
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
            }
        }
    };

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(this)
                        .setTitle("Give Permission")
                        .setMessage("Please grant the app permission to access your location")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            }
        }
    }
    private void checkLocationPermissionService() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)!=PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.FOREGROUND_SERVICE)){
                new AlertDialog.Builder(this)
                        .setTitle("Give Permission")
                        .setMessage("Please grant the app permission to access your location in the background")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.FOREGROUND_SERVICE}, 2);
                            }
                        })
                        .create()
                        .show();
            }
            else{
                new AlertDialog.Builder(this)
                        .setTitle("Give Permission")
                        .setMessage("Please grant the app permission to access your location in the background")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.FOREGROUND_SERVICE}, 2);
                            }
                        })
                        .create()
                        .show();

            }
        }
        else{
            startForegroundService(new Intent(DriverMapActivity.this, LocationUpdates.class));
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:{
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        if(mWorkingSwitch.isChecked()){
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                            mMap.setMyLocationEnabled(true);
                        }
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "Please provide the permission",Toast.LENGTH_LONG).show();
                }
                break;
            }
            case 2:{
                   if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED) {
                        startForegroundService(new Intent(DriverMapActivity.this, LocationUpdates.class));
                    }
            }
        }
    }






    private void connectDriver(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
            checkLocationPermission();

        else if(ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)!=PackageManager.PERMISSION_GRANTED){
            checkLocationPermissionService();
        }
        else{
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
            startForegroundService(new Intent(DriverMapActivity.this, LocationUpdates.class));
        }
    }


    private void disconnectDriver(){
        if(mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        stopService(new Intent(DriverMapActivity.this, LocationUpdates.class));
    }



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

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
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
        mDirections.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==1&&resultCode== Activity.RESULT_OK){
            goToDropoff();
        }
    }
}
