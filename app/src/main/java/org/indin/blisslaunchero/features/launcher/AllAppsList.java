package org.indin.blisslaunchero.features.launcher;

import org.indin.blisslaunchero.data.model.AppItem;

import java.util.ArrayList;
import java.util.List;

public class AllAppsList {

    public List<AppItem> launchableApps;
    public List<AppItem> pinnedApps;

    public AllAppsList(List<AppItem> launchableApps, List<AppItem> pinnedApps){
        this.launchableApps = launchableApps;
        this.pinnedApps = pinnedApps;
    }
}
