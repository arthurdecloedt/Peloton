package com.fietsmetwielen.peloton;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap;
    boolean centered=false;
    String username;
    boolean developing_mode;
    boolean pointers;
    String SERVER= "A2";
    JSONArray downloaded_data_accel;
    JSONArray downloaded_data_light;
    Spinner spinner;

    double MIN_ACCURACY = 10;
    double MIN_SNELHEID = 2;
    double MAX_SNELHEID = 10;
    double MIN_METINGEN = 30;
    double MAX_DISTANCE_BETWEEN_POINTS = 45;
    int LIJNDIKTE = 10;
    List<String> lichtkwaliteit = Arrays.asList("#7F000000", "#7FFFFF00");
    List<String> wegkwaliteit = Arrays.asList("#7FEC0503", "#7FFFCC00","#7F2EEC03");
    Set<String> usernames  = new CopyOnWriteArraySet<String>();
    HashMap<String, User> userList = new HashMap<>();

    int TELLER = 0;
    double laatste_fiets_longitude=0;
    double laatste_fiets_latitude=0;
    int spinner_position = 0;
    List<PolylineOptions> polylines_global_road = new ArrayList<>();
    List<PolylineOptions> polylines_global_light = new ArrayList<>();
    List<PolylineOptions> polylines_user_road = new ArrayList<>();
    List<PolylineOptions> polylines_user_light = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        get_preferences();
        registreer_spinners();
        setUpMapIfNeeded();
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }
    public void get_preferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        username = sharedPreferences.getString("username", "");
        developing_mode = sharedPreferences.getBoolean("Developing_mode", false);
        if (developing_mode) {
            SERVER = sharedPreferences.getString("Server", "A2");
            if (sharedPreferences.getBoolean("Accuracy",false)){
                MIN_ACCURACY=100;
            }
        }
        pointers = sharedPreferences.getBoolean("Pointers", false);
        LIJNDIKTE = Integer.parseInt(sharedPreferences.getString("Lijndikte", "10"));
        laatste_fiets_latitude = Double.parseDouble(sharedPreferences.getString("latitude", "0.0"));
        laatste_fiets_longitude = Double.parseDouble(sharedPreferences.getString("longitude","0.0"));
    }
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }


    private void setUpMap() {

        CameraUpdate center= CameraUpdateFactory.newLatLng(new LatLng(50.879312, 4.700367));
        CameraUpdate zoom=CameraUpdateFactory.zoomTo(13);

        mMap.moveCamera(center);
        mMap.animateCamera(zoom);
        mMap.setMyLocationEnabled(true);
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setMax(100);
        if (is_connected()){
            new get_all_routes_accel().execute("http://daddi.cs.kuleuven.be/peno3/data/"+SERVER+"/1");
            new get_all_routes_light().execute("http://daddi.cs.kuleuven.be/peno3/data/" + SERVER + "/2");
            progressBar.setProgress(10);
        }else{
            Toast.makeText(this, R.string.No_connection_map,Toast.LENGTH_SHORT).show();
        }
    }

    class get_all_routes_accel extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return getDataFromServer(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            save_data_accel(result);
        }
    }
    public String getDataFromServer(String URL){
        String result="";
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
        try {
            progressBar.setProgress(25);
            java.net.URL url = new URL(URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
            progressBar.setProgress(40);
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            while ((output = br.readLine()) != null) {
                result += output;
            }
            conn.disconnect();
            progressBar.setProgress(progressBar.getProgress()+30);
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }
        return result;
    };
    public void save_data_accel(String alle_routes){
        try {
            JSONObject jsonAlle_routes = new JSONObject(alle_routes);
            JSONArray data = jsonAlle_routes.getJSONArray("data");
            downloaded_data_accel = new JSONArray();
            for(int i = 0;i<data.length();i++){
                JSONObject route = data.getJSONObject(i);
                JSONObject echte_route = route.getJSONObject("data");
                usernames.add(echte_route.getString("gebruikersnaam"));
                downloaded_data_accel.put(echte_route);
            }
            bereken_userwaarden(data);
            TELLER+=1;
            if (TELLER==2){
                ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
                progressBar.setVisibility(View.INVISIBLE);
                spinner = (Spinner) findViewById(R.id.spinner);
                spinner.setEnabled(true);
                CameraUpdate center= CameraUpdateFactory.newLatLng(new LatLng(50.879312, 4.700367));
                CameraUpdate zoom=CameraUpdateFactory.zoomTo(13);
                mMap.moveCamera(center);
                mMap.animateCamera(zoom);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }




    class get_all_routes_light extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return getDataFromServer_light(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            save_data_light(result);
        }
    }
    public String getDataFromServer_light(String URL){
        String result="";
        try {
            java.net.URL url = new URL(URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            while ((output = br.readLine()) != null) {
                result += output;
            }
            conn.disconnect();
            ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
            progressBar.setProgress(progressBar.getProgress() + 30);
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }
        return result;
    };
    public void save_data_light(String alle_routes){
        try {
            JSONObject jsonAlle_routes = new JSONObject(alle_routes);
            JSONArray data = jsonAlle_routes.getJSONArray("data");
            downloaded_data_light = new JSONArray();

            for(int i = 0;i<data.length();i++){
                JSONObject route = data.getJSONObject(i);
                JSONObject echte_route = route.getJSONObject("data");
                usernames.add(echte_route.getString("gebruikersnaam"));
                downloaded_data_light.put(echte_route);
            }
            TELLER+=1;
            if (TELLER==2){
                ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
                progressBar.setVisibility(View.INVISIBLE);
                spinner = (Spinner) findViewById(R.id.spinner);
                spinner.setEnabled(true);
                CameraUpdate center= CameraUpdateFactory.newLatLng(new LatLng(50.879312, 4.700367));
                CameraUpdate zoom=CameraUpdateFactory.zoomTo(13);
                mMap.moveCamera(center);
                mMap.animateCamera(zoom);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }





    public boolean is_connected() {
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            connected = true;
        }
        return connected;
    }
    public String bepaal_kleur(double zscore, List kleuren){

        int gradatie;
        if (zscore<-0.2){
            gradatie = 3;
        }else if (zscore>0.8){
            gradatie = 1;
        }else{
            gradatie=2;
        }
        String kleur = (String) kleuren.get(gradatie-1);
        return kleur;

    }
    private double herschaal_variantie(double variance, double snelheid) {

        double rico = Math.sqrt(variance)/snelheid;
        double geschaalde_variantie = rico*(15/3.6)*rico*(15/3.6);

        return geschaalde_variantie;
    }

    public void bereken_userwaarden(JSONArray data) {
        double mean;
        double totaal;
        double totaalstandaard;
        double standaardafwijking;
        int aantal;
        int i;
        int j;
        JSONArray fietstocht = null;
        try {
            Iterator<String> keys = usernames.iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                aantal = 0;
                totaal = 0;
                totaalstandaard = 0;
                for (i = 0; i < data.length(); i++) {
                    JSONObject data_nu = data.getJSONObject(i).getJSONObject("data");
                    fietstocht = data_nu.getJSONArray("fietstocht");
                    for (j = 0; j < fietstocht.length(); j++) {
                        if (fietstocht.getJSONObject(j).getDouble("accuracy") <= MIN_ACCURACY
                                && fietstocht.getJSONObject(j).getDouble("snelheid") >= MIN_SNELHEID
                                && fietstocht.getJSONObject(j).getDouble("snelheid") < MAX_SNELHEID
                                && fietstocht.getJSONObject(j).getDouble("aantal metingen") >= MIN_METINGEN
                                && data_nu.getString("gebruikersnaam").equals(key)) {

                            totaal += fietstocht.getJSONObject(j).getDouble("variance");
                            aantal += 1;
                        }
                    }
                }
                mean = totaal / aantal;

                for (i = 0; i < data.length(); i++) {
                    JSONObject data_nu = data.getJSONObject(i).getJSONObject("data");
                    fietstocht = data_nu.getJSONArray("fietstocht");
                    for (j = 0; j < fietstocht.length(); j++) {
                        if (fietstocht.getJSONObject(j).getDouble("accuracy") <= MIN_ACCURACY
                                && fietstocht.getJSONObject(j).getDouble("snelheid") >= MIN_SNELHEID
                                && fietstocht.getJSONObject(j).getDouble("snelheid") < MAX_SNELHEID
                                && fietstocht.getJSONObject(j).getDouble("aantal metingen") >= MIN_METINGEN
                                && data_nu.getString("gebruikersnaam").equals(key)) {
                            totaalstandaard += (fietstocht.getJSONObject(j).getDouble("variance") - mean) * (fietstocht.getJSONObject(j).getDouble("variance") - mean);
                        }
                    }
                }
                standaardafwijking = Math.sqrt(totaalstandaard / aantal);
                User user = new User(key, mean, standaardafwijking);
                userList.put(user.userName, user);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }





    public double bereken_zscore(double x, double mean, double standaardafwijking){
        double zscore = 0;
        zscore = (x-mean)/standaardafwijking;
        return zscore;
    }




    private void registreer_spinners(){
        spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setEnabled(false);
        List<String> opties = new ArrayList<>();
        opties.add(getString(R.string.show_last_bike_location));
        opties.add(getString(R.string.Spinner_globale_wegkwaliteit));
        opties.add(getString(R.string.Spinner_globale_lichtkwaliteit));
        opties.add(getString(R.string.Spinner_wegkwaliteit_eigen_routes));
        opties.add(getString(R.string.Spinner_lichtkwaliteit_eigen_routes));
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opties);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                if (position == 0) {
                    spinner_position = 0;
                    if (mMap != null) mMap.clear();
                    if (laatste_fiets_latitude != 0.0) {
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(laatste_fiets_latitude, laatste_fiets_longitude))
                                .draggable(false));
                    }
                }
                if (position == 1) {
                    spinner_position = 1;
                    mMap.clear();
                    mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                        @Override
                        public void onMapLongClick(LatLng latLng) {
                            if (spinner_position == 1) {
                                open_street_view(latLng);
                            }
                        }
                    });

                    if (downloaded_data_accel == null) {
                        Toast.makeText(MapsActivity.this, R.string.Toast_no_routes_found, Toast.LENGTH_SHORT).show();
                    } else {
                        if (polylines_global_road.size() == 0) {
                            Thread mThread = new Thread(new bereken_kwaliteit_globaal());
                            mThread.start();
                        } else {
                            new teken_kwaliteit_globaal().run();
                        }
                    }
                }
                if (position == 2) {
                    spinner_position = 2;
                    mMap.clear();
                    if (downloaded_data_light == null) {
                        Toast.makeText(MapsActivity.this, R.string.Toast_no_routes_found, Toast.LENGTH_SHORT).show();
                    } else {
                        if (polylines_global_light.size() == 0) {
                            Thread mThread = new Thread(new bereken_licht_globaal());
                            mThread.start();
                        } else {
                            new teken_licht_globaal().run();
                        }
                    }
                }
                if (position == 3) {
                    spinner_position = 3;
                    mMap.clear();
                    mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                        @Override
                        public void onMapLongClick(LatLng latLng) {
                            if (spinner_position == 3) {
                                open_street_view(latLng);
                            }
                        }
                    });
                    if (downloaded_data_accel == null) {
                        Toast.makeText(MapsActivity.this, R.string.Toast_no_routes_found, Toast.LENGTH_SHORT).show();
                    } else {
                        if (polylines_user_road.size() == 0) {
                            Thread mThread = new Thread(new bereken_kwaliteit_user());
                            mThread.start();
                        } else {
                            new teken_kwaliteit_user().run();
                        }
                    }
                }
                if (position == 4) {
                    spinner_position = 4;
                    mMap.clear();
                    if (downloaded_data_light == null) {
                        Toast.makeText(MapsActivity.this, R.string.Toast_no_routes_found, Toast.LENGTH_SHORT).show();
                    } else {
                        if (polylines_user_light.size() == 0){
                            Thread mThread = new Thread(new bereken_licht_user());
                            mThread.start();
                        } else {
                            new teken_licht_user().run();
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void open_street_view(LatLng latLng){
        Intent intent = new Intent(this,StreetView.class);
        intent.putExtra("Lat",latLng.latitude);
        intent.putExtra("Long",latLng.longitude);
        startActivity(intent);
    }

    public ArrayList<Integer> is_er_licht(JSONObject metingen){
        double lichtminimum;
        ArrayList<Integer> Is_er_licht_lijst = new ArrayList();
        try {
            JSONArray fietstocht = null;
            fietstocht = metingen.getJSONArray("fietstocht");

            if (fietstocht.length() > 3) {
                lichtminimum = fietstocht.getJSONObject(0).getDouble("gemiddelde_lux");
                for (int k = 1; k<fietstocht.length();k++){
                    if (fietstocht.getJSONObject(k).getDouble("gemiddelde_lux") < lichtminimum){
                        lichtminimum = fietstocht.getJSONObject(k).getDouble("gemiddelde_lux");
                    }
                }
                for (int l=0;l<fietstocht.length();l++){
                    int er_is_licht;
                    if (fietstocht.getJSONObject(l).getDouble("gemiddelde_lux")==lichtminimum){
                        er_is_licht =0;
                    } else{
                        er_is_licht=1;
                    }
                    Is_er_licht_lijst.add(er_is_licht);
                }
            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
        return Is_er_licht_lijst;
    }

    public String bepaal_kleur_licht(int is_er_licht, List kleuren){


        int gradatie=1;

        if (is_er_licht==0){
            gradatie = 1;
        }else if (is_er_licht==1) {
            gradatie = 2;
        }
        String kleur = (String) kleuren.get(gradatie-1);
        return kleur;

    }

    public class bereken_licht_globaal implements Runnable {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            int i;
            for (int k = 0; k < downloaded_data_light.length(); k++) {
                try {
                    List<LatLng> puntenlijst = new ArrayList<>();
                    String vorige_kleur;
                    String huidige_kleur;
                    JSONObject metingen_licht = downloaded_data_light.getJSONObject(k);
                    List<Integer> Is_er_licht_lijst = is_er_licht(metingen_licht);
                    JSONArray fietstocht = metingen_licht.getJSONArray("fietstocht");

                    Location startLocation = new Location("");
                    startLocation.setLatitude(metingen_licht.getJSONObject("startpositie").getDouble("lat"));
                    startLocation.setLongitude(metingen_licht.getJSONObject("startpositie").getDouble("long"));

                    Location endLocation = new Location("");
                    endLocation.setLatitude(fietstocht.getJSONObject(0).getDouble("lat"));
                    endLocation.setLongitude(fietstocht.getJSONObject(0).getDouble("long"));

                    if (fietstocht.getJSONObject(0).getDouble("accuracy") <= MIN_ACCURACY
                            && fietstocht.getJSONObject(0).getDouble("snelheid") >= MIN_SNELHEID
                            && fietstocht.getJSONObject(0).getDouble("snelheid") < MAX_SNELHEID
                            && fietstocht.getJSONObject(0).getDouble("aantal metingen") >= MIN_METINGEN
                            && startLocation.distanceTo(endLocation) <= MAX_DISTANCE_BETWEEN_POINTS) {

                        polylines_global_light.add(new PolylineOptions()
                                        .width(LIJNDIKTE)
                                        .add(new LatLng(metingen_licht.getJSONObject("startpositie").getDouble("lat"), metingen_licht.getJSONObject("startpositie").getDouble("long")),
                                                new LatLng(fietstocht.getJSONObject(0).getDouble("lat"), fietstocht.getJSONObject(0).getDouble("long")))
                                        .color(Color.parseColor(bepaal_kleur_licht(Is_er_licht_lijst.get(0), lichtkwaliteit)))
                        );
                    }
                    puntenlijst.add(new LatLng(fietstocht.getJSONObject(0).getDouble("lat"), fietstocht.getJSONObject(0).getDouble("long")));
                    vorige_kleur = bepaal_kleur_licht(Is_er_licht_lijst.get(1), lichtkwaliteit);

                    for (i = 1; i < fietstocht.length(); i++) {

                        JSONObject punt1 = fietstocht.getJSONObject(i - 1);
                        JSONObject punt2 = fietstocht.getJSONObject(i);

                        Location firstLocation = new Location("");
                        firstLocation.setLatitude(punt1.getDouble("lat"));
                        firstLocation.setLongitude(punt1.getDouble("long"));

                        Location secondLocation = new Location("");
                        secondLocation.setLatitude(punt2.getDouble("lat"));
                        secondLocation.setLongitude(punt2.getDouble("long"));

                        huidige_kleur = bepaal_kleur_licht(Is_er_licht_lijst.get(i), lichtkwaliteit);

                        if (!huidige_kleur.equals(vorige_kleur)) {
                            polylines_global_light.add(new PolylineOptions()
                                    .addAll(puntenlijst)
                                    .color(Color.parseColor(vorige_kleur))
                                    .width(LIJNDIKTE));
                            puntenlijst.clear();
                            if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                    && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                    && punt2.getDouble("snelheid") < MAX_SNELHEID
                                    && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                    && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS) {
                                puntenlijst.add(new LatLng(punt1.getDouble("lat"), punt1.getDouble("long")));
                                puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                vorige_kleur = bepaal_kleur_licht(Is_er_licht_lijst.get(i), lichtkwaliteit);
                            } else {
                                boolean geen_punt_gevonden = true;
                                while (i < fietstocht.length() && geen_punt_gevonden){
                                    punt1 = fietstocht.getJSONObject(i - 1);
                                    punt2 = fietstocht.getJSONObject(i);

                                    firstLocation.setLatitude(punt1.getDouble("lat"));
                                    firstLocation.setLongitude(punt1.getDouble("long"));

                                    secondLocation.setLatitude(punt2.getDouble("lat"));
                                    secondLocation.setLongitude(punt2.getDouble("long"));

                                    if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                            && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                            && punt2.getDouble("snelheid") < MAX_SNELHEID
                                            && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                            && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS){
                                        geen_punt_gevonden = false;
                                        puntenlijst.add(new LatLng(punt1.getDouble("lat"), punt1.getDouble("long")));
                                        puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                        vorige_kleur = bepaal_kleur_licht(Is_er_licht_lijst.get(i), lichtkwaliteit);
                                    }
                                    i+=1;
                                }
                            }
                        } else {
                            if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                    && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                    && punt2.getDouble("snelheid") < MAX_SNELHEID
                                    && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                    && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS){
                                puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                            } else {
                                polylines_global_light.add(new PolylineOptions()
                                        .addAll(puntenlijst)
                                        .color(Color.parseColor(vorige_kleur))
                                        .width(LIJNDIKTE));
                                puntenlijst.clear();
                                boolean geen_punt_gevonden = true;
                                while (i < fietstocht.length() && geen_punt_gevonden){
                                    punt1 = fietstocht.getJSONObject(i - 1);
                                    punt2 = fietstocht.getJSONObject(i);

                                    firstLocation.setLatitude(punt1.getDouble("lat"));
                                    firstLocation.setLongitude(punt1.getDouble("long"));

                                    secondLocation.setLatitude(punt2.getDouble("lat"));
                                    secondLocation.setLongitude(punt2.getDouble("long"));

                                    if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                            && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                            && punt2.getDouble("snelheid") < MAX_SNELHEID
                                            && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                            && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS){
                                        geen_punt_gevonden = false;
                                        puntenlijst.add(new LatLng(punt1.getDouble("lat"), punt1.getDouble("long")));
                                        puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                        vorige_kleur = bepaal_kleur_licht(Is_er_licht_lijst.get(i), lichtkwaliteit);
                                    }
                                    i+=1;
                                }
                            }
                        }
                    }
                    polylines_global_light.add(new PolylineOptions().addAll(puntenlijst)
                            .width(LIJNDIKTE)
                            .color(Color.parseColor(vorige_kleur)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(new teken_licht_globaal());
        }
    }

    public class teken_licht_globaal implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < polylines_global_light.size(); i++) {
                mMap.addPolyline(polylines_global_light.get(i));
            }
        }
    }

    private class bereken_kwaliteit_globaal implements Runnable {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            int i;
            for (int k = 0; k < downloaded_data_accel.length(); k++) {
                String vorige_kleur;
                String huidige_kleur;
                List<LatLng> puntenlijst = new ArrayList<>();
                try {
                    JSONObject metingen = downloaded_data_accel.getJSONObject(k);
                    JSONArray fietstocht = metingen.getJSONArray("fietstocht");

                    Location startLocation = new Location("");
                    startLocation.setLatitude(metingen.getJSONObject("startpositie").getDouble("lat"));
                    startLocation.setLongitude(metingen.getJSONObject("startpositie").getDouble("long"));

                    Location endLocation = new Location("");
                    endLocation.setLatitude(fietstocht.getJSONObject(0).getDouble("lat"));
                    endLocation.setLongitude(fietstocht.getJSONObject(0).getDouble("long"));

                    if (fietstocht.getJSONObject(0).getDouble("accuracy") <= MIN_ACCURACY
                            && fietstocht.getJSONObject(0).getDouble("snelheid") >= MIN_SNELHEID
                            && fietstocht.getJSONObject(0).getDouble("snelheid") < MAX_SNELHEID
                            && fietstocht.getJSONObject(0).getDouble("aantal metingen") >= MIN_METINGEN
                            && startLocation.distanceTo(endLocation) <= MAX_DISTANCE_BETWEEN_POINTS) {
                        double variance = herschaal_variantie(fietstocht.getJSONObject(0).getDouble("variance"),
                                fietstocht.getJSONObject(0).getDouble("snelheid"));

                        polylines_global_road.add(new PolylineOptions()
                                .add(new LatLng(metingen.getJSONObject("startpositie").getDouble("lat"), metingen.getJSONObject("startpositie").getDouble("long")),
                                        new LatLng(fietstocht.getJSONObject(0).getDouble("lat"), fietstocht.getJSONObject(0).getDouble("long")))
                                .width(LIJNDIKTE)
                                .color(Color.parseColor(bepaal_kleur(bereken_zscore(variance, userList.get(metingen.getString("gebruikersnaam")).mean,
                                                userList.get(metingen.getString("gebruikersnaam")).standaardafwijking),
                                        wegkwaliteit))));

                    }

                    puntenlijst.add(new LatLng(fietstocht.getJSONObject(0).getDouble("lat"), fietstocht.getJSONObject(0).getDouble("long")));
                    vorige_kleur = bepaal_kleur(bereken_zscore(herschaal_variantie(fietstocht.getJSONObject(1).getDouble("variance"),
                                            fietstocht.getJSONObject(1).getDouble("snelheid")),
                                    userList.get(metingen.getString("gebruikersnaam")).mean,
                                    userList.get(metingen.getString("gebruikersnaam")).standaardafwijking),
                            wegkwaliteit);
                    for (i = 1; i < fietstocht.length(); i++) {

                        JSONObject punt1 = fietstocht.getJSONObject(i - 1);
                        JSONObject punt2 = fietstocht.getJSONObject(i);

                        Location firstLocation = new Location("");
                        firstLocation.setLatitude(punt1.getDouble("lat"));
                        firstLocation.setLongitude(punt1.getDouble("long"));

                        Location secondLocation = new Location("");
                        secondLocation.setLatitude(punt2.getDouble("lat"));
                        secondLocation.setLongitude(punt2.getDouble("long"));

                        double variance = herschaal_variantie(fietstocht.getJSONObject(i).getDouble("variance"),
                                fietstocht.getJSONObject(i).getDouble("snelheid"));
                        huidige_kleur = bepaal_kleur(bereken_zscore(variance,
                                        userList.get(metingen.getString("gebruikersnaam")).mean,
                                        userList.get(metingen.getString("gebruikersnaam")).standaardafwijking),
                                wegkwaliteit);

                        if (!huidige_kleur.equals(vorige_kleur)) {
                            polylines_global_road.add(new PolylineOptions()
                                    .addAll(puntenlijst)
                                    .color(Color.parseColor(vorige_kleur))
                                    .width(LIJNDIKTE));
                            puntenlijst.clear();
                            if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                    && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                    && punt2.getDouble("snelheid") < MAX_SNELHEID
                                    && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                    && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS) {
                                puntenlijst.add(new LatLng(punt1.getDouble("lat"), punt1.getDouble("long")));
                                puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                variance = herschaal_variantie(fietstocht.getJSONObject(i).getDouble("variance"),
                                        fietstocht.getJSONObject(i).getDouble("snelheid"));
                                vorige_kleur = bepaal_kleur(bereken_zscore(variance,
                                                userList.get(metingen.getString("gebruikersnaam")).mean,
                                                userList.get(metingen.getString("gebruikersnaam")).standaardafwijking),
                                        wegkwaliteit);
                            } else {
                                boolean geen_punt_gevonden = true;
                                while (i < fietstocht.length() && geen_punt_gevonden){
                                    punt1 = fietstocht.getJSONObject(i - 1);
                                    punt2 = fietstocht.getJSONObject(i);

                                    firstLocation.setLatitude(punt1.getDouble("lat"));
                                    firstLocation.setLongitude(punt1.getDouble("long"));

                                    secondLocation.setLatitude(punt2.getDouble("lat"));
                                    secondLocation.setLongitude(punt2.getDouble("long"));

                                    if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                            && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                            && punt2.getDouble("snelheid") < MAX_SNELHEID
                                            && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                            && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS){
                                        geen_punt_gevonden = false;
                                        puntenlijst.add(new LatLng(punt1.getDouble("lat"), punt1.getDouble("long")));
                                        puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                        variance = herschaal_variantie(fietstocht.getJSONObject(i).getDouble("variance"),
                                                fietstocht.getJSONObject(i).getDouble("snelheid"));
                                        vorige_kleur = bepaal_kleur(bereken_zscore(variance,
                                                        userList.get(metingen.getString("gebruikersnaam")).mean,
                                                        userList.get(metingen.getString("gebruikersnaam")).standaardafwijking),
                                                wegkwaliteit);
                                    }
                                    i+=1;
                                }
                            }
                        } else {
                            if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                    && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                    && punt2.getDouble("snelheid") < MAX_SNELHEID
                                    && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                    && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS){
                                puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                            } else {
                                polylines_global_road.add(new PolylineOptions()
                                        .addAll(puntenlijst)
                                        .color(Color.parseColor(vorige_kleur))
                                        .width(LIJNDIKTE));
                                puntenlijst.clear();
                                boolean geen_punt_gevonden = true;
                                while (i < fietstocht.length() && geen_punt_gevonden){
                                    punt1 = fietstocht.getJSONObject(i - 1);
                                    punt2 = fietstocht.getJSONObject(i);

                                    firstLocation.setLatitude(punt1.getDouble("lat"));
                                    firstLocation.setLongitude(punt1.getDouble("long"));

                                    secondLocation.setLatitude(punt2.getDouble("lat"));
                                    secondLocation.setLongitude(punt2.getDouble("long"));

                                    if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                            && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                            && punt2.getDouble("snelheid") < MAX_SNELHEID
                                            && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                            && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS){
                                        geen_punt_gevonden = false;
                                        puntenlijst.add(new LatLng(punt1.getDouble("lat"), punt1.getDouble("long")));
                                        puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                        variance = herschaal_variantie(fietstocht.getJSONObject(i).getDouble("variance"),
                                                fietstocht.getJSONObject(i).getDouble("snelheid"));
                                        vorige_kleur = bepaal_kleur(bereken_zscore(variance,
                                                        userList.get(metingen.getString("gebruikersnaam")).mean,
                                                        userList.get(metingen.getString("gebruikersnaam")).standaardafwijking),
                                                wegkwaliteit);
                                    }
                                    i+=1;
                                }
                            }
                        }
                    }
                    polylines_global_road.add(new PolylineOptions().addAll(puntenlijst) .color(Color.parseColor(vorige_kleur)) .width(LIJNDIKTE));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(new teken_kwaliteit_globaal());
        }
    }

    private class teken_kwaliteit_globaal implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < polylines_global_road.size(); i++) {
                mMap.addPolyline(polylines_global_road.get(i));
            }
        }
    }

    private class bereken_kwaliteit_user implements Runnable {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            int i;
            for (int k = 0; k < downloaded_data_accel.length(); k++) {
                String vorige_kleur;
                String huidige_kleur;
                List<LatLng> puntenlijst = new ArrayList<>();
                try {
                    JSONObject metingen = downloaded_data_accel.getJSONObject(k);
                    if (metingen.getString("gebruikersnaam").equals(username)) {
                        JSONArray fietstocht = metingen.getJSONArray("fietstocht");

                        Location startLocation = new Location("");
                        startLocation.setLatitude(metingen.getJSONObject("startpositie").getDouble("lat"));
                        startLocation.setLongitude(metingen.getJSONObject("startpositie").getDouble("long"));

                        Location endLocation = new Location("");
                        endLocation.setLatitude(fietstocht.getJSONObject(0).getDouble("lat"));
                        endLocation.setLongitude(fietstocht.getJSONObject(0).getDouble("long"));

                        if (fietstocht.getJSONObject(0).getDouble("accuracy") <= MIN_ACCURACY
                                && fietstocht.getJSONObject(0).getDouble("snelheid") >= MIN_SNELHEID
                                && fietstocht.getJSONObject(0).getDouble("snelheid") < MAX_SNELHEID
                                && fietstocht.getJSONObject(0).getDouble("aantal metingen") >= MIN_METINGEN
                                && startLocation.distanceTo(endLocation) <= MAX_DISTANCE_BETWEEN_POINTS) {
                            double variance = herschaal_variantie(fietstocht.getJSONObject(0).getDouble("variance"),
                                    fietstocht.getJSONObject(0).getDouble("snelheid"));

                            polylines_user_road.add(new PolylineOptions()
                                    .add(new LatLng(metingen.getJSONObject("startpositie").getDouble("lat"), metingen.getJSONObject("startpositie").getDouble("long")),
                                            new LatLng(fietstocht.getJSONObject(0).getDouble("lat"), fietstocht.getJSONObject(0).getDouble("long")))
                                    .width(LIJNDIKTE)
                                    .color(Color.parseColor(bepaal_kleur(bereken_zscore(variance, userList.get(metingen.getString("gebruikersnaam")).mean,
                                                    userList.get(metingen.getString("gebruikersnaam")).standaardafwijking),
                                            wegkwaliteit))));

                        }

                        puntenlijst.add(new LatLng(fietstocht.getJSONObject(0).getDouble("lat"), fietstocht.getJSONObject(0).getDouble("long")));
                        vorige_kleur = bepaal_kleur(bereken_zscore(herschaal_variantie(fietstocht.getJSONObject(1).getDouble("variance"),
                                                fietstocht.getJSONObject(1).getDouble("snelheid")),
                                        userList.get(metingen.getString("gebruikersnaam")).mean,
                                        userList.get(metingen.getString("gebruikersnaam")).standaardafwijking),
                                wegkwaliteit);

                        for (i = 1; i < fietstocht.length(); i++) {

                            JSONObject punt1 = fietstocht.getJSONObject(i - 1);
                            JSONObject punt2 = fietstocht.getJSONObject(i);

                            Location firstLocation = new Location("");
                            firstLocation.setLatitude(punt1.getDouble("lat"));
                            firstLocation.setLongitude(punt1.getDouble("long"));

                            Location secondLocation = new Location("");
                            secondLocation.setLatitude(punt2.getDouble("lat"));
                            secondLocation.setLongitude(punt2.getDouble("long"));

                            double variance = herschaal_variantie(fietstocht.getJSONObject(i).getDouble("variance"),
                                    fietstocht.getJSONObject(i).getDouble("snelheid"));
                            huidige_kleur = bepaal_kleur(bereken_zscore(variance,
                                            userList.get(metingen.getString("gebruikersnaam")).mean,
                                            userList.get(metingen.getString("gebruikersnaam")).standaardafwijking),
                                    wegkwaliteit);

                            if (!huidige_kleur.equals(vorige_kleur)) {
                                polylines_user_road.add(new PolylineOptions()
                                        .addAll(puntenlijst)
                                        .color(Color.parseColor(vorige_kleur))
                                        .width(LIJNDIKTE));
                                puntenlijst.clear();
                                if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                        && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                        && punt2.getDouble("snelheid") < MAX_SNELHEID
                                        && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                        && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS) {
                                    puntenlijst.add(new LatLng(punt1.getDouble("lat"), punt1.getDouble("long")));
                                    puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                    variance = herschaal_variantie(fietstocht.getJSONObject(i).getDouble("variance"),
                                            fietstocht.getJSONObject(i).getDouble("snelheid"));
                                    vorige_kleur = bepaal_kleur(bereken_zscore(variance,
                                                    userList.get(metingen.getString("gebruikersnaam")).mean,
                                                    userList.get(metingen.getString("gebruikersnaam")).standaardafwijking),
                                            wegkwaliteit);
                                } else {
                                    boolean geen_punt_gevonden = true;
                                    while (i < fietstocht.length() && geen_punt_gevonden){
                                        punt1 = fietstocht.getJSONObject(i - 1);
                                        punt2 = fietstocht.getJSONObject(i);

                                        firstLocation.setLatitude(punt1.getDouble("lat"));
                                        firstLocation.setLongitude(punt1.getDouble("long"));

                                        secondLocation.setLatitude(punt2.getDouble("lat"));
                                        secondLocation.setLongitude(punt2.getDouble("long"));

                                        if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                                && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                                && punt2.getDouble("snelheid") < MAX_SNELHEID
                                                && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                                && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS){
                                            geen_punt_gevonden = false;
                                            puntenlijst.add(new LatLng(punt1.getDouble("lat"), punt1.getDouble("long")));
                                            puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                            variance = herschaal_variantie(fietstocht.getJSONObject(i).getDouble("variance"),
                                                    fietstocht.getJSONObject(i).getDouble("snelheid"));
                                            vorige_kleur = bepaal_kleur(bereken_zscore(variance,
                                                            userList.get(metingen.getString("gebruikersnaam")).mean,
                                                            userList.get(metingen.getString("gebruikersnaam")).standaardafwijking),
                                                    wegkwaliteit);
                                        }
                                        i+=1;
                                    }
                                }
                            } else {
                                if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                        && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                        && punt2.getDouble("snelheid") < MAX_SNELHEID
                                        && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                        && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS){
                                    puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                } else {
                                    polylines_user_road.add(new PolylineOptions()
                                            .addAll(puntenlijst)
                                            .color(Color.parseColor(vorige_kleur))
                                            .width(LIJNDIKTE));
                                    puntenlijst.clear();
                                    boolean geen_punt_gevonden = true;
                                    while (i < fietstocht.length() && geen_punt_gevonden){
                                        punt1 = fietstocht.getJSONObject(i - 1);
                                        punt2 = fietstocht.getJSONObject(i);

                                        firstLocation.setLatitude(punt1.getDouble("lat"));
                                        firstLocation.setLongitude(punt1.getDouble("long"));

                                        secondLocation.setLatitude(punt2.getDouble("lat"));
                                        secondLocation.setLongitude(punt2.getDouble("long"));

                                        if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                                && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                                && punt2.getDouble("snelheid") < MAX_SNELHEID
                                                && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                                && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS){
                                            geen_punt_gevonden = false;
                                            puntenlijst.add(new LatLng(punt1.getDouble("lat"), punt1.getDouble("long")));
                                            puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                            variance = herschaal_variantie(fietstocht.getJSONObject(i).getDouble("variance"),
                                                    fietstocht.getJSONObject(i).getDouble("snelheid"));
                                            vorige_kleur = bepaal_kleur(bereken_zscore(variance,
                                                            userList.get(metingen.getString("gebruikersnaam")).mean,
                                                            userList.get(metingen.getString("gebruikersnaam")).standaardafwijking),
                                                    wegkwaliteit);
                                        }
                                        i+=1;
                                    }
                                }
                            }
                        }
                        polylines_user_road.add(new PolylineOptions().addAll(puntenlijst)
                                .width(LIJNDIKTE)
                                .color(Color.parseColor(vorige_kleur)));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(new teken_kwaliteit_user());
        }
    }

    public class teken_kwaliteit_user implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < polylines_user_road.size(); i++) {
                mMap.addPolyline(polylines_user_road.get(i));
            }
        }
    }

    public class bereken_licht_user implements Runnable {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            int i;
            for (int k = 0; k < downloaded_data_light.length(); k++) {
                try {
                    List<LatLng> puntenlijst = new ArrayList<>();
                    JSONObject metingen_licht = downloaded_data_light.getJSONObject(k);
                    if (metingen_licht.getString("gebruikersnaam").equals(username)) {
                        JSONArray fietstocht = metingen_licht.getJSONArray("fietstocht");
                        String vorige_kleur;
                        String huidige_kleur;
                        List<Integer> Is_er_licht_lijst = is_er_licht(metingen_licht);

                        Location startLocation = new Location("");
                        startLocation.setLatitude(metingen_licht.getJSONObject("startpositie").getDouble("lat"));
                        startLocation.setLongitude(metingen_licht.getJSONObject("startpositie").getDouble("long"));

                        Location endLocation = new Location("");
                        endLocation.setLatitude(fietstocht.getJSONObject(0).getDouble("lat"));
                        endLocation.setLongitude(fietstocht.getJSONObject(0).getDouble("long"));

                        if (fietstocht.getJSONObject(0).getDouble("accuracy") <= MIN_ACCURACY
                                && fietstocht.getJSONObject(0).getDouble("snelheid") >= MIN_SNELHEID
                                && fietstocht.getJSONObject(0).getDouble("snelheid") < MAX_SNELHEID
                                && fietstocht.getJSONObject(0).getDouble("aantal metingen") >= MIN_METINGEN
                                && startLocation.distanceTo(endLocation) <= MAX_DISTANCE_BETWEEN_POINTS) {

                            polylines_user_light.add(new PolylineOptions()
                                            .width(LIJNDIKTE)
                                            .add(new LatLng(metingen_licht.getJSONObject("startpositie").getDouble("lat"), metingen_licht.getJSONObject("startpositie").getDouble("long")),
                                                    new LatLng(fietstocht.getJSONObject(0).getDouble("lat"), fietstocht.getJSONObject(0).getDouble("long")))
                                            .color(Color.parseColor(bepaal_kleur_licht(Is_er_licht_lijst.get(0), lichtkwaliteit)))
                            );
                        }
                        puntenlijst.add(new LatLng(fietstocht.getJSONObject(0).getDouble("lat"), fietstocht.getJSONObject(0).getDouble("long")));
                        vorige_kleur = bepaal_kleur_licht(Is_er_licht_lijst.get(1), lichtkwaliteit);

                        for (i = 1; i < fietstocht.length(); i++) {

                            JSONObject punt1 = fietstocht.getJSONObject(i - 1);
                            JSONObject punt2 = fietstocht.getJSONObject(i);

                            Location firstLocation = new Location("");
                            firstLocation.setLatitude(punt1.getDouble("lat"));
                            firstLocation.setLongitude(punt1.getDouble("long"));

                            Location secondLocation = new Location("");
                            secondLocation.setLatitude(punt2.getDouble("lat"));
                            secondLocation.setLongitude(punt2.getDouble("long"));

                            huidige_kleur = bepaal_kleur_licht(Is_er_licht_lijst.get(i), lichtkwaliteit);

                            if (!huidige_kleur.equals(vorige_kleur)) {
                                polylines_user_light.add(new PolylineOptions()
                                        .addAll(puntenlijst)
                                        .color(Color.parseColor(vorige_kleur))
                                        .width(LIJNDIKTE));
                                puntenlijst.clear();
                                if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                        && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                        && punt2.getDouble("snelheid") < MAX_SNELHEID
                                        && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                        && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS) {
                                    puntenlijst.add(new LatLng(punt1.getDouble("lat"), punt1.getDouble("long")));
                                    puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                    vorige_kleur = bepaal_kleur_licht(Is_er_licht_lijst.get(i), lichtkwaliteit);
                                } else {
                                    boolean geen_punt_gevonden = true;
                                    while (i < fietstocht.length() && geen_punt_gevonden) {
                                        punt1 = fietstocht.getJSONObject(i - 1);
                                        punt2 = fietstocht.getJSONObject(i);

                                        firstLocation.setLatitude(punt1.getDouble("lat"));
                                        firstLocation.setLongitude(punt1.getDouble("long"));

                                        secondLocation.setLatitude(punt2.getDouble("lat"));
                                        secondLocation.setLongitude(punt2.getDouble("long"));

                                        if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                                && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                                && punt2.getDouble("snelheid") < MAX_SNELHEID
                                                && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                                && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS) {
                                            geen_punt_gevonden = false;
                                            puntenlijst.add(new LatLng(punt1.getDouble("lat"), punt1.getDouble("long")));
                                            puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                            vorige_kleur = bepaal_kleur_licht(Is_er_licht_lijst.get(i), lichtkwaliteit);
                                        }
                                        i += 1;
                                    }
                                }
                            } else {
                                if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                        && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                        && punt2.getDouble("snelheid") < MAX_SNELHEID
                                        && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                        && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS) {
                                    puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                } else {
                                    polylines_user_light.add(new PolylineOptions()
                                            .addAll(puntenlijst)
                                            .color(Color.parseColor(vorige_kleur))
                                            .width(LIJNDIKTE));
                                    puntenlijst.clear();
                                    boolean geen_punt_gevonden = true;
                                    while (i < fietstocht.length() && geen_punt_gevonden) {
                                        punt1 = fietstocht.getJSONObject(i - 1);
                                        punt2 = fietstocht.getJSONObject(i);

                                        firstLocation.setLatitude(punt1.getDouble("lat"));
                                        firstLocation.setLongitude(punt1.getDouble("long"));

                                        secondLocation.setLatitude(punt2.getDouble("lat"));
                                        secondLocation.setLongitude(punt2.getDouble("long"));

                                        if (punt2.getDouble("accuracy") <= MIN_ACCURACY
                                                && punt2.getDouble("snelheid") >= MIN_SNELHEID
                                                && punt2.getDouble("snelheid") < MAX_SNELHEID
                                                && punt2.getDouble("aantal metingen") >= MIN_METINGEN
                                                && firstLocation.distanceTo(secondLocation) <= MAX_DISTANCE_BETWEEN_POINTS) {
                                            geen_punt_gevonden = false;
                                            puntenlijst.add(new LatLng(punt1.getDouble("lat"), punt1.getDouble("long")));
                                            puntenlijst.add(new LatLng(punt2.getDouble("lat"), punt2.getDouble("long")));
                                            vorige_kleur = bepaal_kleur_licht(Is_er_licht_lijst.get(i), lichtkwaliteit);
                                        }
                                        i += 1;
                                    }
                                }
                            }
                        }
                        polylines_user_light.add(new PolylineOptions().addAll(puntenlijst)
                                .width(LIJNDIKTE)
                                .color(Color.parseColor(vorige_kleur)));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(new teken_licht_user());
        }
    }

    public class teken_licht_user implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < polylines_user_light.size(); i++) {
                mMap.addPolyline(polylines_user_light.get(i));
            }
        }
    }

}