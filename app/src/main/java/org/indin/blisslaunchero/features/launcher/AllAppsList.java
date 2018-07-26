package org.indin.blisslaunchero.features.launcher;

import org.indin.blisslaunchero.framework.database.model.AppItem;

import java.util.LinkedHashMap;
import java.util.List;

public class AllAppsList {

    public LinkedHashMap<String,AppItem> launchableApps;
    public List<String> defaultPinnedAppsPackages;

    public AllAppsList(LinkedHashMap<String, AppItem> launchableApps, List<String> defaultPinnedAppsPackages){
        this.launchableApps = launchableApps;
        this.defaultPinnedAppsPackages = defaultPinnedAppsPackages;
    }
}
