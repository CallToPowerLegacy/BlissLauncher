package org.indin.blisslaunchero.features.launcher;

import static android.view.View.GONE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.WallpaperManager;
import android.app.usage.UsageStats;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.widget.RxTextView;

import org.indin.blisslaunchero.R;
import org.indin.blisslaunchero.data.db.Storage;
import org.indin.blisslaunchero.data.model.AppItem;
import org.indin.blisslaunchero.data.model.CalendarIcon;
import org.indin.blisslaunchero.features.notification.NotificationRepository;
import org.indin.blisslaunchero.features.notification.NotificationService;
import org.indin.blisslaunchero.features.suggestions.AutoCompleteAdapter;
import org.indin.blisslaunchero.features.suggestions.AutoCompleteService;
import org.indin.blisslaunchero.features.suggestions.AutoCompleteServiceResult;
import org.indin.blisslaunchero.features.usagestats.AppUsageStats;
import org.indin.blisslaunchero.framework.Alarm;
import org.indin.blisslaunchero.framework.DeviceProfile;
import org.indin.blisslaunchero.framework.SystemDragDriver;
import org.indin.blisslaunchero.framework.customviews.BlissDragShadowBuilder;
import org.indin.blisslaunchero.framework.customviews.BlissFrameLayout;
import org.indin.blisslaunchero.framework.customviews.BlissInput;
import org.indin.blisslaunchero.framework.customviews.CustomAnalogClock;
import org.indin.blisslaunchero.framework.customviews.HorizontalPager;
import org.indin.blisslaunchero.framework.customviews.SquareFrameLayout;
import org.indin.blisslaunchero.framework.customviews.SquareImageView;
import org.indin.blisslaunchero.framework.network.RetrofitService;
import org.indin.blisslaunchero.framework.util.AppUtil;
import org.indin.blisslaunchero.framework.util.ConverterUtil;
import org.indin.blisslaunchero.framework.util.GraphicsUtil;
import org.indin.blisslaunchero.framework.util.IconPackUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
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
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import me.relex.circleindicator.CircleIndicator;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class LauncherActivity extends AppCompatActivity implements LauncherContract.View,
        AutoCompleteAdapter.OnSuggestionClickListener {

    private HorizontalPager mHorizontalPager;
    private DockGridLayout mDock;
    private PageIndicatorLinearLayout mIndicator;
    private ViewGroup mFolderWindowContainer;
    private ViewPager mFolderAppsViewPager;
    private BlissInput mBlissInput;
    private BlissInput mSearchInput;
    private View mProgressBar;

    private BroadcastReceiver installReceiver;
    private BroadcastReceiver uninstallReceiver;

    private List<AppItem> launchableApps;
    private List<AppItem> pinnedApps;

    private int currentPageNumber = 0;

    private float maxDistanceForFolderCreation;

    private final static int INVALID = -999;

    private List<GridLayout> pages;

    private Drawable hotBackground;
    private Drawable defaultBackground;
    private Drawable transparentBackground;

    private boolean dragDropEnabled = true;
    // Related to dragging, folder creation and reordering
    private static final int DRAG_MODE_NONE = 0;
    private static final int DRAG_MODE_CREATE_FOLDER = 1;
    private static final int DRAG_MODE_ADD_TO_FOLDER = 2;
    private static final int DRAG_MODE_REORDER = 3;
    private int mDragMode = DRAG_MODE_NONE;
    int mLastReorderX = -1;
    int mLastReorderY = -1;

    private boolean mCreateUserFolderOnDrop = false;
    private boolean mAddToExistingFolderOnDrop = false;
    private float mMaxDistanceForFolderCreation;

    private static final int SNAP_OFF_EMPTY_SCREEN_DURATION = 400;
    private static final int FADE_EMPTY_SCREEN_DURATION = 150;

    private static final int ADJACENT_SCREEN_DROP_DURATION = 300;

    // The screen id used for the empty screen always present to the right.
    public static final long EXTRA_EMPTY_SCREEN_ID = -201;
    // The is the first screen. It is always present, even if its empty.
    public static final long FIRST_SCREEN_ID = 0;

    /**
     * Target drop area calculated during last acceptDrop call.
     */
    int[] mTargetCell = new int[2];
    private int mDragOverX = -1;
    private int mDragOverY = -1;

    // Variables relating to the creation of user folders by hovering shortcuts over shortcuts
    private static final int FOLDER_CREATION_TIMEOUT = 0;
    public static final int REORDER_TIMEOUT = 350;
    private final Alarm mFolderCreationAlarm = new Alarm();
    private final Alarm mReorderAlarm = new Alarm();

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
    private boolean longPressed;
    private int x;
    private int y;

    private CompositeDisposable mCompositeDisposable;

    private Map<String, Integer> notificationCountMap = new HashMap<>();

    private CountDownTimer mWobblingCountDownTimer;
    private long longPressedAt;

    private List<CalendarIcon> mCalendarIcons = new ArrayList<>();
    private static final String TAG = "DesktopActivity";
    private Intent notificationServiceIntent;
    private BroadcastReceiver timeChangedReceiver;
    private boolean isUiDone = false;
    private Set<String> mAppsWithNotifications = new HashSet<>();

    private View mLauncherView;
    private LauncherContract.Presenter mPresenter;
    private DeviceProfile mDeviceProfile;
    private boolean mLongClickStartsDrag = true;
    private boolean isDragging;
    private RecyclerView mSuggestionRecyclerView;
    private AutoCompleteAdapter mSuggestionAdapter;
    private GridLayout suggestedAppsGridLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPresenter = new LauncherPresenter();
        storage = new Storage(getApplicationContext());
        mDeviceProfile = new DeviceProfile(this);
        storage = new Storage(getApplicationContext());

        super.onCreate(savedInstanceState);

        mLauncherView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        setupViews();
        setContentView(mLauncherView);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        prepareBroadcastReceivers();

        // Start NotificationService to add count badge to Icons
        notificationServiceIntent = new Intent(this, NotificationService.class);
        startService(notificationServiceIntent);
        mPresenter.attachView(this);
        mProgressBar.setVisibility(View.VISIBLE);
        mPresenter.loadApps(this);
    }

    private void setupViews() {
        mHorizontalPager = (HorizontalPager) mLauncherView.findViewById(R.id.pages_container);
        mDock = (DockGridLayout) mLauncherView.findViewById(R.id.dock);
        mIndicator = (PageIndicatorLinearLayout) mLauncherView.findViewById(R.id.page_indicator);
        mFolderWindowContainer = (ViewGroup) mLauncherView.findViewById(
                R.id.folder_window_container);
        mFolderAppsViewPager = (ViewPager) mLauncherView.findViewById(R.id.folder_apps);
        mBlissInput = (BlissInput) mLauncherView.findViewById(R.id.folder_title);
        mProgressBar = (ProgressBar) mLauncherView.findViewById(R.id.progressbar);

        maxDistanceForFolderCreation = (int) (0.33f * mDeviceProfile.iconSizePx);

        hotBackground = getResources().getDrawable(R.drawable.rounded_corners_icon_hot, null);
        defaultBackground = getResources().getDrawable(R.drawable.rounded_corners_icon, null);
        scrollCorner = mDeviceProfile.iconDrawablePaddingPx;
        wobbleAnimation = AnimationUtils.loadAnimation(this, R.anim.wobble);
        wobbleReverseAnimation = AnimationUtils.loadAnimation(this, R.anim.wobble_reverse);
        transparentBackground = getResources().getDrawable(R.drawable.transparent, null);

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

    /**
     * Creates broadcast receivers that detect new app installs,
     * and app uninstalls. Without these receivers, the launcher
     * may display outdated or non-existent data.
     */
    private void prepareBroadcastReceivers() {
        IntentFilter installFilter = new IntentFilter();
        installFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        installFilter.addDataScheme("package");

        IntentFilter uninstallFilter = new IntentFilter();
        uninstallFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        uninstallFilter.addDataScheme("package");

        installReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getData() != null) {
                    String packageName = intent.getData().toString();
                    if (packageName.contains(":")) {
                        packageName = packageName.split(":")[1];
                    }
                    addNewApp(packageName);
                }
            }
        };

        uninstallReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String packageName = intent.getData().getSchemeSpecificPart();
                removePackageFromLauncher(packageName);
            }
        };

        timeChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isUiDone) {
                    updateAllCalendarIcons(Calendar.getInstance());
                }
            }
        };
        IntentFilter timeIntentFilter = new IntentFilter(Intent.ACTION_TIME_CHANGED);
        timeIntentFilter.addAction(Intent.ACTION_DATE_CHANGED);

        //registerReceiver(alarmReceiver, new IntentFilter("DAY_CHANGED"));
        registerReceiver(installReceiver, installFilter);
        registerReceiver(uninstallReceiver, uninstallFilter);
        registerReceiver(timeChangedReceiver, timeIntentFilter);
    }

    private void removePackageFromLauncher(String packageName) {
        handleWobbling(false);

        if (mFolderWindowContainer.getVisibility() == View.VISIBLE) {
            Log.d(TAG,
                    "here");
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
                                addToDock(view, mDock.indexOfChild(activeFolderView));
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
                                    addToDock(view, mDock.indexOfChild(activeFolderView));
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

        for (int i = 0; i < pages.size(); i++) {
            GridLayout grid = getGridFromPage(pages.get(i));
            for (int j = 0; j < grid.getChildCount(); j++) {
                AppItem appItem = getAppDetails(grid.getChildAt(j));
                if (appItem.getPackageName().equals(packageName)) {
                    grid.removeViewAt(j);
                    launchableApps.remove(appItem);
                    break;
                }
            }
        }
    }

    private void updateAllCalendarIcons(Calendar calendar) {
        for (CalendarIcon calendarIcon : mCalendarIcons) {
            updateCalendarIcon(calendarIcon, calendar);
        }
    }

    private void updateCalendarIcon(CalendarIcon calendarIcon, Calendar calendar) {
        calendarIcon.monthTextView.setText(
                ConverterUtil.convertMonthToString(calendar.get(Calendar.MONTH)));
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
        overridePendingTransition(R.anim.reenter, R.anim.releave);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(installReceiver);
        unregisterReceiver(uninstallReceiver);
        unregisterReceiver(timeChangedReceiver);
        getCompositeDisposable().dispose();
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

        AppItem appItem = AppUtil.createAppItem(this, packageName);
        if (appItem != null) {
            BlissFrameLayout view = prepareApp(appItem, true);
            int current = getCurrentAppsPageNumber();
            while (current < pages.size() && pages.get(current).getChildCount()
                    == mDeviceProfile.maxAppsPerPage) {
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
        }
    }

    @Override
    public void showApps(List<AppItem> appItemList, List<AppItem> pinnedApps) {
        this.launchableApps = appItemList;
        this.pinnedApps = pinnedApps;
        showIconPackWallpaperFirstTime();
        mProgressBar.setVisibility(GONE);
        createUI();
        isUiDone = true;
        createPageChangeListener();
        createFolderTitleListener();
        createDragListener();
        createWidgetsPage();
        createIndicator();
        createOrUpdateBadgeCount();
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
                                Log.i(TAG, "onComplete: ");
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

    private void showIconPackWallpaperFirstTime() {
        if (storage.isWallpaperShown()) {
            return;
        }

        if (IconPackUtil.iconPackPresent) {

            Bitmap bmap2 = BitmapFactory.decodeStream(
                    getResources().openRawResource(+R.drawable.wall1));

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int height = metrics.heightPixels;
            int width = metrics.widthPixels;
            Bitmap bitmap = Bitmap.createScaledBitmap(bmap2, width, height, true);

            WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
            try {
                wallpaperManager.setBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
            storage.setWallpaperShown();
        }
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
        mBlissInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v);
            }
        });
        mBlissInput.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                updateFolderTitle();
            }
            return false;
        });
        mBlissInput.setOnClickListener(view -> mBlissInput.setCursorVisible(true));
        mFolderWindowContainer.setOnClickListener(view -> hideFolderWindowContainer());
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(
                Activity.INPUT_METHOD_SERVICE);
        assert inputMethodManager != null;
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void updateFolderTitle() {
        String updatedTitle = mBlissInput.getText().toString();
        activeFolder.setLabel(updatedTitle);
        List<Object> tags = (List<Object>) activeFolderView.getTag();
        ((TextView) tags.get(1)).setText(updatedTitle);
        mBlissInput.setText(updatedTitle);
        mBlissInput.setCursorVisible(false);
    }

    /**
     * Adds a scroll listener to the mHorizontalPager in order to keep the currentPageNumber
     * updated
     */
    private void createPageChangeListener() {
        mHorizontalPager.addOnScrollListener(new HorizontalPager.OnScrollListener() {
            @Override
            public void onScroll(int scrollX) {
                dragDropEnabled = false;
            }

            @Override
            public void onViewScrollFinished(int page) {
                currentPageNumber = page;

                // Remove mIndicator and mDock from widgets page, and make them
                // reappear when user swipes to the first apps page
                if (currentPageNumber == 0) {
                    mDock.animate().translationYBy(
                            ConverterUtil.dp2Px(105, LauncherActivity.this)).setDuration(
                            100).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mDock.setVisibility(GONE);
                        }
                    });

                    mIndicator.animate().alpha(0).setDuration(100).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mIndicator.setVisibility(GONE);
                        }
                    });

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
        });
    }

    private void refreshSuggestedApps() {
        AppUsageStats appUsageStats = new AppUsageStats(this);
        List<UsageStats> usageStats = appUsageStats.getUsageStats();

        if(usageStats.size()> 0){
            if(suggestedAppsGridLayout != null && suggestedAppsGridLayout.getChildCount()>0){
                suggestedAppsGridLayout.removeAllViews();
            }
            int i = 0;
            while (suggestedAppsGridLayout.getChildCount() < 4) {
                AppItem appItem = AppUtil.createAppItem(this, usageStats.get(i).getPackageName());
                if (appItem != null) {
                    BlissFrameLayout view = prepareApp(appItem, true);
                    addAppToGrid(suggestedAppsGridLayout, view);
                }
                i++;
            }
        }
    }

    /**
     * Populates the pages and the mDock for the first time.
     */
    private void createUI() {
        mHorizontalPager.setUiCreated(false);
        mDock.setEnabled(false);
        if (storage.isLayoutPresent()) {
            createUIFromStorage();
            mHorizontalPager.setUiCreated(true);
            mDock.setEnabled(true);
            return;
        }
        int nPages = (int) Math.ceil((float) launchableApps.size() / mDeviceProfile.maxAppsPerPage);
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
        for (int i = 0; i < pinnedApps.size(); i++) {
            BlissFrameLayout appView = prepareApp(pinnedApps.get(i), false);
            addToDock(appView, INVALID);
        }
        mHorizontalPager.setUiCreated(true);
        mDock.setEnabled(true);
        Log.i(TAG, "createUI: ");
    }

    private GridLayout preparePage() {
        GridLayout grid = (GridLayout) getLayoutInflater().inflate(R.layout.apps_page, null);
        grid.setRowCount(mDeviceProfile.numRows);
        grid.setLayoutTransition(getDefaultLayoutTransition());
        grid.setPadding(mDeviceProfile.iconDrawablePaddingPx / 2, 0,
                mDeviceProfile.iconDrawablePaddingPx / 2, 0);
        return grid;
    }

    /**
     * Re-creates the launcher layout based on the data stored in the shared-preferences.
     */
    private void createUIFromStorage() {
        Storage.StorageData storageData = storage.load();
        int nPages = storageData.getNPages();
        pages = new ArrayList<>();
        List<AppItem> storedItems = new ArrayList<>();
        List<AppItem> dockItems = new ArrayList<>();

        mDock.setLayoutTransition(getDefaultLayoutTransition());

        for (int i = 0; i < storageData.getNDocked(); i++) {
            try {
                JSONObject currentDockItemData = storageData.dock.getJSONArray(0).getJSONObject(i);
                AppItem appItem = prepareAppFromJSON(currentDockItemData);
                if (appItem != null) {
                    dockItems.add(appItem);
                    BlissFrameLayout appView = prepareApp(appItem, false);
                    if (appView != null) {
                        addToDock(appView, INVALID);
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
                            storedItems.addAll(appItem.getSubApps());
                        } else {
                            storedItems.add(appItem);
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
        }

        launchableApps.removeAll(storedItems);
        launchableApps.removeAll(dockItems);

        for (int i = 0; i < launchableApps.size(); i++) {
            if (pages.get(pages.size() - 1).getChildCount() < mDeviceProfile.maxAppsPerPage) {
                BlissFrameLayout appView = prepareApp(launchableApps.get(i), true);
                if (appView != null) {
                    addAppToGrid(pages.get(pages.size() - 1), appView);
                }
            } else {
                pages.add(preparePage());
                BlissFrameLayout appView = prepareApp(launchableApps.get(i), true);
                if (appView != null) {
                    addAppToGrid(pages.get(pages.size() - 1), appView);
                }
            }
        }
        /*// Always keep an extra empty page handy
        if (pages.get(pages.size() - 1).getChildCount() > 2) {
            pages.add(preparePage());
        }*/

        for (int i = 0; i < pages.size(); i++) {
            mHorizontalPager.addView(pages.get(i));
        }

        currentPageNumber = 0;
    }

    private void createWidgetsPage() {
        ScrollView layout = (ScrollView) getLayoutInflater().inflate(R.layout.widgets_page,
                mHorizontalPager, false);
        mHorizontalPager.addView(layout, 0);
        currentPageNumber = 1;
        mHorizontalPager.setCurrentPage(currentPageNumber);
        mSearchInput = (BlissInput) layout.findViewById(R.id.search_input);
        mSuggestionRecyclerView = layout.findViewById(R.id.suggestionRecyclerView);
        mSuggestionAdapter = new AutoCompleteAdapter(this);
        mSuggestionRecyclerView.setHasFixedSize(true);
        mSuggestionRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mSuggestionRecyclerView.setAdapter(mSuggestionAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL);
        mSuggestionRecyclerView.addItemDecoration(dividerItemDecoration);
        layout.setOnDragListener(null);
        ImageView clearSuggestions = layout.findViewById(R.id.clearSuggestionImageView);
        clearSuggestions.setOnClickListener(v -> {
            mSearchInput.setText("");
            mSearchInput.clearFocus();
        });

        layout.findViewById(R.id.used_apps_layout).setClipToOutline(true);

        suggestedAppsGridLayout = layout.findViewById(R.id.suggestedAppGrid);
        AppUsageStats appUsageStats = new AppUsageStats(this);
        List<UsageStats> usageStats = appUsageStats.getUsageStats();
        if(usageStats.size()> 0){
            int i = 0;
            while (suggestedAppsGridLayout.getChildCount() < 4) {
                AppItem appItem = AppUtil.createAppItem(this, usageStats.get(i).getPackageName());
                if (appItem != null) {
                    BlissFrameLayout view = prepareApp(appItem, true);
                    addAppToGrid(suggestedAppsGridLayout, view);
                }
                i++;
            }
        }

        RxTextView.textChanges(mSearchInput)
                .debounce(300, TimeUnit.MILLISECONDS)
                .map(charSequence -> charSequence.toString())
                .distinctUntilChanged()
                .switchMap(
                        (Function<CharSequence,
                                ObservableSource<AutoCompleteServiceResult>>)
                                charSequence1 -> {
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
                        Log.d(TAG, "onNext() called with: autoCompleteServiceResults = ["
                                + autoCompleteServiceResults + "]");
                        for (int i = 0; i < (autoCompleteServiceResults.items.size() > 5 ? 5
                                : autoCompleteServiceResults.items.size()); i++) {
                            suggestions.add(autoCompleteServiceResults.items.get(i).getPhrase());
                        }
                        mSuggestionAdapter.updateSuggestions(suggestions,
                                autoCompleteServiceResults.queryText);
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });

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
    }

    private ObservableSource<AutoCompleteServiceResult> searchForQuery(
            CharSequence charSequence) {
        Log.d(TAG, "searchForQuery() called with: charSequence = [" + charSequence + "]");

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
        Uri uri = Uri.parse("https://spot.eelo.me/?q=" + query
                + "&time_range=&language=en-US&category_general=on&category_images=on"
                + "&category_videos=on");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        AppUtil.startActivityWithAnimation(this, intent);
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
     * Creates a View that can be displayed by the launcher using just stored
     * JSON data.
     */
    private AppItem prepareAppFromJSON(JSONObject currentItemData) throws Exception {
        String componentName = currentItemData.getString("componentName");
        if (currentItemData.getBoolean("isFolder")) {
            AppItem folderItem = new AppItem(currentItemData.getString("folderName"),
                    "",
                    getDrawable(R.mipmap.ic_folder),
                    null,
                    "FOLDER",
                    false,
                    true,
                    false,
                    false,
                    true);
            folderItem.setFolder(true);
            folderItem.setFolderID(currentItemData.getString("folderID"));
            JSONArray subAppData = currentItemData.getJSONArray("subApps");

            // Ignore empty folders
            if (subAppData.length() == 0) {
                return null;
            }

            for (int k = 0; k < subAppData.length(); k++) {
                AppItem appItem = prepareAppItemFromComponent(subAppData.getString(k));
                if (appItem != null) {
                    appItem.setBelongsToFolder(true);
                    folderItem.getSubApps().add(appItem);
                }
            }

            folderItem.setIcon(new GraphicsUtil(this).generateFolderIcon(this, folderItem));
            folderItem.setIcon(new GraphicsUtil(this).generateFolderIcon(this, folderItem));
            return folderItem;
        } else {
            return prepareAppItemFromComponent(componentName);
        }
    }

    private AppItem prepareAppItemFromComponent(String componentName) {
        for (int i = 0; i < launchableApps.size(); i++) {
            if (launchableApps.get(i).getComponentName().equals(componentName)) {
                return launchableApps.get(i);
            }
        }
        for (int i = 0; i < pinnedApps.size(); i++) {
            if (pinnedApps.get(i).getComponentName().equals(componentName)) {
                return pinnedApps.get(i);
            }
        }
        return null;
    }

    /**
     * Converts an AppItem into a View object that can be rendered inside
     * the pages and the mDock.
     *
     * The View object also has all the required listeners attached to it.
     */
    private BlissFrameLayout prepareApp(final AppItem app, boolean withText) {
        final BlissFrameLayout v = (BlissFrameLayout) getLayoutInflater().inflate(R.layout.app_view,
                null);
        final TextView label = (TextView) v.findViewById(R.id.app_label);
        final SquareFrameLayout icon = v.findViewById(R.id.app_icon);
        final SquareImageView squareImageView = (SquareImageView) v.findViewById(
                R.id.icon_image_view);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) icon.getLayoutParams();
        layoutParams.leftMargin = mDeviceProfile.iconDrawablePaddingPx / 2;
        layoutParams.rightMargin = mDeviceProfile.iconDrawablePaddingPx / 2;

        label.setPadding((int) ConverterUtil.dp2Px(4, this),
                (int) ConverterUtil.dp2Px(0, this),
                (int) ConverterUtil.dp2Px(4, this),
                (int) ConverterUtil.dp2Px(0, this));

        if (app.isFolder()) {
            v.applyBadge(checkHasApp(app, mAppsWithNotifications), withText);
        } else {
            v.applyBadge(mAppsWithNotifications.contains(app.getPackageName()), withText);
        }

        if (app.isClock()) {
            final CustomAnalogClock analogClock = (CustomAnalogClock) v.findViewById(
                    R.id.icon_clock);
            analogClock.setAutoUpdate(true);
            analogClock.setVisibility(View.VISIBLE);
            squareImageView.setVisibility(GONE);
        } else if (app.isCalendar()) {

            TextView monthTextView = (TextView) v.findViewById(R.id.calendar_month_textview);
            monthTextView.getLayoutParams().height = mDeviceProfile.monthTextviewHeight;
            monthTextView.getLayoutParams().width = mDeviceProfile.calendarIconWidth;
            int monthPx = mDeviceProfile.monthTextSize;
            monthTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, monthPx / 2);

            TextView dateTextView = (TextView) v.findViewById(R.id.calendar_date_textview);
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
            longPressedAt = System.currentTimeMillis();
            return true;
        });

        icon.setOnTouchListener(new View.OnTouchListener() {

            float touchX = 0;
            float touchY = 0;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (longPressed && event.getAction() == MotionEvent.ACTION_UP) {
                    longPressed = false;

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
                                mLongClickStartsDrag = true;
                                handleWobbling(false);
                            }
                        }
                    }.start();
                    return true;
                } else if ((longPressed || !mLongClickStartsDrag)
                        && event.getAction() == MotionEvent.ACTION_MOVE) {
                    Log.i(TAG, "move: " + touchX + " " + touchY);
                    Log.i(TAG, "x: " + (event.getX()));
                    Log.i(TAG, "y: " + (event.getY()));
                    if (Math.abs(event.getX() - touchX) >= mDeviceProfile.iconSizePx / 12
                            || Math.abs(
                            event.getY() - touchY) >= mDeviceProfile.iconSizePx / 12) {
                        longPressed = false;
                        movingApp = v;
                        View.DragShadowBuilder dragShadowBuilder = new BlissDragShadowBuilder(
                                icon, (event.getX() < 0 ? 0 : event.getX()),
                                (event.getY() < 0 ? 0 : event.getY()));
                        icon.startDrag(null, dragShadowBuilder, v, 0);
                        /*Toast.makeText(LauncherActivity.this, "Drag started",
                                Toast.LENGTH_SHORT).show();*/
                        if (v.getParent().getParent() instanceof HorizontalPager) {
                            parentPage = LauncherActivity.this.getCurrentAppsPageNumber();
                        } else {
                            parentPage = -99;
                        }
                        v.clearAnimation();
                        v.setVisibility(View.INVISIBLE);
                        dragDropEnabled = true;
                        return true;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    touchX = event.getX();
                    touchY = event.getY();
                    Log.i(TAG, "down: " + touchX + " " + touchY);
                    return false;
                } else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    Log.i(TAG, "onToucch outside");
                    if (!isDragging) {
                        return true;
                    } else {
                        return false;
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
                AppUtil.startActivityWithAnimation(getApplicationContext(), intent);
            } else {
                folderFromDock = !(v.getParent().getParent() instanceof HorizontalPager);
                displayFolder(app, v);
            }
        });

        return v;
    }

    private AppItem activeFolder;
    private BlissFrameLayout activeFolderView;

    private void displayFolder(AppItem app, BlissFrameLayout v) {

        activeFolder = app;
        activeFolderView = v;

        mFolderWindowContainer.setAlpha(0f);
        mFolderWindowContainer.setVisibility(View.VISIBLE);
        mFolderWindowContainer.animate().alpha(1.0f).setDuration(200);

        mBlissInput.setText(app.getLabel());
        mBlissInput.setCursorVisible(false);

        mFolderAppsViewPager.setAdapter(new FolderAppsPagerAdapter(this));
        mFolderAppsViewPager.getLayoutParams().width =
                (int) (mDeviceProfile.cellWidthPx * 3 + mDeviceProfile.iconDrawablePaddingPx);
        mFolderAppsViewPager.getLayoutParams().height =
                (int) (mDeviceProfile.cellHeightPx * 3 + mDeviceProfile.iconDrawablePaddingPx);
        ((CircleIndicator) mLauncherView.findViewById(R.id.indicator)).setViewPager(
                mFolderAppsViewPager);
        Log.d(TAG, "displayFolder() called with: app = [" + app + "], v = [" + v + "]");

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
            Log.d(TAG, "getCount() called " + Math.ceil((float) mFolderAppItems.size() / 9));
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

    /**
     * Handle the wobbling animation.
     */
    private void handleWobbling(boolean shouldPlay) {
        if (mWobblingCountDownTimer != null && !shouldPlay) {
            mWobblingCountDownTimer.cancel();
        }
        isWobbling = shouldPlay;
        mLongClickStartsDrag = !shouldPlay;

        if (mFolderWindowContainer.getVisibility() == View.VISIBLE) {
            for (int i = 0; i < mFolderAppsViewPager.getChildCount(); i++) {
                toggleWobbleAnimation((GridLayout) mFolderAppsViewPager.getChildAt(i), shouldPlay);
            }

        } else {
            for (int i = 0; i < pages.size(); i++) {
                toggleWobbleAnimation(pages.get(i), shouldPlay);
            }
            toggleWobbleAnimation(mDock, shouldPlay);
        }

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
            if (shouldPlayAnimation) {
                if (viewGroup.getAnimation() == null) {
                    addUninstallIcon(viewGroup);

                    if (i % 2 == 0) {
                        viewGroup.startAnimation(wobbleAnimation);
                    } else {
                        viewGroup.startAnimation(wobbleReverseAnimation);
                    }
                }
            } else {
                // Remove uninstall icon
                ImageView imageView = (ImageView) viewGroup.findViewById(R.id.uninstall_app);
                if (imageView != null) {
                    ((ViewGroup) imageView.getParent()).removeView(imageView);
                }
                viewGroup.setAnimation(null);
            }
        }
    }

    /**
     * Display uninstall icon while animating the view.
     */
    private void addUninstallIcon(ViewGroup viewGroup) {
        final AppItem appItem = getAppDetails(viewGroup);
        if (!appItem.isSystemApp()) {
            SquareFrameLayout appIcon = (SquareFrameLayout) viewGroup.findViewById(R.id.app_icon);
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

            private int latestIndex;
            private long interestExpressedAt;
            private final int timeToSwap = 100;
            private boolean latestFolderInterest;

            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                if (dragEvent.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                    isDragging = true;
                }
                if (dragEvent.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                    Log.i(TAG, "onDrag: of dock");
                    // Don't offer rearrange functionality when app is being dragged
                    // out of folder window
                    if (getAppDetails(movingApp).isBelongsToFolder()) {
                        return true;
                    }

                    // Do nothing during scroll operations
                    if (!dragDropEnabled) {
                        return true;
                    }

                    int index = getIndex(mDock, dragEvent.getX(), dragEvent.getY());
                    // If hovering over self, ignore drag/drop
                    if (index == mDock.indexOfChild(movingApp)) {
                        latestIndex = INVALID;
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
                        View latestCollidingApp = mDock.getChildAt(index);
                        if (collidingApp != latestCollidingApp) {
                            makeAppCold(collidingApp, true);
                            collidingApp = (BlissFrameLayout) mDock.getChildAt(index);
                            folderInterest = false;
                        }

                        AppItem moveAppDetails = getAppDetails(movingApp);
                        if (moveAppDetails.isFolder()) {
                            folderInterest = false;
                        } else {
                            latestFolderInterest = checkIfFolderInterest(mDock, index,
                                    dragEvent.getX(), dragEvent.getY());
                            if (latestFolderInterest != folderInterest) {
                                folderInterest = latestFolderInterest;
                            }
                            if (folderInterest) {
                                makeAppHot(collidingApp);
                            } else {
                                makeAppCold(collidingApp,
                                        !(collidingApp.getParent().getParent() instanceof
                                                HorizontalPager));
                            }
                        }
                    }

                    if (index == latestIndex) {
                        long elapsedTime = System.currentTimeMillis() - interestExpressedAt;

                        if (elapsedTime > timeToSwap) {
                            if (!folderInterest) {
                                if (mDock.getChildCount() < mDeviceProfile.numColumns
                                        || parentPage == -99) {
                                    if (movingApp.getParent() != null) {
                                        ((ViewGroup) movingApp.getParent()).removeView(movingApp);
                                    }
                                    parentPage = -99;
                                    addToDock(movingApp, index);
                                }
                            }
                        }
                    } else {
                        latestIndex = index;
                        interestExpressedAt = System.currentTimeMillis();
                    }
                    return true;
                } else if (dragEvent.getAction() == DragEvent.ACTION_DROP) {
                    Log.i(TAG, "onDrop: of dock");
                    handleWobbling(false);
                    if (mFolderWindowContainer.getVisibility() != View.VISIBLE) {
                        // Drop functionality when the folder window container
                        // is not being shown -- default
                        if (!folderInterest) {
                            if (movingApp.getParent() == null) {
                                if (view instanceof HorizontalPager) {
                                    GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                                    if (gridLayout.getChildCount()
                                            < mDeviceProfile.maxAppsPerPage) {
                                        addAppToGrid(gridLayout, movingApp);
                                    }
                                }
                                if (view instanceof GridLayout && view.getId() == R.id.dock) {
                                    if (mDock.getChildCount() >= mDeviceProfile.numColumns) {
                                        Toast.makeText(LauncherActivity.this,
                                                "Dock is already full",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        addToDock(movingApp, INVALID);
                                    }
                                }
                            }
                            movingApp.setVisibility(View.VISIBLE);
                        } else {
                            if (collidingApp.getParent().getParent() instanceof HorizontalPager) {
                                createFolder(false);
                            } else {
                                createFolder(true);
                            }

                        }
                        folderInterest = false;
                    } else {
                        removeAppFromFolder();
                    }
                    return true;
                } else if (dragEvent.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    Log.i(TAG, "onEnd: of dock");

                    if (!dragEvent.getResult()) {
                        movingApp.setVisibility(View.VISIBLE);
                    }

                    if (isDragging) {
                        isDragging = false;
                    }

                    if (getCurrentAppsPageNumber() > 0 && getGridFromPage(
                            pages.get(getCurrentAppsPageNumber() - 1)).getChildCount()
                            <= 0) {
                        pages.remove(getCurrentAppsPageNumber() - 1);
                        mIndicator.removeViewAt(getCurrentAppsPageNumber());
                        mHorizontalPager.removeViewAt(getCurrentAppsPageNumber());
                        mHorizontalPager.scrollLeft(100);
                    }


                    if (getCurrentAppsPageNumber() < pages.size() - 1 && getGridFromPage(
                            pages.get(getCurrentAppsPageNumber() + 1)).getChildCount() <= 0) {
                        pages.remove(getCurrentAppsPageNumber() + 1);
                        mIndicator.removeViewAt(getCurrentAppsPageNumber() + 2);
                        mHorizontalPager.removeViewAt(getCurrentAppsPageNumber() + 2);
                    }
                }
                return true;
            }
        });

        mHorizontalPager.setOnDragListener(new View.OnDragListener() {
            private int latestIndex;
            private long interestExpressedAt;
            private final int timeToSwap = 100;
            private boolean latestFolderInterest;

            private float lastX, lastY;

            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                if (dragEvent.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                    isDragging = true;
                }
                if (dragEvent.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                    lastX = dragEvent.getX();
                    lastY = dragEvent.getY();
                    Log.i(TAG, "onDrag: o pager");


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
                    if (dragEvent.getX() < mDeviceProfile.availableWidthPx - scrollCorner
                            && dragEvent.getX() > scrollCorner) {
                        int index = getIndex(page, dragEvent.getX(), dragEvent.getY());
                        // If hovering over self, ignore drag/drop
                        if (index == getGridFromPage(page).indexOfChild(movingApp)) {
                            latestIndex = INVALID;
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
                                makeAppCold(collidingApp, false);
                                collidingApp = (BlissFrameLayout) latestCollidingApp;
                                folderInterest = false;
                            }

                            AppItem moveAppDetails = getAppDetails(movingApp);
                            assert moveAppDetails != null;
                            if (moveAppDetails.isFolder()) {
                                folderInterest = false;
                            } else {
                                if (view instanceof HorizontalPager) {
                                    latestFolderInterest = checkIfFolderInterest(
                                            getGridFromPage(pages.get(getCurrentAppsPageNumber())),
                                            index,
                                            dragEvent.getX(), dragEvent.getY());
                                } else {
                                    latestFolderInterest = checkIfFolderInterest(mDock, index,
                                            dragEvent.getX(), dragEvent.getY());
                                }

                                if (latestFolderInterest != folderInterest) {
                                    folderInterest = latestFolderInterest;
                                }
                                if (folderInterest) {
                                    makeAppHot(collidingApp);
                                } else {
                                    makeAppCold(collidingApp, false);
                                }
                            }
                        }

                        if (index == latestIndex) {
                            long elapsedTime = System.currentTimeMillis() - interestExpressedAt;
                            if (elapsedTime > timeToSwap && !folderInterest
                                    && (page.getChildCount() < mDeviceProfile.maxAppsPerPage
                                    || parentPage == getCurrentAppsPageNumber())) {
                                if (movingApp.getParent() != null) {
                                    ((ViewGroup) movingApp.getParent()).removeView(movingApp);
                                    addAppToGrid(page, movingApp, index);
                                }
                                //movingApp.setVisibility(View.VISIBLE);
                            }
                        } else {
                            latestIndex = index;
                            interestExpressedAt = System.currentTimeMillis();
                        }
                    } else {
                        if (dragEvent.getX() > mDeviceProfile.availableWidthPx - scrollCorner) {
                            if (getCurrentAppsPageNumber() + 1 < pages.size()) {
                                //removeAppFromPage(page, movingApp);
                                mHorizontalPager.scrollRight(100);
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
                        } else if (dragEvent.getX() < scrollCorner) {
                            if (getCurrentAppsPageNumber() == 0) {
                                return true;
                            }
                            if (getCurrentAppsPageNumber() - 1 >= 0) {
                                //removeAppFromPage(page, movingApp);
                                mHorizontalPager.scrollLeft(100);
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
                    Log.i(TAG, "onDrop: of pager");

                    handleWobbling(false);
                    if (mFolderWindowContainer.getVisibility() != View.VISIBLE) {
                        // Drop functionality when the folder window container
                        // is not being shown -- default
                        if (!folderInterest) {
                            if (movingApp.getParent() == null) {
                                if (view instanceof HorizontalPager) {
                                    GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                                    if (gridLayout.getChildCount()
                                            < mDeviceProfile.maxAppsPerPage) {
                                        addAppToGrid(gridLayout, movingApp);
                                    }
                                }
                                if (view instanceof GridLayout && view.getId() == R.id.dock) {
                                    if (mDock.getChildCount() >= mDeviceProfile.numColumns) {
                                        Toast.makeText(LauncherActivity.this,
                                                "Dock is already full",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        addToDock(movingApp, INVALID);
                                    }
                                }
                            }
                            movingApp.setVisibility(View.VISIBLE);
                        } else {
                            if (collidingApp.getParent().getParent() instanceof HorizontalPager) {
                                createFolder(false);
                            } else {
                                createFolder(true);
                            }

                        }
                        folderInterest = false;
                    } else {
                        // Drop functionality when the folder window is visible
                        Rect bounds = new Rect((int) mFolderAppsViewPager.getX(),
                                (int) mFolderAppsViewPager.getY(),
                                (int) (mFolderAppsViewPager.getWidth()
                                        + mFolderAppsViewPager.getX()),
                                (int) (mFolderAppsViewPager.getHeight()
                                        + mFolderAppsViewPager.getY()));
                        if (!bounds.contains((int) lastX, (int) lastY)) {
                            removeAppFromFolder();
                        } else {
                            movingApp.setVisibility(View.VISIBLE);
                        }
                    }
                } else if (dragEvent.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    Log.i(TAG, "onENd: of pager");

                    if (isDragging) {
                        isDragging = false;
                    }
                    if (!dragEvent.getResult()) {
                        movingApp.setVisibility(View.VISIBLE);
                    }
                    if (getCurrentAppsPageNumber() > 0 && getGridFromPage(
                            pages.get(getCurrentAppsPageNumber() - 1)).getChildCount()
                            <= 0) {
                        pages.remove(getCurrentAppsPageNumber() - 1);
                        mIndicator.removeViewAt(getCurrentAppsPageNumber());
                        mHorizontalPager.removeViewAt(getCurrentAppsPageNumber());
                        mHorizontalPager.scrollLeft(100);
                    }

                    if (getCurrentAppsPageNumber() < pages.size() - 1 && getGridFromPage(
                            pages.get(getCurrentAppsPageNumber() + 1)).getChildCount() <= 0) {
                        pages.remove(getCurrentAppsPageNumber() + 1);
                        mIndicator.removeViewAt(getCurrentAppsPageNumber() + 2);
                        mHorizontalPager.removeViewAt(getCurrentAppsPageNumber() + 2);
                    }
                }
                return true;
            }
        });
    }

    /**
     * Remove app from the folder by dragging out of the folder view.
     */
    private void removeAppFromFolder() {
        if (pages.get(getCurrentAppsPageNumber()).getChildCount()
                >= mDeviceProfile.maxAppsPerPage) {
            Toast.makeText(this, "No more room in page", Toast.LENGTH_SHORT).show();
            movingApp.setVisibility(View.VISIBLE);
        } else {
            AppItem app = getAppDetails(movingApp);
            activeFolder.getSubApps().remove(app);
            mFolderAppsViewPager.getAdapter().notifyDataSetChanged();
            assert app != null;
            app.setBelongsToFolder(false);

            if (activeFolder.getSubApps().size() == 0) {
                BlissFrameLayout view = prepareApp(app, !folderFromDock);
                if (folderFromDock) {
                    addToDock(view, mDock.indexOfChild(activeFolderView));
                } else {
                    GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                    addAppToGrid(gridLayout, view, gridLayout.indexOfChild(activeFolderView));
                }

                ((ViewGroup) activeFolderView.getParent()).removeView(activeFolderView);
            } else {
                if (activeFolder.getSubApps().size() == 1) {
                    AppItem item = activeFolder.getSubApps().get(0);
                    activeFolder.getSubApps().remove(item);
                    mFolderAppsViewPager.getAdapter().notifyDataSetChanged();
                    item.setBelongsToFolder(false);
                    BlissFrameLayout view = prepareApp(item, !folderFromDock);
                    if (folderFromDock) {
                        addToDock(view, mDock.indexOfChild(activeFolderView));
                    } else {
                        GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                        addAppToGrid(gridLayout, view, gridLayout.indexOfChild(activeFolderView));
                    }

                    ((ViewGroup) activeFolderView.getParent()).removeView(activeFolderView);
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
     * Adds items to the mDock making sure that the GridLayout's parameters are
     * not violated.
     */
    private void addToDock(BlissFrameLayout view, int index) {
        view.findViewById(R.id.app_label).setVisibility(GONE);
        GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.LayoutParams iconLayoutParams = new GridLayout.LayoutParams(rowSpec, colSpec);
        /*iconLayoutParams.setMargins(getResources().getDimensionPixelSize(R.dimen.app_col_margin),
                0,
                getResources().getDimensionPixelSize(R.dimen.app_col_margin),
                0);*/
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
     * Creates/updates a folder using the tags associated with the app being dragged,
     * and the target app.
     */
    private void createFolder(boolean fromDock) {
        int index;

        if (fromDock) {
            index = mDock.indexOfChild(
                    collidingApp);
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
                    true,
                    false,
                    false,
                    true);
            folder.setFolder(true);
            folder.setFolderID(UUID.randomUUID().toString());

            List<AppItem> subApps = folder.getSubApps();
            app1.setBelongsToFolder(true);
            app2.setBelongsToFolder(true);
            subApps.add(app1);
            subApps.add(app2);

            BlissFrameLayout folderView = prepareApp(folder, !fromDock);
            if (fromDock) {
                addToDock(folderView, index);
            } else {
                addAppToGrid(pages.get(getCurrentAppsPageNumber()), folderView, index);
            }

            ((ViewGroup) collidingApp.getParent()).removeView(collidingApp);

        } else {
            app2.setBelongsToFolder(true);
            app1.getSubApps().add(app2);
            updateIcon(collidingApp, app1, new GraphicsUtil(this).generateFolderIcon(this, app1),
                    folderFromDock);
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
        Toast.makeText(this, "Size should be increased.", Toast.LENGTH_SHORT).show();
        app.setScaleX(1.2f);
        app.setScaleY(1.2f);
    }

    /**
     * Makes an app look normal
     */
    private void makeAppCold(View app, boolean fromDock) {
        if (app == null) {
            return;
        }

        List<Object> views = (List<Object>) app.getTag();
        //Toast.makeText(this, "Size should be reset.", Toast.LENGTH_SHORT).show();
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
        View v = view.getChildAt(index);
        double distance = getDistance(x, y, v.getX() + v.getWidth() / 2,
                v.getY() + v.getHeight() / 2);
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
            View v = page.getChildAt(i);
            Rect r = new Rect((int) v.getX(), (int) v.getY(), (int) v.getX() + v.getWidth(),
                    (int) v.getY() + v.getHeight());
            Rect r2 = new Rect((int) (x - mDeviceProfile.iconSizePx / 2),
                    (int) (y - mDeviceProfile.iconSizePx / 2),
                    (int) (x + mDeviceProfile.iconSizePx / 2),
                    (int) (y + mDeviceProfile.iconSizePx / 2));
            if (Rect.intersects(r, r2)) {
                float vx = v.getX() + mDeviceProfile.cellWidthPx / 2;
                float vy = v.getY() + mDeviceProfile.cellHeightPx / 2;
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

    private int activeDot;

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
        mBlissInput.clearFocus();
        if (isWobbling) {
            handleWobbling(false);
        }
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
            if (currentPageNumber != 0 && currentPageNumber != 2) {
                mHorizontalPager.setCurrentPage(1);
                currentPageNumber = 1;
            } else {
                if (currentPageNumber == 0) {
                    mHorizontalPager.scrollRight();
                } else {
                    mHorizontalPager.scrollLeft();
                }
            }
        }
    }

    public static LauncherActivity getLauncher(Context context) {
        if (context instanceof LauncherActivity) {
            return (LauncherActivity) context;
        }
        return ((LauncherActivity) ((ContextWrapper) context).getBaseContext());
    }

    public DeviceProfile getDeviceProfile() {
        return mDeviceProfile;
    }

    /*void setDragMode(int dragMode) {
        if (dragMode != mDragMode) {
            if (dragMode == DRAG_MODE_NONE) {
                cleanupAddToFolder();
                // We don't want to cancel the re-order alarm every time the target cell changes
                // as this feels to slow / unresponsive.
                cleanupReorder(false);
                cleanupFolderCreation();
            } else if (dragMode == DRAG_MODE_ADD_TO_FOLDER) {
                cleanupReorder(true);
                cleanupFolderCreation();
            } else if (dragMode == DRAG_MODE_CREATE_FOLDER) {
                cleanupAddToFolder();
                cleanupReorder(true);
            } else if (dragMode == DRAG_MODE_REORDER) {
                cleanupAddToFolder();
                cleanupFolderCreation();
            }
            mDragMode = dragMode;
        }
    }

    class FolderCreationAlarmListener implements Alarm.OnAlarmListener {
        final CellLayout layout;
        final int cellX;
        final int cellY;

        final PreviewBackground bg = new PreviewBackground();

        public FolderCreationAlarmListener(CellLayout layout, int cellX, int cellY) {
            this.layout = layout;
            this.cellX = cellX;
            this.cellY = cellY;

            BubbleTextView cell = (BubbleTextView) layout.getChildAt(cellX, cellY);
            bg.setup(mLauncher, null, cell.getMeasuredWidth(), cell.getPaddingTop());

            // The full preview background should appear behind the icon
            bg.isClipping = false;
        }

        public void onAlarm(Alarm alarm) {
            mFolderCreateBg = bg;
            mFolderCreateBg.animateToAccept(layout, cellX, cellY);
            layout.clearDragOutlines();
            setDragMode(DRAG_MODE_CREATE_FOLDER);
        }
    }


    class ReorderAlarmListener implements Alarm.OnAlarmListener {
        final float[] dragViewCenter;
        final int minSpanX, minSpanY, spanX, spanY;
        final DragObject dragObject;
        final View child;

        public ReorderAlarmListener(float[] dragViewCenter, int minSpanX, int minSpanY, int spanX,
                int spanY, DragObject dragObject, View child) {
            this.dragViewCenter = dragViewCenter;
            this.minSpanX = minSpanX;
            this.minSpanY = minSpanY;
            this.spanX = spanX;
            this.spanY = spanY;
            this.child = child;
            this.dragObject = dragObject;
        }

        public void onAlarm(Alarm alarm) {
            int[] resultSpan = new int[2];
            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], minSpanX, minSpanY, mDragTargetLayout,
                    mTargetCell);
            mLastReorderX = mTargetCell[0];
            mLastReorderY = mTargetCell[1];

            mTargetCell = mDragTargetLayout.performReorder((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY,
                    child, mTargetCell, resultSpan, CellLayout.MODE_DRAG_OVER);

            if (mTargetCell[0] < 0 || mTargetCell[1] < 0) {
                mDragTargetLayout.revertTempState();
            } else {
                setDragMode(DRAG_MODE_REORDER);
            }

            boolean resize = resultSpan[0] != spanX || resultSpan[1] != spanY;
            mDragTargetLayout.visualizeDropLocation(child, mOutlineProvider,
                    mTargetCell[0], mTargetCell[1], resultSpan[0], resultSpan[1], resize,
                    dragObject);
        }
    }
*/

    SystemDragDriver.EventListener workspaceEventListener = new SystemDragDriver.EventListener() {
        @Override
        public void onDriverDragMove(float x, float y) {

        }

        @Override
        public void onDriverDragExitWindow() {

        }

        @Override
        public void onDriverDragEnd(float x, float y) {

        }

        @Override
        public void onDriverDragCancel() {

        }
    };

    SystemDragDriver.EventListener dockEventListener = new SystemDragDriver.EventListener() {
        @Override
        public void onDriverDragMove(float x, float y) {

        }

        @Override
        public void onDriverDragExitWindow() {

        }

        @Override
        public void onDriverDragEnd(float x, float y) {

        }

        @Override
        public void onDriverDragCancel() {

        }
    };
}
