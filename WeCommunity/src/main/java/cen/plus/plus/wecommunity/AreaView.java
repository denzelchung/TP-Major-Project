package cen.plus.plus.wecommunity;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by User on 19/7/2014.
 */
public class AreaView extends ArrayAdapter<String> {
    private final Context context;
    private final List<String> values;

    public AreaView(Context context, List<String> values) {
        super(context, R.layout.areaview, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.areaview, parent, false);
        String value = values.get(position);
        String[] locationRain = value.split("/");
        TextView textView = (TextView) row.findViewById(R.id.textView);
        textView.setText(locationRain[0]);
        if (locationRain.length > 1) {
            TextView raining = (TextView) row.findViewById(R.id.rainStatus);
            raining.setText(locationRain[1]);
        }
        return row;
    }
}
