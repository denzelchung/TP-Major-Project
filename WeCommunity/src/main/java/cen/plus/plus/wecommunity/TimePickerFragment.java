package cen.plus.plus.wecommunity;

import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;

public class TimePickerFragment extends DialogFragment 
					implements TimePickerDialog.OnTimeSetListener {
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the current time as the default values for the picker
		final Calendar c = Calendar.getInstance();
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);
		
		// Create a new instance of TimePickerDialog and return it
		return new TimePickerDialog(getActivity(), this, hour, minute,
				DateFormat.is24HourFormat(getActivity()));
	}

	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		// Set alarm with the time chosen by the user
		//TODO send time to CreateAlarm's TextView
		//TODO update TextView - clock
		Intent data = new Intent();
		Bundle extras = new Bundle();
		extras.putInt("hour", hourOfDay);
		extras.putInt("minute", minute);
		data.putExtras(extras);
		getTargetFragment().onActivityResult(getTargetRequestCode(), 
				Activity.RESULT_OK, data);
		dismiss();
	}
	
}
