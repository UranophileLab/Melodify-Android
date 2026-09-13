package dev.melodify.uranophilelab;

import com.google.firebase.analytics.FirebaseAnalytics;

public class ApplicationClass extends BaseApplicationClass {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.USE_FIREBASE) {
            FirebaseAnalytics.getInstance(this);
        }
    }
}