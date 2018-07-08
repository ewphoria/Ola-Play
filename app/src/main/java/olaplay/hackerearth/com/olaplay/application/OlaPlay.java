package olaplay.hackerearth.com.olaplay.application;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by love on 17/12/17.
 */

public class OlaPlay extends Application {

    private static OlaPlay mInstance;

    /**
     * Connection State class to maintain the states of connectivity.
     */
    public class ConnectionState {
        public static final int UNKNOWN = -1;
        public static final int CONNECTED = 1;
        public static final int NOTCONNECTED = 0;
    }

    public int connectionState = -1;

    @Override
    public void onCreate() {
        super.onCreate();

        /**
         * Database initialization
         */
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder().name("myrealm.realm").build();
        Realm.setDefaultConfiguration(config);

        mInstance = this;
    }

    public static synchronized OlaPlay getInstance() {
        return mInstance;
    }



}
