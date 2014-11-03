package cen.plus.plus.wecommunity;

import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.DatePicker;
import android.widget.TimePicker;

public class DatePickerFragment extends DialogFragment 
					implements DatePickerDialog.OnDateSetListener {
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the current date as the default values for the picker
		final Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);
		
		// Create a new instance of TimePickerDialog and return it
		return new DatePickerDialog(getActivity(), this, year, month, day);
	}

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		// Set TextView with the date chosen
		Intent data = new Intent();
		Bundle extras = new Bundle();
		extras.putInt("year", year);
		extras.putInt("month", monthOfYear);
		extras.putInt("day", dayOfMonth);
		data.putExtras(extras);
		getTargetFragment().onActivityResult(getTargetRequestCode(),
				Activity.RESULT_OK, data);
		dismiss();
	}
	
}
