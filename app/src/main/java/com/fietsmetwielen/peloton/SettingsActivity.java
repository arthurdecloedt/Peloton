package com.fietsmetwielen.peloton;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {
    String PASSWORD = "fiets";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        EditTextPreference username = (EditTextPreference) findPreference("username");
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removePreference(username);
        preferenceScreen.removePreference((Preference) findPreference("longitude"));
        preferenceScreen.removePreference((Preference) findPreference("latitude"));
        Preference EdittxtPref = this.findPreference("developerpass");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean developping_mode = sharedPreferences.getBoolean("Developing_mode", false);
        if (developping_mode){
            Preference develop=findPreference("Developing_mode");
            develop.setEnabled(true);
            Preference server=findPreference("Server");
            server.setEnabled(true);
            Preference pointers = findPreference("Pointers");
            pointers.setEnabled(true);
            Preference accuracy = findPreference("Accuracy");
            accuracy.setEnabled(true);
            Preference metingen = findPreference("Metingen");
            metingen.setEnabled(true);

        }
        EdittxtPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {
                String pass = newValue.toString();
                if (pass.equals("fiets")) {
                    Preference develop=findPreference("Developing_mode");
                    develop.setEnabled(true);
                    Context context = getApplicationContext();
                    CharSequence text = "Pass Correct";
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                }
                else {
                    Context context = getApplicationContext();
                    CharSequence text = "Pass Incorrect";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, text, duration).show();
                }

                return false;

            }
        });
        Preference Switch = this.findPreference("Developing_mode");
        Switch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {
                Boolean waarde = (Boolean) newValue;
                if (!waarde) {
                    Preference develop = findPreference("Developing_mode");
                    develop.setEnabled(false);
                    Preference server = findPreference("Server");
                    server.setEnabled(false);
                    Preference pointers = findPreference("Pointers");
                    pointers.setEnabled(false);
                    Preference accuracy = findPreference("Accuracy");
                    accuracy.setEnabled(false);
                    Preference metingen = findPreference("Metingen");
                    metingen.setEnabled(false);
                } else {
                    Preference server = findPreference("Server");
                    server.setEnabled(true);
                    Preference pointers = findPreference("Pointers");
                    pointers.setEnabled(true);
                    Preference accuracy = findPreference("Accuracy");
                    accuracy.setEnabled(true);
                    Preference metingen = findPreference("Metingen");
                    metingen.setEnabled(true);
                }
                return true;

            }
        });
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
}



