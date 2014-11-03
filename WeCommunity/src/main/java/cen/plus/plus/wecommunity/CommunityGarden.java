package cen.plus.plus.wecommunity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.appspot.cenplusplus.cenplusplus.Cenplusplus;
import com.appspot.cenplusplus.cenplusplus.model.HelloGardenRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CommunityGarden extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private static CharSequence mTitle;
    static List<String> data = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_garden);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
        onSectionAttached(position);
    }

    public void onSectionAttached(int position) {
        Log.i("test", "POSITION: " +position);
        Log.i("test", "dAtA: " + data);
        if (data.size() > position) {
            String[] currentData = (data.get(position)).split(":");
            String neighbourhood = currentData[0];
            setTitle(neighbourhood);
            mTitle = neighbourhood;
        } else {
            mTitle = "";
        }
//        switch (number) {
//            case 1:
//                mTitle = getString(R.string.title_section1);
//                Log.i("test", NavigationDrawerFragment.NEIGHBOURHOODS+"");
//                //mTitle = (NavigationDrawerFragment.NEIGHBOURHOODS.get(number)).split(":")[0];
//                break;
//            case 2:
//                mTitle = getString(R.string.title_section2);
//                break;
//            case 3:
//                mTitle = getString(R.string.title_section3);
//                break;
//        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.community_garden, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_community_garden, container, false);
            //TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            // textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
//            String[] numbers = new String[] {
//                    "Moisture Level: ", "Temperature: ",
//                    "Humidity: ", "Heat Index: "};

            GridView gridView = (GridView) rootView.findViewById(R.id.gridView);
//            ArrayAdapter<String> adapter = new ArrayAdapter<String>(rootView.getContext(),
//                    android.R.layout.simple_list_item_1, numbers);
//            gridView.setAdapter(adapter);

            gridView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (view.getId() == R.id.gridView) {
                        view.getParent().requestDisallowInterceptTouchEvent(true);
                        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                            case MotionEvent.ACTION_UP:
                                view.getParent().requestDisallowInterceptTouchEvent(false);
                                break;
                        }
                    }
                    return false;
                }
            });
            Log.i("test", "TITLE: "+mTitle);
            if (!mTitle.equals("Community Garden")) {
                new getGardenStatus(rootView).execute("" + mTitle);
            }

            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
//            ((CommunityGarden) activity).onSectionAttached(
//                    getArguments().getInt(ARG_SECTION_NUMBER));
            //((CommunityGarden) activity).onSectionAttached("");
        }
    }

    public static class getGardenStatus extends AsyncTask<String, Void, HelloGardenRecord>
            implements DialogInterface.OnCancelListener {
        private Dialog dialog = null;
        private ProgressDialog progressBar = null;
        private View rootView;

        public getGardenStatus(View rootView) {
            this.rootView = rootView;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //dialog = new Dialog(getActivity(), android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
            //progressBar = new ProgressDialog(getActivity());
            //progressBar.setCancelable(false);
            //progressBar.setIndeterminate(true);
            progressBar = ProgressDialog.show(rootView.getContext(), "Retrieving Data", "Loading...");
        }

        @Override
        protected HelloGardenRecord doInBackground(String... location) {
            // Retrieve service handler
            Cenplusplus apiServiceHandler = AppConstants.getApiServiceHandle();
            try {
                String neighbourhood = location[0];
                Cenplusplus.Garden.Status getStatus = apiServiceHandler.garden().status(neighbourhood);
                HelloGardenRecord status = getStatus.execute();
                return status;
            } catch (IOException e) {
                Log.e("Error", "Exception during API call", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(HelloGardenRecord status) {
            //super.onPostExecute(neighbourhoods);
            if (status != null) {
                String[] numbers = new String[] {
                        "Moisture Level: " + status.getMoistureLevel(),
                        "Temperature: " + status.getTemperature(),
                        "Humidity: " + status.getHumidity(),
                        "Heat Index: " + status.getHeatIndex()};

                ImageView imageView = (ImageView) rootView.findViewById(R.id.imageView);
                if (status.getMoistureLevel() < 200) {
                    imageView.setImageResource(R.drawable.plantdry);
                } else if (status.getMoistureLevel() > 600) {
                    imageView.setImageResource(R.drawable.plantwet);
                }
                GridView gridView = (GridView) rootView.findViewById(R.id.gridView);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(rootView.getContext(),
                        android.R.layout.simple_list_item_1, numbers);
                gridView.setAdapter(adapter);
            } else {
                //rootView.getContext().finish();
            }
            progressBar.dismiss();
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            cancel(true);
        }
    }

}
