package cen.plus.plus.wecommunity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;


public class LostAndFoundActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lostandfound);
        setTitle("Report Lost");

        ImageButton lostpeopleButton = (ImageButton) findViewById(R.id.lostpeopleButton);
        lostpeopleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), LostPeopleMainActivity.class);
                view.getContext().startActivity(intent);
            }
        });

        ImageButton lostpetButton = (ImageButton) findViewById(R.id.lostpetButton);
        lostpetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), LostPetsMainActivity.class);
                view.getContext().startActivity(intent);

            }
        });

        ImageButton lostbelongingButton = (ImageButton) findViewById(R.id.lostbelongingButton);
        lostbelongingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), LostBelongingMainActivity.class);
                view.getContext().startActivity(intent);

            }
        });
    }
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.lost_and_found, menu);
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

}
