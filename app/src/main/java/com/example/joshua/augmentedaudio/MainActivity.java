package com.example.joshua.augmentedaudio;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<Status> {

    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    protected Location mCurrentLocation;
    protected String mLastUpdateTime;
    //protected LocationListener mLocationListener;

    protected static final String TAG = "MainActivity";

    protected TextView mLatitudeText;
    protected TextView mLongitudeText;
    protected TextView mLastUpdateTimeTextView;
    protected TextView databaseTextView;
    protected TextView greeting;
    protected EditText nameField;


    ArrayList <Geofence> mGeofenceList = new ArrayList<Geofence>();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Context myContext = getApplicationContext();

        Parse.initialize(new Parse.Configuration.Builder(myContext)
                .applicationId("com.hooapps.alexramey.spring2016.MonkeyPin")
                .server("http://ec2-52-87-160-169.compute-1.amazonaws.com:1337/parse/")

        .build()
        );




        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setSupportActionBar(toolbar);

        mLatitudeText = (TextView) findViewById((R.id.mLatitudeTextView));
        mLongitudeText = (TextView) findViewById((R.id.mLongitudeTextView));
        mLastUpdateTimeTextView = (TextView) findViewById((R.id.lastUpdateTimeTextView));
        databaseTextView = (TextView) findViewById(R.id.textView);
        greeting = (TextView) findViewById(R.id.textView2);
        nameField = (EditText) findViewById(R.id.editText);

        nameField.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyevent) {
                //If the keyevent is a key-down event on the "enter" button
                if ((keyevent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    //...
                    // Perform your action on key press here
                    // ...
                    Log.i(TAG, "KEYBOARD ENTER PRESSED!");
                    updateName();
                    return true;
                }
                return false;
            }
        });





        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final int REQUEST_CODE_ASK_PERMISSIONS = 123;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_ASK_PERMISSIONS);

            Log.i(TAG, "LOCATION ACCESS DENIED");
        }

        Button updateLocationButton = (Button) findViewById(R.id.updateLocationButton);
        updateLocationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Log.i(TAG, "Update Location Button Pressed");
                joshGetLastLocation();


                getAudioLocations();



            }
        });

        final Integer[] counter = {0};
        Button dropSongButton = (Button) findViewById(R.id.dropSongButton);
        dropSongButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                counter[0]++;
                // Perform action on click
                Log.i(TAG, "Drop Song Button Pressed");
                String id = Integer.toString(counter[0]);
                addGeofence(id, joshGetLastLocation().getLatitude(), joshGetLastLocation().getLongitude(), 50);
                saveGeofence(id, joshGetLastLocation(), 50);

                Toast.makeText(MainActivity.this,
                        "Dropped a Song", Toast.LENGTH_LONG).show();
            }
        });


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();

                updateName();
                getUri();
            }
        });

        buildGoogleApiClient();

        //getGeofencingRequest();

        getAudioLocations();

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String defaultName = "non";
        String tName = sharedPref.getString("usernameSP", defaultName);

        greeting.setText("Songs saved under @" + tName + ".");
        nameField.setText(tName);
        username = tName;

        //addAllGeofences();



    }

    PendingIntent mGeofencePendingIntent;


    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        Intent intent = new Intent(getApplicationContext(), AudioPlay.class);
        //intent.setData(Uri.parse("http://developer.android.com/training/location/geofencing.html"));
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }


    public void updateName(){
        String name = nameField.getText().toString();
        greeting.setText("Songs saved under @" + name + ".");
        username = name;

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("usernameSP", name);
        editor.commit();

    }


    ArrayList<Uri> uriArrayList =  new ArrayList<>();
    protected void getUri(){
        ContentResolver cr = getApplicationContext().getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cur = cr.query(uri, null, selection, null, sortOrder);
        int count = 0;

        if(cur != null)
        {
            count = cur.getCount();

            if(count > 0)
            {
                while(cur.moveToNext())
                {
                    String data = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
                    // Add code to get more column here

                    // Save to your list here
                    uriArrayList.add(Uri.parse(data));

                    Log.d("Song", data);
                }
                try {
                    playAudio(uriArrayList.get(0));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d("SONG", "No songs on device");
            }
        }

        cur.close();
    }



    protected void playAudio(Uri inputUri) throws IOException {

        Uri myUri = inputUri;
        MediaPlayer mediaPlayer = new MediaPlayer();

        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDataSource(getApplicationContext(), myUri);

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){

            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.prepareAsync();
    }

    protected synchronized void buildGoogleApiClient() {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        super.onStop();
    }


    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        return mLocationRequest;
    }


    @Override
    public void onConnected(Bundle bundle) {

        Log.i(TAG, "CONNECTED!");
        startLocationUpdates();
        addAllGeofences();

    }



    ArrayList<String> dbPrintout = new ArrayList<>();
    protected String username = "non";

    public void getAudioLocations(){


        Log.d("score", "retrieving last locations");
        ParseQuery<ParseObject> query = ParseQuery.getQuery("augmentedAudio");
        //query.whereEqualTo("owner", username);
        query.orderByDescending("updatedAt");
        query.setLimit(10);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> locList, ParseException e) {
                if (e == null) {
                    Log.d("score", "Retrieved " + locList.size() + " scores");

                    databaseTextView.setText("Songs:\n");

                    for (int i=0; i<locList.size();i++){
                        double tLat = locList.get(i).getParseGeoPoint("location").getLatitude();
                        double tLong = locList.get(i).getParseGeoPoint("location").getLatitude();

                        String tempText = "Song by @" + locList.get(i).getString("owner") + ", planted at " + String.valueOf(tLat) + ", " + String.valueOf(tLong) + "\n";
                        databaseTextView.append(tempText);



                    }
                } else {
                    Log.d("score", "Error: " + e.getMessage());
                }
            }
        });

    }

    public void saveGeofence(String id, Location loc, float radius) {
        ParseGeoPoint tempGeoPoint = new ParseGeoPoint(loc.getLatitude(), loc.getLongitude());
        ParseObject geoSong = new ParseObject("augmentedAudio");
        geoSong.put("location", tempGeoPoint);
        geoSong.put("radius", radius);
        geoSong.put("owner", username);
        geoSong.saveInBackground();
    }

    public void addAllGeofences(){
        //clear mGeofenceList
        mGeofenceList.clear();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("augmentedAudio");
        //query.whereEqualTo("owner", username);
        query.orderByDescending("updatedAt");
        query.setLimit(10);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> locList, ParseException e) {
                if (e == null) {
                    Log.d("score", "Retrieved " + locList.size() + " scores");

                    databaseTextView.setText("Songs:\n");

                    for (int i=0; i<locList.size();i++){
                        double tLat = locList.get(i).getParseGeoPoint("location").getLatitude();
                        double tLong = locList.get(i).getParseGeoPoint("location").getLatitude();
                        //Location tLoc = new Location("nil");
                        Integer radi = (Integer) locList.get(i).getNumber("radius");
                        int bradi = radi;
                        float fradi = (float) bradi;

                        addGeofence(locList.get(i).getString("owner"), tLat, tLong, fradi);

                    }

                    printGeofences();
                } else {
                    Log.d("score", "Error: " + e.getMessage());
                }
            }
        });


    }

    public void printGeofences(){
        for (int j = 0; j < mGeofenceList.size(); j++){
            Log.d("LOCATION_LIST",mGeofenceList.get(j).toString());
        }
    }

    public void addGeofence(String id, double lat, double longi, float radius) {


        mGeofenceList.add(new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(id)

                        //radius is in meters
                .setCircularRegion(lat, longi, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Toast.makeText(this, "You did not authorize location permissions correctly", Toast.LENGTH_LONG).show();
            return;
        }
        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                getGeofencingRequest(),
                getGeofencePendingIntent()
        ).setResultCallback(this);

        printGeofences();
    }

    public Location joshGetLastLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.


            final int REQUEST_CODE_ASK_PERMISSIONS = 123;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_ASK_PERMISSIONS);

            Toast.makeText(this, "You did not authorize location permissions.", Toast.LENGTH_LONG).show();

            Log.i(TAG, "LOCATION ACCESS DENIED");
            return mLastLocation;
        }



        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
            mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
        } else {
            Toast.makeText(this, "NO location detected", Toast.LENGTH_LONG).show();
        }

        return mLastLocation;
    }


    public void joshStartLocationUpdates() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(createLocationRequest());

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());


    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.i(TAG, "LOCATION ACCESS DENIED, startLocationUpdates failed");
            Toast.makeText(this, "You did not authorize location permissions", Toast.LENGTH_LONG).show();
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, createLocationRequest(), this);

    }

    private void updateUI() {
        mLatitudeText.setText(String.valueOf(mCurrentLocation.getLatitude()));
        mLongitudeText.setText(String.valueOf(mCurrentLocation.getLongitude()));
        mLastUpdateTimeTextView.setText(mLastUpdateTime);
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateUI();
    }

    @Override
    public void onResult(Status status) {
        //do nothing
    }
}
