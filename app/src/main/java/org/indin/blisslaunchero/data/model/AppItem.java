package org.indin.blisslaunchero.data.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;


@Entity(tableName = "apps", indices = {@Index(value = "package_name", unique = true)})
public class AppItem {

    @PrimaryKey
    private int uid;

    @ColumnInfo(name = "label")
    private String mLabel;

    @ColumnInfo(name = "package_name")
    private String mPackageName;

    @ColumnInfo(name = "icon")
    private byte[] mIconArray;

    private Drawable mIcon;

    @ColumnInfo(name = "intent_string")
    private String mIntentString;

    private Intent mIntent;

    @ColumnInfo(name = "component_name")
    private String mComponentName;

    @ColumnInfo(name = "is_system_app")
    private boolean mIsSystemApp;

    @ColumnInfo(name = "is_clock_app")
    private boolean mIsClock;

    @ColumnInfo(name = "is_calendar_app")
    private boolean mIsCalendar;

    @ColumnInfo(name = "is_pinned_app")
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

    public void setSystemApp(boolean isSystemApp) {
        this.mIsSystemApp = isSystemApp;
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

    public void setComponentName(String componentName) {
        this.mComponentName = componentName;
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
}
