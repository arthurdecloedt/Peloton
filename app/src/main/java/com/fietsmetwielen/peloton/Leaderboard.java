package com.fietsmetwielen.peloton;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Leaderboard extends AppCompatActivity {

    JSONArray routes = new JSONArray();
    String username= "";
    boolean developing_mode;
    String SERVER="A2";
    HashMap<String,Double> afstanden = new HashMap<>();
    JSONObject Alle_routes;
    int WEEK = 0;
    int MONTH = 1;

    //listview:
    ListView listView;
    ArrayAdapter<JSONObject> adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        get_preferences();
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setMax(100);
        Button week = (Button)findViewById(R.id.button3);
        Button month = (Button)findViewById(R.id.button4);
        Button all_time = (Button)findViewById(R.id.button2);
        week.setEnabled(false);
        month.setEnabled(false);
        all_time.setEnabled(false);
        if (is_connected()){
            new get_all_routes().execute("http://daddi.cs.kuleuven.be/peno3/data/"+SERVER+"/1");
            progressBar.setProgress(10);
        }else{
            Toast.makeText(this, R.string.No_connection_leaderboard, Toast.LENGTH_SHORT).show();
        }
    }
    public void get_preferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        username = sharedPreferences.getString("username", "");
        developing_mode = sharedPreferences.getBoolean("Developing_mode",false);
        if (developing_mode){
            SERVER =sharedPreferences.getString("Server","A2");
        }
    }
    public boolean is_connected() {
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            connected = true;
        } else
            connected = false;
        return connected;
    }
    class get_all_routes extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return getDataFromServer(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonAlle_routes = new JSONObject(result);
                show_leaderboard(jsonAlle_routes);
                Alle_routes = new JSONObject(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }

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
            progressBar.setProgress(70);
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }
        return result;
    };

    public void show_leaderboard(JSONObject jsonAlle_routes){
        Button week = (Button)findViewById(R.id.button3);
        Button month = (Button)findViewById(R.id.button4);
        Button all_time = (Button)findViewById(R.id.button2);
        week.setEnabled(true);
        month.setEnabled(true);
        all_time.setEnabled(true);
        afstanden.clear();
        double totaldist = 0.0;
        try {
            ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
            progressBar.setProgress(85);
            if (jsonAlle_routes.has("data")){
                JSONArray data = jsonAlle_routes.getJSONArray("data");
                for(int i = 0;i<data.length();i++){
                    JSONObject route = data.getJSONObject(i);
                    JSONObject echte_route = route.getJSONObject("data");
                    routes.put(echte_route);
                    String username = echte_route.getString("gebruikersnaam");
                    if (afstanden.containsKey(username)){
                        double afstand = afstanden.get(username);
                        if (echte_route.has("afstand")){
                            afstanden.put(username,afstand+echte_route.getDouble("afstand"));
                            totaldist=totaldist+echte_route.getDouble("afstand");

                        }
                    } else{
                        if (echte_route.has("afstand")) {
                            afstanden.put(username, echte_route.getDouble("afstand"));
                            totaldist=totaldist+echte_route.getDouble("afstand");
                        } else{
                            afstanden.put(username,0.0);
                        }
                    }
                }
                TextView totaldistTV = (TextView)findViewById(R.id.totaldistvalue);
                totaldistTV.setText(String.valueOf(Math.round(totaldist / 1000)));
            }

            List<JSONObject> scores= new ArrayList<JSONObject>();
            for (Map.Entry<String,Double> entry : afstanden.entrySet()){
                JSONObject score = new JSONObject();
                score.put("gebruikersnaam",entry.getKey());
                score.put("score",entry.getValue());
                scores.add(score);
            }

            Collections.sort(scores, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject a, JSONObject b) {
                    int valA = 0;
                    int valB = 0;

                    try {
                        valA = (int) a.getDouble("score");
                        valB = (int) b.getDouble("score");
                    } catch (JSONException e) {
                    }
                    if (valA < valB)
                        return 1;
                    if (valB < valA)
                        return -1;
                    return 0;
                }
            });
            listView = (ListView)findViewById(R.id.listView);
            JSONObject[] scoresarray = new JSONObject[scores.size()];
            scoresarray = scores.toArray(scoresarray);
            for(int i = 0; i<scoresarray.length;i++){
                if (scoresarray[i].getString("gebruikersnaam").equals(username)){
                    Toast.makeText(this, getString(R.string.Toast_your_rank_is) + String.valueOf(i+1), Toast.LENGTH_SHORT).show();
                }
            }
            adapter= new CustomAdapter(this,scoresarray);
            listView.setAdapter(adapter);
            progressBar.setVisibility(View.INVISIBLE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void week(View view){
       show_filtered_leaderboard(WEEK);
    }
    public void month(View view){
        show_filtered_leaderboard(MONTH);
    }
    public void all_time(View view){
        show_leaderboard(Alle_routes);
    }

    public void show_filtered_leaderboard(int week_month){
        try{
            if (Alle_routes.has("data")){
                JSONObject gefilterd_alle_routes = new JSONObject();
                JSONArray gefilterd_data = new JSONArray();
                JSONArray data = new JSONArray();
                data = Alle_routes.getJSONArray("data");
                for(int i = 0;i<data.length();i++){
                    JSONObject route = data.getJSONObject(i);
                    JSONObject echte_route = route.getJSONObject("data");
                    String tijd = echte_route.getString("tijd");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy' 'HH:mm:ss");
                    Date date_of_route = null;
                    try {
                        date_of_route = dateFormat.parse(tijd);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Calendar calendar = Calendar.getInstance();
                    if (week_month==WEEK) {
                        calendar.add(Calendar.DATE, -7);
                    }
                    if (week_month==MONTH){
                        calendar.add(Calendar.DATE, -31);
                    }
                    Date date_then = calendar.getTime();
                    if (date_of_route!=null && date_then!=null){
                        if (date_of_route.compareTo(date_then)>0){
                            if (route!=null){
                                gefilterd_data.put(route);
                            }
                        }
                    }
                }
                gefilterd_alle_routes.put("data",gefilterd_data);
                show_leaderboard(gefilterd_alle_routes);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
