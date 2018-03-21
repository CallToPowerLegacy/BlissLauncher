package org.indin.blisslaunchero.ui;

import org.indin.blisslaunchero.data.model.AppItem;

import java.util.List;
import java.util.Map;

/**
 * Created by falcon on 18/3/18.
 */

public interface LauncherView {
    void showApps(List<AppItem> allAppItems, List<AppItem> pinnedAppItems);
    void showNotificationBadges(Map<String, Integer> map);
    void showLoading();
    void hideLoading();
}
