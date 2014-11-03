package cen.plus.plus.wecommunity;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.List;


public class NeighbourhoodAdapter extends ArrayAdapter<String> {

    int layoutId;
    Context context;
    List<String> data;

    public NeighbourhoodAdapter(Context context, int layoutId, List<String> data) {
        super(context, layoutId, data);
        this.layoutId = layoutId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView neighbourhoodName;
        TextView neighbourhoodPoints;
        String[] currentData;
        String neighbourhood;
        String points;

        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(layoutId, parent, false);

            neighbourhoodName = (TextView) convertView.findViewById(R.id.neighbourhoodName);
            neighbourhoodPoints = (TextView) convertView.findViewById(R.id.neighbourhoodPoints);
            currentData = (data.get(position)).split(":");
            neighbourhood = currentData[0];
            points = currentData[1];

            neighbourhoodName.setText(neighbourhood);
            neighbourhoodPoints.setText(points);
        } else {
            neighbourhoodName = (TextView) convertView.findViewById(R.id.neighbourhoodName);
        }
        //((CommunityGarden) context).onSectionAttached(position, data);

        return convertView;
    }
}
