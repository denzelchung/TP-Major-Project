package cen.plus.plus.wecommunity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
//import com.appspot.cenplusplus.cenplusplus.Cenplusplus;
//import com.appspot.cenplusplus.cenplusplus.Helloworld.Greetings.GetGreeting;
//import com.appspot.cenplusplus.cenplusplus.Helloworld.Greetings.Multiply;
//import com.appspot.cenplusplus.cenplusplus.model.HelloGreeting;
import com.appspot.cenplusplus.cenplusplus.Cenplusplus;
import com.appspot.cenplusplus.cenplusplus.model.HelloLoginUser;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by User on 11/6/2014.
 */
public class LoginActivity extends Activity {

    public static Context context;
    private int SIGNUPREQUEST = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        context = getApplicationContext();

        String username = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_LOGIN_USERNAME_KEY, "");
        if (username != "") {
            String password = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, "");
            // TODO: unhash password


            List<String> loginDetails = new ArrayList<String>();
            loginDetails.add(username);
            loginDetails.add(password);

            // TODO: show loading screen
            new validateLogin().execute(loginDetails);
        }

        Button loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String username = ((EditText) findViewById(R.id.usernameText)).getText().toString();
                String password = ((EditText) findViewById(R.id.passwordText)).getText().toString();
                List<String> loginDetails = new ArrayList<String>();
                loginDetails.add(username);
                loginDetails.add(password);
                new validateLogin().execute(loginDetails);
            }


        });

        Button signupButton = (Button) findViewById(R.id.signupbutton);
        signupButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), SignUpMainActivity.class);
                startActivityForResult(intent, SIGNUPREQUEST);
                //view.getContext().startActivity(intent);
            }
        });
    }

    /* Prevent login screen from showing up when user press back */
    @Override
    protected void onNewIntent(Intent intent) {
        int i = intent.getIntExtra("FLAG", 0);
        if (i == 0) {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SIGNUPREQUEST) {
            if (resultCode == RESULT_OK) {
                String signUpUsername = data.getStringExtra("username");
                String signUpPassword = data.getStringExtra("password");

                if (signUpUsername != null && signUpPassword != null) {
                    EditText usernameField = (EditText) findViewById(R.id.usernameText);
                    usernameField.setText(signUpUsername);

                    EditText passwordField = (EditText) findViewById(R.id.passwordText);
                    passwordField.setText(signUpPassword);

                    Button loginButton = (Button) findViewById(R.id.loginButton);
                    loginButton.performClick();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public class validateLogin extends AsyncTask<List<String>, Void, HelloLoginUser> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // disable EditText fields while loading
            ((EditText) findViewById(R.id.usernameText)).setEnabled(false);
            ((EditText) findViewById(R.id.passwordText)).setEnabled(false);
            ((Button) findViewById(R.id.loginButton)).setEnabled(false);
            ((Button) findViewById(R.id.signupbutton)).setEnabled(false);

            // display loading spinner
            ProgressBar spinner = (ProgressBar) findViewById(R.id.progressBar);
            spinner.setVisibility(View.VISIBLE);
        }

        @Override
        protected HelloLoginUser doInBackground(List<String>... details) {
            // Retrieve service handler
            Cenplusplus apiServiceHandler = AppConstants.getApiServiceHandle();
            try {
                List<String> result = details[0];
                String username = result.get(0);
                String password = result.get(1);
                Cenplusplus.Login.Validate getUserDetails = apiServiceHandler.login().validate(username, password);
                HelloLoginUser userDetails = getUserDetails.execute();

                if (userDetails != null) {
                    // TODO: hash password before saving
                    PrefUtils.saveToPrefs(context, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, password);
                }
                return userDetails;
            } catch (IOException e) {
                Log.e("Error", "Exception during API call", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(HelloLoginUser loginDetails) {
            if (loginDetails != null) {
                // save user details in SharedPreference
                PrefUtils.saveToPrefs(context, PrefUtils.PREFS_LOGIN_USERNAME_KEY, loginDetails.getUsername());
                PrefUtils.saveToPrefs(context, PrefUtils.PREFS_LOGIN_PICTURE_KEY, loginDetails.getPicture());
                PrefUtils.saveToPrefs(context, PrefUtils.PREFS_LOGIN_EMAIL_KEY, loginDetails.getEmail());
                PrefUtils.saveToPrefs(context, PrefUtils.PREFS_LOGIN_GENDER_KEY, loginDetails.getGender());
                PrefUtils.saveToPrefs(context, PrefUtils.PREF_LOGIN_PHONE_KEY, loginDetails.getPhone());
                PrefUtils.saveToPrefs(context, PrefUtils.PREF_LOGIN_LOCATION_KEY, loginDetails.getLocation());
                PrefUtils.saveToPrefs(context, PrefUtils.PREF_LOGIN_JOIN_DATE_KEY, loginDetails.getJoinDate());

                Intent intent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                // disable EditText fields while loading
                ((EditText) findViewById(R.id.usernameText)).setEnabled(true);
                ((EditText) findViewById(R.id.passwordText)).setEnabled(true);
                ((Button) findViewById(R.id.loginButton)).setEnabled(true);
                ((Button) findViewById(R.id.signupbutton)).setEnabled(true);

                // display loading spinner
                ProgressBar spinner = (ProgressBar) findViewById(R.id.progressBar);
                spinner.setVisibility(View.GONE);

                Toast.makeText(context, "Invalid login details", Toast.LENGTH_SHORT).show();
                Log.e("Error", "Invalid login details");
            }

            //super.onPostExecute(helloLoginUser);
        }
    }
}