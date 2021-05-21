package com.PharmaDash.PharmaDashApp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, AdapterView.OnItemSelectedListener {

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationClient;

    private Button mLogout, mRequest, mSettings, mHistory;

    private LatLng dropoffLocation;

    private Boolean requestBol=false;

    private Marker dropoffMarker, pickupMarker;

    private String pickup, medicine;

    private LatLng pickupLatLng;

    private LinearLayout mDriverInfo;

    private ImageView mDriverProfileImage;

    private Spinner mMedicine;

    private TextView mDriverName, mDriverPhone, mDriverCar;

    private RatingBar mRatingBar;

    private AutocompleteSupportFragment autocompleteFragment;

    private int status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyB_bcy42zRFG3pWKFBdGBN-JM1aHo1h-dw", Locale.US);
        }
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        status = -1;
        pickupLatLng = new LatLng(0.0,0.0);

        mDriverInfo=(LinearLayout) findViewById(R.id.driverInfo);

        mDriverProfileImage=(ImageView) findViewById(R.id.driverProfileImage);

        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverPhone= (TextView) findViewById(R.id.driverPhone);
        mDriverCar= (TextView) findViewById(R.id.driverCar);

        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);

        mMedicine = (Spinner) findViewById(R.id.medicine);
        mMedicine.setOnItemSelectedListener(this);
        medicine = "None";
        populateSpinner();

        mLogout = (Button) findViewById(R.id.logout);
        mRequest = (Button) findViewById(R.id.request);
        mSettings = (Button) findViewById(R.id.settings);
        mHistory = (Button) findViewById(R.id.history);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestBol){
                    if(status==0){
                        endRide();
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "The request cannot be cancelled now",Toast.LENGTH_LONG).show();
                    }

                }
                else {
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            int numUnpaid = 0;
                            for(DataSnapshot ride: dataSnapshot.child("history").getChildren())
                            {
                                if(ride.getValue().toString().equals("false"))
                                    numUnpaid++;
                            }
                            if(dataSnapshot.child("name").getValue() == null || dataSnapshot.child("phone").getValue() == null) {
                                Toast.makeText(getApplicationContext(), "Please enter your name and phone number in the settings menu",Toast.LENGTH_LONG).show();
                            }
                            else if(numUnpaid > 0)
                            {
                                Toast.makeText(getApplicationContext(), "You cannot request a delivery if you have more than one unpaid delivery",Toast.LENGTH_LONG).show();
                            }
                            else if(pickupLatLng==null||(pickupLatLng.latitude==0.0&&pickupLatLng.longitude==0.0)){
                                Toast.makeText(getApplicationContext(), "Please select a pharmacy",Toast.LENGTH_LONG).show();
                            }
                            else if(medicine.equals("None")){
                                Toast.makeText(getApplicationContext(), "Please select a medicine",Toast.LENGTH_LONG).show();
                            }
                            else {
                                requestBol=true;

                                dropoffLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                                dropoffMarker=mMap.addMarker(new MarkerOptions().position(dropoffLocation).title("Dropoff Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));
                                pickupMarker=mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_medicine)));

                                mRequest.setText("Getting your Driver....");

                                getClosestDriver();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }
        });
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });

        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomerMapActivity.this, HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Customers");
                startActivity(intent);
                return;
            }
        });

        autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteFragment.setTypeFilter(TypeFilter.ESTABLISHMENT);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                pickup=place.getName().toString();
                pickupLatLng=place.getLatLng();
            }

            @Override
            public void onError(Status status) {
            }
        });
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference inRideRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(userId);
        inRideRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    mMedicine.setEnabled(false);
                    autocompleteFragment.getView().setEnabled(false);
                    autocompleteFragment.setText(dataSnapshot.child("pickup").getValue().toString());
                    driverFoundID = dataSnapshot.child("driver").getValue().toString();
                    requestBol = true;
                    Double dropoffLat = Double.valueOf(dataSnapshot.child("l").child("0").getValue().toString());
                    Double dropoffLng = Double.valueOf(dataSnapshot.child("l").child("1").getValue().toString());
                    dropoffLocation = new LatLng(dropoffLat, dropoffLng);

                    Double pickupLat = Double.valueOf(dataSnapshot.child("pickupLat").getValue().toString());
                    Double pickupLng = Double.valueOf(dataSnapshot.child("pickupLng").getValue().toString());
                    pickupLatLng = new LatLng(pickupLat, pickupLng);

                    dropoffMarker=mMap.addMarker(new MarkerOptions().position(dropoffLocation).title("Dropoff Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));
                    pickupMarker=mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Dropoff Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_medicine)));

                    getDriverLocation();
                    getDriverInfo();
                    getHasRideEnded();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void populateSpinner()
    {
        DatabaseReference medicineRef = FirebaseDatabase.getInstance().getReference().child("medicine");
        final CustomerMapActivity activity = this;
        medicineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    Map<String, Object> medicineMap = (Map<String, Object>) dataSnapshot.getValue();
                    List<String> meds = new ArrayList<String>();
                    meds.addAll(medicineMap.keySet());
                    Collections.sort(meds);
                    meds.add(0, "None");

                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, meds);
                    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    mMedicine.setAdapter(dataAdapter);
                    if(!medicine.equals("None"))
                    {
                        if(meds.contains(medicine))
                            mMedicine.setSelection(meds.indexOf(medicine), true);
                    }
                    else
                    {
                        final List<String> meds2 = (ArrayList<String>) (((ArrayList<String>)meds).clone());
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference inRideRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(userId).child("medicine");
                        inRideRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()){
                                    if(meds2.contains(dataSnapshot.getValue().toString()))
                                        mMedicine.setSelection(meds2.indexOf(dataSnapshot.getValue().toString()), true);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private int radius = 1;
    private Boolean driverFound=false;
    private String driverFoundID;

    GeoQuery geoQuery;
    private void getClosestDriver(){
        DatabaseReference driverLocation=FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(dropoffLocation.latitude, dropoffLocation.longitude),radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound&&requestBol) {
                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0){
                                Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();
                                if(driverFound){
                                    return;
                                }
                                driverFound = true;
                                driverFoundID=dataSnapshot.getKey();
                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                                GeoFire geoFire = new GeoFire(ref);
                                geoFire.setLocation(userId, new GeoLocation(dropoffLocation.latitude, dropoffLocation.longitude));
                                mMedicine.setEnabled(false);
                                autocompleteFragment.getView().setEnabled(false);
                                DatabaseReference requestRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(userId);
                                HashMap map = new HashMap();
                                map.put("pickup",pickup);
                                map.put("medicine", medicine);
                                map.put("distance", 0.0);
                                map.put("pickupLat",pickupLatLng.latitude);
                                map.put("pickupLng",pickupLatLng.longitude);
                                map.put("driver", driverFoundID);
                                map.put("status", 0);
                                requestRef.updateChildren(map);
                                DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest");
                                driverRef.setValue(userId);
                                getDriverLocation();
                                getDriverInfo();
                                getHasRideEnded();
                                mRequest.setText("Looking for Driver Location");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverFound){
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriverLocation(){
        status = 0;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(userId).child("status");
        rideRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    if(Integer.parseInt(dataSnapshot.getValue().toString())!=status)
                        status = Integer.parseInt(dataSnapshot.getValue().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundID).child("l");
        driverLocationRefListener=driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&&requestBol){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat=0;
                    double locationLng = 0;
                    mRequest.setText("Driver Found");
                    if(map.get(0)!=null){
                        locationLat=Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1)!=null){
                        locationLng=Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    if(mDriverMarker !=null){
                        mDriverMarker.remove();
                    }
                    Location loc1=new Location("");
                    loc1.setLatitude(dropoffLocation.latitude);
                    loc1.setLongitude(dropoffLocation.longitude);

                    Location loc2=new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);
                    if(status==0)
                        mRequest.setText("Driver Found: "+ Math.round((distance/1000.0)*10.0)/10.0+"km");
                    if(status==1)
                        mRequest.setText("Driver Arrived at Pharmacy: "+ Math.round((distance/1000.0)*10.0)/10.0+"km");
                    if(status==2)
                        mRequest.setText("Driver Picked up Medicine: "+ Math.round((distance/1000.0)*10.0)/10.0+"km");
                    if(status==3)
                        mRequest.setText("Driver has Arrived");

                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.selected_car)).anchor(0.5f,0.8f));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getDriverInfo(){
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        mDriverName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null){
                        mDriverPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("car")!=null){
                        mDriverCar.setText(map.get("car").toString());
                    }
                    if(map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mDriverProfileImage);
                    }

                    int ratingSum = 0;
                    float ratingsTotal = 0;
                    float ratingsAvg = 0;
                    for(DataSnapshot child : dataSnapshot.child("rating").getChildren()){
                        ratingSum += Integer.valueOf(child.getValue().toString());
                        ratingsTotal++;
                    }
                    if(ratingsTotal!=0){
                        ratingsAvg = ratingSum/ratingsTotal;
                        mRatingBar.setRating(ratingsAvg);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private DatabaseReference rideHasEndedRef;
    private ValueEventListener rideHasEndedRefListener;

    private void getHasRideEnded(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rideHasEndedRef  = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(userId);
        rideHasEndedRefListener = rideHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

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

    private void endRide(){
        requestBol=false;
        if(geoQuery!=null){
            geoQuery.removeAllListeners();
        }
        if(driverLocationRefListener!=null){
            driverLocationRef.removeEventListener(driverLocationRefListener);
            rideHasEndedRef.removeEventListener(driverLocationRefListener);
        }
        if(driverFoundID!=null){
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest");
            driverRef.removeValue();
            driverFoundID=null;
        }

        medicine = "None";
        mMedicine.setEnabled(true);
        autocompleteFragment.getView().setEnabled(true);
        mMedicine.setSelection(0);
        driverFound=false;
        radius=1;
        status = -1;
        pickupLatLng = new LatLng(0.0,0.0);

        autocompleteFragment.setText("");

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);

        if(dropoffMarker!=null){
            dropoffMarker.remove();
        }
        if(mDriverMarker!=null){
            mDriverMarker.remove();
        }
        if(pickupMarker!=null){
            pickupMarker.remove();
        }
        mRequest.setText("Call Driver");
        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.mipmap.ic_default_user);
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
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
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
                if (getApplicationContext() != null) {
                    mLastLocation = location;

                    autocompleteFragment.setLocationBias(RectangularBounds.newInstance(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())));

                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                    if(!getDriversAroundStarted){
                        getDriversAround();
                    }
                }
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
                                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            }
        }
        else{
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:{
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "Please provide the permission",Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    boolean  getDriversAroundStarted = false;
    List<Marker> markerList = new ArrayList<Marker>();
    private void getDriversAround(){
        getDriversAroundStarted = true;
        DatabaseReference driversLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(driversLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 30);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                for(Marker markerIt : markerList){
                    if(markerIt.getTag().equals(key)){
                        return;
                    }
                }

                LatLng driverLocation = new LatLng(location.latitude, location.longitude);
                Marker mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLocation).title("Driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)).anchor(0.5f,0.8f));
                mDriverMarker.setTag(key);
                markerList.add(mDriverMarker);
            }

            @Override
            public void onKeyExited(String key) {
                for(Marker markerIt : markerList){
                    if(markerIt.getTag().equals(key)){
                        markerIt.remove();
                        markerList.remove(markerIt);
                        return;
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                for(Marker markerIt : markerList) {
                    if (markerIt.getTag().equals(key)) {
                        markerIt.setPosition(new LatLng(location.latitude, location.longitude));
                    }
                }
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        medicine = adapterView.getItemAtPosition(i).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        medicine = "";
    }
}