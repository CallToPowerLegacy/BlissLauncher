package org.indin.blisslaunchero.features.launcher;

import com.jakewharton.rxrelay2.BehaviorRelay;

import org.indin.blisslaunchero.framework.database.model.AppItem;

import java.util.LinkedHashMap;

public class AppsRepository {
    private BehaviorRelay<AllAppsList> appsRelay;

    private static AppsRepository sAppsRepository;

    private AppsRepository(){
        appsRelay = BehaviorRelay.create();
    }

    public static AppsRepository getAppsRepository(){
        if(sAppsRepository == null){
            sAppsRepository = new AppsRepository();
        }
        return sAppsRepository;
    }

    public void updateAppsRelay(AllAppsList allAppsList){
        this.appsRelay.accept(allAppsList);
    }

    public BehaviorRelay<AllAppsList> getAppsRelay() {
        return appsRelay;
    }
}

