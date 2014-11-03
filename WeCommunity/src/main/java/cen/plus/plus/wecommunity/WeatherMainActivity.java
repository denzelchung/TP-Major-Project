package cen.plus.plus.wecommunity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;

import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;



public class WeatherMainActivity extends Activity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
	
	//private String array_spinner[];
    //private static final double CAMERA_LNG = 1.3573463;
	//private static final double CAMERA_LAT = 103.8156764;
	private static final long ONE_MIN = 1000 * 60;
	private static final long TWO_MIN = ONE_MIN * 2;
	private static final long FIVE_MIN = ONE_MIN * 5;
	private static final long MEASURE_TIME = 1000 * 30;
	private static final long POLLING_FREQ = 1000 * 10;
	private static final long FASTES_UPDATE_FREQ = 1000 * 2;
	private static final float MIN_ACCURACY = 25.0f;
	private static final float MIN_LAST_READ_ACCURACY = 500.0f;
	
	private static final long INITIAL_ALARM_DELAY = 60 * 1000L;
	
	private GoogleMap mMap;
	//private final static String URL = "https://www.google.com.sg/maps/@1.3573463,103.8156764,12z";
	//GoogleMap googleMap;
	LatLng myPosition;
	
	// Define an object that holds accuracy and frequency parameters
	LocationRequest mLocationRequest;
	
	// Current best location estimate
	private Location mBestReading;
	
	private LocationClient mLocationClient;
	private Location mCurrentLocation;
	
	private GroundOverlay rainOverlay;
	public String rainUrl = "http://weather.nea.gov.sg/data/320_306/RadarMaps/";
	
	private AlarmManager mAlarmManager;
	private Intent mRainReceiverIntent;
	private PendingIntent mRainReceiverPendingIntent;
	
	private Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

		if (!servicesAvailable())
			finish();
		setContentView(R.layout.weather_activity_main);
		context = this;
		setTitle("Weather");
		// Create new Location Client. This class will handle callbacks
		mLocationClient = new LocationClient(this, this, this);

		// Create and define the LocationRequest
		mLocationRequest = LocationRequest.create();

		// Use high accuracy
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		// Update every 10 seconds
		mLocationRequest.setInterval(POLLING_FREQ);

		// Receive updates no more often than every 2 seconds
		mLocationRequest.setFastestInterval(FASTES_UPDATE_FREQ);


		// Get the AlarmManager Service
		mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		// Create PendingIntent to start the AlarmNotificationReceiver
		mRainReceiverIntent = new Intent(WeatherMainActivity.this,
				AlarmRainReceiver.class);
		mRainReceiverPendingIntent = PendingIntent.getBroadcast(
				WeatherMainActivity.this, 0, mRainReceiverIntent, 0);

		// Get Map Object
		mMap = ((MapFragment) getFragmentManager().findFragmentById(
				R.id.map)).getMap();

		LatLng singapore = new LatLng(1.3185849,103.8455665);
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(singapore,10));

		mMap.setOnMapClickListener(new OnMapClickListener() {

			@Override
			public void onMapClick(LatLng arg0) {
				updateRain();
			}
		});
//		mMap.setMyLocationEnabled(true);
//	    LocationManager locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
//	    Criteria criteria=new Criteria();
//	    String provider =locationManager.getBestProvider(criteria, true);
//	    Location location=locationManager.getLastKnownLocation(provider);
//	    Log.i("Location", ""+mCurrentLocation);
//	    if(location!=null){
//	        double latitude =location.getLatitude();
//	        double longitude=location.getLongitude();
//	        LatLng latlng=new LatLng(latitude, longitude);
//	        myPosition=new LatLng(latitude, longitude);
//	        mMap.addMarker(new MarkerOptions().position(myPosition).title("Here"));
//	    }
//	    if(mCurrentLocation != null) {
//	    	double latitude =location.getLatitude();
//	        double longitude=location.getLongitude();
//	        LatLng latlng=new LatLng(latitude, longitude);
//	        myPosition=new LatLng(latitude, longitude);
//	        mMap.addMarker(new MarkerOptions().position(myPosition).title("Here!"));
//	    }
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Connect to LocationServices
		mLocationClient.connect();

//		mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME,
//				SystemClock.elapsedRealtime() + INITIAL_ALARM_DELAY,
//				ONE_MIN,
//				mRainReceiverPendingIntent);
	}

	@Override
	protected void onStop() {

		// Stop updates
		mLocationClient.removeLocationUpdates(this);

		// Disconnect from LocationServices
		mLocationClient.disconnect();

		// Stop Alarm
		mAlarmManager.cancel(mRainReceiverPendingIntent);
		super.onStop();
	}
	
	// Called back when location changes

	@Override
	public void onLocationChanged(Location location) {

		// Determine whether new location is better than current best
		// estimate
		if (null == mBestReading
				|| location.getAccuracy() < mBestReading.getAccuracy()) {

			// Update best estimate
			mBestReading = location;

			// Update display
			updateDisplay(location);

			if (mBestReading.getAccuracy() < MIN_ACCURACY)
				mLocationClient.removeLocationUpdates(this);

		}
	}
	
	@Override
	public void onConnected(Bundle dataBundle) {

		// Get first reading. Get additional location updates if necessary

		if (servicesAvailable()) {
			// Get best last location measurement meeting criteria
			mBestReading = bestLastKnownLocation(MIN_LAST_READ_ACCURACY,
					FIVE_MIN);

			// Display last reading information
			if (null != mBestReading) {
				updateDisplay(mBestReading);

			} else {

				//mAccuracyView.setText("No Initial Reading Available");

			}

			if (null == mBestReading
					|| mBestReading.getAccuracy() > MIN_LAST_READ_ACCURACY
					|| mBestReading.getTime() < System.currentTimeMillis()
							- TWO_MIN) {
				mLocationClient.requestLocationUpdates(mLocationRequest, this);
				
				// Schedule a runnable to unregister location listeners
				Executors.newScheduledThreadPool(1).schedule(new Runnable() {

					@Override
					public void run() {

						mLocationClient.removeLocationUpdates(WeatherMainActivity.this);

					}
				}, MEASURE_TIME, TimeUnit.MILLISECONDS);
			}

		}
	}

	// Get the last known location from all providers
	// return best reading is as accurate as minAccuracy and
	// was taken no longer then minTime milliseconds ago

	private Location bestLastKnownLocation(float minAccuracy, long minTime) {

		Location bestResult = null;
		float bestAccuracy = Float.MAX_VALUE;
		long bestTime = Long.MIN_VALUE;

		// Get the best most recent location currently available
		mCurrentLocation = mLocationClient.getLastLocation();

		if (mCurrentLocation != null) {

			float accuracy = mCurrentLocation.getAccuracy();
			long time = mCurrentLocation.getTime();

			if (accuracy < bestAccuracy) {

				bestResult = mCurrentLocation;
				bestAccuracy = accuracy;
				bestTime = time;

			}
		}

		// Return best reading or null
		if (bestAccuracy > minAccuracy || bestTime < minTime) {
			return null;
		} else {
			return bestResult;
		}
	}


	@Override
	public void onDisconnected() {

		//Log.i(TAG, "Disconnected. Try again later.");

	}
	
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		//Log.i(TAG, "Connection Failed. Try again later.");
	}
	
	private boolean servicesAvailable() {

		// Check that Google Play Services are available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);

		// If Google Play services is available

		return (ConnectionResult.SUCCESS == resultCode);

	}

	// Update display
	private void updateDisplay(Location location) {

//		if(location!=null) {
//	        double latitude =location.getLatitude();
//	        double longitude=location.getLongitude();
//	        LatLng latlng=new LatLng(latitude, longitude);
//	        myPosition=new LatLng(latitude, longitude);
//	        mMap.clear();
//	        mMap.addMarker(new MarkerOptions().position(myPosition).title("Here"));
//	    }
		//Log.i("Location", "UpdateDisplay: "+mCurrentLocation);
		if(mCurrentLocation != null) {
	    	double latitude =location.getLatitude();
	        double longitude=location.getLongitude();
	        LatLng latlng=new LatLng(latitude, longitude);
	        myPosition=new LatLng(latitude, longitude);
	        mMap.clear();
	        mMap.addMarker(new MarkerOptions().position(myPosition).title("You're Here!"));
	    }
		updateRain();
//		mAccuracyView.setText("Accuracy:" + location.getAccuracy());
//
//		mTimeView.setText("Time:"
//				+ new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale
//						.getDefault()).format(new Date(location.getTime())));
//
//		mLatView.setText("Longitude:" + location.getLongitude());
//
//		mLngView.setText("Latitude:" + location.getLatitude());

	}

	// Update rain cloud
	private void updateRain() {
		//201405281115_0.png
		TimeZone tz = TimeZone.getTimeZone("GMT+8");
		Calendar c = Calendar.getInstance(tz);
		if (c.get(Calendar.MINUTE) % 5 == 0) {
			String time = String.format("%04d%02d%02d%02d%02d", 
										c.get(Calendar.YEAR), 
										c.get(Calendar.MONTH)+1,
										c.get(Calendar.DATE),
										c.get(Calendar.HOUR_OF_DAY),
										c.get(Calendar.MINUTE));
			String urlAdd = time + "_0.png";
			Log.i("time", urlAdd);
			new RetrieveRainImg().execute(rainUrl + urlAdd);
		} else {
			int currentMinute = c.get(Calendar.MINUTE);
			while (currentMinute % 5 != 0) {
				currentMinute -= 1;
			}
			String time = String.format("%04d%02d%02d%02d%02d", 
					c.get(Calendar.YEAR), 
					c.get(Calendar.MONTH)+1,
					c.get(Calendar.DATE),
					c.get(Calendar.HOUR_OF_DAY),
					currentMinute);
			String urlAdd = time + "_0.png";
			Log.i("time", urlAdd);
			new RetrieveRainImg().execute(rainUrl + urlAdd);
		}
	}
	
	


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu; this adds items to the action bar if it is present.
	    getMenuInflater().inflate(R.menu.weather, menu);
	    return true;
	}

	
//	    @Override
//	    public void onCreate(Bundle savedInstanceState) {
//	        super.onCreate(savedInstanceState);
	        
			// Center the map 
			// Should compute map center from the actual data
			
//			mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(
//					CAMERA_LAT, CAMERA_LNG)));
	        
//	        array_spinner=new String[5];
//	        array_spinner[0]="------";
//	        array_spinner[1]="Tampines";
//	        array_spinner[2]="Pasir Ris";
//	        array_spinner[3]="City Hall";
//	        array_spinner[4]="Yishun";
//	        Spinner s = (Spinner) findViewById(R.id.spinner1);
//	        ArrayAdapter adapter = new ArrayAdapter(this,
//	        android.R.layout.simple_spinner_item, array_spinner);
//	        s.setAdapter(adapter);

			//new HttpGetTask().execute(URL);
				
//			mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(
//					CAMERA_LAT, CAMERA_LNG)));
//	    }
	
	class RetrieveRainImg extends AsyncTask<String, Void, BitmapDescriptor> {

	    private Exception exception;

	    protected BitmapDescriptor doInBackground(String... src) {
	        try {
	            URL url= new URL(src[0]);
	            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		        connection.setDoInput(true);
		        connection.connect();
		        InputStream input = connection.getInputStream();
		        Bitmap myBitmap = BitmapFactory.decodeStream(input);
		        BitmapDescriptor myBitmapDesc = BitmapDescriptorFactory.fromBitmap(myBitmap);
		        return myBitmapDesc;
	        } catch (Exception e) {
	        	e.printStackTrace();
	            return null;
	        }
	    }

	    protected void onPostExecute(BitmapDescriptor img) {
	    	LatLng southwest = new LatLng(1.1744234361432542, 103.60077735371101);
			LatLng northeast = new LatLng(1.5698170552467112, 104.03130408710945);
			LatLngBounds bounds = new LatLngBounds(southwest, northeast);
			GroundOverlayOptions rainOptions = new GroundOverlayOptions()
					.image(img)
					.positionFromBounds(bounds);
			if (rainOverlay != null) {
				rainOverlay.remove();
			}
			rainOverlay = mMap.addGroundOverlay(rainOptions) ;
			
			Toast.makeText(context, "Updated", Toast.LENGTH_LONG).show();
	    }
	}
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.area_view) {
            Intent intent = new Intent(this, ListArea.class);
            startActivity(intent);
            return true;
        }

        else if(id == R.id.haze_view) {
            Intent intent = new Intent(this, HazeView.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
