package org.indin.blisslaunchero.data.model;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;


public class AppItem {

    private String mLabel;
    private String mPackageName;
    private Drawable mIcon;
    private Intent mIntent;
    private String mComponentName;
    private boolean mIconFromIconPack;
    private boolean mIsSystemApp;
    private boolean mIsClock;
    private boolean mIsCalendar;
    private boolean isPinnedApp;
    private boolean isAdaptive;

    // Folder specific
    private boolean mBelongsToFolder;
    private boolean mIsFolder;
    private String mFolderID;
    private List<AppItem> mSubApps;


    public AppItem(String label, String packageName, Drawable icon,
            Intent intent, String componentName, boolean iconFromIconPack, boolean isSystemApp,
            boolean isClock, boolean isCalendar, boolean adaptive) {
        this.mLabel = label;
        this.mPackageName = packageName;
        this.mIcon = icon;
        this.mIntent = intent;
        this.mComponentName = componentName;
        this.mIconFromIconPack = iconFromIconPack;
        this.mIsSystemApp = isSystemApp;
        this.mIsClock = isClock;
        this.mIsCalendar = isCalendar;
        this.isAdaptive = adaptive;
    }

    public boolean isAdaptive(){
        return isAdaptive;
    }

    public void setAdaptive(boolean adaptive){
        this.isAdaptive = adaptive;
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

    public boolean isIconFromIconPack() {
        return mIconFromIconPack;
    }

    public void setIconFromIconPack(boolean iconFromIconPack) {
        this.mIconFromIconPack = iconFromIconPack;
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
