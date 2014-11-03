package cen.plus.plus.wecommunity;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.appspot.cenplusplus.cenplusplus.Cenplusplus;
import javax.annotation.Nullable;

public class AppConstants {

    /**
     * Class instance of the JSON factory.
     */
    public static final JsonFactory JSON_FACTORY = new AndroidJsonFactory();

    /**
     * Class instance of the HTTP transport.
     */
    public static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();


    /**
     * Retrieve a Helloworld api service handle to access the API.
     */
    public static Cenplusplus getApiServiceHandle() {
        // Use a builder to help formulate the API request.
        Cenplusplus.Builder helloWorld = new Cenplusplus.Builder(AppConstants.HTTP_TRANSPORT,
                AppConstants.JSON_FACTORY,null);

        return helloWorld.build();
    }

}