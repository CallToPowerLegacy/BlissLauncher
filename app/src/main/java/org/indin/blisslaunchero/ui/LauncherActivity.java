package org.indin.blisslaunchero.ui;

import static android.view.View.GONE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.indin.blisslaunchero.R;
import org.indin.blisslaunchero.data.db.Storage;
import org.indin.blisslaunchero.data.model.AppItem;
import org.indin.blisslaunchero.data.model.CalendarIcon;
import org.indin.blisslaunchero.notification.NotificationRepository;
import org.indin.blisslaunchero.notification.NotificationService;
import org.indin.blisslaunchero.utils.AppUtil;
import org.indin.blisslaunchero.utils.ConverterUtil;
import org.indin.blisslaunchero.utils.GraphicsUtil;
import org.indin.blisslaunchero.utils.IconPackUtil;
import org.indin.blisslaunchero.widgets.BlissDragShadowBuilder;
import org.indin.blisslaunchero.widgets.BlissFrameLayout;
import org.indin.blisslaunchero.widgets.BlissInput;
import org.indin.blisslaunchero.widgets.CustomAnalogClock;
import org.indin.blisslaunchero.widgets.HorizontalPager;
import org.indin.blisslaunchero.widgets.SquareFrameLayout;
import org.indin.blisslaunchero.widgets.SquareImageView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import me.relex.circleindicator.CircleIndicator;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class LauncherActivity extends AppCompatActivity {

    private HorizontalPager mHorizontalPager;
    private GridLayout mDock;
    private LinearLayout mIndicator;
    private ViewGroup mFolderWindowContainer;
    private ViewPager mFolderAppsViewPager;
    private BlissInput mBlissInput;
    private EditText mSearchInput;
    private View mProgressBar;

    private BroadcastReceiver installReceiver;
    private BroadcastReceiver uninstallReceiver;

    private List<AppItem> launchableApps;
    private List<AppItem> pinnedApps;

    private int nRows;
    private int nCols;
    private int currentPageNumber = 0;
    private int maxAppsPerPage;

    private int maxDistanceForFolderCreation;

    private final static int INVALID = -999;

    private List<GridLayout> pages;

    private Drawable hotBackground;
    private Drawable defaultBackground;
    private Drawable transparentBackground;

    private boolean dragDropEnabled = true;

    private BlissFrameLayout movingApp;
    private BlissFrameLayout collidingApp;
    private boolean folderInterest;

    private Animation wobbleAnimation;
    private Animation wobbleReverseAnimation;

    private int scrollCorner;

    private Storage storage;
    private int parentPage = -99;

    private int mPagerHeight;
    private int mPagerWidth;
    private int mWidthPixels;
    private int mHeightPixels;
    private int iconHeight;
    private int iconWidth;
    private int folderIconWidth;
    private boolean folderFromDock;
    private int appIconMargin;
    private boolean isWobbling = false;
    private boolean longPressed;
    private int x;
    private int y;
    public static int appIconWidth;
    public static int labelTextSizeSp;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHorizontalPager = (HorizontalPager) findViewById(R.id.pages_container);
        mDock = (GridLayout) findViewById(R.id.dock);
        mIndicator = (LinearLayout) findViewById(R.id.page_indicator);
        mFolderWindowContainer = (ViewGroup) findViewById(R.id.folder_window_container);
        mFolderAppsViewPager = (ViewPager) findViewById(R.id.folder_apps);
        mBlissInput = (BlissInput) findViewById(R.id.folder_title);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        storage = new Storage(getApplicationContext());

        prepareBroadcastReceivers();

        // Start NotificationService to add count badge to Icons
        notificationServiceIntent = new Intent(this, NotificationService.class);
        startService(notificationServiceIntent);

        mHorizontalPager.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mPagerHeight = mHorizontalPager.getHeight();
                        mPagerWidth = mHorizontalPager.getWidth();
                        //we only wanted the first call back so now remove
                        mHorizontalPager.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        createLauncher();
                    }
                });
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
                recreate();
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
            View view = prepareApp(appItem, true);
            int current = getCurrentAppsPageNumber();
            while (current < pages.size() && pages.get(current).getChildCount() == maxAppsPerPage) {
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
            addAppToPage(pages.get(current), view);
            storage.save(pages, mDock);
        }
    }

    private void loadApps() {
        // Cache icons before loading apps
        IconPackUtil.cacheIconsFromIconPack(this);

        launchableApps = AppUtil.loadLaunchableApps(getApplicationContext());
        pinnedApps = AppUtil.getPinnedApps(this, launchableApps);
        launchableApps.removeAll(pinnedApps);

        if (nCols > 4) {
            // On large devices, find an app to fill the 5th position.
            // Calculator is a good option, if you can find it.

            int i = 0;
            while (i < launchableApps.size()) {
                if (launchableApps.get(i).getLabel().toString().toLowerCase().startsWith("calc")) {
                    break;
                }
                i++;
            }
            if (i < launchableApps.size()) {
                AppItem calculator = launchableApps.remove(i);
                pinnedApps.add(calculator);
            } else {
                // Pick the last launchable app otherwise
                AppItem item = launchableApps.remove(launchableApps.size() - 1);
                pinnedApps.add(item);
            }
        }
    }

    private void prepareResources() {

        setRealDeviceSizeInPixels();

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        double x = Math.pow(mWidthPixels / dm.xdpi, 2);
        double y = Math.pow(mHeightPixels / dm.ydpi, 2);
        double screenInches = Math.sqrt(x + y);

        if (screenInches <= 4.5) {
            nRows = 4;
            appIconMargin = getResources().getDimensionPixelSize(R.dimen.margin_inch1);
            labelTextSizeSp = 12;
        } else if (screenInches <= 5.5) {
            nRows = 5;
            appIconMargin = getResources().getDimensionPixelSize(R.dimen.margin_inch2);
            labelTextSizeSp = 12;
        } else {
            nRows = 6;
            appIconMargin = getResources().getDimensionPixelSize(R.dimen.margin_inch3);
            labelTextSizeSp = 14;
        }

        nCols = getResources().getInteger(R.integer.col_count);
        maxAppsPerPage = nRows * nCols;

        iconHeight = (mPagerHeight) / nRows;
        iconWidth = (mPagerWidth - 2 * getResources().getDimensionPixelSize(
                R.dimen.app_col_margin)) / nCols;
        folderIconWidth = (mPagerWidth - 2 * getResources().getDimensionPixelSize(
                R.dimen.app_col_margin)) / nCols;

        appIconWidth = iconWidth - 2 * appIconMargin;
        maxDistanceForFolderCreation = getResources()
                .getDimensionPixelSize(R.dimen.maxDistanceForFolderCreation) * mPagerWidth / 540;

        hotBackground = getResources().getDrawable(R.drawable.rounded_corners_icon_hot, null);
        defaultBackground = getResources().getDrawable(R.drawable.rounded_corners_icon, null);
        scrollCorner = getResources().getDimensionPixelSize(R.dimen.scrollCorner) * mPagerWidth
                / 480;
        wobbleAnimation = AnimationUtils.loadAnimation(this, R.anim.wobble);
        wobbleReverseAnimation = AnimationUtils.loadAnimation(this, R.anim.wobble_reverse);
        transparentBackground = getResources().getDrawable(R.drawable.transparent, null);
    }

    private void setRealDeviceSizeInPixels() {
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);


        // since SDK_INT = 1;
        mWidthPixels = displayMetrics.widthPixels;
        mHeightPixels = displayMetrics.heightPixels;

        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) {
            try {
                mWidthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                mHeightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (Exception ignored) {
            }
        }

        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 17) {
            try {
                Point realSize = new Point();
                Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
                mWidthPixels = realSize.x;
                mHeightPixels = realSize.y;
            } catch (Exception ignored) {
            }
        }
    }

    private void createLauncher() {
        mProgressBar.setVisibility(View.VISIBLE);
        AsyncTask.execute(() -> {
            // Load the apps and icons in a separate thread
            prepareResources();
            loadApps();

            // Return to the UI thread to render them
            runOnUiThread(() -> {
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
            });
        });
    }

    private void createOrUpdateBadgeCount() {
        getCompositeDisposable().add(
                NotificationRepository.getNotificationRepository().getNotifications().subscribeWith(
                        new DisposableObserver<Set<String>>() {
                            @Override
                            public void onNext(Set<String> packages) {
                                Log.d(TAG, "onNext() called with: packages = [" + packages + "]");
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
                for(int j = 0; j<gridLayout.getChildCount(); j++){
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
        int nPages = (int) Math.ceil((float) launchableApps.size() / maxAppsPerPage);
        pages = new ArrayList<>();
        for (int i = 0; i < nPages; i++) {
            GridLayout page = preparePage();
            pages.add(page);
        }
        int count = 0;
        for (int i = 0; i < launchableApps.size(); i++) {
            View appView = prepareApp(launchableApps.get(i), true);
            addAppToPage(pages.get(currentPageNumber), appView);
            count++;
            if (count >= maxAppsPerPage) {
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
        grid.setRowCount(nRows);
        grid.setLayoutTransition(getDefaultLayoutTransition());
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
                dockItems.add(appItem);
                BlissFrameLayout appView = prepareApp(appItem, false);
                if (appView != null) {
                    addToDock(appView, INVALID);
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

                        View appView = prepareApp(appItem, true);
                        if (appView != null) {
                            addAppToPage(page, appView);
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
            if (pages.get(pages.size() - 1).getChildCount() < maxAppsPerPage) {
                View appView = prepareApp(launchableApps.get(i), true);
                if (appView != null) {
                    addAppToPage(pages.get(pages.size() - 1), appView);
                }
            } else {
                pages.add(preparePage());
                View appView = prepareApp(launchableApps.get(i), true);
                if (appView != null) {
                    addAppToPage(pages.get(pages.size() - 1), appView);
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
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.widgets_page,
                null);
        mHorizontalPager.addView(layout, 0);
        currentPageNumber = 1;
        mHorizontalPager.setCurrentPage(currentPageNumber);
        mSearchInput = (EditText) layout.findViewById(R.id.search_input);
        layout.setOnDragListener(null);

        mSearchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v);
            }
        });

        mSearchInput.setOnEditorActionListener((textView, action, keyEvent) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                runSearch(mSearchInput.getText().toString());
                mSearchInput.setText("");
            }
            return false;
        });
    }

    private void runSearch(String query) {
        Uri uri = Uri.parse("https://www.google.com/search?q=" + query);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        AppUtil.startActivityWithAnimation(this, intent);
    }

    private int getCurrentAppsPageNumber() {
        return currentPageNumber - 1;
    }

    private void addAppToPage(GridLayout page, View view) {
        GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.LayoutParams iconLayoutParams = new GridLayout.LayoutParams(rowSpec, colSpec);
        iconLayoutParams.height = iconHeight;
        iconLayoutParams.width = iconWidth;
        view.findViewById(R.id.app_label).setVisibility(View.VISIBLE);
        view.setLayoutParams(iconLayoutParams);
        page.addView(view);
    }

    private void addAppToPage(GridLayout page, View view, int index) {
        GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.LayoutParams iconLayoutParams = new GridLayout.LayoutParams(rowSpec, colSpec);
        iconLayoutParams.height = iconHeight;
        iconLayoutParams.width = iconWidth;
        view.findViewById(R.id.app_label).setVisibility(View.VISIBLE);
        view.setLayoutParams(iconLayoutParams);
        page.addView(view, index);
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
                    false);
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

            folderItem.setIcon(GraphicsUtil.generateFolderIcon(this, folderItem));
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
        Log.d(TAG, "prepareApp() called with: app = [" + app + "]");
        final BlissFrameLayout v = (BlissFrameLayout) getLayoutInflater().inflate(R.layout.app_view,
                null);
        final TextView label = (TextView) v.findViewById(R.id.app_label);
        final View icon = v.findViewById(R.id.app_icon);
        final SquareImageView squareImageView = (SquareImageView) v.findViewById(
                R.id.icon_image_view);

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) icon.getLayoutParams();
        layoutParams.leftMargin = appIconMargin;
        layoutParams.rightMargin = appIconMargin;
        label.setPadding(getResources().getDimensionPixelSize(R.dimen.app_col_margin),
                (int) ConverterUtil.dp2Px(4, this),
                getResources().getDimensionPixelSize(R.dimen.app_col_margin),
                (int) ConverterUtil.dp2Px(4, this));

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
            monthTextView.getLayoutParams().height = appIconWidth * 40 / 192;
            monthTextView.getLayoutParams().width = appIconWidth;
            int monthPx = appIconWidth * 48 / 192;
            monthTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, monthPx / 2);

            TextView dateTextView = (TextView) v.findViewById(R.id.calendar_date_textview);
            int datePx = appIconWidth * 154 / 192;
            dateTextView.getLayoutParams().height = appIconWidth * 152 / 192;
            dateTextView.getLayoutParams().width = appIconWidth;
            dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, datePx / 2);

            v.findViewById(R.id.icon_calendar).setVisibility(View.VISIBLE);
            squareImageView.setVisibility(GONE);

            CalendarIcon calendarIcon = new CalendarIcon(monthTextView, dateTextView);
            updateCalendarIcon(calendarIcon, Calendar.getInstance());
            mCalendarIcons.add(calendarIcon);
        }

        final Intent intent = app.getIntent();
        if (!app.isClock()) {
            squareImageView.setImageDrawable(app.getIcon());
        }
        label.setText(app.getLabel());
        label.setTextSize(labelTextSizeSp);
        List<Object> tags = new ArrayList<>();
        tags.add(squareImageView);
        tags.add(label);
        tags.add(app);
        v.setTag(tags);

        if (IconPackUtil.iconPackPresent) {
            if (app.isIconFromIconPack()) {
                squareImageView.setBackgroundResource(0);
            } else {
                if (!app.isFolder()) {
                    squareImageView.setBackground(
                            IconPackUtil.getIconBackground(app.getLabel().charAt(0)));
                } else {
                    squareImageView.setBackground(IconPackUtil.folderBackground);
                }
            }
        }

        icon.setOnLongClickListener(view -> {
            if (mWobblingCountDownTimer != null) {
                mWobblingCountDownTimer.cancel();
            }

            mWobblingCountDownTimer = new CountDownTimer(25000, 1000) {

                boolean firstTick = false;

                @Override
                public void onTick(long millisUntilFinished) {
                    if (!firstTick) {
                        firstTick = true;
                        handleWobbling(true);
                        longPressed = true;
                        longPressedAt = System.currentTimeMillis();
                    }
                }

                @Override
                public void onFinish() {
                    if (isWobbling) {
                        handleWobbling(false);
                    }
                }
            }.start();
            return true;
        });

        icon.setOnTouchListener((view, event) -> {
            if (longPressed && event.getAction() == MotionEvent.ACTION_UP) {
                longPressed = false;
                return true;
            } else if (longPressed && event.getAction() == MotionEvent.ACTION_MOVE
                    && System.currentTimeMillis() - longPressedAt > 200) {
                longPressed = false;
                movingApp = v;
                View.DragShadowBuilder dragShadowBuilder = new BlissDragShadowBuilder(
                        icon);
                v.startDrag(null, dragShadowBuilder, v, 0);

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
            return false;
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
                (int) (iconWidth * 3 + 2 * getResources().getDimensionPixelSize(
                        R.dimen.app_grid_padding));
        mFolderAppsViewPager.getLayoutParams().height =
                (int) (iconHeight * 3);
        ((CircleIndicator) findViewById(R.id.indicator)).setViewPager(mFolderAppsViewPager);
        Log.d(TAG, "displayFolder() called with: app = [" + app + "], v = [" + v + "]");

    }

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
            Log.d(TAG,
                    "instantiateItem() called with: container = [" + container + "], position = ["
                            + position + "]");
            GridLayout viewGroup = (GridLayout) LayoutInflater.from(mContext).inflate(
                    R.layout.apps_page, container, false);
            viewGroup.setRowCount(3);
            viewGroup.setColumnCount(3);
            int i = 0;
            while (9 * position + i < mFolderAppItems.size() && i < 9) {
                AppItem appItem = mFolderAppItems.get(9 * position + i);
                BlissFrameLayout appView = prepareApp(appItem, true);
                GridLayout.LayoutParams iconLayoutParams = new GridLayout.LayoutParams();
                iconLayoutParams.height = iconHeight;
                iconLayoutParams.width = iconWidth;
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

            int size = (appIcon.getRight() - appIcon.getLeft()) / 5;
            if (size > appIcon.getTop() || size > appIcon.getLeft()) {
                size = Math.min(appIcon.getTop(), appIcon.getLeft());
            }
            int paddingRight = appIconMargin - size;
            int paddingLeft = paddingRight;
            int paddingTop = appIcon.getTop() - size;
            int paddingBottom =
                    (2 * paddingLeft > paddingTop) ? (paddingLeft + paddingRight - paddingTop)
                            : paddingTop;

            ImageView imageView = new ImageView(this);
            imageView.setId(R.id.uninstall_app);
            imageView.setImageResource(R.drawable.remove_icon_72);
            imageView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

            imageView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DELETE,
                        Uri.fromParts("package", appItem.getPackageName(),
                                null));
                LauncherActivity.this.startActivity(intent);
            });
            FrameLayout.LayoutParams layoutParams =
                    new FrameLayout.LayoutParams(2 * size + paddingRight + paddingLeft,
                            2 * size + paddingTop + paddingBottom);
            layoutParams.gravity = Gravity.END | Gravity.TOP;
            viewGroup.addView(imageView, layoutParams);
        }
    }

    /**
     * Creates drag listeners for the mDock and pages, which are responsible for almost
     * all the drag and drop functionality present in this app.
     */
    private void createDragListener() {
        mDock.setOnDragListener(new View.OnDragListener() {

            private int latestIndex;
            private long interestExpressedAt;
            private final int timeToSwap = 100;
            private boolean latestFolderInterest;

            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                if (mDock.getChildCount() > nCols) {
                    Toast.makeText(LauncherActivity.this, "Dock is already full",
                            Toast.LENGTH_SHORT).show();
                }

                // Don't offer rearrange functionality when app is being dragged
                // out of folder window
                if (getAppDetails(movingApp).isBelongsToFolder()) {
                    return true;
                }

                // Do nothing during scroll operations
                if (!dragDropEnabled) {
                    return true;
                }

                if (dragEvent.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                    ;

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
                                interestExpressedAt = System.currentTimeMillis();
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

                        if (elapsedTime > timeToSwap && !folderInterest
                                && mDock.getChildCount() < nCols) {
                            if (movingApp.getParent() != null) {
                                ((ViewGroup) movingApp.getParent()).removeView(movingApp);
                            }
                            addToDock(movingApp, index);
                            parentPage = -99;
                            //movingApp.setVisibility(View.VISIBLE);
                        }
                    } else {
                        latestIndex = index;
                        interestExpressedAt = System.currentTimeMillis();
                    }


                }
                movingApp = (BlissFrameLayout) dragEvent.getLocalState();
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
                if (dragEvent.getAction() == DragEvent.ACTION_DRAG_LOCATION) {

                    lastX = dragEvent.getX();
                    lastY = dragEvent.getY();

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
                    if (dragEvent.getX() < mPagerWidth - scrollCorner
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
                                collidingApp =
                                        (BlissFrameLayout) getGridFromPage(page).getChildAt(index);
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
                                    interestExpressedAt = System.currentTimeMillis();
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
                                    && (page.getChildCount() < maxAppsPerPage
                                    || parentPage == getCurrentAppsPageNumber())) {
                                if (movingApp.getParent() != null) {
                                    ((ViewGroup) movingApp.getParent()).removeView(movingApp);
                                }
                                if (index != INVALID) {
                                    addAppToPage(page, movingApp, index);
                                } else {
                                    addAppToPage(page, movingApp);
                                }
                                parentPage = -99;
                                //movingApp.setVisibility(View.VISIBLE);
                            }
                        } else {
                            latestIndex = index;
                            interestExpressedAt = System.currentTimeMillis();
                        }
                    } else {
                        if (dragEvent.getX() > mPagerWidth - scrollCorner) {
                            if (getCurrentAppsPageNumber() + 1 < pages.size()) {
                                //removeAppFromPage(page, movingApp);
                                mHorizontalPager.scrollRight();
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
                                mHorizontalPager.scrollLeft();
                            } else if (getCurrentAppsPageNumber() + 1 == pages.size() - 2
                                    && getGridFromPage(pages.get(pages.size() - 1)).getChildCount()
                                    <= 0) {
                                mIndicator.removeViewAt(pages.size());
                                mHorizontalPager.removeViewAt(pages.size());
                                pages.remove(pages.size() - 1);
                            }
                        }
                    }
                } else if (dragEvent.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    handleWobbling(false);
                    if (mFolderWindowContainer.getVisibility() != View.VISIBLE) {
                        // Drop functionality when the folder window container
                        // is not being shown -- default
                        if (!folderInterest) {
                            if (movingApp.getParent() == null) {
                                if (view instanceof HorizontalPager) {
                                    GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                                    if (gridLayout.getChildCount() < maxAppsPerPage) {
                                        addAppToPage(gridLayout, movingApp);
                                    }
                                }
                                if (view instanceof GridLayout && view.getId() == R.id.dock) {
                                    addToDock(movingApp, INVALID);
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

                    if (getCurrentAppsPageNumber() > 0 && getGridFromPage(
                            pages.get(getCurrentAppsPageNumber() - 1)).getChildCount()
                            <= 0) {
                        pages.remove(getCurrentAppsPageNumber() - 1);
                        mIndicator.removeViewAt(getCurrentAppsPageNumber());
                        mHorizontalPager.removeViewAt(getCurrentAppsPageNumber());
                        mHorizontalPager.scrollLeft();
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

    private void removeAppFromFolder() {
        if (pages.get(getCurrentAppsPageNumber()).getChildCount() >= maxAppsPerPage) {
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
                    addAppToPage(gridLayout, view, gridLayout.indexOfChild(activeFolderView));
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
                        addAppToPage(gridLayout, view, gridLayout.indexOfChild(activeFolderView));
                    }

                    ((ViewGroup) activeFolderView.getParent()).removeView(activeFolderView);
                } else {
                    updateIcon(activeFolderView, activeFolder,
                            GraphicsUtil.generateFolderIcon(this, activeFolder), folderFromDock);
                }
                if (movingApp.getParent() != null) {
                    ((ViewGroup) movingApp.getParent()).removeView(movingApp);
                }
                int current = getCurrentAppsPageNumber();
                addAppToPage(pages.get(current), movingApp);
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
        iconLayoutParams.height = getResources().getDimensionPixelSize(R.dimen.dockHeight);
        iconLayoutParams.width = iconWidth;
        iconLayoutParams.setGravity(Gravity.CENTER);
        view.setLayoutParams(iconLayoutParams);
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

        Drawable folderIcon = GraphicsUtil.generateFolderIcon(this,
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
                    false);
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
                addAppToPage(pages.get(getCurrentAppsPageNumber()), folderView, index);
            }

            ((ViewGroup) collidingApp.getParent()).removeView(collidingApp);

        } else {
            app2.setBelongsToFolder(true);
            app1.getSubApps().add(app2);
            updateIcon(collidingApp, app1, GraphicsUtil.generateFolderIcon(this, app1),
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

        List<Object> views = (List<Object>) app.getTag();
        ((View) (views.get(0))).setBackground(
                hotBackground
        );
        ((View) views.get(1)).setVisibility(GONE);
        app.setScaleX(1.2f);
        app.setScaleY(1.2f);
    }

    private Drawable getDefaultBackground(AppItem appItem) {
        if (IconPackUtil.iconPackPresent) {
            if (appItem.isFolder()) {
                return IconPackUtil.folderBackground;
            } else {
                if (appItem.isIconFromIconPack()) {
                    return transparentBackground;
                } else {
                    return IconPackUtil.getIconBackground(appItem.getLabel().charAt(0));
                }
            }
        } else {
            return defaultBackground;
        }
    }

    /**
     * Makes an app look normal
     */
    private void makeAppCold(View app, boolean fromDock) {
        if (app == null) {
            return;
        }

        List<Object> views = (List<Object>) app.getTag();
        ((View) (views.get(0))).setBackground(
                getDefaultBackground(((AppItem) views.get(2)))
        );
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

        for (int i = 0; i < page.getChildCount(); i++) {
            View v = page.getChildAt(i);
            Rect r = new Rect((int) v.getX(), (int) v.getY(), (int) v.getX() + v.getWidth(),
                    (int) v.getY() + v.getHeight());
            Rect r2 = new Rect((int) (x - appIconWidth / 2), (int) (y - appIconWidth / 2),
                    (int) (x + appIconWidth / 2), (int) (y + appIconWidth / 2));
            if (Rect.intersects(r, r2)) {
                return i;
            }
        }

        return INVALID;
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
                getResources().getDimensionPixelSize(R.dimen.dotSize),
                getResources().getDimensionPixelSize(R.dimen.dotSize)
        );

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
}
