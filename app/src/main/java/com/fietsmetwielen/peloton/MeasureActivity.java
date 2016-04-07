package com.fietsmetwielen.peloton;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.RoadsApi;
import com.google.maps.model.SnappedPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class MeasureActivity extends AppCompatActivity implements SensorEventListener {
    Boolean summertime = false;
    Boolean permission = false;
    Boolean light_tracking = false;
    Boolean developing_mode = false;
    Boolean metingen = false;

    //gps
    double longitude = 0;
    double latitude = 0;
    float gps_accuracy = 1000;
    //sensoren
    private SensorManager sensorManager;
    private Sensor accel_sensor;
    private Sensor light_sensor;
    double x = 0;
    double y = 0;
    double z = 0;
    double x_new = 0;
    double y_new = 0;
    double z_new = 0;
    double lux = 0;
    float standaardafwijking;
    boolean tracking_active = false;
    double[][] matrix = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    ArrayList<double[]> data = new ArrayList<>();

    LocationManager locationManager;
    LocationListener locationListener;
    String username = new String();
    JSONObject jsonMainAccel = new JSONObject();
    JSONArray jsonFietstochtAccel = new JSONArray();
    JSONObject jsonMainLight = new JSONObject();
    JSONArray jsonFietstochtLight = new JSONArray();
    int AFSTAND_TUSSEN_OPSLAGPUNTEN = 20;

    String SESSION_ID_ACCEL= "1";
    String SESSION_ID_LIGHT= "2";
    String SERVER = "A2"; //standaard server, als niet in developer mode



    Location previous_location;
    Location final_location = null;
    float gps_speed = 0;
    double total_distance = 0;
    GeoApiContext context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure);
        get_preferences(); //zet summertime en permission naar de juiste waarde van in de instellingen.
        if (!developing_mode || !metingen){
            disable_developer_options();
        }
        init_location_services();// initieer locatieservices, en slaag de laatst gekende locatie al op in longitude en latitude
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        light_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        context = new GeoApiContext().setApiKey(getString(R.string.google_maps_key_server));
        TextView textViewhelp = (TextView)findViewById(R.id.help);
        textViewhelp.setText(getString(R.string.User_guide_metingen_1));
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_measure, menu);
        return true;
    }

    public void get_preferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        summertime = sharedPreferences.getBoolean("DST", false);
        permission = sharedPreferences.getBoolean("NSA", false);
        username = sharedPreferences.getString("username", "");
        developing_mode = sharedPreferences.getBoolean("Developing_mode", false);
        if (developing_mode){
            SERVER =sharedPreferences.getString("Server", "A2");
        }
        metingen = sharedPreferences.getBoolean("Metingen", false);
    }

    public void disable_developer_options(){
        TextView location1= (TextView)findViewById(R.id.location1);
        TextView location2= (TextView)findViewById(R.id.location2);
        TextView x_value= (TextView)findViewById(R.id.x_value);
        TextView y_value= (TextView)findViewById(R.id.y_value);
        TextView z_value= (TextView)findViewById(R.id.z_value);
        TextView textViewJSON= (TextView)findViewById(R.id.JSON);
        TextView textViewdata= (TextView)findViewById(R.id.DATA);
        TextView textViewaccuracy= (TextView)findViewById(R.id.accuracy);
        TextView textViewlux = (TextView)findViewById(R.id.lux);
        Switch switch1 = (Switch)findViewById(R.id.switch1);
        location1.setVisibility(View.INVISIBLE);
        location2.setVisibility(View.INVISIBLE);
        x_value.setVisibility(View.INVISIBLE);
        y_value.setVisibility(View.INVISIBLE);
        z_value.setVisibility(View.INVISIBLE);
        textViewJSON.setVisibility(View.INVISIBLE);
        textViewdata.setVisibility(View.INVISIBLE);
        textViewaccuracy.setVisibility(View.INVISIBLE);
        textViewlux.setVisibility(View.INVISIBLE);
        switch1.setVisibility(View.INVISIBLE);
        TextView textViewhelp = (TextView)findViewById(R.id.help);
        textViewhelp.setVisibility(View.VISIBLE);
    }
    public void enable_developer_options(){
        TextView location1= (TextView)findViewById(R.id.location1);
        TextView location2= (TextView)findViewById(R.id.location2);
        TextView x_value= (TextView)findViewById(R.id.x_value);
        TextView y_value= (TextView)findViewById(R.id.y_value);
        TextView z_value= (TextView)findViewById(R.id.z_value);
        TextView textViewJSON= (TextView)findViewById(R.id.JSON);
        TextView textViewdata= (TextView)findViewById(R.id.DATA);
        TextView textViewaccuracy= (TextView)findViewById(R.id.accuracy);
        TextView textViewlux = (TextView)findViewById(R.id.lux);
        Switch switch1 = (Switch)findViewById(R.id.switch1);
        location1.setVisibility(View.VISIBLE);
        location2.setVisibility(View.VISIBLE);
        x_value.setVisibility(View.VISIBLE);
        y_value.setVisibility(View.VISIBLE);
        z_value.setVisibility(View.VISIBLE);
        textViewJSON.setVisibility(View.VISIBLE);
        textViewdata.setVisibility(View.VISIBLE);
        textViewaccuracy.setVisibility(View.VISIBLE);
        textViewlux.setVisibility(View.VISIBLE);
        switch1.setVisibility(View.VISIBLE);
        TextView textViewhelp = (TextView)findViewById(R.id.help);
        textViewhelp.setVisibility(View.INVISIBLE);
    }

    public void init_location_services() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener();
    }

    public class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if (longitude == 0 && latitude == 0) {
                TextView status = (TextView) findViewById(R.id.status);
                status.setText(R.string.Location_found);
            }
            final_location = location;
            if (!(final_location == null)) {
                longitude = final_location.getLongitude();
                latitude = final_location.getLatitude();
                gps_accuracy = location.getAccuracy();
                gps_speed = location.getSpeed();
                TextView TV3 = (TextView) findViewById(R.id.location1);
                TextView TV4 = (TextView) findViewById(R.id.location2);
                TextView TV5 = (TextView) findViewById(R.id.accuracy);
                TV3.setText(String.valueOf(longitude));
                TV4.setText(String.valueOf(latitude));
                TV5.setText(String.valueOf(gps_accuracy));
                if (!tracking_active) {
                    Button button = (Button) findViewById(R.id.button_calibrate);
                    button.setEnabled(true);
                }
                if (previous_location != null && tracking_active) {
                    double distance =previous_location.distanceTo(final_location);
                    if (distance> AFSTAND_TUSSEN_OPSLAGPUNTEN) {

                        append_fietstocht();
                        total_distance+=distance;
                    }
                }
            }
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

    public void enable_lighttracking() {
        new get_sun_times().execute("http://api.sunrise-sunset.org/json?lat=" + latitude + "&lng=" + longitude + "&formatted=0");
    }

    public class get_sun_times extends AsyncTask<String, String, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                String line = "";
                StringBuffer buffer = new StringBuffer();
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                String finalJson = buffer.toString();
                JSONObject parentObject = new JSONObject(finalJson);
                JSONObject results = parentObject.getJSONObject("results");
                String sunrise = results.getString("astronomical_twilight_begin");
                String sunset = results.getString("astronomical_twilight_end");

                String[] tijden = {sunset, sunrise};
                return tijden;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            Date sunset = null;
            Date sunrise = null;
            SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'");
            TextView TV7=(TextView)findViewById(R.id.y_value);
            TV7.setText(result[0]);
            try {
                sunset = dateFormat2.parse(result[0]);
                sunrise = dateFormat2.parse(result[1]);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            //Als het zomeruur is: neem het winteruur voor de vergelijking met de uren van de API
            //Als het winteruur is, neem het verander het uur niet.
            Calendar calendar = Calendar.getInstance();
            Date date;
            if (summertime) {
                calendar.add(Calendar.HOUR, -1);
                date = calendar.getTime();
            } else {
                date = calendar.getTime();
            }
            light_tracking = date.compareTo(sunset) > 0 || date.compareTo(sunrise) < 0;
            Switch switch1 = (Switch)findViewById(R.id.switch1);
            if (switch1.isChecked()){
                light_tracking = true;
            }
            //na het calibreren en data ophalen kan er begonnen worden met tracken
            if (!tracking_active) {
                Button button = (Button) findViewById(R.id.button_start);
                button.setEnabled(true);
            }
            TextView textViewhelp = (TextView)findViewById(R.id.help);
            textViewhelp.setText(getString(R.string.User_guide_metingen_2));
        }
    }

    public void calibrate(View view) {
        if (permission){
                if (is_connected()){
                    //controleer op basis van gps locatie of het donker is:
                    TextView status = (TextView) findViewById(R.id.status);
                    enable_lighttracking();
                    //stel matrix op voor transformatie van de assen.
                    status.setText(R.string.Transformation_accel_axis);
                    float A_x = (float) (-z / Math.sqrt(x * x + z * z));
                    float A_z = (float) (x / Math.sqrt(x * x + z * z));
                    float alpha = (float) Math.acos(y / Math.sqrt(x * x + y * y + z * z));
                    float q_0 = (float) Math.cos(alpha / 2);
                    float q_1 = (float) Math.sin(alpha / 2) * A_x;
                    float q_2 = 0;
                    float q_3 = (float) Math.sin(alpha / 2) * A_z;
                    matrix = new double[][]{{(1 - 2 * (q_2 * q_2 + q_3 * q_3)), 2 * (q_1 * q_2 - q_0 * q_3), 2 * (q_0 * q_2 + q_1 * q_3)}, {2 * (q_1 * q_2 + q_0 * q_3), 1 - 2 * (q_1 * q_1 + q_3 * q_3), 2 * (q_2 * q_3 - q_0 * q_1)}, {2 * (q_1 * q_3 - q_0 * q_2), 2 * (q_0 * q_1 + q_2 * q_3), 1 - 2 * (q_1 * q_1 + q_2 * q_2)}};
                    double afwijking = Math.abs(Math.sqrt(x * x + y * y + z * z) - 9.81);
                    Button button = (Button) findViewById(R.id.button_calibrate);
                    if (afwijking < 0.2) {
                        button.setBackgroundColor(0xFF00FF00);
                    } else if (afwijking < 0.5) {
                        button.setBackgroundColor(0xFFFF8000);
                    } else if (afwijking >= 0.5) {
                        button.setBackgroundColor(0xFFFF0000);
                    }
                    //omdat dit proces op de achtergrond draait moet deze status na da transformatie gegeven worden...
                    status.setText(R.string.Searching_day_night);
                } else{
                    Toast.makeText(this, R.string.toast_no_internet_on_calibrate,Toast.LENGTH_SHORT).show();
                }
        }else Toast.makeText(this, R.string.Toast_no_permission_settings,Toast.LENGTH_SHORT).show();
    }

    public boolean is_connected() {
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            connected = true;
        } else
            connected = false;
        return connected;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER) {
            x=event.values[0];
            y=event.values[1];
            z=event.values[2];
            x_new= (double) (matrix[0][0]*x+matrix[0][1]*y+matrix[0][2]*z);
            y_new= (double) (matrix[1][0]*x+matrix[1][1]*y+matrix[1][2]*z);
            z_new= (double) (matrix[2][0]*x+matrix[2][1]*y+matrix[2][2]*z);
            TextView TV1= (TextView)findViewById(R.id.x_value);
            TextView TV2= (TextView)findViewById(R.id.y_value);
            TextView TV3= (TextView)findViewById(R.id.z_value);
            TextView TV4= (TextView)findViewById(R.id.lux);
            TV1.setText(String.valueOf(x_new));
            TV2.setText(String.valueOf(y_new));
            TV3.setText(String.valueOf(z_new));
            TV4.setText(String.valueOf(lux));
            if (tracking_active){
                if (light_tracking){
                    double[] array = {y_new,lux,gps_accuracy,gps_speed};
                    data.add(array);
                }else{
                    double[] array = {y_new,0,gps_accuracy,gps_speed};
                    data.add(array);
                }
            }
        }
        if (event.sensor.getType()==Sensor.TYPE_LIGHT){
            lux=event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();

        //sensoren:
        sensorManager.registerListener(this, accel_sensor,20000);
        sensorManager.registerListener(this, light_sensor, 100000);
        //gps:
        registerLocationListener();
        get_preferences();
        if (!developing_mode || !metingen){
            disable_developer_options();
        }else {
            enable_developer_options();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //sensoren:
        sensorManager.unregisterListener(this);
        //gps:
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

    public void stop(View view){
        tracking_active=false;

        try {
            if (is_connected()){
                Button button_stop=(Button)findViewById(R.id.button_stop);
                button_stop.setEnabled(false);
                TextView status = (TextView)findViewById(R.id.status);
                status.setText(R.string.Status_saving_data);
                snapToRoad(jsonFietstochtAccel);
                jsonMainAccel.put("fietstocht", jsonFietstochtAccel);
                if (light_tracking){
                    snapToRoad(jsonFietstochtLight);
                    jsonMainLight.put("fietstocht",jsonFietstochtLight);
                }
                jsonMainAccel.put("afstand", String.valueOf(total_distance));
                jsonMainLight.put("afstand", String.valueOf(total_distance));

                Button button_calibrate = (Button)findViewById(R.id.button_calibrate);
                button_calibrate.setEnabled(true);
                button_stop.setText(R.string.stop);
                Button button_start=(Button)findViewById(R.id.button_start);
                button_start.setEnabled(true);
                save_data();
                status.setText(R.string.Ready_for_tracking);
                data.clear();
                TextView textViewhelp = (TextView)findViewById(R.id.help);
                textViewhelp.setText(getString(R.string.User_guide_metingen_4));
            } else {
                Toast.makeText(this, R.string.Toast_no_connection_on_saving_data,Toast.LENGTH_SHORT).show();
                Button button_stop = (Button)findViewById(R.id.button_stop);
                button_stop.setText(R.string.button_stop_stopped_save);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }



    }

    public void start(View view){
        total_distance = 0;
        tracking_active=true;
        Button button_start=(Button)findViewById(R.id.button_start);
        button_start.setEnabled(false);
        Button button_stop=(Button)findViewById(R.id.button_stop);
        button_stop.setEnabled(true);
        Button button_calibrate=(Button)findViewById(R.id.button_calibrate);
        button_calibrate.setEnabled(false);
        TextView status = (TextView)findViewById(R.id.status);
        TextView textViewhelp = (TextView)findViewById(R.id.help);
        textViewhelp.setText(getString(R.string.User_guide_metingen_3));
        if (light_tracking) status.setText(R.string.Tracking_data_roads_lights);
        else status.setText(R.string.Tracking_data_roads_only);

        previous_location=final_location;

        jsonMainAccel = new JSONObject();
        jsonFietstochtAccel = new JSONArray();
        try {
            jsonMainAccel.put("gebruikersnaam", username);
            Calendar calendar = Calendar.getInstance();
            Date date = calendar.getTime();
            //todo: date formatting
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy' 'HH:mm:ss");
            String stringDate = dateFormat.format(date);
            jsonMainAccel.put("tijd", stringDate);
            JSONObject startpositie = new JSONObject();
            startpositie.put("long",String.valueOf(longitude));
            startpositie.put("lat", String.valueOf(latitude));

            com.google.maps.model.LatLng oldPoint = new com.google.maps.model.LatLng(startpositie.getDouble("lat"),
                    startpositie.getDouble("long"));

            PendingResult<SnappedPoint[]> request = RoadsApi.snapToRoads(context, false
                    , oldPoint);
            try{
                SnappedPoint pt = request.await()[0];
                startpositie.put("lat", pt.location.lat);
                startpositie.put("long", pt.location.lng);

            } catch (Exception e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }


            jsonMainAccel.put("startpositie", startpositie);
            if (light_tracking) {
                jsonMainLight = new JSONObject();
                jsonFietstochtLight = new JSONArray();
                jsonMainLight.put("gebruikersnaam", username);
                //todo: date formatting
                jsonMainLight.put("tijd", stringDate);
                JSONObject startpositie2 = new JSONObject();
                startpositie2.put("long",String.valueOf(longitude));
                startpositie2.put("lat", String.valueOf(latitude));
                jsonMainLight.put("startpositie", startpositie2);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void append_fietstocht(){
        analyse_data();
        previous_location=final_location;
    }
    public void analyse_data(){
        float gemiddelde_lux;
        float gemiddelde_snelheid;
        float gemiddelde_accuracy;
        double sumuntilnow_accel = 0;
        float sumuntilnow_snelheid = 0;
        float sumuntilnow_accuracy = 0;

        double totquaddev = 0;
        for(double[] array:data){
            sumuntilnow_accel += array[0];
            sumuntilnow_accuracy += array[2];
            sumuntilnow_snelheid += array[3];

        }
        double avg = sumuntilnow_accel / data.size();
        gemiddelde_snelheid = sumuntilnow_snelheid/data.size();
        gemiddelde_accuracy = sumuntilnow_accuracy/data.size();
        for(double[] array:data){
            totquaddev += Math.pow((avg - array[0]), 2);
        }
        double variance = totquaddev/data.size();
        standaardafwijking = (float) Math.sqrt(variance);
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("long", String.valueOf(longitude));
            jsonObject.put("lat", String.valueOf(latitude));
            jsonObject.put("variance", String.valueOf(variance));
            jsonObject.put("snelheid", String.valueOf(gemiddelde_snelheid));
            jsonObject.put("accuracy", String.valueOf(gemiddelde_accuracy));
            jsonObject.put("aantal metingen", String.valueOf(data.size()));
            jsonFietstochtAccel.put(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (light_tracking){
            float sum = 0;
            for(double[] array:data){
                sum+=array[1];
            }
            gemiddelde_lux = sum/data.size();
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("long", String.valueOf(longitude));
                jsonObject.put("lat", String.valueOf(latitude));
                jsonObject.put("gemiddelde_lux", String.valueOf(gemiddelde_lux));
                jsonObject.put("snelheid", String.valueOf(gemiddelde_snelheid));
                jsonObject.put("accuracy", String.valueOf(gemiddelde_accuracy));
                jsonObject.put("aantal metingen", String.valueOf(data.size()));
                jsonFietstochtLight.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        TextView TV = (TextView)findViewById(R.id.JSON);
        JSONObject nuivsdf = jsonMainAccel;
        try {
            nuivsdf.put("fietstocht",jsonFietstochtAccel);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        TV.setText(nuivsdf.toString());
        TextView TV2 = (TextView)findViewById(R.id.DATA);
        StringBuilder stringBuilder = new StringBuilder();
        for (double[] array: data){
            stringBuilder.append(array[0]);
            stringBuilder.append(" ");
        }
        TV2.setText(stringBuilder.toString());
        data.clear();
    }

    public void save_data(){
        new PutAsyncTask().execute("http://daddi.cs.kuleuven.be/peno3/data/"+SERVER+"/"+SESSION_ID_ACCEL);
        if (light_tracking){
            new PutAsyncTask2().execute("http://daddi.cs.kuleuven.be/peno3/data/"+SERVER+"/"+SESSION_ID_LIGHT);
        }
    }
    public String putDataToServer(String URL){
        String status=getString(R.string.data_succesfull_to_server);

        try {

            URL url = new URL(URL);
            String url_string = url.toString();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");

            //conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Type", "application/json");

            OutputStream os = conn.getOutputStream();
            os.write(jsonMainAccel.toString().getBytes());
            os.flush();


            //Read the acknowledgement message after putting data to server
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }
            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return status;
    }
    public String putDataToServer2(String URL){
        String status=getString(R.string.data_succesfull_to_server);
        try {
            URL url = new URL(URL);
            String url_string = url.toString();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");

            //conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Type", "application/json");
            OutputStream os = conn.getOutputStream();
            os.write(jsonMainLight.toString().getBytes());
            os.flush();
            //Read the acknowledgement message after putting data to server
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return status;
    }
    class PutAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return putDataToServer(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {

        }
    }
    class PutAsyncTask2 extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return putDataToServer2(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {

        }
    }

    public void snapToRoad(JSONArray fietstocht){
        try {
            for (int i = 0; i < fietstocht.length(); i++) {
                JSONObject punt = fietstocht.getJSONObject(i);
                com.google.maps.model.LatLng oldPoint = new com.google.maps.model.LatLng(punt.getDouble("lat"),
                        punt.getDouble("long"));

                PendingResult<SnappedPoint[]> request = RoadsApi.snapToRoads(context, false
                        , oldPoint);
                try{
                    SnappedPoint pt = request.await()[0];
                    Location eerste_punt = new Location("punt1");
                    Location tweede_punt = new Location("punt2");
                    eerste_punt.setLatitude(punt.getDouble("lat"));
                    eerste_punt.setLongitude(punt.getDouble("long"));
                    tweede_punt.setLatitude(pt.location.lat);
                    tweede_punt.setLongitude(pt.location.lng);
                    double afstand_tussen_locaties = eerste_punt.distanceTo(tweede_punt);
                    if (afstand_tussen_locaties<20){
                        punt.put("lat", pt.location.lat);
                        punt.put("long", pt.location.lng);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings_button) {
            Intent intentsettings = new Intent(this, SettingsActivity.class);
            startActivity(intentsettings);
        }
        return super.onOptionsItemSelected(item);
    }

}