package cen.plus.plus.wecommunity;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.appspot.cenplusplus.cenplusplus.Cenplusplus;
import com.appspot.cenplusplus.cenplusplus.model.HelloLoginUser;
import com.appspot.cenplusplus.cenplusplus.model.HelloRainLocation;
import com.appspot.cenplusplus.cenplusplus.model.HelloRainLocationCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ListArea extends ListActivity {

    static final List<String> empty = new ArrayList<String>();
    AreaView areaView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new getRainArea().execute();
        areaView = new AreaView(this, empty);
        setListAdapter(areaView);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.list_area, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class getRainArea extends AsyncTask<Void, Void, HelloRainLocationCollection> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected HelloRainLocationCollection doInBackground(Void... voids) {
            // Retrieve service handler
            Cenplusplus apiServiceHandler = AppConstants.getApiServiceHandle();
            try {
                Cenplusplus.Rain.ListAreas getAreas = apiServiceHandler.rain().listAreas();
                HelloRainLocationCollection areas = getAreas.execute();

                return areas;
            } catch (IOException e) {
                Log.e("Error", "Exception during API call", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(HelloRainLocationCollection areas) {
            if (areas != null) {
                // set list view
                List<String> NOT_RAINING = new ArrayList<String>();
                List<String> RAINING = new ArrayList<String>();
                List<HelloRainLocation> allLocation = areas.getLocations();
                HelloRainLocation current = null;
                for (int i = 0; i < allLocation.size(); i++) {
                    current = allLocation.get(i);
                    if (current.getIsRaining()) {
                        RAINING.add(current.getLocation() + "/" + current.getRainStatus());
                    } else {
                        NOT_RAINING.add(current.getLocation());
                    }
                }
                areaView.addAll(NOT_RAINING);
                areaView.addAll(RAINING);
                areaView.notifyDataSetChanged();
            } else {
                finish();
                Log.e("Error", "Failed to retrieve data");
            }

            //super.onPostExecute(helloLoginUser);
        }
    }
}
