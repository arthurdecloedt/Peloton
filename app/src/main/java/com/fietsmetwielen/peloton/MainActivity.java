package com.fietsmetwielen.peloton;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getString("username","").isEmpty() || sharedPreferences.getString("username","").equals("")){
            Intent intentusername = new Intent(this, Username.class);
            startActivity(intentusername);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }



    public void naarmap(View view) {
        Intent intentmaps = new Intent(this, MapsActivity.class);
        intentmaps.putExtra("route_meegegeven?",false);
        startActivity(intentmaps);

    }

    public void naarmeten(View view) {
        Intent intentmeasure = new Intent(this, MeasureActivity.class);
        startActivity(intentmeasure);
    }

    public void naarsettings(View view) {
        Intent intentsettings = new Intent(this, SettingsActivity.class);
        startActivity(intentsettings);
    }
    public void naarleaderboard(View view) {
        Intent intentleaderboard = new Intent(this, Leaderboard.class);
        startActivity(intentleaderboard);
    }
    public void naarusername(View view) {
        Intent intentusername = new Intent(this, Username.class);
        startActivity(intentusername);
    }
    public void naarlocation(View view) {
        Intent intentlocation = new Intent(this, Location_saver.class);
        startActivity(intentlocation);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.help_button) {
            onHelpButtonClicked();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onHelpButtonClicked() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(getString(R.string.User_guide_home_page_a) + "\n"+"\n"+ getString(R.string.User_guide_home_page_b) + "\n"+ "\n"+
                getString(R.string.User_guide_home_page_c) + "\n"+"\n"+ getString(R.string.User_guide_home_page_d) + "\n"+ "\n"+
                getString(R.string.User_guide_home_page_e) + "\n"+"\n"+ getString(R.string.User_guide_home_page_f) + "\n"+ "\n"+
                getString(R.string.User_guide_home_page_g) + "\n"+ "\n");
        alert.setPositiveButton(R.string.help_screen_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        AlertDialog dialog = alert.create();
        dialog.show();
    }


}