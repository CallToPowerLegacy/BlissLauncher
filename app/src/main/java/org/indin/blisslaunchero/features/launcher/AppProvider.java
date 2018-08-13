package org.indin.blisslaunchero.features.launcher;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.UserManager;
import android.support.annotation.Nullable;
import android.util.Log;

import org.indin.blisslaunchero.framework.broadcast.PackageAddedRemovedHandler;
import org.indin.blisslaunchero.framework.utils.UserHandle;

public class AppProvider extends Service implements Provider {

    private AllAppsList mAllAppsList;

    private boolean appsLoaded = false;

    private long appLoadingStart;

    private AppsRepository mAppsRepository;

    public static final String MICROG_PACKAGE = "com.google.android.gms";
    public static final String MUPDF_PACKAGE = "com.artifex.mupdf.mini.app";

    private IBinder mBinder = new LocalBinder();

    private static final String TAG = "AppProvider";

    public AppProvider() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");
        final UserManager manager = (UserManager) this.getSystemService(Context.USER_SERVICE);
        assert manager != null;

        final LauncherApps launcher = (LauncherApps) this.getSystemService(
                Context.LAUNCHER_APPS_SERVICE);
        assert launcher != null;

        launcher.registerCallback(new LauncherApps.Callback() {
            @Override
            public void onPackageRemoved(String packageName, android.os.UserHandle user) {
                Log.d(TAG, "onPackageRemoved() called with: packageName = [" + packageName
                        + "], user = [" + user + "]");

                if(packageName.equalsIgnoreCase(MICROG_PACKAGE)|| packageName.equalsIgnoreCase(MUPDF_PACKAGE)){
                    return;
                }

                PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                        "android.intent.action.PACKAGE_REMOVED",
                        packageName, new UserHandle(manager.getSerialNumberForUser(user), user),
                        false
                );
            }

            @Override
            public void onPackageAdded(String packageName, android.os.UserHandle user) {
                Log.d(TAG, "onPackageAdded() called with: packageName = [" + packageName
                        + "], user = [" + user + "]");

                if(packageName.equalsIgnoreCase(MICROG_PACKAGE)|| packageName.equalsIgnoreCase(MUPDF_PACKAGE)){
                    return;
                }

                PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                        "android.intent.action.PACKAGE_ADDED",
                        packageName, new UserHandle(manager.getSerialNumberForUser(user), user),
                        false
                );
            }

            @Override
            public void onPackageChanged(String packageName, android.os.UserHandle user) {
                Log.d(TAG, "onPackageChanged() called with: packageName = [" + packageName
                        + "], user = [" + user + "]");

                if(packageName.equalsIgnoreCase(MICROG_PACKAGE)|| packageName.equalsIgnoreCase(MUPDF_PACKAGE)){
                    return;
                }

                PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                        "android.intent.action.PACKAGE_CHANGED",
                        packageName, new UserHandle(manager.getSerialNumberForUser(user), user),
                        true
                );
            }

            @Override
            public void onPackagesAvailable(String[] packageNames, android.os.UserHandle user,
                    boolean replacing) {
                PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                        "android.intent.action.MEDIA_MOUNTED",
                        null, new UserHandle(manager.getSerialNumberForUser(user), user), false
                );

            }

            @Override
            public void onPackagesUnavailable(String[] packageNames, android.os.UserHandle user,
                    boolean replacing) {
                PackageAddedRemovedHandler.handleEvent(AppProvider.this,
                        "android.intent.action.MEDIA_UNMOUNTED",
                        null, new UserHandle(manager.getSerialNumberForUser(user), user), false
                );
            }
        });

        // Get notified when app changes on standard user profile
        IntentFilter appChangedFilter = new IntentFilter();
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        appChangedFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        appChangedFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        appChangedFilter.addDataScheme("package");
        appChangedFilter.addDataScheme("file");
        this.registerReceiver(new PackageAddedRemovedHandler(), appChangedFilter);

        mAppsRepository = AppsRepository.getAppsRepository();
        super.onCreate();
        reload();
    }

    @Override
    public void reload() {
        initializeAppLoading(new LoadAppsTask(this));
    }

    @Override
    public boolean isAppsLoaded() {
        return appsLoaded;
    }

    private void initializeAppLoading(LoadAppsTask loader) {
        appLoadingStart = System.currentTimeMillis();

        Log.i(TAG, "Starting app provider: " + this.getClass().getSimpleName());
        loader.setAppProvider(this);
        loader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void loadAppsOver(AllAppsList allAppsList) {
        long time = System.currentTimeMillis() - appLoadingStart;
        Log.i(TAG, "Time to load " + this.getClass().getSimpleName() + ": " + time + "ms");
        this.mAllAppsList = allAppsList;
        appsLoaded = true;
        handleAllProviderLoaded();

    }

    private synchronized void handleAllProviderLoaded() {
        if(appsLoaded) {
           mAppsRepository.updateAppsRelay(mAllAppsList);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags
                + "], startId = [" + startId + "]");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public AppProvider getService(){
            return AppProvider.this;
        }
    }
}
