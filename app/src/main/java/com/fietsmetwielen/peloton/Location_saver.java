package com.fietsmetwielen.peloton;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Location_saver extends AppCompatActivity {
    double longitude=0;
    double latitude=0;

    LocationManager locationManager;
    LocationListener locationListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_saver);
        get_preferences();
        Toast.makeText(this, R.string.Toast_searching_for_location_save_bike_location, Toast.LENGTH_LONG).show();
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();
    }
    public void get_preferences(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        longitude = Double.parseDouble(sharedPreferences.getString("longitude","0.0"));
        latitude = Double.parseDouble(sharedPreferences.getString("latitude", "0.0"));

    }
    public void set_preferences(Location location){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("longitude",String.valueOf(location.getLongitude()));
        editor.putString("latitude", String.valueOf(location.getLatitude()));
        editor.apply();
        Toast.makeText(this, R.string.Toast_locatie_gevonden_en_opgeslagen, Toast.LENGTH_LONG).show();
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                locationManager.removeUpdates(locationListener);
            }
        }
        else {
            locationManager.removeUpdates(locationListener);
        }

    }
    public class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            set_preferences(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }
    public void registerLocationListener() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
        finish();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    registerLocationListener();
                } else {
                    Toast.makeText(this, R.string.Toast_no_permission_location, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this,MainActivity.class));
                }
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        registerLocationListener();
    }


}
