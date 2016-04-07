package com.fietsmetwielen.peloton;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaOrientation;

public class StreetView extends AppCompatActivity implements OnStreetViewPanoramaReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_street_view);
        ActionBar ab = getSupportActionBar();
        ab.hide();
        StreetViewPanoramaFragment streetViewPanoramaFragment =
                (StreetViewPanoramaFragment) getFragmentManager()
                        .findFragmentById(R.id.streetviewpanorama);
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(this);
    }

    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama streetViewPanorama) {
        Intent intent = getIntent();
        LatLng latLng = new LatLng(intent.getDoubleExtra("Lat",0.0),intent.getDoubleExtra("Long",0.0));
        streetViewPanorama.setPosition(latLng);
        streetViewPanorama.setStreetNamesEnabled(false);
        streetViewPanorama.orientationToPoint(new StreetViewPanoramaOrientation(70,0));
    }

}
