package com.fietsmetwielen.peloton;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Username extends AppCompatActivity {
    boolean developing_mode;
    String SERVER = "A2";
    String device_id;
    JSONObject data_to_server;
    List<String> all_usernames = new ArrayList<String>();

    @Override
    public void onBackPressed() {
        //mag niets doen, mag niet er uit gaan
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_username);
        get_preferences();
        if(Build.VERSION.SDK_INT>=23){
            if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            }
        } else {
            get_device_id();
        }
        if (is_connected()){
            new get_usernames().execute("http://daddi.cs.kuleuven.be/peno3/data/"+SERVER+"/3");
        }else{
            Toast.makeText(this, R.string.no_connection, Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch (requestCode){
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    get_device_id();
                }
        }
    }

    public void get_preferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        developing_mode = sharedPreferences.getBoolean("Developing_mode",false);
        if (developing_mode){
            SERVER =sharedPreferences.getString("Server","A2");
        }
    }

    public void get_device_id(){
        TelephonyManager telephonyManager = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        device_id = telephonyManager.getDeviceId();
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
    class get_usernames extends AsyncTask<String, Void, String>    {
        @Override
        protected String doInBackground(String... urls) {
            return getDataFromServer(urls[0]);
        }
        @Override
        protected void onPostExecute(String result) {
            show_usernames(result);
        }
    }
    public String getDataFromServer(String URL){
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
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }
        return result;
    };
    public void show_usernames(String result){

        try {
            List<String> possible_usernames = new ArrayList<String>();
            JSONObject jsonResult = new JSONObject(result);
            if (jsonResult.has("data")){
                JSONArray all_users = jsonResult.getJSONArray("data");
                for(int i = 0;i<all_users.length();i++){

                    JSONObject one_user = all_users.getJSONObject(i);
                    JSONObject echte_user = one_user.getJSONObject("data");
                    Iterator<String> keys = echte_user.keys();
                    while (keys.hasNext()){
                        String key = keys.next();
                        all_usernames.add(echte_user.getString(key));
                    }
                    if (echte_user.has(device_id)){
                        possible_usernames.add(echte_user.getString(device_id));
                    }
                }
            }
            show_on_listview(possible_usernames);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void show_on_listview(final List<String> possible_usernames){
        ListView listView = (ListView)findViewById(R.id.listView2);
        ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,possible_usernames);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                choose_username(possible_usernames.get(position));
            }
        });
    }
    public void choose_username(String username){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.apply();
        Toast.makeText(this, getString(R.string.Toast_your_username)+username,Toast.LENGTH_LONG).show();
        super.onBackPressed();

    }
    public void button_choose_username(View view){
        EditText editText = (EditText)findViewById(R.id.editText);
        String username= editText.getText().toString();

        String expression = "[a-zA-Z0-9.? ]*";
        CharSequence inputStr = username;
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);
        if (matcher.matches() && !username.isEmpty()){
            if (all_usernames.contains(username)){
                Toast.makeText(this, R.string.username_already_taken,Toast.LENGTH_LONG).show();
            } else{
                try {
                    data_to_server = new JSONObject().put(device_id,username);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(is_connected()){
                    new PutAsyncTask().execute("http://daddi.cs.kuleuven.be/peno3/data/"+SERVER+"/3");
                    choose_username(username);
                } else {
                    Toast.makeText(this, R.string.No_connection_try_again,Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, R.string.username_letters_numbers_error,Toast.LENGTH_LONG).show();
        }
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
    public String putDataToServer(String URL){
        String status="Put the data to server successfully!";

        try {

            URL url = new URL(URL);
            String url_string = url.toString();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");

            //conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Type", "application/json");

            OutputStream os = conn.getOutputStream();
            os.write(data_to_server.toString().getBytes());
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
}