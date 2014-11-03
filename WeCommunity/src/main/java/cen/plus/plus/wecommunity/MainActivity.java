package cen.plus.plus.wecommunity;

import android.content.Context;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.view.View;

import com.amazonaws.android.auth.CognitoCredentialsProvider;
import com.amazonaws.android.mobileanalytics.*;
//import com.amazonaws.android.auth.CognitoCredentialsProvider;

public class MainActivity extends Activity {

    private static Context context;
    private static AmazonMobileAnalytics analytics;
    private final static String AWS_ACCOUNT_ID = "829035896787";
    private final static String COGNITO_IDENTITY_POOL = "us-east-1:1f75b79b-4605-450e-a360-cc5509c0d9c1";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        setTitle("Unify");
        context = getApplicationContext();

        Log.i("CTX", ""+context);
        Log.i("CTX", ""+MainActivity.this);
        Log.i("CTX", ""+getApplicationContext());
        // Create a credentials provider
//        CognitoCredentialsProvider cognitoProvider = new CognitoCredentialsProvider(
//                getApplicationContext(),
//                AWS_ACCOUNT_ID,
//                COGNITO_IDENTITY_POOL,
//                "arn:aws:iam:829035896787:role/Cognito_cenplusplusUnauth_DefaultRole",
//                "arn:aws:iam:829035896787:role/Cognito_cenplusplusAuth_DefaultRole"
//        );
//        Log.i("CTX", "After cognitoProvider: " + cognitoProvider);

//        try {
//            AnalyticsOptions options = new AnalyticsOptions();
//            options.withAllowsWANDelivery(true);
//            analytics = new AmazonMobileAnalytics(
//                    cognitoProvider,
//                    MainActivity.this,
//                    "cenplusplus.Unify",
//                    options
//            );
//        } catch (InitializationException ex) {
//            Log.e("AWS Analytics", "Failed to initialize Amazon Mobile Analytics", ex);
//        }

		ImageButton weatherButton = (ImageButton) findViewById(R.id.weatherButton);
		weatherButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), WeatherMainActivity.class);
                view.getContext().startActivity(intent);
            }


        });

//        ImageButton happeningsButton = (ImageButton) findViewById(R.id.happeningsButton);
//        happeningsButton.setOnClickListener(new OnClickListener() {
//            public void onClick(View view) {
//                // TODO: happenings
//                //Intent intent = new Intent(view.getContext(), CommunityGarden.class);
//                //view.getContext().startActivity(intent);
//            }
//        });

        ImageButton lostAndFoundButton = (ImageButton) findViewById(R.id.lostAndFoundButton);
        lostAndFoundButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // TODO: lost and found
                //Intent intent = new Intent(view.getContext(), LostAndFoundActivity.class);
                Intent intent = new Intent(view.getContext(), LostAndFoundView.class);
                view.getContext().startActivity(intent);
            }
        });

        ImageButton treeButton = (ImageButton) findViewById(R.id.treeButton);
        treeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), CommunityGarden.class);
                view.getContext().startActivity(intent);


            }
        });

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    /* Prevent LoginActivity to show up when user press back
    * */
    @Override
    public void onBackPressed() {
        Intent intent;
        intent = new Intent(context, LoginActivity.class);
        intent.putExtra("FLAG", 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}
