/*
 * Copyright 2018 /e/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.indin.blisslaunchero.framework.database.model;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public class AppItem {

    private String mLabel;

    private String mPackageName;

    private Drawable mIcon;

    private Intent mIntent;

    private String mComponentName;

    private boolean mIsSystemApp;

    private boolean mIsClock;

    private boolean mIsCalendar;

    private boolean isPinnedApp;

    // Folder specific
    private boolean mBelongsToFolder;
    private boolean mIsFolder;
    private String mFolderID;
    private List<AppItem> mSubApps;


    public AppItem(String label, String packageName, Drawable icon,
            Intent intent, String componentName, boolean isSystemApp,
            boolean isClock, boolean isCalendar) {
        this.mLabel = label;
        this.mPackageName = packageName;
        this.mIcon = icon;
        this.mIntent = intent;
        this.mComponentName = componentName;
        this.mIsSystemApp = isSystemApp;
        this.mIsClock = isClock;
        this.mIsCalendar = isCalendar;
    }

    public CharSequence getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        this.mLabel = label;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String packageName) {
        this.mPackageName = packageName;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public boolean isSystemApp() {
        return mIsSystemApp;
    }

    public boolean isClock() {
        return mIsClock;
    }

    public boolean isCalendar() {
        return mIsCalendar;
    }

    public void setIcon(Drawable icon) {
        this.mIcon = icon;
    }

    public Intent getIntent() {
        return mIntent;
    }

    public void setIntent(Intent intent) {
        this.mIntent = intent;
    }

    public String getComponentName() {
        return mComponentName;
    }

    public boolean isFolder() {
        return mIsFolder;
    }

    public void setFolder(boolean folder) {
        mIsFolder = folder;
    }

    public String getFolderID() {
        return mFolderID;
    }

    public void setFolderID(String folderID) {
        this.mFolderID = folderID;
    }

    public List<AppItem> getSubApps() {
        if (mSubApps == null) {
            mSubApps = new ArrayList<>();
        }
        return mSubApps;
    }

    public void setSubApps(List<AppItem> subApps) {
        this.mSubApps = subApps;
    }

    public boolean isBelongsToFolder() {
        return mBelongsToFolder;
    }

    public void setBelongsToFolder(boolean belongsToFolder) {
        this.mBelongsToFolder = belongsToFolder;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof AppItem){
            return ((AppItem)obj).mPackageName.equals(this.mPackageName);
        }else return false;
    }

    public boolean isPinnedApp() {
        return isPinnedApp;
    }

    public void setPinnedApp(boolean pinnedApp) {
        isPinnedApp = pinnedApp;
    }
}
