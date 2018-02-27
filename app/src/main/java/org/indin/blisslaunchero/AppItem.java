package org.indin.blisslaunchero;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

public class AppItem {
    private CharSequence label;
    private String packageName;
    private Drawable icon;
    private Intent intent;
    private String componentName;
    private boolean iconFromIconPack;
    private boolean isSystemApp;

    // Folder specific
    private boolean belongsToFolder;
    private boolean isFolder;
    private String folderID;
    private List<AppItem> subApps;


    public AppItem(CharSequence label, String packageName, Drawable icon,
                   Intent intent, String componentName, boolean iconFromIconPack, boolean isSystemApp) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
        this.intent = intent;
        this.componentName = componentName;
        this.iconFromIconPack = iconFromIconPack;
        this.isSystemApp = isSystemApp;
    }

    public CharSequence getLabel() {
        return label;
    }

    public void setLabel(CharSequence label) {
        this.label = label;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isSystemApp(){
        return isSystemApp;
    }

    public void setSystemApp(boolean isSystemApp){
        this.isSystemApp = isSystemApp;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public boolean isIconFromIconPack() {
        return iconFromIconPack;
    }

    public void setIconFromIconPack(boolean iconFromIconPack) {
        this.iconFromIconPack = iconFromIconPack;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public String getFolderID() {
        return folderID;
    }

    public void setFolderID(String folderID) {
        this.folderID = folderID;
    }

    public List<AppItem> getSubApps() {
        if(subApps == null)
            subApps = new ArrayList<>();
        return subApps;
    }

    public void setSubApps(List<AppItem> subApps) {
        this.subApps = subApps;
    }

    public boolean isBelongsToFolder() {
        return belongsToFolder;
    }

    public void setBelongsToFolder(boolean belongsToFolder) {
        this.belongsToFolder = belongsToFolder;
    }
}
