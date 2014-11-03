package cen.plus.plus.wecommunity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class ReportFragment extends ListFragment implements AbsListView.OnScrollListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "section_number";
    //private static final String ARG_PARAM2 = "context";

    private static final String WEB_MAIN = "http://cenplusplus.appspot.com";
    private static final String WEB_URL = "http://cenplusplus.appspot.com/lost";
    private static String missing_type;
    private static String page_number;
    private static int page_num;
    private JSONArray report = null;

    // TODO: Rename and change types of parameters
    private int mParam1;
    //private String mParam2;

    private OnFragmentInteractionListener mListener;

    // TODO: Rename and change types of parameters
    public static ReportFragment newInstance(int param1) {
        ReportFragment fragment = new ReportFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, param1);
        //args.putBundle(ARG_PARAM2, context);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ReportFragment() {
    }

    MissingAdapter adapter;
    boolean END_OF_REPORTS = false;
    int REPORT_PER_PAGE = 5;
    int next_page;
    int old_next_page = 0;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnScrollListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getInt(ARG_PARAM1);
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }
        adapter = new MissingAdapter();
        setListAdapter(adapter);
        //getListView().setOnScrollListener(this);
        // TODO: Change Adapter to display your content
        //setListAdapter(new ArrayAdapter<DummyContent.DummyItem>(getActivity(),
        //        android.R.layout.simple_list_item_1, android.R.id.text1, DummyContent.ITEMS));
    }

    class MissingAdapter extends BaseAdapter {
        int count = REPORT_PER_PAGE; /* starting amount */
        int prevCount = 5;
        int maxPosView = 0;
        ArrayList<Integer> seen = new ArrayList<Integer>();
        HashMap<Integer, View> views = new HashMap<Integer, View>();

        public int getCount() { return count; }
        public Object getItem(int pos) { return pos; }
        public long getItemId(int pos) { return pos; }

        public View getView(int pos, View v, ViewGroup p) {
            Log.i("test", "View GET: " + v);
            Log.i("test", "HAshMAP: " + views.get(pos));
            if (views.get(pos) == null || !seen.contains(pos)) {
                seen.add(pos);
                LayoutInflater inflater = (LayoutInflater) getActivity().getApplicationContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.missing_report, null);
                //((TextView) v.findViewById(R.id.textView)).setText("a");

                // Missing People
                if (mParam1 == 1) {
                    missing_type = "/people.json";
                }
                // Missing Pets
                else if (mParam1 == 2) {
                    missing_type = "/pets.json";
                }
                // Missing Belongings
                else if (mParam1 == 3) {
                    missing_type = "/belongings.json";
                }
                page_num = (pos / 5) + 1;
                Log.i("test", "Pos: " + pos);
                Log.i("test", "PageNum: " + page_num);
                page_number = "?q=" + page_num;
                String URL = WEB_URL + missing_type + page_number;
                //if (count != prevCount) {
                //prevCount = count;
                v.setVisibility(View.INVISIBLE);
                new LoadMissingJson(v, pos, page_num).execute(URL);
                //}
                //TextView view = new TextView(getActivity().getApplicationContext());
                //view.setText("entry " + pos);
                views.put(pos, v);
            }
            v = views.get(pos);
            return v;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        boolean loadMore = /* maybe add a padding */
                firstVisibleItem + visibleItemCount >= totalItemCount;
        if(loadMore && next_page != 0 && old_next_page != next_page) {
            old_next_page = next_page;
            adapter.count += next_page; // or any other amount
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Log.i("test", "View: " + l);
        Log.i("test", "Pos: " + position);
        Log.i("test", "ID: " + id);
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            //mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
            mListener.onFragmentInteraction(l, v);
        }
    }

    /**
    * This interface must be implemented by activities that contain this
    * fragment to allow an interaction in this fragment to be communicated
    * to the activity and potentially other fragments contained in that
    * activity.
    * <p>
    * See the Android Training lesson <a href=
    * "http://developer.android.com/training/basics/fragments/communicating.html"
    * >Communicating with Other Fragments</a> for more information.
    */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(ListView l, View view);
    }

    private class LoadMissingJson extends AsyncTask<String, Void, JSONArray> {
        private ProgressDialog dialog;
        private String name;
        private String age;
        private String pictureUrl;
        private String contactDetails;
        private String lastSeen;
        private String lastSeenDate;
        private String lastSeenTime;
        private String additionalDetails;
        private String postedDate;
        private String postedTime;
        private String lastModifiedDate;
        private String lastModifiedTime;
        private String postedUser;
        private boolean isFound;
        private View view;
        private int pos;
        private int page_num;

        LoadMissingJson(View view, int pos, int page_num) {
            this.view = view;
            this.pos = pos % 5;
            this.page_num = page_num;
        }

        @Override
        protected void onPreExecute() {
            //super.onPreExecute();
            //Log.i("test", "Context: " + getActivity().getApplicationContext());
            //dialog = ProgressDialog.show(getActivity().getApplicationContext(), "title", "msg");
        }

        @Override
        protected JSONArray doInBackground(String... url) {
            JSONParser jParser = new JSONParser();
            // Getting JSON from URL
            JSONArray json = jParser.getJSONFromUrl(url[0]);

            page_number = "?q=" + page_num + 1;
            String URL = url[0].substring(0, url[0].indexOf("=") + 1) + (page_num + 1);
            Log.i("test", "NEXtpAGE: " + URL);
            next_page = jParser.getJSONFromUrl(URL).length();
            Log.i("test", "NEXTPAGE: " + next_page);
            try {
                JSONArray sortedJsonArray = new JSONArray();
                List<JSONObject> jsonList = new ArrayList<JSONObject>();
                for (int i = 0; i < json.length(); i++) {
                    jsonList.add(json.getJSONObject(i));
                }
                Collections.sort(jsonList, new Comparator<JSONObject>() {
                    @Override
                    public int compare(JSONObject jsonObject, JSONObject jsonObject2) {
                        String objDate = new String();
                        String objTime = new String();
                        String objDate2 = new String();
                        String objTime2 = new String();

                        try {
                            objDate = jsonObject.getString("posted_date");
                            objTime = jsonObject.getString("posted_time");
                            objDate2 = jsonObject2.getString("posted_date");
                            objTime2 = jsonObject2.getString("posted_time");
                        } catch (JSONException e) {
                        }

                        if (objDate != objDate2) {
                            return objTime.compareTo(objTime2);
                        } else {
                            return objDate.compareTo(objDate2);
                        }
                    }
                });

                for (int i = 0; i < json.length(); i++) {
                    sortedJsonArray.put(jsonList.get(i));
                }
                Log.i("test", ""+sortedJsonArray);
            } catch (JSONException e) {
            }
            //Log.i("test", "URL: " + url[0]);
            //Log.i("test", "JSON: " + json);
            return json;
        }

        @Override
        protected void onPostExecute(JSONArray json) {
            //super.onPostExecute(stringStringHashMap);
            //dialog.dismiss();
            try {
                // loop through each report
                //for (int i = 0; i < pos; i++) {
                Log.i("test", "Position: " + pos);
                Log.i("test", "JSONlen: " + json.length());
                if (pos < json.length()) {
                    final JSONObject jsonObject = json.getJSONObject(pos);
                    //Log.i("test", "jsonObj: " + jsonObject);
                    name = jsonObject.getString("name");
                    // if Missing People, get age
                    if (mParam1 == 1) {
                        age = jsonObject.getString("age");
                    }
                    //Log.i("test", "Name: " + name);
                    //Log.i("test", "Age: " + age);
                    if (jsonObject.has("picture")) {
                        pictureUrl = WEB_MAIN + jsonObject.getString("picture"); // optional
                        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
                        new DownloadImageTask((ImageView) view.findViewById(R.id.photoImage), progressBar).execute(pictureUrl);
                    }

                    //Log.i("test", "picture: " + pictureUrl);
                    contactDetails = jsonObject.getString("contact_details");
                    //Log.i("test", "contact: " + contactDetails);
                    lastSeen = jsonObject.getString("last_seen");
                    //Log.i("test", "lastseen: " + lastSeen);
                    lastSeenDate = jsonObject.getString("last_seen_date");
                    //Log.i("test", "lastseendate: " + lastSeenDate);
                    lastSeenTime = jsonObject.getString("last_seen_time");
                    //Log.i("test", "lastseentime: " + lastSeenTime);
                    if (jsonObject.has("additional_details")) {
                        additionalDetails = jsonObject.getString("additional_details"); // optional
                    }
                    //Log.i("test", "additional: " + additionalDetails);
                    postedDate = jsonObject.getString("posted_date");
                    //Log.i("test", "postedDate: " + postedDate);
                    postedTime = jsonObject.getString("posted_time");
                    //Log.i("test", "postedTime: " + postedTime);
                    //TODO: if no lastmodifiedDate, program stop running
//                        if (jsonObject.has("last_modified_date")) {
//                            lastModifiedDate = jsonObject.getString("last_modified_date"); // optional
//                        }

                    //Log.i("test", "lastModifiedDate: " + lastModifiedDate);
                    //lastModifiedTime = jsonObject.getString("last_modified_time"); // optional
                    //Log.i("test", "lastModifiedTime: " + lastModifiedTime);
                    postedUser = jsonObject.getString("user");
                    //Log.i("test", "user: " + postedUser);
                    isFound = jsonObject.getBoolean("found");

                    //TextView view = new TextView(getActivity().getApplicationContext());
                    TextView nameTv = (TextView) view.findViewById(R.id.nameTv);
                    nameTv.setText(name);

                    TextView ageTv = (TextView) view.findViewById(R.id.ageTv);
                    ageTv.setText(age);
                    if (mParam1 != 1) {
                        ageTv.setVisibility(View.GONE);
                    }

                    TextView contactTv = (TextView) view.findViewById(R.id.contactTv);
                    contactTv.setText(contactDetails);

                    TextView lastSeenTv = (TextView) view.findViewById(R.id.lastSeenTv);
                    lastSeenTv.setText(lastSeen);

                    TextView lastSeenDateTimeTv = (TextView) view.findViewById(R.id.lastSeenDateTimeTv);
                    lastSeenDateTimeTv.setText(lastSeenDate + "     " + lastSeenTime);

                    //TextView lastSeenTimeTv = (TextView) view.findViewById(R.id.lastSeenTimeTv);
                    //lastSeenTimeTv.setText(lastSeenTime);

                    TextView additionalTv = (TextView) view.findViewById(R.id.additionalTv);
                    if (additionalDetails == null) {
                        additionalTv.setVisibility(View.GONE);
                    } else {
                        additionalTv.setText(additionalDetails);
                        additionalTv.setEllipsize(TextUtils.TruncateAt.END);
                        additionalTv.setHorizontallyScrolling(false);
                        additionalTv.setSingleLine(false);
                        additionalTv.setLines(3);
                    }

                    TextView postedDateTv = (TextView) view.findViewById(R.id.postedDateTv);
                    postedDateTv.setText(postedDate);

                    TextView postedTimeTv = (TextView) view.findViewById(R.id.postedTimeTv);
                    postedTimeTv.setText(postedTime);

                    TextView postedUserTv = (TextView) view.findViewById(R.id.userTv);
                    postedUserTv.setText(postedUser);

                    if (isFound) {
                        // do something
                    }

                    view.setVisibility(View.VISIBLE);
                }

                if (json.length() != REPORT_PER_PAGE && page_num == 1 && !END_OF_REPORTS) {
                    Log.i("test", "Count reduced JSONLEN");
                    END_OF_REPORTS = true;
                    adapter.count -= REPORT_PER_PAGE - json.length();
                    adapter.notifyDataSetChanged();
                }
                Log.i("test", "END: " + END_OF_REPORTS);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        ProgressBar progressBar;

        public DownloadImageTask(ImageView bmImage, ProgressBar progressBar) {
            this.bmImage = bmImage;
            this.progressBar = progressBar;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bmImage.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
            bmImage.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

}
