package cen.plus.plus.wecommunity;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;

/**
 * Created by User on 19/7/2014.
 */
public class HazeView extends Activity {
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.map_view) {
            Intent intent = new Intent(this, WeatherMainActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.area_view) {
            Intent intent = new Intent(this, AreaView.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}