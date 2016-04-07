package com.fietsmetwielen.peloton;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

class CustomAdapter extends ArrayAdapter<JSONObject>{
    public CustomAdapter(Context context, JSONObject[] data) {
        super(context,R.layout.list_view_element, data);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View customView = inflater.inflate(R.layout.list_view_element, parent, false);
        JSONObject jsonObject = getItem(position);
        TextView username = (TextView)customView.findViewById(R.id.username);
        TextView score = (TextView)customView.findViewById(R.id.score);
        TextView rank = (TextView)customView.findViewById(R.id.rank);
        rank.setText(String.valueOf(position+1));
        try {
            username.setText(jsonObject.getString("gebruikersnaam"));
            score.setText(String.valueOf(Math.round(jsonObject.getDouble("score"))));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return customView;
    }
}
