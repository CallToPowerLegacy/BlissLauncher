package org.indin.blisslaunchero.features.launcher;

import android.content.Context;

import org.indin.blisslaunchero.data.model.AppItem;
import org.indin.blisslaunchero.framework.mvp.MvpContract;

import java.util.List;

public interface LauncherContract {

    interface View extends MvpContract.View {

        void showApps(List<AppItem> appItemList, List<AppItem> pinnedApps);
    }

    interface Presenter extends MvpContract.Presenter<View> {

        void loadApps(Context context);
    }
}
