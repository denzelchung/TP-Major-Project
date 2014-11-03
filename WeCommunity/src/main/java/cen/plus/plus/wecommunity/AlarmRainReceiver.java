package cen.plus.plus.wecommunity;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import cen.plus.plus.wecommunity.WeatherMainActivity.RetrieveRainImg;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class AlarmRainReceiver extends BroadcastReceiver {
	// Notification ID to allow for future updates
	private static final int MY_NOTIFICATION_ID = 1;

	// Notification Text Elements
	private final CharSequence tickerText = "Are You Playing Angry Birds Again!";
	private final CharSequence contentTitle = "A Kind Reminder";
	private final CharSequence contentText = "Get back to studying!!";

	// Notification Action Elements
	private Intent mNotificationIntent;
	private PendingIntent mContentIntent;

	// Notification Sound and Vibration on Arrival
//	private Uri soundURI = Uri
//			.parse("android.resource://course.examples.Alarms.AlarmCreate/"
//					+ R.raw.alarm_rooster);
	private long[] mVibratePattern = { 0, 200, 200, 300 };

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("time", "onReceive");
		mNotificationIntent = new Intent(context, WeatherMainActivity.class);
		mContentIntent = PendingIntent.getActivity(context, 0,
				mNotificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);

		Notification.Builder notificationBuilder = new Notification.Builder(
				context).setTicker(tickerText)
				.setSmallIcon(android.R.drawable.stat_sys_warning)
				.setAutoCancel(true).setContentTitle(contentTitle)
				.setContentText(contentText).setContentIntent(mContentIntent)
				.setVibrate(mVibratePattern);
				//.setSound(soundURI);

		// Pass the Notification to the NotificationManager:
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(MY_NOTIFICATION_ID,
				notificationBuilder.build());
		WeatherMainActivity weatherActivity = new WeatherMainActivity();
		
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
			weatherActivity.new RetrieveRainImg().execute(weatherActivity.rainUrl + urlAdd);
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
			weatherActivity.new RetrieveRainImg().execute(weatherActivity.rainUrl + urlAdd);
		}
		
	}
}
