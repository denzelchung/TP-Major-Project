package cen.plus.plus.wecommunity;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by User on 17/6/2014.
 */
public class PrefUtils {
    public static final String PREFS_LOGIN_USERNAME_KEY = "__USERNAME__";
    public static final String PREFS_LOGIN_PASSWORD_KEY = "__PASSWORD__";
    public static final String PREFS_LOGIN_PICTURE_KEY = "__PICTURE__";
    public static final String PREFS_LOGIN_EMAIL_KEY = "__EMAIL__";
    public static final String PREFS_LOGIN_GENDER_KEY = "__GENDER__";
    public static final String PREF_LOGIN_PHONE_KEY = "__PHONE__";
    public static final String PREF_LOGIN_LOCATION_KEY = "__LOCATION__";
    public static final String PREF_LOGIN_JOIN_DATE_KEY = "__JOIN_DATE__";

    public static void saveToPrefs(Context context, String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String getFromPrefs(Context context, String key, String defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return sharedPreferences.getString(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }
}
