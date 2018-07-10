package org.indin.blisslaunchero.features.launcher;

import android.content.Context;

import org.indin.blisslaunchero.framework.database.model.AppItem;
import org.indin.blisslaunchero.framework.mvp.MvpContract;

import java.util.List;

public interface Provider {

    void reload();

    boolean isAppsLoaded();
}
