package cen.plus.plus.wecommunity;

import android.app.Activity;
import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import java.io.FileNotFoundException;
import java.util.Calendar;


public class LostBelongingFragment extends Fragment {
    private static final int SELECT_IMAGE = 1;
    private static final int SELECT_DATE = 2;
    private static final int SELECT_TIME = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View reportView = inflater.inflate(R.layout.lost_belonging, container);
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);


        Button upload = (Button) reportView.findViewById(R.id.uploadButton);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, SELECT_IMAGE);
            }
        });

        TextView lastSeenDate = (TextView) reportView.findViewById(R.id.lastSeenDate);
        lastSeenDate.setText(day + "/" + month + "/" + year);

        final DatePickerFragment datePicker = new DatePickerFragment();
        lastSeenDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePicker.setCancelable(true);
                datePicker.setTargetFragment(getFragmentManager()
                        .findFragmentById(R.id.people_report_fragment), SELECT_DATE);
                datePicker.show(getFragmentManager(), "datePicker");
            }
        });

        TextView lastSeenTime = (TextView) reportView.findViewById(R.id.lastSeenTime);
        int minutes = c.get(Calendar.MINUTE);
        //if (DateFormat.is24HourFormat(getActivity())) {
        int hours = c.get(Calendar.HOUR_OF_DAY);
        lastSeenTime.setText((hours < 10 ? "0" + hours : hours) + ":" + (minutes < 10 ? "0" + minutes : minutes));

        final TimePickerFragment timePicker = new TimePickerFragment();
        lastSeenTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timePicker.setCancelable(true);
                timePicker.setTargetFragment(getFragmentManager()
                        .findFragmentById(R.id.belonging_report_fragment), SELECT_TIME);
                timePicker.show(getFragmentManager(), "timePicker");
            }
        });


//                Date start = null, end = null;
//
//                // Get end date and time
//                String date = (String) endDate.getText();
//                String endTime = (String) endClock.getText();
//                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
//                try {
//                    end = sdf.parse(endDate.getText() + " " + endClock.getText());
//                } catch (ParseException e) {
//                }

        EditText additional = (EditText) reportView.findViewById(R.id.additionalDetails);
        additional.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (view.getId() == R.id.additionalDetails) {
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

        Button submit = (Button) reportView.findViewById(R.id.button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = ((EditText) reportView.findViewById(R.id.nameText)).getText().toString();
                String age = ((EditText) reportView.findViewById(R.id.ageText)).getText().toString();
                Drawable photo = ((ImageView) reportView.findViewById(R.id.imageView)).getDrawable();
            }
        });
        return reportView;
    }


    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case SELECT_IMAGE:
                if(resultCode == LostPeopleMainActivity.RESULT_OK){
                    Uri selectedImage = data.getData();
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
            case SELECT_DATE:
                if(resultCode == LostPeopleMainActivity.RESULT_OK) {
                    // Update TextView to display new date
                    TextView lastSeenDate = (TextView) getActivity().findViewById(R.id.lastSeenDate);
                    int year = (Integer) data.getExtras().get("year");
                    int month = (Integer) data.getExtras().get("month") + 1;
                    int day = (Integer) data.getExtras().get("day");
                    lastSeenDate.setText(day + "/" + month + "/" + year);
                }
                else if(resultCode == LostPeopleMainActivity.RESULT_CANCELED) {
                    // user cancelled
                }
                else {
                    // failed
                }
                break;

            case SELECT_TIME:
                if(resultCode == LostPeopleMainActivity.RESULT_OK) {
                    // Update TextView to display new time
                    TextView lastSeenTime = (TextView) getActivity().findViewById(R.id.lastSeenTime);
                    int hours = (Integer) data.getExtras().get("hour");
                    int minutes = (Integer) data.getExtras().get("minute");
                    lastSeenTime.setText((hours < 10 ? "0" + hours : hours) + ":" + (minutes < 10 ? "0" + minutes : minutes));
                }
                else if(resultCode == LostPeopleMainActivity.RESULT_CANCELED) {
                    // user cancelled
                }
                else {
                    // failed
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
}
