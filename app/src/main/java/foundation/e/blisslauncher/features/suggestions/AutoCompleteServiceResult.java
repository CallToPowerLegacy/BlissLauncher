package foundation.e.blisslauncher.features.suggestions;

import java.util.List;

import foundation.e.blisslauncher.core.database.model.LauncherItem;

public class AutoCompleteServiceResult {

    public static final int TYPE_LAUNCHER_ITEM = 567;
    public static final int TYPE_NETWORK_ITEM = 568;

    public List<AutoCompleteServiceRawResult> networkItems;
    private List<LauncherItem> launcherItems;
    public String queryText;
    public int type = -1;


    public AutoCompleteServiceResult(String queryText) {
        this.queryText = queryText;
    }

    public List<AutoCompleteServiceRawResult> getNetworkItems() {
        return networkItems;
    }

    public void setNetworkItems(
            List<AutoCompleteServiceRawResult> networkItems) {
        this.networkItems = networkItems;
        this.type = TYPE_NETWORK_ITEM;
    }

    public List<LauncherItem> getLauncherItems() {
        return launcherItems;
    }

    public void setLauncherItems(
            List<LauncherItem> launcherItems) {
        this.launcherItems = launcherItems;
        this.type = TYPE_LAUNCHER_ITEM;
    }
}
