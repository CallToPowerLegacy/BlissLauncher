package org.indin.blisslaunchero.features.launcher;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.usage.UsageStats;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
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
import android.os.Process;
import android.os.StrictMode;
import android.os.UserManager;
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
import android.util.Log;
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
import org.indin.blisslaunchero.core.Alarm;
import org.indin.blisslaunchero.core.DeviceProfile;
import org.indin.blisslaunchero.core.Preferences;
import org.indin.blisslaunchero.core.Utilities;
import org.indin.blisslaunchero.core.customviews.BlissDragShadowBuilder;
import org.indin.blisslaunchero.core.customviews.BlissFrameLayout;
import org.indin.blisslaunchero.core.customviews.BlissInput;
import org.indin.blisslaunchero.core.customviews.DockGridLayout;
import org.indin.blisslaunchero.core.customviews.HorizontalPager;
import org.indin.blisslaunchero.core.customviews.PageIndicatorLinearLayout;
import org.indin.blisslaunchero.core.customviews.RoundedWidgetView;
import org.indin.blisslaunchero.core.customviews.SquareFrameLayout;
import org.indin.blisslaunchero.core.customviews.SquareImageView;
import org.indin.blisslaunchero.core.customviews.WidgetHost;
import org.indin.blisslaunchero.core.database.DatabaseManager;
import org.indin.blisslaunchero.core.database.LauncherDB;
import org.indin.blisslaunchero.core.database.model.ApplicationItem;
import org.indin.blisslaunchero.core.database.model.CalendarIcon;
import org.indin.blisslaunchero.core.database.model.FolderItem;
import org.indin.blisslaunchero.core.database.model.LauncherItem;
import org.indin.blisslaunchero.core.database.model.ShortcutItem;
import org.indin.blisslaunchero.core.events.AppAddEvent;
import org.indin.blisslaunchero.core.events.AppChangeEvent;
import org.indin.blisslaunchero.core.events.AppRemoveEvent;
import org.indin.blisslaunchero.core.events.ShortcutAddEvent;
import org.indin.blisslaunchero.core.executors.AppExecutors;
import org.indin.blisslaunchero.core.network.RetrofitService;
import org.indin.blisslaunchero.core.utils.AppUtils;
import org.indin.blisslaunchero.core.utils.Constants;
import org.indin.blisslaunchero.core.utils.GraphicsUtil;
import org.indin.blisslaunchero.core.utils.ListUtil;
import org.indin.blisslaunchero.features.notification.NotificationRepository;
import org.indin.blisslaunchero.features.notification.NotificationService;
import org.indin.blisslaunchero.features.shortcuts.DeepShortcutManager;
import org.indin.blisslaunchero.features.shortcuts.ShortcutKey;
import org.indin.blisslaunchero.features.suggestions.AutoCompleteAdapter;
import org.indin.blisslaunchero.features.suggestions.AutoCompleteService;
import org.indin.blisslaunchero.features.suggestions.AutoCompleteServiceResult;
import org.indin.blisslaunchero.features.usagestats.AppUsageStats;
import org.indin.blisslaunchero.features.weather.DeviceStatusService;
import org.indin.blisslaunchero.features.weather.ForecastBuilder;
import org.indin.blisslaunchero.features.weather.WeatherPreferences;
import org.indin.blisslaunchero.features.weather.WeatherSourceListenerService;
import org.indin.blisslaunchero.features.weather.WeatherUpdateService;
import org.indin.blisslaunchero.features.widgets.WidgetManager;
import org.indin.blisslaunchero.features.widgets.WidgetsActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import me.relex.circleindicator.CircleIndicator;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class LauncherActivity extends AppCompatActivity implements
        AutoCompleteAdapter.OnSuggestionClickListener {

    public static final int REORDER_TIMEOUT = 350;
    private final static int INVALID = -999;
    private static final int REQUEST_PERMISSION_CALL_PHONE = 14;
    private static final int REQUEST_LOCATION_SOURCE_SETTING = 267;
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
    private int parentPage = -99;
    private boolean folderFromDock;
    private boolean isWobbling = false;
    private CompositeDisposable mCompositeDisposable;
    private CountDownTimer mWobblingCountDownTimer;
    private List<BlissFrameLayout> mCalendarIcons = new ArrayList<>();
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
    private boolean forceRefreshSuggestedApps = false;

    private List<UsageStats> mUsageStats;

    private BroadcastReceiver mWeatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getBooleanExtra(WeatherUpdateService.EXTRA_UPDATE_CANCELLED, false)) {
                updateWeatherPanel();
            }
        }
    };
    private FolderItem activeFolder;
    private BlissFrameLayout activeFolderView;
    private int activeDot;
    private int statusBarHeight;
    private List<LauncherItem> mLauncherItems;

    private static final String TAG = "LauncherActivity";
    private TextView openUsageAccessTextView;
    private AppWidgetManager mAppWidgetManager;
    private WidgetHost mAppWidgetHost;
    private LinearLayout widgetContainer;

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BlissLauncher.getApplication(this).initAppProvider();

        prepareBroadcastReceivers();

        mDeviceProfile = BlissLauncher.getApplication(this).getDeviceProfile();

        mAppWidgetManager = BlissLauncher.getApplication(this).getAppWidgetManager();
        mAppWidgetHost = BlissLauncher.getApplication(this).getAppWidgetHost();

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
                        .subscribeWith(new DisposableObserver<List<LauncherItem>>() {
                            @Override
                            public void onNext(List<LauncherItem> launcherItems) {
                                if (launcherItems.size() <= 0) {
                                    BlissLauncher.getApplication(
                                            LauncherActivity.this).getAppProvider().reload();
                                } else {
                                    if (!allAppsDisplayed) {
                                        mLauncherItems = launcherItems;
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
        for (BlissFrameLayout blissIcon : mCalendarIcons) {
            CalendarIcon calendarIcon = new CalendarIcon(
                    blissIcon.findViewById(R.id.calendar_month_textview),
                    blissIcon.findViewById(R.id.calendar_date_textview));
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

        if (suggestedAppsGridLayout != null) {
            refreshSuggestedApps(forceRefreshSuggestedApps);
        }

        WidgetManager widgetManager = WidgetManager.getInstance();
        Integer id = widgetManager.dequeRemoveId();
        while (id != null) {
            for (int i = 0; i < widgetContainer.getChildCount(); i++) {
                if (widgetContainer.getChildAt(i) instanceof RoundedWidgetView) {
                    RoundedWidgetView appWidgetHostView =
                            (RoundedWidgetView) widgetContainer.getChildAt(i);
                    if (appWidgetHostView.getAppWidgetId() == id) {
                        widgetContainer.removeViewAt(i);
                        break;
                    }
                }
            }
            id = widgetManager.dequeRemoveId();
        }

        RoundedWidgetView widgetView = widgetManager.dequeAddWidgetView();
        while (widgetView != null) {
            int appWidgetId = widgetView.getAppWidgetId();
            AppWidgetProviderInfo info = widgetView.getAppWidgetInfo();
            widgetView.post(() -> updateWidgetOption(appWidgetId, info));
            addWidgetToContainer(widgetContainer, widgetView);
            widgetView = widgetManager.dequeAddWidgetView();
        }
    }

    private void addWidgetToContainer(LinearLayout widgetHolderLinearLayout,
            RoundedWidgetView widgetView) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = getResources().getDimensionPixelSize(R.dimen.widget_margin);
        layoutParams.setMargins(0, margin, 0, margin);
        widgetView.setLayoutParams(layoutParams);
        widgetView.setPadding(0, 0, 0, 0);
        widgetHolderLinearLayout.addView(widgetView);
        widgetView.setOnTouchListener((v, event) -> {
            v.getParent().getParent().getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
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
        ApplicationItem applicationItem = AppUtils.createAppItem(this, appAddEvent.packageName);
        addLauncherItem(applicationItem);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAppRemoveEvent(AppRemoveEvent appRemoveEvent) {
        forceRefreshSuggestedApps = true;
        removePackageFromLauncher(appRemoveEvent.packageName);
        DatabaseManager.getManager(this).saveLayouts(pages, mDock);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAppChangeEvent(AppChangeEvent appChangeEvent) {
        updateApp(appChangeEvent.packageName);
        DatabaseManager.getManager(this).saveLayouts(pages, mDock);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShortcutAddEvent(ShortcutAddEvent shortcutAddEvent) {
        updateOrAddShortcut(shortcutAddEvent.getShortcutItem());
        DatabaseManager.getManager(this).saveLayouts(pages, mDock);
        Toast.makeText(this, "Shortcut has been added", Toast.LENGTH_SHORT).show();
    }

    private void addLauncherItem(LauncherItem launcherItem) {
        if (pages == null || pages.size() == 0) {
            return;
        }
        if (launcherItem != null) {
            BlissFrameLayout view = prepareLauncherItem(launcherItem);

            int current = 0;
            while ((current < pages.size() && pages.get(current).getChildCount()
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
            launcherItem.screenId = current;
            launcherItem.cell = pages.get(current).getChildCount() - 1;
            launcherItem.container = Constants.CONTAINER_DESKTOP;
            addAppToGrid(pages.get(current), view);
        }
    }

    private void updateOrAddShortcut(ShortcutItem shortcutItem) {
        if (mFolderWindowContainer.getVisibility() == View.VISIBLE) {
            for (int i = 0; i < mFolderAppsViewPager.getChildCount(); i++) {
                GridLayout gridLayout = (GridLayout) mFolderAppsViewPager.getChildAt(i);
                for (int j = 0; j < gridLayout.getChildCount(); j++) {
                    BlissFrameLayout viewGroup =
                            (BlissFrameLayout) gridLayout.getChildAt(j);
                    final LauncherItem existingItem = getAppDetails(viewGroup);
                    if (existingItem.itemType == Constants.ITEM_TYPE_SHORTCUT) {
                        ShortcutItem existingShortcutItem = (ShortcutItem) existingItem;
                        if (existingShortcutItem.id.equalsIgnoreCase(shortcutItem.id)) {
                            BlissFrameLayout blissFrameLayout = prepareLauncherItem(shortcutItem);
                            GridLayout.LayoutParams iconLayoutParams =
                                    new GridLayout.LayoutParams();
                            iconLayoutParams.height = mDeviceProfile.cellHeightPx;
                            iconLayoutParams.width = mDeviceProfile.cellWidthPx;
                            gridLayout.removeViewAt(j);
                            gridLayout.addView(blissFrameLayout, j, iconLayoutParams);
                            return;
                        }
                    }
                }
            }
        }

        for (int i = 0; i < mDock.getChildCount(); i++) {
            BlissFrameLayout viewGroup =
                    (BlissFrameLayout) mDock.getChildAt(i);
            LauncherItem launcherItem = getAppDetails(viewGroup);
            if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                FolderItem folderItem = (FolderItem) launcherItem;
                for (int k = 0; k < folderItem.items.size(); k++) {
                    if (folderItem.items.get(k).itemType == Constants.ITEM_TYPE_SHORTCUT) {
                        ShortcutItem existingShortcutItem = (ShortcutItem) folderItem.items.get(k);
                        if (existingShortcutItem.id.equalsIgnoreCase(
                                shortcutItem.id)) {
                            folderItem.items.set(k, shortcutItem);
                            folderItem.icon = new GraphicsUtil(this).generateFolderIcon(this,
                                    folderItem);
                            BlissFrameLayout blissFrameLayout = prepareLauncherItem(
                                    launcherItem);
                            mDock.removeViewAt(i);
                            addAppToDock(blissFrameLayout, i);
                            return;
                        }
                    }

                }
            } else {
                if (launcherItem.itemType == Constants.ITEM_TYPE_SHORTCUT) {
                    ShortcutItem existingShortcutItem = (ShortcutItem) launcherItem;
                    if (existingShortcutItem.id.equalsIgnoreCase(shortcutItem.id)) {
                        BlissFrameLayout blissFrameLayout = prepareLauncherItem(shortcutItem);
                        mDock.removeViewAt(i);
                        addAppToDock(blissFrameLayout, i);
                        return;
                    }
                }
            }
        }

        for (int i = 0; i < pages.size(); i++) {
            GridLayout gridLayout = pages.get(i);
            for (int j = 0; j < gridLayout.getChildCount(); j++) {
                BlissFrameLayout viewGroup =
                        (BlissFrameLayout) gridLayout.getChildAt(j);
                LauncherItem launcherItem = getAppDetails(viewGroup);
                if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                    FolderItem folderItem = (FolderItem) launcherItem;
                    for (int k = 0; k < folderItem.items.size(); k++) {
                        if (folderItem.items.get(k).itemType == Constants.ITEM_TYPE_SHORTCUT) {
                            ShortcutItem existingShortcutItem =
                                    (ShortcutItem) folderItem.items.get(k);
                            if (existingShortcutItem.id.equalsIgnoreCase(
                                    shortcutItem.id)) {
                                folderItem.items.set(k, shortcutItem);
                                folderItem.icon = new GraphicsUtil(this).generateFolderIcon(this,
                                        folderItem);
                                BlissFrameLayout blissFrameLayout = prepareLauncherItem(
                                        launcherItem);
                                gridLayout.removeViewAt(j);
                                addAppToGrid(gridLayout, blissFrameLayout, j);
                                return;
                            }
                        }

                    }
                } else {
                    if (launcherItem.itemType == Constants.ITEM_TYPE_SHORTCUT) {
                        ShortcutItem existingShortcutItem = (ShortcutItem) launcherItem;
                        if (existingShortcutItem.id.equalsIgnoreCase(shortcutItem.id)) {
                            BlissFrameLayout blissFrameLayout = prepareLauncherItem(shortcutItem);
                            gridLayout.removeViewAt(j);
                            addAppToGrid(gridLayout, blissFrameLayout, j);
                            return;
                        }
                    }
                }
            }
        }

        addLauncherItem(shortcutItem);
    }

    private void removePackageFromLauncher(String packageName) {
        Log.d(TAG, "removePackageFromLauncher() called with: packageName = [" + packageName + "]");
        handleWobbling(false);
        if (mFolderWindowContainer.getVisibility() == View.VISIBLE) {
            for (int i = 0; i < mFolderAppsViewPager.getChildCount(); i++) {
                GridLayout grid = (GridLayout) mFolderAppsViewPager.getChildAt(i);
                for (int j = 0; j < grid.getChildCount(); j++) {
                    LauncherItem launcherItem = getAppDetails(grid.getChildAt(j));
                    if (launcherItem.itemType == Constants.ITEM_TYPE_APPLICATION) {
                        ApplicationItem app = (ApplicationItem) launcherItem;
                        if (app.packageName.equals(packageName)) {
                            activeFolder.items.remove(app);
                        }
                    } else if (launcherItem.itemType == Constants.ITEM_TYPE_SHORTCUT) {
                        ShortcutItem shortcutItem = (ShortcutItem) launcherItem;
                        if (shortcutItem.packageName.equals(packageName)) {
                            activeFolder.items.remove(shortcutItem);
                        }
                    }
                }
            }
            updateFolder();
        }

        for (int j = 0; j < mDock.getChildCount(); j++) {
            LauncherItem appItem = getAppDetails(mDock.getChildAt(j));
            if (appItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                FolderItem folderItem = (FolderItem) appItem;
                for (LauncherItem item : folderItem.items) {
                    if (item.itemType == Constants.ITEM_TYPE_APPLICATION) {
                        ApplicationItem applicationItem = (ApplicationItem) item;
                        if (applicationItem.packageName.equalsIgnoreCase(packageName)) {
                            folderItem.items.remove(applicationItem);
                        }
                    } else if (item.itemType == Constants.ITEM_TYPE_SHORTCUT) {
                        ShortcutItem shortcutItem = (ShortcutItem) item;
                        if (shortcutItem.packageName.equalsIgnoreCase(packageName)) {
                            folderItem.items.remove(shortcutItem);
                        }
                    }
                }
                updateFolderInGrid(mDock, folderItem, j);
            } else if (appItem.itemType == Constants.ITEM_TYPE_APPLICATION) {
                ApplicationItem applicationItem = (ApplicationItem) appItem;
                if (applicationItem.packageName.equalsIgnoreCase(packageName)) {
                    mDock.removeViewAt(j);
                }
            } else if (appItem.itemType == Constants.ITEM_TYPE_SHORTCUT) {
                ShortcutItem shortcutItem = (ShortcutItem) appItem;
                if (shortcutItem.packageName.equalsIgnoreCase(packageName)) {
                    mDock.removeViewAt(j);
                }
            }
        }

        for (int i = 0; i < pages.size(); i++) {
            GridLayout grid = getGridFromPage(pages.get(i));
            for (int j = 0; j < grid.getChildCount(); j++) {
                LauncherItem launcherItem = getAppDetails(grid.getChildAt(j));
                if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                    FolderItem folderItem = (FolderItem) launcherItem;
                    for (LauncherItem item : folderItem.items) {
                        if (item.itemType == Constants.ITEM_TYPE_APPLICATION) {
                            ApplicationItem applicationItem = (ApplicationItem) item;
                            if (applicationItem.packageName.equalsIgnoreCase(packageName)) {
                                folderItem.items.remove(applicationItem);
                            }
                        } else if (item.itemType == Constants.ITEM_TYPE_SHORTCUT) {
                            ShortcutItem shortcutItem = (ShortcutItem) item;
                            if (shortcutItem.packageName.equalsIgnoreCase(packageName)) {
                                folderItem.items.remove(shortcutItem);
                            }
                        }
                    }
                    updateFolderInGrid(grid, folderItem, j);
                } else if (launcherItem.itemType == Constants.ITEM_TYPE_APPLICATION) {
                    ApplicationItem applicationItem = (ApplicationItem) launcherItem;
                    if (applicationItem.packageName.equalsIgnoreCase(packageName)) {
                        grid.removeViewAt(j);
                    }
                } else if (launcherItem.itemType == Constants.ITEM_TYPE_SHORTCUT) {
                    ShortcutItem shortcutItem = (ShortcutItem) launcherItem;
                    if (shortcutItem.packageName.equalsIgnoreCase(packageName)) {
                        grid.removeViewAt(j);
                    }
                }
            }
        }
    }

    private void updateFolder() {
        mFolderAppsViewPager.getAdapter().notifyDataSetChanged();
        if (activeFolder.items.size() == 0) {
            ((ViewGroup) activeFolderView.getParent()).removeView
                    (activeFolderView);
            hideFolderWindowContainer();
        } else {
            if (activeFolder.items.size() == 1) {
                LauncherItem item = activeFolder.items.get(0);
                activeFolder.items.remove(item);
                mFolderAppsViewPager.getAdapter().notifyDataSetChanged();
                BlissFrameLayout view = prepareLauncherItem(item);

                if (folderFromDock) {
                    addAppToDock(view, mDock.indexOfChild(activeFolderView));
                } else {
                    GridLayout gridLayout = pages.get
                            (getCurrentAppsPageNumber());
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

    private void updateFolderInGrid(GridLayout grid, FolderItem folderItem,
            int folderIndex) {
        if (folderItem.items.size() == 0) {
            grid.removeViewAt(folderIndex);
        } else {
            folderItem.icon = new GraphicsUtil(this).generateFolderIcon(this,
                    folderItem);
            BlissFrameLayout blissFrameLayout = prepareLauncherItem(folderItem);
            grid.removeViewAt(folderIndex);
            if (grid instanceof DockGridLayout) {
                addAppToDock(blissFrameLayout, folderIndex);
            } else {
                addAppToGrid(grid, blissFrameLayout, folderIndex);
            }
        }
    }

    private void updateApp(String packageName) {
        handleWobbling(false);
        ApplicationItem updatedAppItem = AppUtils.createAppItem(this, packageName);
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
                    final LauncherItem existingItem =
                            getAppDetails(viewGroup);
                    if (existingItem.itemType == Constants.ITEM_TYPE_APPLICATION) {
                        ApplicationItem existingAppItem = (ApplicationItem) existingItem;
                        if (existingAppItem.packageName.equalsIgnoreCase(packageName)) {
                            BlissFrameLayout blissFrameLayout = prepareLauncherItem(updatedAppItem);
                            GridLayout.LayoutParams iconLayoutParams =
                                    new GridLayout.LayoutParams();
                            iconLayoutParams.height = mDeviceProfile.cellHeightPx;
                            iconLayoutParams.width = mDeviceProfile.cellWidthPx;
                            gridLayout.removeViewAt(j);
                            gridLayout.addView(blissFrameLayout, j, iconLayoutParams);
                            return;
                        }
                    }

                }
            }
        }

        for (int i = 0; i < mDock.getChildCount(); i++) {
            BlissFrameLayout viewGroup =
                    (BlissFrameLayout) mDock.getChildAt(i);
            LauncherItem existingAppItem = getAppDetails(viewGroup);
            if (existingAppItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                FolderItem folderItem = (FolderItem) existingAppItem;
                for (int k = 0; k < folderItem.items.size(); k++) {
                    if (folderItem.items.get(k).itemType == Constants.ITEM_TYPE_APPLICATION) {
                        ApplicationItem applicationItem = (ApplicationItem) folderItem.items.get(k);
                        if (applicationItem.packageName.equalsIgnoreCase(
                                packageName)) {
                            folderItem.items.set(k, updatedAppItem);
                            folderItem.icon = new GraphicsUtil(this).generateFolderIcon(this,
                                    folderItem);
                            BlissFrameLayout blissFrameLayout = prepareLauncherItem(
                                    existingAppItem);
                            mDock.removeViewAt(i);
                            addAppToDock(blissFrameLayout, i);
                            return;
                        }
                    }

                }
            } else {
                if (existingAppItem.itemType == Constants.ITEM_TYPE_APPLICATION) {
                    ApplicationItem applicationItem = (ApplicationItem) existingAppItem;
                    if (applicationItem.packageName.equalsIgnoreCase(packageName)) {
                        BlissFrameLayout blissFrameLayout = prepareLauncherItem(updatedAppItem);
                        mDock.removeViewAt(i);
                        addAppToDock(blissFrameLayout, i);
                        return;
                    }
                }
            }
        }

        for (int i = 0; i < pages.size(); i++) {
            GridLayout gridLayout = pages.get(i);
            for (int j = 0; j < gridLayout.getChildCount(); j++) {
                BlissFrameLayout viewGroup =
                        (BlissFrameLayout) gridLayout.getChildAt(j);
                LauncherItem existingAppItem = getAppDetails(viewGroup);
                if (existingAppItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                    FolderItem folderItem = (FolderItem) existingAppItem;
                    for (int k = 0; k < folderItem.items.size(); k++) {
                        if (folderItem.items.get(k).itemType == Constants.ITEM_TYPE_APPLICATION) {
                            ApplicationItem applicationItem =
                                    (ApplicationItem) folderItem.items.get(k);
                            if (applicationItem.packageName.equalsIgnoreCase(
                                    packageName)) {
                                folderItem.items.set(k, updatedAppItem);
                                folderItem.icon = new GraphicsUtil(this).generateFolderIcon(this,
                                        folderItem);
                                BlissFrameLayout blissFrameLayout = prepareLauncherItem(
                                        existingAppItem);
                                gridLayout.removeViewAt(j);
                                addAppToGrid(gridLayout, blissFrameLayout, j);
                                return;
                            }
                        }

                    }
                } else {
                    if (existingAppItem.itemType == Constants.ITEM_TYPE_APPLICATION) {
                        ApplicationItem applicationItem = (ApplicationItem) existingAppItem;
                        if (applicationItem.packageName.equalsIgnoreCase(packageName)) {
                            BlissFrameLayout blissFrameLayout = prepareLauncherItem(updatedAppItem);
                            gridLayout.removeViewAt(j);
                            addAppToGrid(gridLayout, blissFrameLayout, j);
                            return;
                        }
                    }
                }
            }
        }

        addLauncherItem(updatedAppItem);
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
                    final LauncherItem appItem = getAppDetails(viewGroup);
                    if (appItem.itemType != Constants.ITEM_TYPE_SHORTCUT) {
                        updateBadgeToApp(viewGroup, appItem, appsWithNotifications, true);
                    }
                }
            }
        }
        for (int i = 0; i < pages.size(); i++) {
            GridLayout gridLayout = pages.get(i);
            for (int j = 0; j < gridLayout.getChildCount(); j++) {
                BlissFrameLayout viewGroup =
                        (BlissFrameLayout) gridLayout.getChildAt(j);
                final LauncherItem appItem = getAppDetails(viewGroup);
                if (appItem.itemType != Constants.ITEM_TYPE_SHORTCUT) {
                    updateBadgeToApp(viewGroup, appItem, appsWithNotifications, true);
                }
            }
        }

        for (int i = 0; i < mDock.getChildCount(); i++) {
            BlissFrameLayout viewGroup =
                    (BlissFrameLayout) mDock.getChildAt(i);
            final LauncherItem appItem = getAppDetails(viewGroup);
            if (appItem.itemType != Constants.ITEM_TYPE_SHORTCUT) {
                updateBadgeToApp(viewGroup, appItem, appsWithNotifications, false);
            }
        }
    }

    private void updateBadgeToApp(BlissFrameLayout viewGroup, LauncherItem appItem,
            Set<String> appsWithNotifications, boolean withText) {
        if (appItem != null) {
            if (appItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                viewGroup.applyBadge(checkHasApp((FolderItem) appItem, appsWithNotifications),
                        withText);
            } else {
                ApplicationItem applicationItem = (ApplicationItem) appItem;
                String pkgName = applicationItem.packageName;
                viewGroup.applyBadge(appsWithNotifications.contains(pkgName), withText);
            }
        }
    }

    private boolean checkHasApp(FolderItem appItem, Set<String> packages) {
        for (LauncherItem item : appItem.items) {
            if (item.itemType == Constants.ITEM_TYPE_APPLICATION) {
                ApplicationItem applicationItem = (ApplicationItem) item;
                if (packages.contains(applicationItem.packageName)) {
                    return true;
                }
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
        activeFolder.title = updatedTitle;
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

                        refreshSuggestedApps(forceRefreshSuggestedApps);
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

    private void refreshSuggestedApps(boolean forceRefresh) {
        AppUsageStats appUsageStats = new AppUsageStats(this);
        List<UsageStats> usageStats = appUsageStats.getUsageStats();
        if (usageStats.size() > 0) {
            openUsageAccessTextView.setVisibility(GONE);
            suggestedAppsGridLayout.setVisibility(VISIBLE);

            // Check if usage stats have been changed or not to avoid unnecessary flickering
            if (forceRefresh || mUsageStats == null || mUsageStats.size() != usageStats.size()
                    || !ListUtil.areEqualLists(mUsageStats, usageStats)) {
                if (forceRefresh) forceRefresh = false;
                mUsageStats = usageStats;
                if (suggestedAppsGridLayout != null
                        && suggestedAppsGridLayout.getChildCount() > 0) {
                    suggestedAppsGridLayout.removeAllViews();
                }
                int i = 0;
                while (suggestedAppsGridLayout.getChildCount() < 4 && i < mUsageStats.size()) {
                    ApplicationItem appItem = AppUtils.createAppItem(this,
                            mUsageStats.get(i).getPackageName());
                    if (appItem != null) {
                        BlissFrameLayout view = prepareSuggestedApp(appItem);
                        addAppToGrid(suggestedAppsGridLayout, view);
                    }
                    i++;
                }
            }
        } else {
            openUsageAccessTextView.setVisibility(VISIBLE);
            suggestedAppsGridLayout.setVisibility(GONE);
        }
    }

    /**
     * Populates the pages and the mDock for the first time.
     */
    private void createUI() {
        mHorizontalPager.setUiCreated(false);
        mDock.setEnabled(false);

        pages = new ArrayList<>();

        int hotseatCell = 0;

        // Prepare first screen of workspace here.
        GridLayout workspaceScreen = preparePage();
        pages.add(workspaceScreen);

        for (int i = 0; i < mLauncherItems.size(); i++) {
            LauncherItem launcherItem = mLauncherItems.get(i);
            BlissFrameLayout appView = prepareLauncherItem(launcherItem);
            if (launcherItem.container == Constants.CONTAINER_HOTSEAT) {
                addAppToDock(appView, launcherItem.cell);
                if (launcherItem.cell == -1) {
                    launcherItem.cell = hotseatCell;
                    hotseatCell++;
                } else {
                    hotseatCell = launcherItem.cell;
                }
            } else if (launcherItem.container == Constants.CONTAINER_DESKTOP) {
                if (workspaceScreen.getChildCount() >= mDeviceProfile.maxAppsPerPage
                        || launcherItem.screenId > pages.size() - 1) {
                    workspaceScreen = preparePage();
                    pages.add(workspaceScreen);
                }
                launcherItem.screenId = pages.size() - 1;
                launcherItem.cell = workspaceScreen.getChildCount();
                addAppToGrid(workspaceScreen, appView);
            }
        }
        for (int i = 0; i < pages.size(); i++) {
            mHorizontalPager.addView(pages.get(i));
        }
        currentPageNumber = 0;

        mHorizontalPager.setUiCreated(true);
        new Thread(() -> LauncherDB.getDatabase(this).launcherDao().insertAll(
                mLauncherItems)).start();
        mDock.setEnabled(true);
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
        widgetContainer = layout.findViewById(R.id.widget_container);
        layout.setPadding(0,
                (int) (statusBarHeight + Utilities.pxFromDp(8, this)),
                0, 0);
        mHorizontalPager.addView(layout, 0);
        layout.setOnDragListener(null);
        currentPageNumber = 1;
        mHorizontalPager.setCurrentPage(currentPageNumber);

        layout.findViewById(R.id.used_apps_layout).setClipToOutline(true);

        // Prepare app suggestions view
        // [[BEGIN]]
        suggestedAppsGridLayout = layout.findViewById(R.id.suggestedAppGrid);
        openUsageAccessTextView = layout.findViewById(R.id.openUsageAccessSettings);
        openUsageAccessTextView.setOnClickListener(
                view -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));

        // divided by 2 because of left and right padding.
        int padding =
                (int) (mDeviceProfile.availableWidthPx / 2 - Utilities.pxFromDp(8, this)
                        - 2
                        * mDeviceProfile.cellWidthPx);
        suggestedAppsGridLayout.setPadding(padding, 0, padding, 0);
        // [[END]]

        // Prepare search suggestion view
        // [[BEGIN]]
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
        getCompositeDisposable().add(RxTextView.textChanges(mSearchInput)
                .debounce(300, TimeUnit.MILLISECONDS)
                .map(CharSequence::toString)
                .distinctUntilChanged()
                .switchMap(charSequence -> {
                    if (charSequence != null && charSequence.length() > 0) {
                        return searchForQuery(charSequence);
                    } else {
                        return Observable.just(
                                new AutoCompleteServiceResult(charSequence));
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<AutoCompleteServiceResult>() {
                    @Override
                    public void onNext(
                            AutoCompleteServiceResult autoCompleteServiceResults) {
                        if (autoCompleteServiceResults.type
                                == AutoCompleteServiceResult.TYPE_NETWORK_ITEM) {
                            List<String> suggestions = new ArrayList<>();
                            for (int i = 0;
                                    i < (autoCompleteServiceResults.networkItems.size() > 5 ? 5
                                            : autoCompleteServiceResults.networkItems.size());
                                    i++) {
                                suggestions.add(
                                        autoCompleteServiceResults.networkItems.get(i).getPhrase());
                            }
                            mSuggestionAdapter.updateSuggestions(suggestions,
                                    autoCompleteServiceResults.queryText);
                        } else if (autoCompleteServiceResults.type
                                == AutoCompleteServiceResult.TYPE_LAUNCHER_ITEM) {
                            suggestedAppsGridLayout.removeAllViews();
                            openUsageAccessTextView.setVisibility(GONE);
                            suggestedAppsGridLayout.setVisibility(VISIBLE);
                            for (LauncherItem launcherItem : autoCompleteServiceResults
                                    .getLauncherItems()) {
                                BlissFrameLayout blissFrameLayout = prepareSuggestedApp(
                                        launcherItem);
                                addAppToGrid(suggestedAppsGridLayout, blissFrameLayout);
                            }
                        } else {
                            refreshSuggestedApps(true);
                            mSuggestionAdapter.updateSuggestions(new ArrayList<>(),
                                    autoCompleteServiceResults.queryText);
                        }
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
        // [[END]]

        // Prepare edit widgets button
        findViewById(R.id.edit_widgets_button).setOnClickListener(
              view -> startActivity(new Intent(this, WidgetsActivity.class)));

        // Prepare weather widget view
        // [[BEGIN]]
        findViewById(R.id.weather_setting_imageview).setOnClickListener(
                v -> startActivity(new Intent(this, WeatherPreferences.class)));

        mWeatherSetupTextView = findViewById(R.id.weather_setup_textview);
        mWeatherPanel = findViewById(R.id.weather_panel);
        mWeatherPanel.setOnClickListener(v -> {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(
                    "foundation.e.weather");
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
            }
        });
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
        // [[END]]

        for (int id : mAppWidgetHost.getAppWidgetIds()) {
            AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(id);
            if(appWidgetInfo != null){
                RoundedWidgetView hostView = (RoundedWidgetView) mAppWidgetHost.createView(
                        getApplicationContext(), id,
                        appWidgetInfo);
                hostView.setAppWidget(id, appWidgetInfo);
                hostView.post(() -> updateWidgetOption(id, appWidgetInfo));
                addWidgetToContainer(widgetContainer, hostView);
            }
        }
    }

    private void updateWidgetOption(int appWidgetId, AppWidgetProviderInfo info) {
        Bundle newOps = new Bundle();
        newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, info.minWidth);
        newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, BlissLauncher.getApplication(this).getDeviceProfile().getMaxWidgetWidth());
        newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, info.minHeight);
        newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, BlissLauncher.getApplication(this).getDeviceProfile().getMaxWidgetHeight());
        mAppWidgetManager.updateAppWidgetOptions(appWidgetId, newOps);
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
        ForecastBuilder.buildLargePanel(this, mWeatherPanel,
                Preferences.getCachedWeatherInfo(this));
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
                    startActivityForResult(intent, REQUEST_LOCATION_SOURCE_SETTING);
                });
        builder.setNegativeButton(R.string.cancel, null);
        dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOCATION_SOURCE_SETTING) {
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
        Observable<AutoCompleteServiceResult> launcherItems = searchForLauncherItems(
                charSequence.toString()).subscribeOn(Schedulers.io());
        Observable<AutoCompleteServiceResult> networkItems = searchForNetworkItems(
                charSequence).subscribeOn(Schedulers.io());
        return launcherItems.mergeWith(networkItems);
    }

    private Observable<AutoCompleteServiceResult> searchForLauncherItems(
            CharSequence charSequence) {
        String query = charSequence.toString().toLowerCase();
        AutoCompleteServiceResult autoCompleteServiceResult = new AutoCompleteServiceResult(
                query);
        List<LauncherItem> launcherItems = new ArrayList<>();
        pages.parallelStream().forEach(gridLayout -> {
            for (int i = 0; i < gridLayout.getChildCount(); i++) {
                BlissFrameLayout blissFrameLayout = (BlissFrameLayout) gridLayout.getChildAt(i);
                LauncherItem launcherItem = blissFrameLayout.getLauncherItem();
                if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                    FolderItem folderItem = (FolderItem) launcherItem;
                    for (LauncherItem item : folderItem.items) {
                        if (item.title.toString().toLowerCase().contains(query)) {
                            launcherItems.add(item);
                        }
                    }
                } else if (launcherItem.title.toString().toLowerCase().contains(query)) {
                    launcherItems.add(launcherItem);
                }
            }
        });

        for (int i = 0; i < mDock.getChildCount(); i++) {
            BlissFrameLayout blissFrameLayout = (BlissFrameLayout) mDock.getChildAt(i);
            LauncherItem launcherItem = blissFrameLayout.getLauncherItem();
            if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                FolderItem folderItem = (FolderItem) launcherItem;
                for (LauncherItem item : folderItem.items) {
                    if (item.title.toString().toLowerCase().contains(query)) {
                        launcherItems.add(item);
                    }
                }
            } else if (launcherItem.title.toString().toLowerCase().contains(query)) {
                launcherItems.add(launcherItem);
            }
        }

        launcherItems.sort(Comparator.comparing(launcherItem ->
                launcherItem.title.toString().toLowerCase().indexOf(query)
        ));

        if (launcherItems.size() > 4) {
            autoCompleteServiceResult.setLauncherItems(launcherItems.subList(0, 4));
        } else {
            autoCompleteServiceResult.setLauncherItems(launcherItems);
        }
        return Observable.just(autoCompleteServiceResult)
                .onErrorReturn(throwable -> {
                    autoCompleteServiceResult.setLauncherItems(new ArrayList<>());
                    return autoCompleteServiceResult;
                });
    }

    private Observable<AutoCompleteServiceResult> searchForNetworkItems(CharSequence charSequence) {
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
                    .map(autoCompleteServiceRawResults -> {
                        AutoCompleteServiceResult result = new AutoCompleteServiceResult(query);
                        result.setNetworkItems(autoCompleteServiceRawResults);
                        return result;
                    });
        } else {
            return Observable.just(new AutoCompleteServiceResult(query));
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
        startActivity(intent);
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
     * Adds networkItems to the mDock making sure that the GridLayout's parameters are
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
        if (index == LauncherItem.INVALID_CELL) {
            mDock.addView(view);
        } else {
            mDock.addView(view, index);
        }
    }

    /**
     * Converts an AppItem into a View object that can be rendered inside
     * the pages and the mDock.
     * <p>
     * The View object also has all the required listeners attached to it.
     */
    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    private BlissFrameLayout prepareLauncherItem(final LauncherItem launcherItem) {
        final BlissFrameLayout v = (BlissFrameLayout) getLayoutInflater().inflate(R.layout.app_view,
                null);
        v.setLauncherItem(launcherItem);
        final SquareFrameLayout icon = v.findViewById(R.id.app_icon);
        if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
            v.applyBadge(checkHasApp((FolderItem) launcherItem, mAppsWithNotifications),
                    launcherItem.container != Constants.CONTAINER_HOTSEAT);
        } else if (launcherItem.itemType == Constants.ITEM_TYPE_APPLICATION) {
            ApplicationItem applicationItem = (ApplicationItem) launcherItem;
            v.applyBadge(mAppsWithNotifications.contains(applicationItem.packageName),
                    launcherItem.container != Constants.CONTAINER_HOTSEAT);
        }

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

            if (launcherItem.itemType != Constants.ITEM_TYPE_FOLDER) {
                startActivitySafely(getApplicationContext(), launcherItem, view);
            } else {
                folderFromDock = !(v.getParent().getParent() instanceof HorizontalPager);
                displayFolder((FolderItem) launcherItem, v);
            }
        });

        return v;
    }

    private BlissFrameLayout prepareSuggestedApp(final LauncherItem launcherItem) {
        final BlissFrameLayout v = (BlissFrameLayout) getLayoutInflater().inflate(R.layout.app_view,
                null);
        v.setLauncherItem(launcherItem);
        final SquareFrameLayout icon = v.findViewById(R.id.app_icon);

        if (launcherItem.itemType == Constants.ITEM_TYPE_APPLICATION) {
            v.applyBadge(mAppsWithNotifications.contains(
                    ((ApplicationItem) launcherItem).packageName), true);
        }
        icon.setOnClickListener(view -> startActivitySafely(this, launcherItem, view));
        return v;
    }

    private void startActivitySafely(Context context, LauncherItem launcherItem, View v) {
        Intent intent = launcherItem.getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Bundle bundle = ActivityOptions.makeCustomAnimation(
                context, R.anim.enter, R.anim.leave).toBundle();
        if (v != null) {
            intent.setSourceBounds(getViewBounds(v));
        }

        if (launcherItem.itemType == Constants.ITEM_TYPE_SHORTCUT) {
            startShortcutIntentSafely(context, intent, bundle, launcherItem);
        } else {
            context.startActivity(intent, bundle);
        }
    }

    private void startShortcutIntentSafely(Context context, Intent intent,
            Bundle optsBundle, LauncherItem appItem) {
        try {
            StrictMode.VmPolicy oldPolicy = StrictMode.getVmPolicy();
            try {
                // Temporarily disable deathPenalty on all default checks. For eg, shortcuts
                // containing file Uri's would cause a crash as penaltyDeathOnFileUriExposure
                // is enabled by default on NYC.
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
                        .penaltyLog().build());

                if (appItem.itemType == Constants.ITEM_TYPE_SHORTCUT) {
                    if (Utilities.ATLEAST_OREO) {
                        String id = appItem.id;
                        String packageName = intent.getPackage();
                        DeepShortcutManager.getInstance(context).startShortcut(
                                packageName, id, intent.getSourceBounds(), optsBundle,
                                Process.myUserHandle());
                    } else {
                        context.startActivity(intent, optsBundle);
                    }

                } else {
                    // Could be launching some bookkeeping activity
                    context.startActivity(intent, optsBundle);
                }
            } finally {
                StrictMode.setVmPolicy(oldPolicy);
            }
        } catch (SecurityException e) {
            // Due to legacy reasons, direct call shortcuts require Launchers to have the
            // corresponding permission. Show the appropriate permission prompt if that
            // is the case.
            if (intent.getComponent() == null
                    && Intent.ACTION_CALL.equals(intent.getAction())
                    && context.checkSelfPermission(Manifest.permission.CALL_PHONE) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CALL_PHONE},
                        REQUEST_PERMISSION_CALL_PHONE);
            } else {
                // No idea why this was thrown.
                throw e;
            }
        }
    }

    private Rect getViewBounds(View v) {
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        return new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight());
    }

    private void displayFolder(FolderItem app, BlissFrameLayout v) {

        activeFolder = app;
        activeFolderView = v;

        mFolderWindowContainer.setAlpha(0f);
        mFolderWindowContainer.setVisibility(View.VISIBLE);
        mFolderWindowContainer.animate().alpha(1.0f).setDuration(200);

        mFolderTitleInput.setText(app.title);
        mFolderTitleInput.setCursorVisible(false);

        mFolderAppsViewPager.setAdapter(new FolderAppsPagerAdapter(this, app.items));
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
    }

    /**
     * Toggle the wobbling animation.
     */
    private void toggleWobbleAnimation(GridLayout gridLayout, boolean shouldPlayAnimation) {
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            BlissFrameLayout blissFrameLayout = (BlissFrameLayout) gridLayout.getChildAt(i);
            makeAppWobble(blissFrameLayout, shouldPlayAnimation, i);
        }
    }

    private void makeAppWobble(BlissFrameLayout blissFrameLayout, boolean shouldPlayAnimation,
            int i) {
        UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        Bundle restrictions = userManager.getUserRestrictions();
        boolean uninstallDisabled = restrictions.getBoolean(UserManager.DISALLOW_APPS_CONTROL,
                false)
                || restrictions.getBoolean(UserManager.DISALLOW_UNINSTALL_APPS, false);
        if (shouldPlayAnimation) {
            if (blissFrameLayout.getAnimation() == null) {
                ImageView imageView = blissFrameLayout.findViewById(R.id.uninstall_app);
                if (imageView == null) {
                    if (!uninstallDisabled) {
                        new Handler(Looper.getMainLooper()).post(
                                () -> addUninstallIcon(blissFrameLayout));
                    }
                }

                if (i % 2 == 0) {
                    blissFrameLayout.startAnimation(wobbleAnimation);
                } else {
                    blissFrameLayout.startAnimation(wobbleReverseAnimation);
                }
            }
        } else {
            blissFrameLayout.setAnimation(null);
            new Handler(Looper.getMainLooper()).post(
                    () -> removeUninstallIcon(blissFrameLayout));
        }
    }

    private void removeUninstallIcon(BlissFrameLayout blissFrameLayout) {
        ImageView imageView = blissFrameLayout.findViewById(R.id.uninstall_app);
        if (imageView != null) {
            ((ViewGroup) imageView.getParent()).removeView(imageView);
        }
    }

    /**
     * Display uninstall icon while animating the view.
     */
    private void addUninstallIcon(BlissFrameLayout blissFrameLayout) {
        final LauncherItem launcherItem = getAppDetails(blissFrameLayout);
        if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
            return;
        }

        if (launcherItem.itemType == Constants.ITEM_TYPE_APPLICATION) {
            ApplicationItem applicationItem = (ApplicationItem) launcherItem;
            if (applicationItem.isSystemApp != ApplicationItem.FLAG_SYSTEM_UNKNOWN) {
                if ((applicationItem.isSystemApp & ApplicationItem.FLAG_SYSTEM_NO) == 0) {
                    return;
                }
            }
        }

        SquareFrameLayout appIcon = blissFrameLayout.findViewById(R.id.app_icon);
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
            if (launcherItem.itemType == Constants.ITEM_TYPE_APPLICATION) {
                ComponentName componentName = launcherItem.getTargetComponent();
                if (componentName == null) {
                    // System applications cannot be installed. For now, show a toast explaining
                    // that.
                    // We may give them the option of disabling apps this way.
                    Toast.makeText(this, "Can not uninstall this app", Toast.LENGTH_SHORT).show();
                } else {
                    Uri packageUri = Uri.fromParts("package", componentName.getPackageName(),
                            componentName.getClassName());
                    Intent i = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
                            .putExtra(Intent.EXTRA_USER, launcherItem.user);
                    startActivity(i);
                }
            } else if (launcherItem.itemType == Constants.ITEM_TYPE_SHORTCUT) {
                removeShortcutView((ShortcutItem) launcherItem, blissFrameLayout);
            }
        });
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                size + 2 * rightPadding, size + 2 * topPadding);
        layoutParams.gravity = Gravity.END | Gravity.TOP;
        blissFrameLayout.addView(imageView, layoutParams);
    }

    private void removeShortcutView(ShortcutItem shortcutItem, BlissFrameLayout blissFrameLayout) {
        ShortcutKey shortcutKey = ShortcutKey.fromItem(shortcutItem);
        AppExecutors.getInstance().diskIO().execute(
                () -> DeepShortcutManager.getInstance(this).unpinShortcut(shortcutKey));
        DatabaseManager.getManager(this).removeLauncherItem(shortcutItem.id);
        if (mFolderWindowContainer.getVisibility() == View.VISIBLE) {
            activeFolder.items.remove(shortcutItem);
            mFolderAppsViewPager.getAdapter().notifyDataSetChanged();
            blissFrameLayout.setAnimation(null);
            ((ViewGroup) blissFrameLayout.getParent()).removeView(blissFrameLayout);
            if (activeFolder.items.size() == 0) {
                ((ViewGroup) activeFolderView.getParent()).removeView
                        (activeFolderView);
                hideFolderWindowContainer();
            } else if (activeFolder.items.size() == 1) {
                LauncherItem item = activeFolder.items.get(0);
                activeFolder.items.remove(item);
                mFolderAppsViewPager.getAdapter().notifyDataSetChanged();
                BlissFrameLayout view = prepareLauncherItem(item);

                if (folderFromDock) {
                    addAppToDock(view, mDock.indexOfChild(activeFolderView));
                } else {
                    GridLayout gridLayout = pages.get
                            (getCurrentAppsPageNumber());
                    addAppToGrid(gridLayout, view,
                            gridLayout.indexOfChild(activeFolderView));
                }
                activeFolderView.setAnimation(null);
                ((ViewGroup) activeFolderView.getParent()).removeView(
                        activeFolderView);
                hideFolderWindowContainer();
            } else {
                updateIcon(activeFolderView, activeFolder,
                        new GraphicsUtil(this).generateFolderIcon(this, activeFolder),
                        folderFromDock);
                hideFolderWindowContainer();
            }
        } else {
            blissFrameLayout.setAnimation(null);
            ((ViewGroup) blissFrameLayout.getParent()).removeView(blissFrameLayout);
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
                    if (getAppDetails(movingApp).container != Constants.CONTAINER_DESKTOP
                            && getAppDetails(movingApp).container != Constants.CONTAINER_HOTSEAT) {
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

                        LauncherItem movingLauncherItem = movingApp.getLauncherItem();
                        if (movingLauncherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
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
                                createOrUpdateFolder(false);
                            } else {
                                createOrUpdateFolder(true);
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
                    if (getAppDetails(movingApp).container != Constants.CONTAINER_DESKTOP
                            && getAppDetails(movingApp).container != Constants.CONTAINER_HOTSEAT) {
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

                            if (getAppDetails(movingApp).itemType == Constants.ITEM_TYPE_FOLDER) {
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
                                createOrUpdateFolder(false);
                            } else {
                                createOrUpdateFolder(true);
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
                    DatabaseManager.getManager(LauncherActivity.this).saveLayouts(pages, mDock);
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
            LauncherItem app = getAppDetails(movingApp);
            activeFolder.items.remove(app);
            mFolderAppsViewPager.getAdapter().notifyDataSetChanged();
            assert app != null;
            app.container =
                    folderFromDock ? Constants.CONTAINER_HOTSEAT : Constants.CONTAINER_DESKTOP;
            app.screenId = folderFromDock ? -1 : currentPageNumber;

            if (activeFolder.items.size() == 0) {
                BlissFrameLayout view = prepareLauncherItem(app);
                if (folderFromDock) {
                    int index = mDock.indexOfChild(activeFolderView);
                    removeUninstallIcon(activeFolderView);
                    mDock.removeView(activeFolderView);
                    addAppToDock(view, index);
                    makeAppWobble(view, true, index);
                } else {
                    GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                    int index = gridLayout.indexOfChild(activeFolderView);
                    activeFolderView.setAnimation(null);
                    removeUninstallIcon(activeFolderView);
                    gridLayout.removeView(activeFolderView);
                    addAppToGrid(gridLayout, view, index);
                    makeAppWobble(view, true, index);
                }
                DatabaseManager.getManager(this).removeLauncherItem(activeFolder.id);
            } else {
                if (activeFolder.items.size() == 1) {
                    Log.i(TAG, "removeAppFromFolder: here");
                    LauncherItem item = activeFolder.items.get(0);
                    activeFolder.items.remove(item);
                    mFolderAppsViewPager.getAdapter().notifyDataSetChanged();
                    item.container =
                            folderFromDock ? Constants.CONTAINER_HOTSEAT
                                    : Constants.CONTAINER_DESKTOP;
                    item.screenId = folderFromDock ? -1 : currentPageNumber;
                    BlissFrameLayout view = prepareLauncherItem(item);
                    if (folderFromDock) {
                        int index = mDock.indexOfChild(activeFolderView);
                        activeFolderView.setAnimation(null);
                        removeUninstallIcon(activeFolderView);
                        mDock.removeView(activeFolderView);
                        addAppToDock(view, index);
                        makeAppWobble(view, true, index);
                    } else {
                        GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                        int index = gridLayout.indexOfChild(activeFolderView);
                        activeFolderView.setAnimation(null);
                        removeUninstallIcon(activeFolderView);
                        gridLayout.removeView(activeFolderView);
                        addAppToGrid(gridLayout, view, index);
                        makeAppWobble(view, true, index);
                    }
                    DatabaseManager.getManager(this).removeLauncherItem(activeFolder.id);
                } else {
                    updateIcon(activeFolderView, activeFolder,
                            new GraphicsUtil(this).generateFolderIcon(this, activeFolder),
                            folderFromDock);
                    activeFolderView.applyBadge(checkHasApp(activeFolder, mAppsWithNotifications),
                            !folderFromDock);
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
    private void createOrUpdateFolder(boolean fromDock) {
        int index;

        collidingApp.setAnimation(null);

        if (fromDock) {
            index = mDock.indexOfChild(collidingApp);
        } else {
            index = getGridFromPage(pages.get(getCurrentAppsPageNumber())).indexOfChild(
                    collidingApp);
        }

        LauncherItem app1 = getAppDetails(collidingApp);
        LauncherItem app2 = getAppDetails(movingApp);

        if (app1.itemType == Constants.ITEM_TYPE_FOLDER) {
            FolderItem folderItem = (FolderItem) app1;
            app2.container = Long.parseLong(folderItem.id);
            app2.screenId = -1;
            app2.cell = folderItem.items.size();
            folderItem.items.add(app2);
            updateIcon(collidingApp, app1,
                    new GraphicsUtil(this).generateFolderIcon(this, folderItem),
                    folderFromDock);
            collidingApp.applyBadge(checkHasApp(folderItem, mAppsWithNotifications), !fromDock);
            makeAppWobble(collidingApp, true,
                    index);
        } else {
            FolderItem folder = new FolderItem();
            folder.title = "Untitled";
            folder.id = String.valueOf(System.currentTimeMillis());
            folder.items = new ArrayList<>();
            app1.container = Long.parseLong(folder.id);
            app2.container = Long.parseLong(folder.id);
            app1.screenId = -1;
            app2.screenId = -1;
            app1.cell = folder.items.size();
            folder.items.add(app1);
            app2.cell = folder.items.size();
            folder.items.add(app2);
            Drawable folderIcon = new GraphicsUtil(this).generateFolderIcon(this,
                    app1.icon, app2.icon);
            folder.icon = folderIcon;
            BlissFrameLayout folderView = prepareLauncherItem(folder);
            makeAppWobble(collidingApp, false, index);
            ((ViewGroup) collidingApp.getParent()).removeView(collidingApp);
            if (fromDock) {
                addAppToDock(folderView, index);
            } else {
                addAppToGrid(pages.get(getCurrentAppsPageNumber()), folderView, index);
            }
            makeAppWobble(folderView, true, index);
        }

        if (movingApp.getParent() != null) {
            ((ViewGroup) movingApp.getParent()).removeView(movingApp);
        }

        makeAppCold(collidingApp, fromDock);
        makeAppCold(movingApp, fromDock);

        //DatabaseManager.getManager(LauncherActivity.this).saveLayouts(pages, mDock);
    }

    private void updateIcon(BlissFrameLayout appView, LauncherItem app, Drawable drawable,
            boolean folderFromDock) {
        app.icon = drawable;
        List<Object> tags = (List<Object>) appView.getTag();
        SquareImageView iv = (SquareImageView) tags.get(0);
        iv.setImageDrawable(drawable);
        //appView.applyBadge(checkHasApp(app, mAppsWithNotifications), !folderFromDock);
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
    private LauncherItem getAppDetails(View app) {
        if (app instanceof BlissFrameLayout) {
            return ((BlissFrameLayout) app).getLauncherItem();
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
        DatabaseManager.getManager(LauncherActivity.this).saveLayouts(pages, mDock);
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
        final boolean alreadyOnHome =
                ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                        != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        if (alreadyOnHome) {
            returnToHomeScreen();
        }
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
        private List<LauncherItem> mFolderAppItems;

        public FolderAppsPagerAdapter(Context context, List<LauncherItem> items) {
            this.mContext = context;
            this.mFolderAppItems = items;
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
                LauncherItem appItem = mFolderAppItems.get(9 * position + i);
                BlissFrameLayout appView = prepareLauncherItem(appItem);
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
