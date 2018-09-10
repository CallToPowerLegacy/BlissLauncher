package org.indin.blisslaunchero.features.launcher;

import com.jakewharton.rxrelay2.BehaviorRelay;

import android.util.Log;

public class AppsRepository {

    private static final String TAG = "AppsRepository";
    private BehaviorRelay<AllAppsList> appsRelay;

    private static AppsRepository sAppsRepository;

    private AppsRepository() {
        appsRelay = BehaviorRelay.create();
    }

    public static AppsRepository getAppsRepository() {
        if (sAppsRepository == null) {
            Log.d(TAG, "getAppsRepository() called");
            sAppsRepository = new AppsRepository();
        }
        return sAppsRepository;
    }

    public void updateAppsRelay(AllAppsList allAppsList) {
        this.appsRelay.accept(allAppsList);
    }

    public BehaviorRelay<AllAppsList> getAppsRelay() {
        return appsRelay;
    }
}
