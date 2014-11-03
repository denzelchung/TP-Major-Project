package cen.plus.plus.wecommunity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.appspot.cenplusplus.cenplusplus.Cenplusplus;
import com.appspot.cenplusplus.cenplusplus.model.HelloLoginUser;
import com.appspot.cenplusplus.cenplusplus.model.HelloValidUser;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SignUpFragment extends Fragment {

    private static final int SELECT_IMAGE = 1;
    private static String[] str = {"BEDOK", "TAMPINES", "TOA PAYOH"};
    private static Context context;
    private static Uri chosenImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View signUpView = inflater.inflate(R.layout.sign_up, container);
        context = getActivity().getApplicationContext();

        Spinner spinner = (Spinner) signUpView.findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(signUpView.getContext(), android.R.layout.simple_spinner_item, str);
        spinner.setAdapter(adapter);

        Button upload = (Button) signUpView.findViewById(R.id.uploadButton);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, SELECT_IMAGE);
            }
        });

        Button cancel = (Button) signUpView.findViewById(R.id.cancelButton);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });

        Button signUp = (Button) signUpView.findViewById(R.id.signupbutton);
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = ((EditText) signUpView.findViewById(R.id.usernameText)).getText().toString();
                String password = ((EditText)signUpView.findViewById(R.id.passwordText)).getText().toString();
                String confirmpassword = ((EditText)signUpView.findViewById(R.id.confirmpasswordText)).getText().toString();
                String email = ((EditText)signUpView.findViewById(R.id.emailText)).getText().toString();
                String mobilenumber = ((EditText)signUpView.findViewById(R.id.mobilenumberText)).getText().toString();
                RadioGroup genderGroup = ((RadioGroup) signUpView.findViewById(R.id.radioGroup));
                String gender = ((RadioButton) signUpView.findViewById(genderGroup.getCheckedRadioButtonId()))
                                                        .getText().toString().toLowerCase();
                Spinner neighbourhoodSpinner = (Spinner) signUpView.findViewById(R.id.spinner);
                String neighbourhood = neighbourhoodSpinner.getSelectedItem().toString();
                Drawable photo = ((ImageView) signUpView.findViewById(R.id.imageView)).getDrawable();

                ((TextView) getView().findViewById(R.id.usernameError)).setText("");
                ((TextView) getView().findViewById(R.id.passwordError)).setText("");
                ((TextView) getView().findViewById(R.id.passwordConfirmError)).setText("");
                ((TextView) getView().findViewById(R.id.emailError)).setText("");
                ((TextView) getView().findViewById(R.id.mobileNumberError)).setText("");

                if ((!username.equals("")) && (!password.equals("")) && password.equals(confirmpassword) && (!mobilenumber.equals(""))
                        && (!gender.equals("")) && (!neighbourhood.equals(""))) {
                    new validateDetails(username, password, email, mobilenumber, gender, neighbourhood, photo).execute();
                } else {
                    if (username.equals("")) {
                        ((TextView) signUpView.findViewById(R.id.usernameError)).setText("Field cannot be empty");
                    }
                    if (password.equals("")) {
                        ((TextView) signUpView.findViewById(R.id.passwordError)).setText("Field cannot be empty");
                    }
                    if (!password.equals(confirmpassword)) {
                        ((TextView) signUpView.findViewById(R.id.passwordConfirmError)).setText("Your passwords didn't match");
                    }
                    if (email.equals("")) {
                        ((TextView) signUpView.findViewById(R.id.emailError)).setText("Field cannot be empty");
                    }
                    if (mobilenumber.equals("")) {
                        ((TextView) signUpView.findViewById(R.id.mobileNumberError)).setText("Field cannot be empty");
                    }
                }
            }
        });
        return signUpView;
    }


    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case SELECT_IMAGE:
                if(resultCode == LostPeopleMainActivity.RESULT_OK){
                    Uri selectedImage = data.getData();
                    chosenImage = data.getData();
                    try {
                        final Bitmap yourSelectedImage = decodeUri(selectedImage);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ImageView preview = (ImageView) getActivity().findViewById(R.id.imageView);
                                preview.setImageBitmap(yourSelectedImage);
                            }
                        });
                    } catch (FileNotFoundException e) {

                    }
                }
                break;

        }
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getActivity().getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 200;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getActivity().getContentResolver().openInputStream(selectedImage), null, o2);

    }

    public class validateDetails extends AsyncTask<Void, Void, HelloValidUser> {
        String username;
        String password;
        String email;
        String phone;
        String gender;
        String neighbourhood;
        Drawable photo;

        validateDetails(String username, String password, String email, String phone,
                        String gender, String neighbourhood, Drawable photo) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.phone = phone;
            this.gender = gender;
            this.neighbourhood = neighbourhood;
            this.photo = photo;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // disable EditText fields while loading
            ((EditText) getView().findViewById(R.id.usernameText)).setEnabled(false);
            ((EditText) getView().findViewById(R.id.passwordText)).setEnabled(false);
            ((EditText) getView().findViewById(R.id.confirmpasswordText)).setEnabled(false);
            ((EditText) getView().findViewById(R.id.emailText)).setEnabled(false);
            ((EditText) getView().findViewById(R.id.mobilenumberText)).setEnabled(false);
            ((RadioButton) getView().findViewById(R.id.radioButtonMale)).setEnabled(false);
            ((RadioButton) getView().findViewById(R.id.radioButtonFemale)).setEnabled(false);
            ((Spinner) getView().findViewById(R.id.spinner)).setEnabled(false);
            ((Button) getView().findViewById(R.id.uploadButton)).setEnabled(false);
            ((Button) getView().findViewById(R.id.signupbutton)).setEnabled(false);

            // display loading spinner
            ProgressBar spinner = (ProgressBar) getView().findViewById(R.id.progressBar);
            spinner.setVisibility(View.VISIBLE);
        }

        @Override
        protected HelloValidUser doInBackground(Void... voids) {
            // Retrieve service handler
            Cenplusplus apiServiceHandler = AppConstants.getApiServiceHandle();
            try {
                Cenplusplus.Login.Register getUserDetails = apiServiceHandler.login().register(username, password, email);
                HelloValidUser userDetails = getUserDetails.execute();
                return userDetails;
            } catch (IOException e) {
                Log.e("Error", "Exception during API call", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(HelloValidUser validUser) {
            // enable EditText fields
            ((EditText) getView().findViewById(R.id.usernameText)).setEnabled(true);
            ((EditText) getView().findViewById(R.id.passwordText)).setEnabled(true);
            ((EditText) getView().findViewById(R.id.confirmpasswordText)).setEnabled(true);
            ((EditText) getView().findViewById(R.id.emailText)).setEnabled(true);
            ((EditText) getView().findViewById(R.id.mobilenumberText)).setEnabled(true);
            ((RadioButton) getView().findViewById(R.id.radioButtonMale)).setEnabled(true);
            ((RadioButton) getView().findViewById(R.id.radioButtonFemale)).setEnabled(true);
            ((Spinner) getView().findViewById(R.id.spinner)).setEnabled(true);
            ((Button) getView().findViewById(R.id.uploadButton)).setEnabled(true);
            ((Button) getView().findViewById(R.id.signupbutton)).setEnabled(true);

            // display loading spinner
            ProgressBar spinner = (ProgressBar) getView().findViewById(R.id.progressBar);
            spinner.setVisibility(View.GONE);

            if (validUser != null) {
                String usernameError = validUser.getUsernameError();
                String passwordError = validUser.getPasswordError();
                String emailError = validUser.getEmailError();

                if (usernameError.equals("") && passwordError.equals("") &&
                        emailError.equals("")) {
                    new validateRegister(username, password, email, phone, gender, neighbourhood, photo).execute();
                }
                ((TextView) getView().findViewById(R.id.usernameError)).setText(usernameError);
                ((TextView) getView().findViewById(R.id.passwordError)).setText(passwordError);
                ((TextView) getView().findViewById(R.id.emailError)).setText(emailError);
            } else {
                Toast.makeText(context, "Failed to create an account", Toast.LENGTH_SHORT).show();
                Log.e("Error", "Register failed: validate data");
            }
        }
    }

    public class validateRegister extends AsyncTask<Void, Void, String> {
        String username;
        String password;
        String email;
        String phone;
        String gender;
        String neighbourhood;
        Drawable photo;

        validateRegister(String username, String password, String email, String phone,
                         String gender, String neighbourhood, Drawable photo) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.phone = phone;
            this.gender = gender;
            this.neighbourhood = neighbourhood;
            this.photo = photo;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // disable EditText fields while loading
            ((EditText) getView().findViewById(R.id.usernameText)).setEnabled(false);
            ((EditText) getView().findViewById(R.id.passwordText)).setEnabled(false);
            ((EditText) getView().findViewById(R.id.confirmpasswordText)).setEnabled(false);
            ((EditText) getView().findViewById(R.id.emailText)).setEnabled(false);
            ((EditText) getView().findViewById(R.id.mobilenumberText)).setEnabled(false);
            ((RadioButton) getView().findViewById(R.id.radioButtonMale)).setEnabled(false);
            ((RadioButton) getView().findViewById(R.id.radioButtonFemale)).setEnabled(false);
            ((Spinner) getView().findViewById(R.id.spinner)).setEnabled(false);
            ((Button) getView().findViewById(R.id.uploadButton)).setEnabled(false);
            ((Button) getView().findViewById(R.id.signupbutton)).setEnabled(false);

            // display loading spinner
            ProgressBar spinner = (ProgressBar) getView().findViewById(R.id.progressBar);
            spinner.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("https://cenplusplus.appspot.com/signup");

            try {
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                builder.addTextBody("username", username);
                builder.addTextBody("password", password);
                builder.addTextBody("verify", password);
                builder.addTextBody("email", email);
                builder.addTextBody("phone", phone);
                builder.addTextBody("gender", gender);
                builder.addTextBody("location", neighbourhood);
                if (photo != null) {
                    builder.addPart("photo", new FileBody(new File(getRealPathFromURI(context, chosenImage))));
                }
                HttpEntity entity = builder.build();
                httppost.setEntity(entity);
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity httpEntity = response.getEntity();
                String result = EntityUtils.toString(httpEntity);
                Log.i("test", result);
//                    Log.i("test", "" + response.getEntity());
//                    Log.i("test", ""+response.getStatusLine().getStatusCode());
                return result;
            } catch (ClientProtocolException e) {
                Log.e("Error", ""+e);
            } catch (IOException e) {
                Log.e("Error", ""+e);
            }
            return null;
        }

        public String getRealPathFromURI(Context context, Uri contentUri) {
            Cursor cursor = null;
            try {
                String[] proj = { MediaStore.Images.Media.DATA };
                cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO: if invalid, what's result
            if (result != null) {
                Intent intent = new Intent(context, LoginActivity.class);
                intent.putExtra("username", username);
                intent.putExtra("password", password);
                getActivity().setResult(Activity.RESULT_OK, intent);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(intent);
                getActivity().finish();
            } else {
                // enable EditText fields after loading
                ((EditText) getView().findViewById(R.id.usernameText)).setEnabled(true);
                ((EditText) getView().findViewById(R.id.passwordText)).setEnabled(true);
                ((EditText) getView().findViewById(R.id.confirmpasswordText)).setEnabled(true);
                ((EditText) getView().findViewById(R.id.emailText)).setEnabled(true);
                ((EditText) getView().findViewById(R.id.mobilenumberText)).setEnabled(true);
                ((RadioButton) getView().findViewById(R.id.radioButtonMale)).setEnabled(true);
                ((RadioButton) getView().findViewById(R.id.radioButtonFemale)).setEnabled(true);
                ((Spinner) getView().findViewById(R.id.spinner)).setEnabled(true);
                ((Button) getView().findViewById(R.id.uploadButton)).setEnabled(true);
                ((Button) getView().findViewById(R.id.signupbutton)).setEnabled(true);

                // display loading spinner
                ProgressBar spinner = (ProgressBar) getView().findViewById(R.id.progressBar);
                spinner.setVisibility(View.GONE);

                Toast.makeText(context, "Failed to create an account", Toast.LENGTH_SHORT).show();
                Log.e("Error", "Register failed");
            }

            //super.onPostExecute(helloLoginUser);
        }
    }
}
