package org.indin.blisslaunchero.features.launcher;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static lineageos.providers.WeatherContract.WeatherColumns.TempUnit.CELSIUS;
import static lineageos.providers.WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT;
import static lineageos.providers.WeatherContract.WeatherColumns.WindSpeedUnit.KPH;
import static lineageos.providers.WeatherContract.WeatherColumns.WindSpeedUnit.MPH;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.usage.UsageStats;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.widget.RxTextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.indin.blisslaunchero.BlissLauncher;
import org.indin.blisslaunchero.BuildConfig;
import org.indin.blisslaunchero.R;
import org.indin.blisslaunchero.features.notification.NotificationRepository;
import org.indin.blisslaunchero.features.notification.NotificationService;
import org.indin.blisslaunchero.features.suggestions.AutoCompleteAdapter;
import org.indin.blisslaunchero.features.suggestions.AutoCompleteService;
import org.indin.blisslaunchero.features.suggestions.AutoCompleteServiceResult;
import org.indin.blisslaunchero.features.usagestats.AppUsageStats;
import org.indin.blisslaunchero.features.weather.DeviceStatusService;
import org.indin.blisslaunchero.features.weather.ForecastBuilder;
import org.indin.blisslaunchero.features.weather.WeatherIconUtils;
import org.indin.blisslaunchero.features.weather.WeatherPreferences;
import org.indin.blisslaunchero.features.weather.WeatherSourceListenerService;
import org.indin.blisslaunchero.features.weather.WeatherUpdateService;
import org.indin.blisslaunchero.framework.Alarm;
import org.indin.blisslaunchero.framework.DeviceProfile;
import org.indin.blisslaunchero.framework.Preferences;
import org.indin.blisslaunchero.framework.Utilities;
import org.indin.blisslaunchero.framework.customviews.BlissDragShadowBuilder;
import org.indin.blisslaunchero.framework.customviews.BlissFrameLayout;
import org.indin.blisslaunchero.framework.customviews.BlissInput;
import org.indin.blisslaunchero.framework.customviews.CustomAnalogClock;
import org.indin.blisslaunchero.framework.customviews.HorizontalPager;
import org.indin.blisslaunchero.framework.customviews.SquareFrameLayout;
import org.indin.blisslaunchero.framework.customviews.SquareImageView;
import org.indin.blisslaunchero.framework.database.Storage;
import org.indin.blisslaunchero.framework.database.model.AppItem;
import org.indin.blisslaunchero.framework.database.model.CalendarIcon;
import org.indin.blisslaunchero.framework.events.AppAddEvent;
import org.indin.blisslaunchero.framework.events.AppChangeEvent;
import org.indin.blisslaunchero.framework.events.AppRemoveEvent;
import org.indin.blisslaunchero.framework.network.RetrofitService;
import org.indin.blisslaunchero.framework.utils.AppUtils;
import org.indin.blisslaunchero.framework.utils.GraphicsUtil;
import org.indin.blisslaunchero.framework.utils.ListUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import lineageos.weather.WeatherInfo;
import lineageos.weather.util.WeatherUtils;
import me.relex.circleindicator.CircleIndicator;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class LauncherActivity extends AppCompatActivity implements
        AutoCompleteAdapter.OnSuggestionClickListener {

    public static final int REORDER_TIMEOUT = 350;
    private final static int INVALID = -999;
    public static boolean longPressed;
    private final Alarm mReorderAlarm = new Alarm();
    private final Alarm mDockReorderAlarm = new Alarm();
    private HorizontalPager mHorizontalPager;
    private DockGridLayout mDock;
    private PageIndicatorLinearLayout mIndicator;
    private ViewGroup mFolderWindowContainer;
    private ViewPager mFolderAppsViewPager;
    private BlissInput mFolderTitleInput;
    private BlissInput mSearchInput;
    private View mProgressBar;
    private List<AppItem> launchableApps = new ArrayList<>();
    private List<AppItem> pinnedApps = new ArrayList<>();
    private AllAppsList allLoadedApps;
    private int currentPageNumber = 0;
    private float maxDistanceForFolderCreation;
    private List<GridLayout> pages;
    private boolean dragDropEnabled = true;
    private BlissFrameLayout movingApp;
    private BlissFrameLayout collidingApp;
    private boolean folderInterest;
    private Animation wobbleAnimation;
    private Animation wobbleReverseAnimation;
    private int scrollCorner;
    private Storage storage;
    private int parentPage = -99;
    private boolean folderFromDock;
    private boolean isWobbling = false;
    private CompositeDisposable mCompositeDisposable;
    private CountDownTimer mWobblingCountDownTimer;
    private List<CalendarIcon> mCalendarIcons = new ArrayList<>();
    private BroadcastReceiver timeChangedReceiver;
    private boolean isUiDone = false;
    private Set<String> mAppsWithNotifications = new HashSet<>();

    private View mLauncherView;
    private DeviceProfile mDeviceProfile;
    private boolean mLongClickStartsDrag = true;
    private boolean isDragging;
    private AutoCompleteAdapter mSuggestionAdapter;
    private GridLayout suggestedAppsGridLayout;
    private BlissDragShadowBuilder dragShadowBuilder;
    private View mWeatherPanel;
    private View mWeatherSetupTextView;
    private boolean allAppsDisplayed;

    private List<UsageStats> mUsageStats;

    private BroadcastReceiver mWeatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getBooleanExtra(WeatherUpdateService.EXTRA_UPDATE_CANCELLED, false)) {
                updateWeatherPanel();
            }
        }
    };
    private AppItem activeFolder;
    private BlissFrameLayout activeFolderView;
    private int activeDot;
    private int statusBarHeight;

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BlissLauncher.getApplication(this).initAppProvider();

        prepareBroadcastReceivers();

        storage = new Storage(getApplicationContext());

        mDeviceProfile = BlissLauncher.getApplication(this).getDeviceProfile();

        mLauncherView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        setContentView(mLauncherView);
        setupViews();

        mProgressBar.setVisibility(View.VISIBLE);

        ContentResolver cr = getContentResolver();
        String setting = "enabled_notification_listeners";
        String permissionString = Settings.Secure.getString(cr, setting);
        if (permissionString == null || !permissionString.contains(getPackageName())) {
            if (BuildConfig.DEBUG) {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            } else if (!Preferences.getNotificationAccess(this)) {
                ComponentName cn = new ComponentName(this, NotificationService.class);
                if (permissionString == null) {
                    permissionString = "";
                } else {
                    permissionString += ":";
                }
                permissionString += cn.flattenToString();
                boolean success = Settings.Secure.putString(cr, setting, permissionString);
                if (success) {
                    Preferences.setNotificationAccess(this);
                }
            }
        }
        // Start NotificationService to add count badge to Icons
        Intent notificationServiceIntent = new Intent(this, NotificationService.class);
        startService(notificationServiceIntent);

        createOrUpdateIconGrid();
    }

    private void setupViews() {
        mHorizontalPager = mLauncherView.findViewById(R.id.pages_container);
        statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        mDock = mLauncherView.findViewById(R.id.dock);
        mIndicator = mLauncherView.findViewById(R.id.page_indicator);
        mFolderWindowContainer = mLauncherView.findViewById(
                R.id.folder_window_container);
        mFolderAppsViewPager = mLauncherView.findViewById(R.id.folder_apps);
        mFolderTitleInput = mLauncherView.findViewById(R.id.folder_title);
        mProgressBar = mLauncherView.findViewById(R.id.progressbar);

        maxDistanceForFolderCreation = (int) (0.45f * mDeviceProfile.iconSizePx);

        scrollCorner = mDeviceProfile.iconDrawablePaddingPx / 2;
        wobbleAnimation = AnimationUtils.loadAnimation(this, R.anim.wobble);
        wobbleReverseAnimation = AnimationUtils.loadAnimation(this, R.anim.wobble_reverse);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void createOrUpdateIconGrid() {
        getCompositeDisposable().add(
                AppsRepository.getAppsRepository().getAppsRelay().subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableObserver<AllAppsList>() {
                            @Override
                            public void onNext(
                                    AllAppsList allAppsList) {
                                if (allAppsList.launchableApps.size() <= 0) {
                                    BlissLauncher.getApplication(
                                            LauncherActivity.this).getAppProvider().reload();
                                } else {
                                    allLoadedApps = allAppsList;
                                    if (!allAppsDisplayed) {
                                        showApps();
                                    }
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                            }

                            @Override
                            public void onComplete() {

                            }
                        })
        );
    }

    private void prepareBroadcastReceivers() {
        IntentFilter timeIntentFilter = new IntentFilter(Intent.ACTION_TIME_CHANGED);
        timeIntentFilter.addAction(Intent.ACTION_DATE_CHANGED);
        timeChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isUiDone) {
                    updateAllCalendarIcons(Calendar.getInstance());
                }
            }
        };
        registerReceiver(timeChangedReceiver, timeIntentFilter);
        EventBus.getDefault().register(this);
    }

    protected void attachBaseContext(Context context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(context));
    }

    public CompositeDisposable getCompositeDisposable() {
        if (mCompositeDisposable == null || mCompositeDisposable.isDisposed()) {
            mCompositeDisposable = new CompositeDisposable();
        }
        return mCompositeDisposable;
    }

    private void updateAllCalendarIcons(Calendar calendar) {
        for (CalendarIcon calendarIcon : mCalendarIcons) {
            updateCalendarIcon(calendarIcon, calendar);
        }
    }

    private void updateCalendarIcon(CalendarIcon calendarIcon, Calendar calendar) {
        calendarIcon.monthTextView.setText(
                Utilities.convertMonthToString(calendar.get(Calendar.MONTH)));
        calendarIcon.dayTextView.setText(
                String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWeatherPanel != null) {
            updateWeatherPanel();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unregisterReceiver(timeChangedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mWeatherReceiver);
        getCompositeDisposable().dispose();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAppAddEvent(AppAddEvent appAddEvent) {
        addNewApp(appAddEvent.packageName);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAppRemoveEvent(AppRemoveEvent appRemoveEvent) {
        removePackageFromLauncher(appRemoveEvent.packageName);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAppChangeEvent(AppChangeEvent appChangeEvent) {
        updateApp(appChangeEvent.packageName);
    }

    private void addNewApp(String packageName) {
        if (pages == null || pages.size() == 0) {
            return;
        }

        // Don't add an app that's already present on any of the pages
        for (int i = 0; i < pages.size(); i++) {
            GridLayout grid = getGridFromPage(pages.get(i));
            for (int j = 0; j < grid.getChildCount(); j++) {
                if (getAppDetails(grid.getChildAt(j)).getPackageName().equals(packageName)) {
                    return;
                }
            }
        }
        // Don't add an app that's already present in the mDock
        for (int i = 0; i < mDock.getChildCount(); i++) {
            if (getAppDetails(mDock.getChildAt(i)).getPackageName().equals(packageName)) {
                return;
            }
        }

        AppItem appItem = AppUtils.createAppItem(this, packageName);
        if (appItem != null) {
            BlissFrameLayout view = prepareApp(appItem, true);
            int current = getCurrentAppsPageNumber();
            while (current == -1 || (current < pages.size() && pages.get(current).getChildCount()
                    == mDeviceProfile.maxAppsPerPage)) {
                current++;
            }

            if (current == pages.size()) {
                pages.add(preparePage());
                ImageView dot = new ImageView(LauncherActivity.this);
                dot.setImageDrawable(getDrawable(R.drawable.dot_off));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        getResources().getDimensionPixelSize(R.dimen.dotSize),
                        getResources().getDimensionPixelSize(R.dimen.dotSize)
                );
                dot.setLayoutParams(params);
                mIndicator.addView(dot);
                mHorizontalPager.addView(pages.get(current));
            }
            addAppToGrid(pages.get(current), view);
            storage.save(pages, mDock);
            //BlissDb.getBlissDatabase(this).mStoreDao().insert(storeAppItem);
        }
    }

    private void removePackageFromLauncher(String packageName) {
        handleWobbling(false);
        if (mFolderWindowContainer.getVisibility() == View.VISIBLE) {
            for (int i = 0; i < mFolderAppsViewPager.getChildCount(); i++) {
                GridLayout grid = (GridLayout) mFolderAppsViewPager.getChildAt(i);
                for (int j = 0; j < grid.getChildCount(); j++) {
                    AppItem app = getAppDetails(grid.getChildAt(j));
                    if (app.getPackageName().equals(packageName)) {
                        activeFolder.getSubApps().remove(app);
                        launchableApps.remove(app);
                        mFolderAppsViewPager.getAdapter().notifyDataSetChanged();

                        if (activeFolder.getSubApps().size() == 0) {
                            BlissFrameLayout view = prepareApp(app, !folderFromDock);
                            if (folderFromDock) {
                                addAppToDock(view, mDock.indexOfChild(activeFolderView));
                            } else {
                                GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                                addAppToGrid(gridLayout, view,
                                        gridLayout.indexOfChild(activeFolderView));
                            }
                            ((ViewGroup) activeFolderView.getParent()).removeView(activeFolderView);
                            hideFolderWindowContainer();
                        } else {
                            if (activeFolder.getSubApps().size() == 1) {
                                AppItem item = activeFolder.getSubApps().get(0);
                                activeFolder.getSubApps().remove(item);
                                mFolderAppsViewPager.getAdapter().notifyDataSetChanged();
                                item.setBelongsToFolder(false);
                                BlissFrameLayout view = prepareApp(item, !folderFromDock);

                                if (folderFromDock) {
                                    addAppToDock(view, mDock.indexOfChild(activeFolderView));
                                } else {
                                    GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                                    addAppToGrid(gridLayout, view,
                                            gridLayout.indexOfChild(activeFolderView));
                                }

                                ((ViewGroup) activeFolderView.getParent()).removeView(
                                        activeFolderView);
                                hideFolderWindowContainer();
                            } else {
                                updateIcon(activeFolderView, activeFolder,
                                        new GraphicsUtil(this).generateFolderIcon(this,
                                                activeFolder),
                                        folderFromDock);
                                hideFolderWindowContainer();
                            }
                        }

                    }
                }
            }
        }

        for (int j = 0; j < mDock.getChildCount(); j++) {
            AppItem appItem = getAppDetails(mDock.getChildAt(j));
            if (appItem.isFolder()) {
                for (AppItem folderItem : appItem.getSubApps()) {
                    if (folderItem.getPackageName().equalsIgnoreCase(packageName)) {
                        appItem.getSubApps().remove(folderItem);
                        if (appItem.getSubApps().size() == 0) {
                            mDock.removeViewAt(j);
                            return;
                        }
                        appItem.setIcon(
                                new GraphicsUtil(this).generateFolderIcon(this, appItem));
                        BlissFrameLayout blissFrameLayout = prepareApp(appItem, false);
                        mDock.removeViewAt(j);
                        addAppToDock(blissFrameLayout, j);
                        return;
                    }
                }
            } else if (appItem.getPackageName().equalsIgnoreCase(packageName)) {
                mDock.removeViewAt(j);
                launchableApps.remove(appItem);
                return;
            }
        }


        for (int i = 0; i < pages.size(); i++) {
            GridLayout grid = getGridFromPage(pages.get(i));
            for (int j = 0; j < grid.getChildCount(); j++) {
                AppItem appItem = getAppDetails(grid.getChildAt(j));
                if (appItem.isFolder()) {
                    for (AppItem folderItem : appItem.getSubApps()) {
                        if (folderItem.getPackageName().equalsIgnoreCase(packageName)) {
                            appItem.getSubApps().remove(folderItem);
                            if (appItem.getSubApps().size() == 0) {
                                grid.removeViewAt(j);
                                return;
                            }
                            appItem.setIcon(
                                    new GraphicsUtil(this).generateFolderIcon(this, appItem));
                            BlissFrameLayout blissFrameLayout = prepareApp(appItem, false);
                            grid.removeViewAt(j);
                            addAppToGrid(grid, blissFrameLayout, j);
                            return;
                        }
                    }
                } else if (appItem.getPackageName().equalsIgnoreCase(packageName)) {
                    grid.removeViewAt(j);
                    launchableApps.remove(appItem);
                    return;
                }
            }
        }
    }

    private void updateApp(String packageName) {
        handleWobbling(false);
        AppItem updatedAppItem = AppUtils.createAppItem(this, packageName);
        if (updatedAppItem == null) {
            removePackageFromLauncher(packageName);
            return;
        }

        if (mFolderWindowContainer.getVisibility() == View.VISIBLE) {
            for (int i = 0; i < mFolderAppsViewPager.getChildCount(); i++) {
                GridLayout gridLayout = (GridLayout) mFolderAppsViewPager.getChildAt(i);
                for (int j = 0; j < gridLayout.getChildCount(); j++) {
                    BlissFrameLayout viewGroup =
                            (BlissFrameLayout) gridLayout.getChildAt(j);
                    final AppItem existingAppItem = getAppDetails(viewGroup);
                    if (existingAppItem.getPackageName().equalsIgnoreCase(packageName)) {
                        updatedAppItem.setPinnedApp(existingAppItem.isPinnedApp());
                        BlissFrameLayout blissFrameLayout = prepareApp(updatedAppItem, true);
                        GridLayout.LayoutParams iconLayoutParams = new GridLayout.LayoutParams();
                        iconLayoutParams.height = mDeviceProfile.cellHeightPx;
                        iconLayoutParams.width = mDeviceProfile.cellWidthPx;
                        gridLayout.removeViewAt(j);
                        gridLayout.addView(blissFrameLayout, j, iconLayoutParams);
                        return;
                    }
                }
            }
        }

        for (int i = 0; i < mDock.getChildCount(); i++) {
            BlissFrameLayout viewGroup =
                    (BlissFrameLayout) mDock.getChildAt(i);
            AppItem existingAppItem = getAppDetails(viewGroup);
            if (existingAppItem.isFolder()) {
                for (int k = 0; k < existingAppItem.getSubApps().size(); k++) {
                    if (existingAppItem.getSubApps().get(k).getPackageName().equalsIgnoreCase(
                            packageName)) {
                        existingAppItem.getSubApps().set(k, updatedAppItem);
                        existingAppItem.setIcon(
                                new GraphicsUtil(this).generateFolderIcon(this, existingAppItem));
                        BlissFrameLayout blissFrameLayout = prepareApp(existingAppItem, false);
                        mDock.removeViewAt(i);
                        addAppToDock(blissFrameLayout, i);
                        return;
                    }
                }
            } else {
                if (existingAppItem.getPackageName().equalsIgnoreCase(packageName)) {
                    BlissFrameLayout blissFrameLayout = prepareApp(updatedAppItem, false);
                    mDock.removeViewAt(i);
                    addAppToDock(blissFrameLayout, i);
                    return;
                }
            }
        }

        for (int i = 0; i < pages.size(); i++) {
            GridLayout gridLayout = pages.get(i);
            for (int j = 0; j < gridLayout.getChildCount(); j++) {
                BlissFrameLayout viewGroup =
                        (BlissFrameLayout) gridLayout.getChildAt(j);
                AppItem existingAppItem = getAppDetails(viewGroup);
                if (existingAppItem.isFolder()) {
                    for (int k = 0; k < existingAppItem.getSubApps().size(); k++) {
                        if (existingAppItem.getSubApps().get(k).getPackageName().equalsIgnoreCase(
                                packageName)) {
                            existingAppItem.getSubApps().set(k, updatedAppItem);
                            existingAppItem.setIcon(
                                    new GraphicsUtil(this).generateFolderIcon(this,
                                            existingAppItem));
                            BlissFrameLayout blissFrameLayout = prepareApp(existingAppItem, true);
                            gridLayout.removeViewAt(j);
                            addAppToGrid(gridLayout, blissFrameLayout, j);
                            return;

                        }
                    }
                } else {
                    if (existingAppItem.getPackageName().equalsIgnoreCase(packageName)) {
                        BlissFrameLayout blissFrameLayout = prepareApp(updatedAppItem, true);
                        gridLayout.removeViewAt(j);
                        addAppToGrid(gridLayout, blissFrameLayout, j);
                        return;
                    }
                }
            }
        }
    }

    public void showApps() {
        mProgressBar.setVisibility(GONE);
        createUI();
        isUiDone = true;
        createPageChangeListener();
        createFolderTitleListener();
        createDragListener();
        createWidgetsPage();
        createIndicator();
        createOrUpdateBadgeCount();
        allAppsDisplayed = true;
    }

    private void createOrUpdateBadgeCount() {
        getCompositeDisposable().add(
                NotificationRepository.getNotificationRepository().getNotifications().subscribeWith(
                        new DisposableObserver<Set<String>>() {
                            @Override
                            public void onNext(Set<String> packages) {
                                mAppsWithNotifications = packages;
                                updateBadges(mAppsWithNotifications);
                            }

                            @Override
                            public void onError(Throwable e) {
                                Toast.makeText(LauncherActivity.this, "Recreating Launcher",
                                        Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                                recreate();
                            }

                            @Override
                            public void onComplete() {
                            }
                        }));
    }

    private void updateBadges(Set<String> appsWithNotifications) {
        if (mFolderWindowContainer.getVisibility() == View.VISIBLE) {
            for (int i = 0; i < mFolderAppsViewPager.getChildCount(); i++) {
                GridLayout gridLayout = (GridLayout) mFolderAppsViewPager.getChildAt(i);
                for (int j = 0; j < gridLayout.getChildCount(); j++) {
                    BlissFrameLayout viewGroup =
                            (BlissFrameLayout) gridLayout.getChildAt(j);
                    final AppItem appItem = getAppDetails(viewGroup);
                    updateBadgeToApp(viewGroup, appItem, appsWithNotifications, true);
                }
            }
        }
        for (int i = 0; i < pages.size(); i++) {
            GridLayout gridLayout = pages.get(i);
            for (int j = 0; j < gridLayout.getChildCount(); j++) {
                BlissFrameLayout viewGroup =
                        (BlissFrameLayout) gridLayout.getChildAt(j);
                final AppItem appItem = getAppDetails(viewGroup);
                updateBadgeToApp(viewGroup, appItem, appsWithNotifications, true);
            }
        }

        for (int i = 0; i < mDock.getChildCount(); i++) {
            BlissFrameLayout viewGroup =
                    (BlissFrameLayout) mDock.getChildAt(i);
            final AppItem appItem = getAppDetails(viewGroup);
            updateBadgeToApp(viewGroup, appItem, appsWithNotifications, false);
        }
    }

    private void updateBadgeToApp(BlissFrameLayout viewGroup, AppItem appItem,
            Set<String> appsWithNotifications, boolean withText) {
        if (appItem != null) {
            if (appItem.isFolder()) {
                viewGroup.applyBadge(checkHasApp(appItem, appsWithNotifications), withText);
            } else {
                String pkgName = appItem.getPackageName();
                viewGroup.applyBadge(appsWithNotifications.contains(pkgName), withText);
            }
        }
    }

    private boolean checkHasApp(AppItem appItem, Set<String> packages) {
        for (AppItem item : appItem.getSubApps()) {
            if (packages.contains(item.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a listener to the folder title.
     * When clicked, the TextView transforms into an EditText.
     * When the user changes the title and presses the DONE button,
     * the EditText becomes a TextView again.
     * This logic was necessary because on API 24, permanently having
     * an EditText in the layout was breaking the drag/drop functionality.
     */
    private void createFolderTitleListener() {
        mFolderTitleInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v);
            }
        });
        mFolderTitleInput.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                updateFolderTitle();
            }
            return false;
        });
        mFolderTitleInput.setOnClickListener(view -> mFolderTitleInput.setCursorVisible(true));
        mFolderWindowContainer.setOnClickListener(view -> hideFolderWindowContainer());
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(
                Activity.INPUT_METHOD_SERVICE);
        assert inputMethodManager != null;
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void updateFolderTitle() {
        String updatedTitle = mFolderTitleInput.getText().toString();
        activeFolder.setLabel(updatedTitle);
        List<Object> tags = (List<Object>) activeFolderView.getTag();
        ((TextView) tags.get(1)).setText(updatedTitle);
        mFolderTitleInput.setText(updatedTitle);
        mFolderTitleInput.setCursorVisible(false);
    }

    /**
     * Adds a scroll listener to the mHorizontalPager in order to keep the currentPageNumber
     * updated
     */
    private void createPageChangeListener() {
        mHorizontalPager.addOnScrollListener(new HorizontalPager.OnScrollListener() {
            boolean isViewScrolling = true;

            @Override
            public void onScroll(int scrollX) {
                if (isViewScrolling) {
                    dragDropEnabled = false;
                }
            }

            @Override
            public void onViewScrollFinished(int page) {
                isViewScrolling = false;

                if (currentPageNumber != page) {

                    currentPageNumber = page;
                    // Remove mIndicator and mDock from widgets page, and make them
                    // reappear when user swipes to the first apps page
                    if (currentPageNumber == 0) {
                        mDock.animate().translationYBy(
                                Utilities.pxFromDp(105, LauncherActivity.this)).setDuration(
                                100).withEndAction(() -> mDock.setVisibility(GONE));

                        mIndicator.animate().alpha(0).setDuration(100).withEndAction(
                                () -> mIndicator.setVisibility(GONE));

                        refreshSuggestedApps();
                    } else {
                        if (mIndicator.getAlpha() != 1.0f) {
                            mIndicator.setVisibility(View.VISIBLE);
                            mDock.setVisibility(View.VISIBLE);
                            mIndicator.animate().alpha(1).setDuration(100);
                            mDock.animate().translationY(0).setDuration(100);
                        }
                    }

                    dragDropEnabled = true;
                    updateIndicator();
                }
            }
        });
    }

    private void refreshSuggestedApps() {
        AppUsageStats appUsageStats = new AppUsageStats(this);
        List<UsageStats> usageStats = appUsageStats.getUsageStats();
        if (usageStats.size() > 0) {
            // Check if usage stats have been changed or not to avoid unnecessary flickering
            if (mUsageStats == null || mUsageStats.size() != usageStats.size()
                    || !ListUtil.areEqualLists(mUsageStats, usageStats)) {
                mUsageStats = usageStats;
                if (suggestedAppsGridLayout != null
                        && suggestedAppsGridLayout.getChildCount() > 0) {
                    suggestedAppsGridLayout.removeAllViews();
                }
                int i = 0;
                while (suggestedAppsGridLayout.getChildCount() < 4 && i < mUsageStats.size()) {
                    AppItem appItem = AppUtils.createAppItem(this,
                            mUsageStats.get(i).getPackageName());
                    if (appItem != null) {
                        BlissFrameLayout view = prepareSuggestedApp(appItem);
                        addAppToGrid(suggestedAppsGridLayout, view);
                    }
                    i++;
                }
            }
        }
    }

    /**
     * Populates the pages and the mDock for the first time.
     */
    private void createUI() {
        mHorizontalPager.setUiCreated(false);
        mDock.setEnabled(false);
        launchableApps.clear();
        pinnedApps.clear();
        if (storage.isLayoutPresent()) {
            createUIFromStorage();
        } else {
            for (String defaultPinnedAppsPackage : allLoadedApps.defaultPinnedAppsPackages) {
                pinnedApps.add(allLoadedApps.launchableApps.get(defaultPinnedAppsPackage));
            }
            for (Map.Entry<String, AppItem> stringAppItemEntry : allLoadedApps.launchableApps
                    .entrySet()) {
                launchableApps.add(stringAppItemEntry.getValue());
            }

            launchableApps.removeAll(pinnedApps);

            int nPages = (int) Math.ceil(
                    (float) launchableApps.size() / mDeviceProfile.maxAppsPerPage);
            pages = new ArrayList<>();
            for (int i = 0; i < nPages; i++) {
                GridLayout page = preparePage();
                pages.add(page);
            }
            int count = 0;
            for (int i = 0; i < launchableApps.size(); i++) {
                BlissFrameLayout appView = prepareApp(launchableApps.get(i), true);
                addAppToGrid(pages.get(currentPageNumber), appView);
                count++;
                if (count >= mDeviceProfile.maxAppsPerPage) {
                    count = 0;
                    currentPageNumber++;
                }
            }
            for (int i = 0; i < nPages; i++) {
                mHorizontalPager.addView(pages.get(i));
            }
            currentPageNumber = 0;

            mDock.setLayoutTransition(getDefaultLayoutTransition());
            mDock.setPadding(mDeviceProfile.iconDrawablePaddingPx / 2, 0,
                    mDeviceProfile.iconDrawablePaddingPx / 2, 0);
            for (int i = 0; i < pinnedApps.size(); i++) {
                BlissFrameLayout appView = prepareApp(pinnedApps.get(i), false);
                addAppToDock(appView, INVALID);
            }
        }

        mHorizontalPager.setUiCreated(true);
        mDock.setEnabled(true);
    }

    /**
     * Re-creates the launcher layout based on the data stored in the shared-preferences.
     */
    private void createUIFromStorage() {
        Storage.StorageData storageData = storage.load();
        int nPages = storageData.getNPages();
        pages = new ArrayList<>();
        Set<String> keySet = new HashSet<>();
        List<AppItem> storedItems = new ArrayList<>();

        mDock.setLayoutTransition(getDefaultLayoutTransition());

        for (int i = 0; i < storageData.getNDocked(); i++) {
            try {
                JSONObject currentDockItemData = storageData.dock.getJSONArray(0).getJSONObject(i);
                AppItem appItem = prepareAppFromJSON(currentDockItemData);
                if (appItem != null) {
                    if (appItem.isFolder()) {
                        for (AppItem item : appItem.getSubApps()) {
                            keySet.add(item.getPackageName());
                        }
                    } else {
                        keySet.add(appItem.getPackageName());
                    }
                    BlissFrameLayout appView = prepareApp(appItem, false);
                    if (appView != null) {
                        addAppToDock(appView, INVALID);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        for (int i = 0; i < nPages; i++) {
            GridLayout page = preparePage();
            pages.add(page);
            try {
                JSONArray pageData = storageData.pages.getJSONArray(i);
                for (int j = 0; j < pageData.length(); j++) {
                    JSONObject currentItemData = pageData.getJSONObject(j);
                    AppItem appItem = prepareAppFromJSON(currentItemData);
                    if (appItem != null) {
                        if (appItem.isFolder()) {
                            for (AppItem item : appItem.getSubApps()) {
                                keySet.add(item.getPackageName());
                            }
                        } else {
                            keySet.add(appItem.getPackageName());
                        }
                        BlissFrameLayout appView = prepareApp(appItem, true);
                        if (appView != null) {
                            addAppToGrid(page, appView);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (page.getChildCount() <= 0) {
                pages.remove(page);
            }
        }

        LinkedHashMap<String, AppItem> map = allLoadedApps.launchableApps;
        map.keySet().removeAll(keySet);
        for (Map.Entry<String, AppItem> stringAppItemEntry : map.entrySet()) {
            int size = pages.size();
            if (pages.get(pages.size() - 1).getChildCount() < mDeviceProfile.maxAppsPerPage) {
                BlissFrameLayout appView = prepareApp(stringAppItemEntry.getValue(), true);
                if (appView != null) {
                    addAppToGrid(pages.get(pages.size() - 1), appView);
                }
            } else {
                pages.add(preparePage());
                BlissFrameLayout appView = prepareApp(stringAppItemEntry.getValue(), true);
                if (appView != null) {
                    addAppToGrid(pages.get(pages.size() - 1), appView);
                }
            }
        }

        for (int i = 0; i < pages.size(); i++) {
            mHorizontalPager.addView(pages.get(i));
        }
        currentPageNumber = 0;
    }

    @SuppressLint("InflateParams")
    private GridLayout preparePage() {
        GridLayout grid = (GridLayout) getLayoutInflater().inflate(R.layout.apps_page, null);
        grid.setRowCount(mDeviceProfile.numRows);
        grid.setLayoutTransition(getDefaultLayoutTransition());
        grid.setPadding(mDeviceProfile.iconDrawablePaddingPx / 2,
                (int) (statusBarHeight + Utilities.pxFromDp(8, this)),
                mDeviceProfile.iconDrawablePaddingPx / 2, 0);
        return grid;
    }

    private void createWidgetsPage() {
        ScrollView layout = (ScrollView) getLayoutInflater().inflate(R.layout.widgets_page,
                mHorizontalPager, false);
        layout.setPadding(0,
                (int) (statusBarHeight + Utilities.pxFromDp(8, this)),
                0, 0);
        mHorizontalPager.addView(layout, 0);
        currentPageNumber = 1;
        mHorizontalPager.setCurrentPage(currentPageNumber);

        ImageView clearSuggestions = layout.findViewById(R.id.clearSuggestionImageView);
        clearSuggestions.setOnClickListener(v -> {
            mSearchInput.setText("");
            mSearchInput.clearFocus();
        });

        mSearchInput = layout.findViewById(R.id.search_input);
        mSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    clearSuggestions.setVisibility(GONE);
                } else {
                    clearSuggestions.setVisibility(VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        RecyclerView suggestionRecyclerView = layout.findViewById(R.id.suggestionRecyclerView);
        mSuggestionAdapter = new AutoCompleteAdapter(this);
        suggestionRecyclerView.setHasFixedSize(true);
        suggestionRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        suggestionRecyclerView.setAdapter(mSuggestionAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL);
        suggestionRecyclerView.addItemDecoration(dividerItemDecoration);
        layout.setOnDragListener(null);

        layout.findViewById(R.id.used_apps_layout).setClipToOutline(true);

        suggestedAppsGridLayout = layout.findViewById(R.id.suggestedAppGrid);
        AppUsageStats appUsageStats = new AppUsageStats(this);
        List<UsageStats> usageStats = appUsageStats.getUsageStats();
        if (usageStats.size() > 0) {
            mUsageStats = usageStats;
            int i = 0;
            while (suggestedAppsGridLayout.getChildCount() < 4 && i < usageStats.size()) {
                AppItem appItem = AppUtils.createAppItem(this, usageStats.get(i).getPackageName());
                if (appItem != null) {
                    BlissFrameLayout view = prepareSuggestedApp(appItem);
                    addAppToGrid(suggestedAppsGridLayout, view);
                }
                i++;
            }
        }

        getCompositeDisposable().add(RxTextView.textChanges(mSearchInput)
                .debounce(300, TimeUnit.MILLISECONDS)
                .map(CharSequence::toString)
                .distinctUntilChanged()
                .switchMap(charSequence1 -> {
                    if (charSequence1 != null && charSequence1.length() > 0) {
                        return searchForQuery(charSequence1);
                    } else {
                        return Observable.just(
                                new AutoCompleteServiceResult(new ArrayList<>(),
                                        charSequence1.toString()));
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<AutoCompleteServiceResult>() {
                    @Override
                    public void onNext(
                            AutoCompleteServiceResult autoCompleteServiceResults) {
                        List<String> suggestions = new ArrayList<>();
                        for (int i = 0; i < (autoCompleteServiceResults.items.size() > 5 ? 5
                                : autoCompleteServiceResults.items.size()); i++) {
                            suggestions.add(autoCompleteServiceResults.items.get(i).getPhrase());
                        }
                        mSuggestionAdapter.updateSuggestions(suggestions,
                                autoCompleteServiceResults.queryText);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                    }
                }));

        mSearchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v);
            }
        });

        mSearchInput.setOnEditorActionListener((textView, action, keyEvent) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard(mSearchInput);
                runSearch(mSearchInput.getText().toString());
                mSearchInput.setText("");
                mSearchInput.clearFocus();
                return true;
            }
            return false;
        });

        findViewById(R.id.weather_setting_imageview).setOnClickListener(
                v -> startActivity(new Intent(this, WeatherPreferences.class)));

        mWeatherSetupTextView = findViewById(R.id.weather_setup_textview);
        mWeatherPanel = findViewById(R.id.weather_panel);
        updateWeatherPanel();

        if (org.indin.blisslaunchero.features.weather.WeatherUtils.isWeatherServiceAvailable(
                this)) {
            startService(new Intent(this, WeatherSourceListenerService.class));
            startService(new Intent(this, DeviceStatusService.class));
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mWeatherReceiver, new IntentFilter(
                WeatherUpdateService.ACTION_UPDATE_FINISHED));

        if (!Preferences.useCustomWeatherLocation(this)) {
            if (!WeatherPreferences.hasLocationPermission(this)) {
                String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                requestPermissions(permissions,
                        WeatherPreferences.LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                        && Preferences.getEnableLocation(this)) {
                    showLocationEnableDialog();
                    Preferences.setEnableLocation(this);
                } else {
                    startService(new Intent(this, WeatherUpdateService.class)
                            .putExtra(WeatherUpdateService.ACTION_FORCE_UPDATE, true));
                }
            }
        } else {
            startService(new Intent(this, WeatherUpdateService.class)
                    .putExtra(WeatherUpdateService.ACTION_FORCE_UPDATE, true));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == WeatherPreferences.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We only get here if user tried to enable the preference,
                // hence safe to turn it on after permission is granted
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    showLocationEnableDialog();
                    Preferences.setEnableLocation(this);
                } else {
                    startService(new Intent(this, WeatherUpdateService.class)
                            .putExtra(WeatherUpdateService.ACTION_FORCE_UPDATE, true));
                }
            }
        }
    }

    private void updateWeatherPanel() {
        if (Preferences.getCachedWeatherInfo(this) == null) {
            mWeatherSetupTextView.setVisibility(VISIBLE);
            mWeatherPanel.setVisibility(GONE);
            mWeatherSetupTextView.setOnClickListener(
                    v -> startActivity(
                            new Intent(LauncherActivity.this, WeatherPreferences.class)));
            return;
        }
        mWeatherSetupTextView.setVisibility(GONE);
        mWeatherPanel.setVisibility(VISIBLE);
        WeatherInfo w = Preferences.getCachedWeatherInfo(this);


        int color = Preferences.weatherFontColor(this);
        final boolean useMetric = Preferences.useMetricUnits(this);
        double temp = w.getTemperature();
        double todayLow = w.getTodaysLow();
        double todayHigh = w.getTodaysHigh();

        int tempUnit = w.getTemperatureUnit();
        if (tempUnit == FAHRENHEIT && useMetric) {
            temp = WeatherUtils.fahrenheitToCelsius(temp);
            todayLow = WeatherUtils.fahrenheitToCelsius(todayLow);
            todayHigh = WeatherUtils.fahrenheitToCelsius(todayHigh);
            tempUnit = CELSIUS;
        } else if (tempUnit == CELSIUS && !useMetric) {
            temp = WeatherUtils.celsiusToFahrenheit(temp);
            todayLow = WeatherUtils.celsiusToFahrenheit(todayLow);
            todayHigh = WeatherUtils.celsiusToFahrenheit(todayHigh);
            tempUnit = FAHRENHEIT;
        }

        // Set the current conditions
        // Weather Image
        ImageView weatherImage = mWeatherPanel.findViewById(R.id.weather_image);
        String iconsSet = Preferences.getWeatherIconSet(this);
        weatherImage.setImageBitmap(
                WeatherIconUtils.getWeatherIconBitmap(this, iconsSet, color,
                        w.getConditionCode(), WeatherIconUtils.getNextHigherDensity(this)));

        // City
        TextView city = mWeatherPanel.findViewById(R.id.weather_city);
        city.setText(w.getCity());

        // Weather Condition
        TextView weatherCondition = mWeatherPanel.findViewById(R.id.weather_condition);
        weatherCondition.setText(
                org.indin.blisslaunchero.features.weather.WeatherUtils.resolveWeatherCondition(this,
                        w.getConditionCode()));

        // Weather Temps
        TextView weatherTemp = mWeatherPanel.findViewById(
                R.id.weather_current_temperature);
        weatherTemp.setText(WeatherUtils.formatTemperature(temp, tempUnit));

        // Weather Temps Panel additional items
        final String low = WeatherUtils.formatTemperature(todayLow, tempUnit);
        final String high = WeatherUtils.formatTemperature(todayHigh, tempUnit);
        TextView weatherLowHigh = mWeatherPanel.findViewById(R.id.weather_low_high);
        weatherLowHigh.setText(String.format("%s / %s", low, high));

        double windSpeed = w.getWindSpeed();
        int windSpeedUnit = w.getWindSpeedUnit();
        if (windSpeedUnit == MPH && useMetric) {
            windSpeedUnit = KPH;
            windSpeed = org.indin.blisslaunchero.features.weather.WeatherUtils.milesToKilometers(
                    windSpeed);
        } else if (windSpeedUnit == KPH && !useMetric) {
            windSpeedUnit = MPH;
            windSpeed = org.indin.blisslaunchero.features.weather.WeatherUtils.kilometersToMiles(
                    windSpeed);
        }


        // Humidity and Wind
        TextView weatherHumWind = mWeatherPanel.findViewById(R.id.weather_chance_rain);
        weatherHumWind.setText(
                String.format("%s, %s %s",
                        org.indin.blisslaunchero.features.weather.WeatherUtils.formatHumidity(
                                w.getHumidity()),
                        org.indin.blisslaunchero.features.weather.WeatherUtils.formatWindSpeed(
                                this, windSpeed, windSpeedUnit),
                        org.indin.blisslaunchero.features.weather.WeatherUtils
                                .resolveWindDirection(
                                        this, w.getWindDirection())));
        LinearLayout forecastView = mWeatherPanel.findViewById(R.id.forecast_view);

        ForecastBuilder.buildSmallPanel(this, forecastView, w);
    }

    private void showLocationEnableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Dialog dialog;

        // Build and show the dialog
        builder.setTitle(R.string.weather_retrieve_location_dialog_title);
        builder.setMessage(R.string.weather_retrieve_location_dialog_message);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.weather_retrieve_location_dialog_enable_button,
                (dialog1, whichButton) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivityForResult(intent, 203);
                });
        builder.setNegativeButton(R.string.cancel, null);
        dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 203) {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Toast.makeText(this, "Set custom location in weather settings.",
                        Toast.LENGTH_SHORT).show();
            } else {
                startService(new Intent(this, WeatherUpdateService.class)
                        .putExtra(WeatherUpdateService.ACTION_FORCE_UPDATE, true));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private ObservableSource<AutoCompleteServiceResult> searchForQuery(
            CharSequence charSequence) {
        AutoCompleteService autoCompleteService = RetrofitService.getInstance(
                "https://duckduckgo.com").create(AutoCompleteService.class);
        String query = charSequence.toString().toLowerCase(Locale.getDefault()).trim();

        if (autoCompleteService != null) {
            return autoCompleteService.query(query)
                    .retryWhen(errors -> errors.flatMap(error -> {
                        // For IOExceptions, we  retry
                        if (error instanceof IOException) {
                            return Observable.just(null);
                        }
                        // For anything else, don't retry
                        return Observable.error(error);
                    }))
                    .onErrorReturn(throwable -> new ArrayList<>())
                    .map(autoCompleteServiceRawResults -> new AutoCompleteServiceResult(
                            autoCompleteServiceRawResults, query));
        } else {
            return Observable.just(new AutoCompleteServiceResult(new ArrayList<>(), query));
        }
    }

    @Override
    public void onClick(String suggestion) {
        mSearchInput.setText(suggestion);
        runSearch(suggestion);
        mSearchInput.clearFocus();
        mSearchInput.setText("");
    }

    private void runSearch(String query) {
        Uri uri = Uri.parse("https://spot.ecloud.global/?q=" + query);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        AppUtils.startActivityWithAnimation(this, intent);
    }

    private int getCurrentAppsPageNumber() {
        return currentPageNumber - 1;
    }

    private void addAppToGrid(GridLayout page, BlissFrameLayout view) {
        addAppToGrid(page, view, INVALID);
    }

    private void addAppToGrid(GridLayout page, BlissFrameLayout view, int index) {
        GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.LayoutParams iconLayoutParams = new GridLayout.LayoutParams(rowSpec, colSpec);
        iconLayoutParams.height = mDeviceProfile.cellHeightPx;
        iconLayoutParams.width = mDeviceProfile.cellWidthPx;
        view.findViewById(R.id.app_label).setVisibility(View.VISIBLE);
        view.setLayoutParams(iconLayoutParams);
        view.setWithText(true);
        if (index != INVALID) {
            page.addView(view, index);
        } else {
            page.addView(view);
        }
    }

    /**
     * Adds items to the mDock making sure that the GridLayout's parameters are
     * not violated.
     */
    private void addAppToDock(BlissFrameLayout view, int index) {
        view.findViewById(R.id.app_label).setVisibility(GONE);
        GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.LayoutParams iconLayoutParams = new GridLayout.LayoutParams(rowSpec, colSpec);
        iconLayoutParams.height = mDeviceProfile.hotseatCellHeightPx;
        iconLayoutParams.width = mDeviceProfile.cellWidthPx;
        iconLayoutParams.setGravity(Gravity.CENTER);
        view.setLayoutParams(iconLayoutParams);
        view.setWithText(false);
        if (index != INVALID) {
            mDock.addView(view, index);
        } else {
            mDock.addView(view);
        }
    }

    /**
     * Creates a View that can be displayed by the launcher using just stored
     * JSON data.
     */
    private AppItem prepareAppFromJSON(JSONObject currentItemData) throws Exception {
        String componentName = currentItemData.getString("packageName");
        if (currentItemData.getBoolean("isFolder")) {
            AppItem folderItem = new AppItem(currentItemData.getString("folderName"),
                    "",
                    getDrawable(R.mipmap.ic_folder),
                    null,
                    "FOLDER",
                    false,
                    false,
                    false);
            folderItem.setFolder(true);
            folderItem.setFolderID(currentItemData.getString("folderID"));
            JSONArray subAppData = currentItemData.getJSONArray("subApps");

            // Ignore empty folders
            if (subAppData.length() == 0) {
                return null;
            }

            for (int k = 0; k < subAppData.length(); k++) {
                AppItem appItem = prepareAppItemFromPackage(subAppData.getString(k));
                if (appItem != null) {
                    appItem.setBelongsToFolder(true);
                    folderItem.getSubApps().add(appItem);
                }
            }

            folderItem.setIcon(new GraphicsUtil(this).generateFolderIcon(this, folderItem));
            folderItem.setIcon(new GraphicsUtil(this).generateFolderIcon(this, folderItem));
            return folderItem;
        } else {
            return prepareAppItemFromPackage(componentName);
        }
    }

    private AppItem prepareAppItemFromPackage(String packageName) {
        if (allLoadedApps.launchableApps.containsKey(packageName)) {
            AppItem appItem = allLoadedApps.launchableApps.get(packageName);
            return appItem;
        } else {
            return null;
        }
    }

    /**
     * Converts an AppItem into a View object that can be rendered inside
     * the pages and the mDock.
     * <p>
     * The View object also has all the required listeners attached to it.
     */
    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    private BlissFrameLayout prepareApp(final AppItem app, boolean withText) {
        final BlissFrameLayout v = (BlissFrameLayout) getLayoutInflater().inflate(R.layout.app_view,
                null);
        final TextView label = v.findViewById(R.id.app_label);
        final SquareFrameLayout icon = v.findViewById(R.id.app_icon);
        final SquareImageView squareImageView = v.findViewById(
                R.id.icon_image_view);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) icon.getLayoutParams();
        layoutParams.leftMargin = mDeviceProfile.iconDrawablePaddingPx / 2;
        layoutParams.rightMargin = mDeviceProfile.iconDrawablePaddingPx / 2;

        label.setPadding((int) Utilities.pxFromDp(4, this),
                (int) Utilities.pxFromDp(0, this),
                (int) Utilities.pxFromDp(4, this),
                (int) Utilities.pxFromDp(0, this));

        if (app.isFolder()) {
            v.applyBadge(checkHasApp(app, mAppsWithNotifications), withText);
        } else {
            v.applyBadge(mAppsWithNotifications.contains(app.getPackageName()), withText);
        }

        if (app.isClock()) {
            final CustomAnalogClock analogClock = v.findViewById(
                    R.id.icon_clock);
            analogClock.setAutoUpdate(true);
            analogClock.setVisibility(View.VISIBLE);
            squareImageView.setVisibility(GONE);
        } else if (app.isCalendar()) {

            TextView monthTextView = v.findViewById(R.id.calendar_month_textview);
            monthTextView.getLayoutParams().height = mDeviceProfile.monthTextviewHeight;
            monthTextView.getLayoutParams().width = mDeviceProfile.calendarIconWidth;
            int monthPx = mDeviceProfile.monthTextSize;
            monthTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, monthPx / 2);

            TextView dateTextView = v.findViewById(R.id.calendar_date_textview);
            dateTextView.getLayoutParams().height = mDeviceProfile.dateTextviewHeight;
            dateTextView.getLayoutParams().width = mDeviceProfile.calendarIconWidth;
            int datePx = mDeviceProfile.dateTextSize;
            dateTextView.setPadding(0, mDeviceProfile.dateTextTopPadding, 0,
                    mDeviceProfile.dateTextBottomPadding);

            dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, datePx / 2);

            v.findViewById(R.id.icon_calendar).setVisibility(View.VISIBLE);
            squareImageView.setVisibility(GONE);

            CalendarIcon calendarIcon = new CalendarIcon(monthTextView, dateTextView);
            updateCalendarIcon(calendarIcon, Calendar.getInstance());
            mCalendarIcons.add(calendarIcon);
        }

        final Intent intent = app.getIntent();
        if (!app.isClock() || !app.isCalendar()) {
            squareImageView.setImageDrawable(app.getIcon());
        }
        label.setText(app.getLabel());
        label.setTextSize(12);
        List<Object> tags = new ArrayList<>();
        tags.add(squareImageView);
        tags.add(label);
        tags.add(app);
        v.setTag(tags);

        icon.setOnLongClickListener(view -> {
            handleWobbling(true);
            longPressed = true;
            return true;
        });

        icon.setOnTouchListener(new View.OnTouchListener() {

            long iconPressedAt = 0;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (!mLongClickStartsDrag) {
                        iconPressedAt = System.currentTimeMillis();
                    }
                } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    if (longPressed || (!mLongClickStartsDrag
                            && (System.currentTimeMillis() - iconPressedAt) > 150)) {
                        longPressed = false;
                        movingApp = v;
                        dragShadowBuilder = new BlissDragShadowBuilder(
                                icon, (event.getX() < 0 ? 0 : event.getX()),
                                (event.getY() < 0 ? 0 : event.getY()));
                        icon.startDrag(null, dragShadowBuilder, v, 0);
                        if (v.getParent().getParent() instanceof HorizontalPager) {
                            parentPage = getCurrentAppsPageNumber();
                        } else {
                            parentPage = -99;
                        }
                        v.clearAnimation();
                        v.setVisibility(View.INVISIBLE);
                        dragDropEnabled = true;
                        return true;
                    }
                }
                return false;
            }
        });

        icon.setOnClickListener(view -> {
            if (isWobbling) {
                handleWobbling(false);
                return;
            }
            // Make outside apps not clickable when the folder window is visible
            if (!app.isBelongsToFolder() &&
                    mFolderWindowContainer.getVisibility() == View.VISIBLE) {
                return;
            }

            if (!app.isFolder()) {
                AppUtils.startActivityWithAnimation(getApplicationContext(), intent);
            } else {
                folderFromDock = !(v.getParent().getParent() instanceof HorizontalPager);
                displayFolder(app, v);
            }
        });

        return v;
    }

    private BlissFrameLayout prepareSuggestedApp(final AppItem app) {
        final BlissFrameLayout v = (BlissFrameLayout) getLayoutInflater().inflate(R.layout.app_view,
                null);
        final TextView label = v.findViewById(R.id.app_label);
        final SquareFrameLayout icon = v.findViewById(R.id.app_icon);
        final SquareImageView squareImageView = v.findViewById(
                R.id.icon_image_view);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) icon.getLayoutParams();
        layoutParams.leftMargin = mDeviceProfile.iconDrawablePaddingPx / 2;
        layoutParams.rightMargin = mDeviceProfile.iconDrawablePaddingPx / 2;

        label.setPadding((int) Utilities.pxFromDp(4, this),
                (int) Utilities.pxFromDp(0, this),
                (int) Utilities.pxFromDp(4, this),
                (int) Utilities.pxFromDp(0, this));

        if (app.isFolder()) {
            v.applyBadge(checkHasApp(app, mAppsWithNotifications), true);
        } else {
            v.applyBadge(mAppsWithNotifications.contains(app.getPackageName()), true);
        }

        if (app.isClock()) {
            final CustomAnalogClock analogClock = v.findViewById(
                    R.id.icon_clock);
            analogClock.setAutoUpdate(true);
            analogClock.setVisibility(View.VISIBLE);
            squareImageView.setVisibility(GONE);
        } else if (app.isCalendar()) {

            TextView monthTextView = v.findViewById(R.id.calendar_month_textview);
            monthTextView.getLayoutParams().height = mDeviceProfile.monthTextviewHeight;
            monthTextView.getLayoutParams().width = mDeviceProfile.calendarIconWidth;
            int monthPx = mDeviceProfile.monthTextSize;
            monthTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, monthPx / 2);

            TextView dateTextView = v.findViewById(R.id.calendar_date_textview);
            dateTextView.getLayoutParams().height = mDeviceProfile.dateTextviewHeight;
            dateTextView.getLayoutParams().width = mDeviceProfile.calendarIconWidth;
            int datePx = mDeviceProfile.dateTextSize;
            dateTextView.setPadding(0, mDeviceProfile.dateTextTopPadding, 0,
                    mDeviceProfile.dateTextBottomPadding);

            dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, datePx / 2);

            v.findViewById(R.id.icon_calendar).setVisibility(View.VISIBLE);
            squareImageView.setVisibility(GONE);

            CalendarIcon calendarIcon = new CalendarIcon(monthTextView, dateTextView);
            updateCalendarIcon(calendarIcon, Calendar.getInstance());
            mCalendarIcons.add(calendarIcon);
        }

        final Intent intent = app.getIntent();
        if (!app.isClock() || !app.isCalendar()) {
            squareImageView.setImageDrawable(app.getIcon());
        }
        label.setText(app.getLabel());
        label.setTextSize(12);
        List<Object> tags = new ArrayList<>();
        tags.add(squareImageView);
        tags.add(label);
        tags.add(app);
        v.setTag(tags);

        icon.setOnClickListener(view -> {
            AppUtils.startActivityWithAnimation(getApplicationContext(), intent);
        });

        return v;
    }

    private void displayFolder(AppItem app, BlissFrameLayout v) {

        activeFolder = app;
        activeFolderView = v;

        mFolderWindowContainer.setAlpha(0f);
        mFolderWindowContainer.setVisibility(View.VISIBLE);
        mFolderWindowContainer.animate().alpha(1.0f).setDuration(200);

        mFolderTitleInput.setText(app.getLabel());
        mFolderTitleInput.setCursorVisible(false);

        mFolderAppsViewPager.setAdapter(new FolderAppsPagerAdapter(this));
        mFolderAppsViewPager.getLayoutParams().width =
                mDeviceProfile.cellWidthPx * 3 + mDeviceProfile.iconDrawablePaddingPx;
        mFolderAppsViewPager.getLayoutParams().height =
                mDeviceProfile.cellHeightPx * 3 + mDeviceProfile.iconDrawablePaddingPx;
        ((CircleIndicator) mLauncherView.findViewById(R.id.indicator)).setViewPager(
                mFolderAppsViewPager);

    }

    /**
     * Handle the wobbling animation.
     */
    private void handleWobbling(boolean shouldPlay) {
        if (mWobblingCountDownTimer != null && !shouldPlay) {
            mWobblingCountDownTimer.cancel();
        }
        isWobbling = shouldPlay;
        mLongClickStartsDrag = !shouldPlay;
        longPressed = false;

        if (mFolderWindowContainer.getVisibility() == View.VISIBLE) {
            for (int i = 0; i < mFolderAppsViewPager.getChildCount(); i++) {
                toggleWobbleAnimation((GridLayout) mFolderAppsViewPager.getChildAt(i), shouldPlay);
            }
        }

        for (int i = 0; i < pages.size(); i++) {
            toggleWobbleAnimation(pages.get(i), shouldPlay);
        }
        toggleWobbleAnimation(mDock, shouldPlay);

        if (!shouldPlay) {
            storage.save(pages, mDock);
        }
    }

    /**
     * Toggle the wobbling animation.
     */
    private void toggleWobbleAnimation(GridLayout gridLayout, boolean shouldPlayAnimation) {
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            ViewGroup viewGroup = (ViewGroup) gridLayout.getChildAt(i);
            makeAppWobble(viewGroup, shouldPlayAnimation, i);
        }
    }

    private void makeAppWobble(ViewGroup viewGroup, boolean shouldPlayAnimation, int i) {

        if (shouldPlayAnimation) {
            if (viewGroup.getAnimation() == null) {
                ImageView imageView = viewGroup.findViewById(R.id.uninstall_app);
                if (imageView == null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        addUninstallIcon(viewGroup);
                    });
                }

                if (i % 2 == 0) {
                    viewGroup.startAnimation(wobbleAnimation);
                } else {
                    viewGroup.startAnimation(wobbleReverseAnimation);
                }
            }
        } else {
            ImageView imageView = viewGroup.findViewById(R.id.uninstall_app);
            if (imageView != null) {
                ((ViewGroup) imageView.getParent()).removeView(imageView);
            }
            viewGroup.setAnimation(null);
        }

    }

    /**
     * Display uninstall icon while animating the view.
     */
    private void addUninstallIcon(ViewGroup viewGroup) {
        final AppItem appItem = getAppDetails(viewGroup);
        if (!appItem.isSystemApp() && !appItem.isFolder()) {
            SquareFrameLayout appIcon = viewGroup.findViewById(R.id.app_icon);
            int size = mDeviceProfile.uninstallIconSizePx;
            int topPadding = (appIcon.getTop() - mDeviceProfile.uninstallIconSizePx / 2
                    + mDeviceProfile.uninstallIconPadding > 0) ?
                    appIcon.getTop() - mDeviceProfile.uninstallIconSizePx / 2
                            + mDeviceProfile.uninstallIconPadding : 0;
            int bottomPadding = topPadding;
            int rightPadding = (appIcon.getLeft() - mDeviceProfile.uninstallIconSizePx / 2
                    + mDeviceProfile.uninstallIconPadding > 0) ?
                    appIcon.getLeft() - mDeviceProfile.uninstallIconSizePx / 2
                            + mDeviceProfile.uninstallIconPadding : 0;
            int leftPadding = rightPadding;

            ImageView imageView = new ImageView(this);
            imageView.setId(R.id.uninstall_app);
            imageView.setImageResource(R.drawable.remove_icon_72);
            imageView.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);

            imageView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DELETE,
                        Uri.fromParts("package", appItem.getPackageName(),
                                null));
                LauncherActivity.this.startActivity(intent);
            });
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    size + 2 * rightPadding, size + 2 * topPadding);
            layoutParams.gravity = Gravity.END | Gravity.TOP;
            viewGroup.addView(imageView, layoutParams);
        }
    }

    /**
     * Creates drag listeners for the mDock and pages, which are responsible for almost
     * all the drag and drop functionality present in this app.
     */
    private void createDragListener() {
        /*mHorizontalPager.setOnDragListener(new SystemDragDriver(workspaceEventListener));
        mDock.setOnDragListener(new SystemDragDriver(dockEventListener));*/
        mDock.setOnDragListener(new View.OnDragListener() {
            public float cX;
            public float cY;
            private boolean latestFolderInterest;

            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                if (dragEvent.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                    isDragging = true;
                    if (mWobblingCountDownTimer != null) {
                        mWobblingCountDownTimer.cancel();
                    }
                }
                if (dragEvent.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                    // Don't offer rearrange functionality when app is being dragged
                    // out of folder window
                    if (getAppDetails(movingApp).isBelongsToFolder()) {
                        return true;
                    }

                    // Do nothing during scroll operations
                    if (!dragDropEnabled) {
                        return true;
                    }

                    cX = dragEvent.getX() - dragShadowBuilder.xOffset;
                    cY = mDock.getY() + dragEvent.getY() - dragShadowBuilder.yOffset;

                    int index = getIndex(mDock, cX, cY);
                    // If hovering over self, ignore drag/drop
                    if (index == mDock.indexOfChild(movingApp)) {
                        discardCollidingApp();
                        return true;
                    }

                    // If hovering over an empty location, ignore drag/drop
                    if (index == INVALID) {
                        discardCollidingApp();
                    }

                    // If hovering over another app icon
                    // either move it or create a folder
                    // depending on time and distance
                    if (index != INVALID) {
                        BlissFrameLayout latestCollidingApp =
                                (BlissFrameLayout) mDock.getChildAt(index);
                        if (collidingApp != latestCollidingApp) {
                            if (collidingApp != null) {
                                makeAppCold(collidingApp,
                                        !(collidingApp.getParent().getParent() instanceof
                                                HorizontalPager));
                            }
                            collidingApp = latestCollidingApp;
                            folderInterest = false;
                        }

                        AppItem moveAppDetails = getAppDetails(movingApp);
                        if (moveAppDetails.isFolder()) {
                            folderInterest = false;
                        } else {
                            latestFolderInterest = checkIfFolderInterest(mDock, index, cX, cY);
                            if (latestFolderInterest != folderInterest) {
                                folderInterest = latestFolderInterest;
                            }
                            if (folderInterest) {
                                cleanupDockReorder(true);
                                cleanupReorder(true);
                                makeAppHot(collidingApp);
                            } else {
                                View app = collidingApp;
                                makeAppCold(app,
                                        !(app.getParent().getParent() instanceof HorizontalPager));
                            }
                        }
                    }

                    if (!folderInterest && !mDockReorderAlarm.alarmPending()) {
                        DockReorderAlarmListener dockReorderAlarmListener =
                                new DockReorderAlarmListener(index);
                        mDockReorderAlarm.setOnAlarmListener(dockReorderAlarmListener);
                        mDockReorderAlarm.setAlarm(REORDER_TIMEOUT);
                    }
                    return true;
                } else if (dragEvent.getAction() == DragEvent.ACTION_DROP) {
                    cleanupDockReorder(true);
                    cleanupReorder(true);
                    if (mFolderWindowContainer.getVisibility() != View.VISIBLE) {
                        // Drop functionality when the folder window container
                        // is not being shown -- default
                        if (!folderInterest) {
                            if (movingApp.getParent() == null) {
                                if (mDock.getChildCount() >= mDeviceProfile.numColumns) {
                                    Toast.makeText(LauncherActivity.this,
                                            "Dock is already full",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    addAppToDock(movingApp, INVALID);
                                }
                            }
                            movingApp.setVisibility(View.VISIBLE);
                            makeAppWobble(movingApp, true, mDock.indexOfChild(movingApp));
                        } else {
                            if (collidingApp.getParent().getParent() instanceof HorizontalPager) {
                                createFolder(false);
                            } else {
                                createFolder(true);
                            }

                        }
                        folderInterest = false;
                    } else {
                        cX = dragEvent.getX() - dragShadowBuilder.xOffset;
                        cY = mDock.getY() + dragEvent.getY() - dragShadowBuilder.yOffset;
                        // Drop functionality when the folder window is visible
                        Rect bounds = new Rect((int) mFolderAppsViewPager.getX(),
                                (int) mFolderAppsViewPager.getY(),
                                (int) (mFolderAppsViewPager.getWidth()
                                        + mFolderAppsViewPager.getX()),
                                (int) (mFolderAppsViewPager.getHeight()
                                        + mFolderAppsViewPager.getY()));

                        if (!bounds.contains((int) cX, (int) cY)) {
                            removeAppFromFolder();
                        } else {
                            movingApp.setVisibility(View.VISIBLE);
                            int currentItem = mFolderAppsViewPager.getCurrentItem();
                            makeAppWobble(movingApp, true,
                                    ((GridLayout) mFolderAppsViewPager.getChildAt(
                                            currentItem)).indexOfChild(movingApp));
                        }
                    }
                    return true;
                }
                return true;
            }
        });

        mHorizontalPager.setOnDragListener(new View.OnDragListener() {
            public float cY;
            public float cX;
            private boolean latestFolderInterest;

            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {

                if (dragEvent.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                    isDragging = true;
                    if (mWobblingCountDownTimer != null) {
                        mWobblingCountDownTimer.cancel();
                    }
                }

                if (dragEvent.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                    cX = dragEvent.getX() - dragShadowBuilder.xOffset;
                    cY = mHorizontalPager.getY() + dragEvent.getY()
                            - dragShadowBuilder.yOffset;

                    // Don't offer rearrange functionality when app is being dragged
                    // out of folder window
                    if (getAppDetails(movingApp).isBelongsToFolder()) {
                        return true;
                    }

                    // Do nothing during scroll operations
                    if (!dragDropEnabled) {
                        return true;
                    }


                    GridLayout page = pages.get(getCurrentAppsPageNumber());

                    if (cX < mDeviceProfile.availableWidthPx - scrollCorner
                            && cX > scrollCorner) {

                        int index = getIndex(page, cX, cY);
                        // If hovering over self, ignore drag/drop
                        if (index == getGridFromPage(page).indexOfChild(movingApp)) {
                            discardCollidingApp();
                            return true;
                        }

                        // If hovering over an empty location, ignore drag/drop
                        if (index == INVALID) {
                            discardCollidingApp();
                        }

                        // If hovering over another app icon
                        // either move it or create a folder
                        // depending on time and distance
                        if (index != INVALID) {
                            View latestCollidingApp = getGridFromPage(page).getChildAt(index);
                            if (collidingApp != latestCollidingApp) {
                                if (collidingApp != null) {
                                    makeAppCold(collidingApp,
                                            !(collidingApp.getParent().getParent() instanceof
                                                    HorizontalPager));
                                }
                                collidingApp = (BlissFrameLayout) latestCollidingApp;
                                folderInterest = false;
                            }

                            AppItem moveAppDetails = getAppDetails(movingApp);
                            assert moveAppDetails != null;
                            if (moveAppDetails.isFolder()) {
                                folderInterest = false;
                            } else {
                                latestFolderInterest = checkIfFolderInterest(
                                        getGridFromPage(pages.get(getCurrentAppsPageNumber())),
                                        index, cX, cY);

                                if (latestFolderInterest != folderInterest) {
                                    folderInterest = latestFolderInterest;
                                }
                                if (folderInterest) {
                                    cleanupReorder(true);
                                    cleanupDockReorder(true);
                                    makeAppHot(collidingApp);
                                } else {
                                    makeAppCold(collidingApp,
                                            !(collidingApp.getParent().getParent() instanceof
                                                    HorizontalPager));
                                }
                            }
                        }

                        if (!folderInterest && !mReorderAlarm.alarmPending()) {
                            ReorderAlarmListener reorderAlarmListener = new ReorderAlarmListener(
                                    page, (ViewGroup) movingApp.getParent(), index);
                            mReorderAlarm.setOnAlarmListener(reorderAlarmListener);
                            mReorderAlarm.setAlarm(REORDER_TIMEOUT);
                        }
                    } else {
                        if (cX > mDeviceProfile.availableWidthPx - scrollCorner) {
                            if (getCurrentAppsPageNumber() + 1 < pages.size()) {
                                mHorizontalPager.scrollRight(300);
                            } else if (getCurrentAppsPageNumber() + 1 == pages.size()
                                    && getGridFromPage(page).getChildCount() > 1) {
                                GridLayout layout = preparePage();
                                pages.add(layout);
                                ImageView dot = new ImageView(LauncherActivity.this);
                                dot.setImageDrawable(getDrawable(R.drawable.dot_off));
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        getResources().getDimensionPixelSize(R.dimen.dotSize),
                                        getResources().getDimensionPixelSize(R.dimen.dotSize)
                                );
                                dot.setLayoutParams(params);
                                mIndicator.addView(dot);
                                mHorizontalPager.addView(layout);
                            }
                        } else if (cX < scrollCorner) {
                            if (getCurrentAppsPageNumber() == 0) {
                                return true;
                            }
                            if (getCurrentAppsPageNumber() - 1 >= 0) {
                                mHorizontalPager.scrollLeft(300);
                            } else if (getCurrentAppsPageNumber() + 1 == pages.size() - 2
                                    && getGridFromPage(pages.get(pages.size() - 1)).getChildCount()
                                    <= 0) {
                                mIndicator.removeViewAt(pages.size());
                                mHorizontalPager.removeViewAt(pages.size());
                                pages.remove(pages.size() - 1);
                            }
                        }
                    }
                } else if (dragEvent.getAction() == DragEvent.ACTION_DROP) {
                    cleanupReorder(true);
                    cleanupDockReorder(true);
                    if (mFolderWindowContainer.getVisibility() != View.VISIBLE) {
                        // Drop functionality when the folder window container
                        // is not being shown -- default
                        GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                        if (!folderInterest) {
                            if (movingApp.getParent() == null) {
                                if (gridLayout.getChildCount()
                                        < mDeviceProfile.maxAppsPerPage) {
                                    addAppToGrid(gridLayout, movingApp);
                                }
                            }
                            movingApp.setVisibility(View.VISIBLE);
                            makeAppWobble(movingApp, true, gridLayout.indexOfChild(movingApp));
                        } else {
                            if (collidingApp.getParent().getParent() instanceof HorizontalPager) {
                                createFolder(false);
                            } else {
                                createFolder(true);
                            }

                        }
                        folderInterest = false;
                    } else {
                        cX = dragEvent.getX() - dragShadowBuilder.xOffset;
                        cY = mHorizontalPager.getY() + dragEvent.getY()
                                - dragShadowBuilder.yOffset;

                        // Drop functionality when the folder window is visible
                        Rect bounds = new Rect((int) mFolderAppsViewPager.getX(),
                                (int) mFolderAppsViewPager.getY(),
                                (int) (mFolderAppsViewPager.getWidth()
                                        + mFolderAppsViewPager.getX()),
                                (int) (mFolderAppsViewPager.getHeight()
                                        + mFolderAppsViewPager.getY()));
                        if (!bounds.contains((int) cX, (int) cY)) {
                            removeAppFromFolder();
                        } else {
                            movingApp.setVisibility(View.VISIBLE);
                            int currentItem = mFolderAppsViewPager.getCurrentItem();
                            makeAppWobble(movingApp, true,
                                    ((GridLayout) mFolderAppsViewPager.getChildAt(
                                            currentItem)).indexOfChild(movingApp));
                        }
                    }
                } else if (dragEvent.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    if (isDragging) {
                        isDragging = false;

                    }
                    if (!dragEvent.getResult()) {
                        movingApp.setVisibility(View.VISIBLE);
                        if (mFolderWindowContainer.getVisibility() == View.VISIBLE) {
                            int currentItem = mFolderAppsViewPager.getCurrentItem();
                            makeAppWobble(movingApp, true,
                                    ((GridLayout) mFolderAppsViewPager.getChildAt(
                                            currentItem)).indexOfChild(movingApp));
                        } else if (movingApp.getParent().getParent() instanceof HorizontalPager) {
                            GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                            makeAppWobble(movingApp, true,
                                    gridLayout.indexOfChild(movingApp));
                        } else {
                            makeAppWobble(movingApp, true, mDock.indexOfChild(movingApp));
                        }
                    }

                    if (mWobblingCountDownTimer != null) {
                        mWobblingCountDownTimer.cancel();
                    }
                    mLongClickStartsDrag = false;
                    mWobblingCountDownTimer = new CountDownTimer(25000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                        }

                        @Override
                        public void onFinish() {
                            if (isWobbling) {
                                handleWobbling(false);
                            }
                        }
                    }.start();

                    if (getCurrentAppsPageNumber() > 0 && getGridFromPage(
                            pages.get(getCurrentAppsPageNumber() - 1)).getChildCount()
                            <= 0) {
                        pages.remove(getCurrentAppsPageNumber() - 1);
                        mIndicator.removeViewAt(getCurrentAppsPageNumber());
                        mHorizontalPager.removeViewAt(getCurrentAppsPageNumber());
                        mHorizontalPager.scrollLeft(300);
                    }

                    if (getCurrentAppsPageNumber() < pages.size() - 1 && getGridFromPage(
                            pages.get(getCurrentAppsPageNumber() + 1)).getChildCount() <= 0) {
                        pages.remove(getCurrentAppsPageNumber() + 1);
                        mIndicator.removeViewAt(getCurrentAppsPageNumber() + 2);
                        mHorizontalPager.removeViewAt(getCurrentAppsPageNumber() + 2);
                    }

                    storage.save(pages, mDock);
                }
                return true;
            }
        });
    }

    private void cleanupDockReorder(boolean cancelAlarm) {
        if (cancelAlarm) {
            mDockReorderAlarm.cancelAlarm();
        }
    }

    private void cleanupReorder(boolean cancelAlarm) {
        if (cancelAlarm) {
            mReorderAlarm.cancelAlarm();
        }
    }

    /**
     * Remove app from the folder by dragging out of the folder view.
     */
    private void removeAppFromFolder() {
        if (pages.get(getCurrentAppsPageNumber()).getChildCount()
                >= mDeviceProfile.maxAppsPerPage) {
            Toast.makeText(this, "No more room in page", Toast.LENGTH_SHORT).show();
            movingApp.setVisibility(View.VISIBLE);
            int currentItem = mFolderAppsViewPager.getCurrentItem();
            makeAppWobble(movingApp, true,
                    ((GridLayout) mFolderAppsViewPager.getChildAt(
                            currentItem)).indexOfChild(movingApp));
        } else {
            AppItem app = getAppDetails(movingApp);
            activeFolder.getSubApps().remove(app);
            mFolderAppsViewPager.getAdapter().notifyDataSetChanged();
            assert app != null;
            app.setBelongsToFolder(false);

            if (activeFolder.getSubApps().size() == 0) {
                BlissFrameLayout view = prepareApp(app, !folderFromDock);
                if (folderFromDock) {
                    int index = mDock.indexOfChild(activeFolderView);
                    mDock.removeView(activeFolderView);
                    addAppToDock(view, index);
                    makeAppWobble(view, true, index);
                } else {
                    GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                    int index = gridLayout.indexOfChild(activeFolderView);
                    activeFolderView.setAnimation(null);
                    gridLayout.removeView(activeFolderView);
                    addAppToGrid(gridLayout, view, index);
                    makeAppWobble(view, true, index);
                }

            } else {
                if (activeFolder.getSubApps().size() == 1) {
                    AppItem item = activeFolder.getSubApps().get(0);
                    activeFolder.getSubApps().remove(item);
                    mFolderAppsViewPager.getAdapter().notifyDataSetChanged();
                    item.setBelongsToFolder(false);
                    BlissFrameLayout view = prepareApp(item, !folderFromDock);
                    if (folderFromDock) {
                        int index = mDock.indexOfChild(activeFolderView);
                        activeFolderView.setAnimation(null);
                        mDock.removeView(activeFolderView);
                        addAppToDock(view, index);
                        makeAppWobble(view, true, index);
                    } else {
                        GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                        int index = gridLayout.indexOfChild(activeFolderView);
                        activeFolderView.setAnimation(null);
                        gridLayout.removeView(activeFolderView);
                        addAppToGrid(gridLayout, view, index);
                        makeAppWobble(view, true, index);
                    }
                } else {
                    updateIcon(activeFolderView, activeFolder,
                            new GraphicsUtil(this).generateFolderIcon(this, activeFolder),
                            folderFromDock);
                }
                if (movingApp.getParent() != null) {
                    ((ViewGroup) movingApp.getParent()).removeView(movingApp);
                }
                int current = getCurrentAppsPageNumber();
                addAppToGrid(pages.get(current), movingApp);
                makeAppWobble(movingApp, true, pages.get(current).getChildCount() - 1);
            }

            hideFolderWindowContainer();
            movingApp.setVisibility(View.VISIBLE);
        }

    }

    /**
     * Returns an app to normal if the user doesn't express move/folder-creation interests.
     */
    private void discardCollidingApp() {
        if (collidingApp != null) {
            makeAppCold(collidingApp,
                    !(collidingApp.getParent().getParent() instanceof HorizontalPager));
            collidingApp = null;
            folderInterest = false;
        }
    }

    /**
     * Creates/updates a folder using the tags associated with the app being dragged,
     * and the target app.
     */
    private void createFolder(boolean fromDock) {
        int index;

        collidingApp.setAnimation(null);

        if (fromDock) {
            index = mDock.indexOfChild(collidingApp);
        } else {
            index = getGridFromPage(pages.get(getCurrentAppsPageNumber())).indexOfChild(
                    collidingApp);
        }

        AppItem app1 = (AppItem) ((List<Object>) collidingApp.getTag()).get(2);
        AppItem app2 = (AppItem) ((List<Object>) movingApp.getTag()).get(2);

        Drawable folderIcon = new GraphicsUtil(this).generateFolderIcon(this,
                app1.getIcon(), app2.getIcon());

        AppItem folder;

        if (!app1.isFolder()) {
            folder = new AppItem("Untitled",
                    "",
                    folderIcon,
                    null,
                    "FOLDER",
                    false,
                    false,
                    false);
            folder.setFolder(true);
            folder.setFolderID(UUID.randomUUID().toString());

            List<AppItem> subApps = folder.getSubApps();
            app1.setBelongsToFolder(true);
            app2.setBelongsToFolder(true);
            subApps.add(app1);
            subApps.add(app2);

            BlissFrameLayout folderView = prepareApp(folder, !fromDock);
            makeAppWobble(collidingApp, false, index);
            ((ViewGroup) collidingApp.getParent()).removeView(collidingApp);

            if (fromDock) {
                addAppToDock(folderView, index);

            } else {
                addAppToGrid(pages.get(getCurrentAppsPageNumber()), folderView, index);
            }
            makeAppWobble(folderView, true, index);


        } else {
            app2.setBelongsToFolder(true);
            app1.getSubApps().add(app2);
            updateIcon(collidingApp, app1, new GraphicsUtil(this).generateFolderIcon(this, app1),
                    folderFromDock);
            makeAppWobble(collidingApp, true,
                    index);
        }


        if (movingApp.getParent() != null) {
            ((ViewGroup) movingApp.getParent()).removeView(movingApp);
        }

        makeAppCold(collidingApp, fromDock);
        makeAppCold(movingApp, fromDock);

        storage.save(pages, mDock);
    }

    private void updateIcon(BlissFrameLayout appView, AppItem app, Drawable drawable,
            boolean folderFromDock) {
        app.setIcon(drawable);
        List<Object> tags = (List<Object>) appView.getTag();
        SquareImageView iv = (SquareImageView) tags.get(0);
        iv.setImageDrawable(drawable);
        appView.applyBadge(checkHasApp(app, mAppsWithNotifications), !folderFromDock);
    }

    /**
     * Highlights an app
     */
    private void makeAppHot(View app) {
        if (app == null) {
            return;
        }
        app.setScaleX(1.2f);
        app.setScaleY(1.2f);
    }

    /**
     * Makes an app look normal
     */
    private synchronized void makeAppCold(View app, boolean fromDock) {
        if (app == null) {
            return;
        }

        List<Object> views = (List<Object>) app.getTag();
        if (!fromDock) {
            ((View) views.get(1)).setVisibility(View.VISIBLE);
        } else {
            ((View) views.get(1)).setVisibility(GONE);
        }
        app.setScaleX(1.0f);
        app.setScaleY(1.0f);
        collidingApp = null;
    }

    /**
     * Reads the tags of a View
     */
    private AppItem getAppDetails(View app) {
        if (app.getTag() != null) {
            List<Object> details = (List<Object>) app.getTag();
            return (AppItem) details.get(2);
        }
        return null;
    }

    /**
     * Checks if the user wants to create a folder based on the distance
     * between the dragged app and the dragged-over app
     */
    private boolean checkIfFolderInterest(ViewGroup view, int index, float x, float y) {
        View v = view.getChildAt(index).findViewById(R.id.app_icon);
        Rect r = new Rect();
        v.getGlobalVisibleRect(r);
        float vx = r.left + (r.right - r.left) / 2;
        float vy = r.top + (r.bottom - r.top) / 2;
        double distance = getDistance(x, y, vx, vy);
        return distance < maxDistanceForFolderCreation;
    }

    private GridLayout getGridFromPage(ViewGroup page) {
        return (GridLayout) page;
    }

    /**
     * Calculates the euclidean distance between two points
     */
    private double getDistance(float x1, float y1, float x2, float y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    /**
     * Identifies the app that surrounds a given point
     */
    private int getIndex(ViewGroup page, float x, float y) {
        float minDistance = Float.MAX_VALUE;
        int index = INVALID;


        for (int i = 0; i < page.getChildCount(); i++) {
            View v = page.getChildAt(i).findViewById(R.id.app_icon);
            Rect r = new Rect();
            v.getGlobalVisibleRect(r);
            Rect r2 = new Rect((int) (x - mDeviceProfile.iconSizePx / 2),
                    (int) (y - mDeviceProfile.iconSizePx / 2),
                    (int) (x + mDeviceProfile.iconSizePx / 2),
                    (int) (y + mDeviceProfile.iconSizePx / 2));
            if (Rect.intersects(r, r2)) {
                float vx = r.left + (float) (r.right - r.left) / 2;
                float vy = r.top + (float) (r.bottom - r.top) / 2;
                float distance = (float) Math.hypot(vx - x, vy - y);
                if (minDistance > distance) {
                    minDistance = distance;
                    index = i;
                }
            }
        }
        return index;
    }

    /**
     * Creates the animation effect that runs when apps are moved around
     */
    private LayoutTransition getDefaultLayoutTransition() {
        LayoutTransition transition = new LayoutTransition();
        transition.disableTransitionType(LayoutTransition.APPEARING);
        transition.disableTransitionType(LayoutTransition.DISAPPEARING);
        transition.setStartDelay(LayoutTransition.CHANGE_APPEARING, 0);
        transition.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
        transition.addTransitionListener(new LayoutTransition.TransitionListener() {
            @Override
            public void startTransition(LayoutTransition layoutTransition,
                    ViewGroup viewGroup, View view, int i) {
                dragDropEnabled = false;
            }

            @Override
            public void endTransition(LayoutTransition layoutTransition,
                    ViewGroup viewGroup, View view, int i) {
                dragDropEnabled = true;
            }
        });
        return transition;
    }

    /**
     * Creates a dots-based mIndicator using two simple drawables.
     */
    private void createIndicator() {
        if (mIndicator.getChildCount() != 0) {
            mIndicator.removeAllViews();
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                mDeviceProfile.pageIndicatorSizePx, mDeviceProfile.pageIndicatorSizePx);

        for (int i = 0; i < pages.size() + 1; i++) {
            ImageView dot = new ImageView(this);
            dot.setImageDrawable(getDrawable(R.drawable.dot_off));
            dot.setLayoutParams(params);
            mIndicator.addView(dot);
        }
        updateIndicator();
    }

    private void updateIndicator() {
        if (mIndicator.getChildAt(activeDot) != null) {
            ((ImageView) mIndicator.getChildAt(activeDot)).setImageResource(R.drawable.dot_off);
        }
        if (mIndicator.getChildAt(currentPageNumber) != null) {
            ((ImageView) mIndicator.getChildAt(currentPageNumber)).setImageResource(
                    R.drawable.dot_on);
            activeDot = currentPageNumber;
        }
    }

    /**
     * Ensures that backpress is not allowed, except when the folder window is open.
     */
    @Override
    public void onBackPressed() {
        returnToHomeScreen();
    }

    /**
     * Hides folder window with an animation
     */
    private void hideFolderWindowContainer() {
        storage.save(pages, mDock);
        mFolderTitleInput.clearFocus();
        folderFromDock = false;
        mFolderWindowContainer.animate().alpha(0f)
                .setDuration(200).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mFolderWindowContainer.setVisibility(GONE);
                activeFolder = null;
                mFolderWindowContainer.animate().setListener(null);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        returnToHomeScreen();
    }

    private void returnToHomeScreen() {
        if (mSearchInput != null) {
            mSearchInput.setText("");
        }
        if (isWobbling) {
            handleWobbling(false);
        } else if (mFolderWindowContainer.getVisibility() == View.VISIBLE) {
            hideFolderWindowContainer();
        } else {
            mHorizontalPager.setCurrentPage(1);
        }
    }

    /**
     * Adapter for folder apps.
     */
    public class FolderAppsPagerAdapter extends PagerAdapter {

        private Context mContext;
        private List<AppItem> mFolderAppItems;

        public FolderAppsPagerAdapter(Context context) {
            this.mContext = context;
            this.mFolderAppItems = activeFolder.getSubApps();
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            GridLayout viewGroup = (GridLayout) LayoutInflater.from(mContext).inflate(
                    R.layout.apps_page, container, false);
            viewGroup.setRowCount(3);
            viewGroup.setColumnCount(3);
            viewGroup.setPadding(mDeviceProfile.iconDrawablePaddingPx / 2,
                    mDeviceProfile.iconDrawablePaddingPx / 2,
                    mDeviceProfile.iconDrawablePaddingPx / 2,
                    mDeviceProfile.iconDrawablePaddingPx / 2);
            int i = 0;
            while (9 * position + i < mFolderAppItems.size() && i < 9) {
                AppItem appItem = mFolderAppItems.get(9 * position + i);
                BlissFrameLayout appView = prepareApp(appItem, true);
                GridLayout.LayoutParams iconLayoutParams = new GridLayout.LayoutParams();
                iconLayoutParams.height = mDeviceProfile.cellHeightPx;
                iconLayoutParams.width = mDeviceProfile.cellWidthPx;
                appView.findViewById(R.id.app_label).setVisibility(View.VISIBLE);
                appView.setLayoutParams(iconLayoutParams);
                viewGroup.addView(appView);
                i++;
            }
            container.addView(viewGroup);
            return viewGroup;
        }

        @Override
        public int getCount() {
            return (int) Math.ceil((float) mFolderAppItems.size() / 9);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position,
                @NonNull Object object) {
            container.removeView((View) object);
        }
    }

    class ReorderAlarmListener implements Alarm.OnAlarmListener {

        private final ViewGroup mParent;
        private final int mIndex;
        private GridLayout mPage;

        public ReorderAlarmListener(GridLayout page, ViewGroup parent, int index) {
            mPage = page;
            mParent = parent;
            mIndex = index;
        }

        public void onAlarm(Alarm alarm) {
            GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
            if (movingApp.getParent() != null && (parentPage == getCurrentAppsPageNumber()
                    || gridLayout.getChildCount() < mDeviceProfile.maxAppsPerPage)) {
                ((ViewGroup) movingApp.getParent()).removeView(movingApp);
                if (gridLayout.getChildCount() < mDeviceProfile.maxAppsPerPage) {
                    addAppToGrid(gridLayout, movingApp, mIndex);
                    parentPage = getCurrentAppsPageNumber();
                }
            }
        }
    }

    class DockReorderAlarmListener implements Alarm.OnAlarmListener {
        private final int mIndex;

        DockReorderAlarmListener(int index) {
            mIndex = index;
        }

        public void onAlarm(Alarm alarm) {
            if (mDock.getChildCount() < mDeviceProfile.numColumns
                    || parentPage == -99) {
                if (movingApp.getParent() != null) {
                    ((ViewGroup) movingApp.getParent()).removeView(movingApp);
                }
                parentPage = -99;
                addAppToDock(movingApp, mIndex);
            }

        }
    }
}
