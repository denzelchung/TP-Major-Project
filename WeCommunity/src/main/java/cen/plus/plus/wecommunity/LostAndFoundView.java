package cen.plus.plus.wecommunity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import java.net.URI;
import java.net.URLEncoder;


public class LostAndFoundView extends ActionBarActivity implements ActionBar.OnNavigationListener, ReportFragment.OnFragmentInteractionListener {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lost_and_found_view);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Missing");
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(
                // Specify a SpinnerAdapter to populate the dropdown list.
                new ArrayAdapter<String>(
                        actionBar.getThemedContext(),
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        new String[] {
                                getString(R.string.title_section_people),
                                getString(R.string.title_section_pets),
                                getString(R.string.title_section_belongings),
                        }),
                this);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            getSupportActionBar().setSelectedNavigationItem(
                    savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
                getSupportActionBar().getSelectedNavigationIndex());
    }

    @Override
    public void onFragmentInteraction(ListView listView, View view) {
        TextView additionalTv = (TextView) view.findViewById(R.id.additionalTv);
        Log.i("test", "onFragmentInteraction: " + additionalTv.getText());
        if (additionalTv.getEllipsize() == TextUtils.TruncateAt.END) {
            additionalTv.setEllipsize(null);
            additionalTv.setMaxLines(Integer.MAX_VALUE);
        } else {
            additionalTv.setEllipsize(TextUtils.TruncateAt.END);
            additionalTv.setHorizontallyScrolling(false);
            additionalTv.setSingleLine(false);
            additionalTv.setLines(3);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.lost_and_found_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, LostAndFoundActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_qr) {
//            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
//            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
//            startActivityForResult(intent, 0);
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.initiateScan();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        // When the given dropdown item is selected, show its contents in the
        // container view.
        Context context = getApplicationContext();
        getSupportFragmentManager().beginTransaction()
                //.replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .replace(R.id.container, ReportFragment.newInstance(position + 1))
                .commit();
        return true;
    }
    //scanning
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.i("SCAN", "RESULT");
        Log.i("SCAN", requestCode + " " + resultCode);
        IntentResult scanResult = IntentIntegrator.parseActivityResult(
                requestCode, resultCode, intent);
        Log.i("SCAN", ""+scanResult);
        if (scanResult != null) {
            Log.i("SCAN", scanResult.getContents());
            Log.i("SCAN", scanResult.getFormatName());
            // TODO: if getFormatName() == QR_CODE
            // TODO: getContents()
            if (scanResult.getFormatName().equals("QR_CODE")) {
                final String qrContents = scanResult.getContents();
                AlertDialog.Builder alert = new AlertDialog.Builder(LostAndFoundView.this);
                alert.setTitle("Message");
                //alert.setMessage("Hi");

                final EditText input = new EditText(LostAndFoundView.this);
                input.setText("Hi, I found your item at _____. Contact me at ______.");
                alert.setView(input);

                alert.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            String value = URLEncoder.encode(input.getText().toString(), "UTF-8");
                            Log.i("SCAN", value);

                            final String base = String.format("http://cenplusplus.appspot.com/services/twil/YuwfE2fEBHqJDtEXF5/%s/%s", value, qrContents);
                            new SendSms().execute(base);
                        } catch (Exception e) {
                            Log.e("SCAN", "Failed to encode URL: " + e);
                        }
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                alert.show();
            }
            //this.tvStatus.setText(scanResult.getContents());
            //this.tvResult.setText(scanResult.getFormatName());
        }
    }

    public class SendSms extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            //super.onPreExecute();

        }

        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                String base = strings[0];

                HttpClient httpClient = new DefaultHttpClient();
                httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Custom-Android-App");
                HttpGet httpGet = new HttpGet();
                URI website = new URI(base);
                httpGet.setURI(website);
                httpClient.execute(httpGet);
                return true;
            } catch (Exception e) {
                Log.e("SCAN", "Failed to scan: " + e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            Log.i("SCAN", "Send SMS: " + success);
        }
    }
//    /**
//     * A placeholder fragment containing a simple view.
//     */
//    public static class PlaceholderFragment extends Fragment {
//        /**
//         * The fragment argument representing the section number for this
//         * fragment.
//         */
//        private static final String ARG_SECTION_NUMBER = "section_number";
//
//        /**
//         * Returns a new instance of this fragment for the given section
//         * number.
//         */
//        public static PlaceholderFragment newInstance(int sectionNumber) {
//            PlaceholderFragment fragment = new PlaceholderFragment();
//            Bundle args = new Bundle();
//            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
//            fragment.setArguments(args);
//            return fragment;
//        }
//
//        public PlaceholderFragment() {
//        }
//
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                Bundle savedInstanceState) {
//            View rootView = inflater.inflate(R.layout.lost_and_found_view_fragment, container, false);
//            // ARG_SECTION_NUMBER = 1 for pets, 2 for people, 3 for belongings
//            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
//            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
//            return rootView;
//        }
//    }

}
