package org.indin.blisslaunchero;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DesktopActivity extends AppCompatActivity {

    private HorizontalPager pager;
    private GridLayout dock;
    private LinearLayout indicator;
    private ViewGroup folderWindowContainer;
    private GridLayout folderApps;
    private BlissInput folderTitle;
    private EditText searchInput;
    private View progressBar;

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

    private View movingApp;
    private View collidingApp;
    private boolean folderInterest;

    private Animation wobbleAnimation;
    private Animation wobbleReverseAnimation;
    private static String TAG = "BLISSLAUNCHER_HOME";
    private boolean backAllowed = true;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = findViewById(R.id.pages_container);
        dock = findViewById(R.id.dock);
        indicator = findViewById(R.id.page_indicator);
        folderWindowContainer = findViewById(R.id.folder_window_container);
        folderApps = findViewById(R.id.folder_apps);
        folderTitle = findViewById(R.id.folder_title);
        progressBar = findViewById(R.id.progressbar);

        /*DesignSpec designSpec = DesignSpec.fromResource(workspace, R.raw.spec);
        workspace.getOverlay().add(designSpec);*/

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        storage = new Storage(getApplicationContext());

        prepareBroadcastReceivers();

        pager.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mPagerHeight = pager.getHeight();
                        mPagerWidth = pager.getWidth();
                        //we only wanted the first call back so now remove
                        pager.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        createLauncher();
                    }
                });


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
                    Log.i(TAG, "Installed " + packageName);
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
        registerReceiver(installReceiver, installFilter);
        registerReceiver(uninstallReceiver, uninstallFilter);
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
        // Don't add an app that's already present in the dock
        for (int i = 0; i < dock.getChildCount(); i++) {
            if (getAppDetails(dock.getChildAt(i)).getPackageName().equals(packageName)) {
                return;
            }
        }

        AppItem appItem = AppUtil.createAppItem(this, packageName);
        if (appItem != null) {
            View view = prepareApp(appItem);
            int current = getCurrentAppsPageNumber();
            while (current < pages.size() && pages.get(current).getChildCount() == maxAppsPerPage) {
                current++;
            }

            if (current == pages.size()) {
                pages.add(preparePage());
                ImageView dot = new ImageView(DesktopActivity.this);
                dot.setImageDrawable(getDrawable(R.drawable.dot_off));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        getResources().getDimensionPixelSize(R.dimen.dotSize),
                        getResources().getDimensionPixelSize(R.dimen.dotSize)
                );
                dot.setLayoutParams(params);
                indicator.addView(dot);
                pager.addView(pages.get(current));
            }
            addAppToPage(pages.get(current), view);
            storage.save(pages, dock);
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
        Log.d("debug", "Screen inches : " + screenInches);

        if (screenInches <= 4.7) {
            nRows = 4;
        } else if (screenInches <= 5.4) {
            nRows = 5;
        } else {
            nRows = 6;
        }

        Log.i(TAG, "prepareResources: " + nRows);
        nCols = getResources().getInteger(R.integer.col_count);
        maxAppsPerPage = nRows * nCols;

        iconHeight = (mPagerHeight) / nRows;
        iconWidth = (mPagerWidth - 10 * getResources().getDimensionPixelSize(
                R.dimen.app_col_margin)) / nCols;
        folderIconWidth = (mPagerWidth - 10 * getResources().getDimensionPixelSize(
                R.dimen.app_col_margin)) / nCols;

        maxDistanceForFolderCreation = getResources()
                .getDimensionPixelSize(R.dimen.maxDistanceForFolderCreation) * mPagerWidth / 540;

        hotBackground = getResources().getDrawable(R.drawable.rounded_corners_icon_hot, null);
        defaultBackground = getResources().getDrawable(R.drawable.rounded_corners_icon, null);
        scrollCorner = getResources().getDimensionPixelSize(R.dimen.scrollCorner);
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
        progressBar.setVisibility(View.VISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // Load the apps and icons in a separate thread
                prepareResources();
                loadApps();

                // Return to the UI thread to render them
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showIconPackWallpaperFirstTime();

                        progressBar.setVisibility(View.GONE);
                        createUI();
                        createPageChangeListener();
                        createFolderTitleListener();
                        createDragListener();
                        createWidgetsPage();
                        createIndicator();
                        // fixMinimumGridWidth();
                        Log.i(TAG, "Launcher ready.");
                    }
                });
            }
        });
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
            Log.i(TAG, "Icon pack wallpaper shown");
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
        folderTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });
        folderTitle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    updateFolderTitle();
                }
                return false;
            }
        });
        folderTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                folderTitle.setCursorVisible(true);
            }
        });
        folderWindowContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideFolderWindowContainer();
            }
        });
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(
                Activity.INPUT_METHOD_SERVICE);
        assert inputMethodManager != null;
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void updateFolderTitle() {
        String updatedTitle = folderTitle.getText().toString();
        activeFolder.setLabel(updatedTitle);
        List<Object> tags = (List<Object>) activeFolderView.getTag();
        ((TextView) tags.get(1)).setText(updatedTitle);
        folderTitle.setText(updatedTitle);
        folderTitle.setCursorVisible(false);
    }

    /**
     * Adds a scroll listener to the pager in order to keep the currentPageNumber
     * updated
     */
    private void createPageChangeListener() {
        pager.addOnScrollListener(new HorizontalPager.OnScrollListener() {
            @Override
            public void onScroll(int scrollX) {
                dragDropEnabled = false;
            }

            @Override
            public void onViewScrollFinished(int page) {
                Log.d(TAG, "onViewScrollFinished() called with: page = [" + page + "]");
                currentPageNumber = page;

                // Remove indicator and dock from widgets page, and make them
                // reappear when user swipes to the first apps page
                if (currentPageNumber == 0) {
                    dock.animate().translationYBy(
                            Utils.dp2Px(105, DesktopActivity.this)).setDuration(
                            100).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            dock.setVisibility(View.GONE);
                        }
                    });

                    indicator.animate().alpha(0).setDuration(100).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            indicator.setVisibility(View.GONE);
                        }
                    });

                } else {
                    if (indicator.getAlpha() != 1.0f) {
                        indicator.setVisibility(View.VISIBLE);
                        dock.setVisibility(View.VISIBLE);
                        indicator.animate().alpha(1).setDuration(100);
                        dock.animate().translationY(0).setDuration(100);
                        Log.d(TAG, "Making dock and indicator reappear");
                    }
                }

                dragDropEnabled = true;
                updateIndicator();
            }
        });
    }

    /**
     * Populates the pages and the dock for the first time.
     */
    private void createUI() {
        pager.setUiCreated(false);
        dock.setEnabled(false);
        if (storage.isLayoutPresent()) {
            Log.i(TAG, "Preparing UI from storage");
            createUIFromStorage();
            pager.setUiCreated(true);
            dock.setEnabled(true);
            return;
        }
        int nPages = (int) Math.ceil((float) launchableApps.size() / maxAppsPerPage);
        Log.i(TAG, "createUI: npages: " + nPages + ", max: " + maxAppsPerPage);
        pages = new ArrayList<>();
        for (int i = 0; i < nPages; i++) {
            GridLayout page = preparePage();
            pages.add(page);
        }
        int count = 0;
        for (int i = 0; i < launchableApps.size(); i++) {
            View appView = prepareApp(launchableApps.get(i));
            addAppToPage(pages.get(currentPageNumber), appView);
            count++;
            if (count >= maxAppsPerPage) {
                count = 0;
                currentPageNumber++;
            }
        }
        for (int i = 0; i < nPages; i++) {
            pager.addView(pages.get(i));
        }
        currentPageNumber = 0;

        dock.setLayoutTransition(getDefaultLayoutTransition());
        for (int i = 0; i < pinnedApps.size(); i++) {
            View appView = prepareApp(pinnedApps.get(i));
            addToDock(appView, INVALID);
        }
        pager.setUiCreated(true);
        dock.setEnabled(true);

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

        dock.setLayoutTransition(getDefaultLayoutTransition());

        for (int i = 0; i < storageData.getNDocked(); i++) {
            try {
                JSONObject currentDockItemData = storageData.dock.getJSONArray(0).getJSONObject(i);
                AppItem appItem = prepareAppFromJSON(currentDockItemData);
                dockItems.add(appItem);
                View appView = prepareApp(appItem);
                if (appView != null) {
                    addToDock(appView, INVALID);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading dock data " + e);
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
                    if (appItem.isFolder()) {
                        storedItems.addAll(appItem.getSubApps());
                    } else {
                        storedItems.add(appItem);
                    }

                    View appView = prepareApp(appItem);
                    if (appView != null) {
                        addAppToPage(page, appView);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading page data " + e);
            }

        }

        launchableApps.removeAll(storedItems);
        launchableApps.removeAll(dockItems);

        for (int i = 0; i < launchableApps.size(); i++) {
            if (pages.get(pages.size() - 1).getChildCount() < maxAppsPerPage) {
                View appView = prepareApp(launchableApps.get(i));
                if (appView != null) {
                    addAppToPage(pages.get(pages.size() - 1), appView);
                }
            } else {
                pages.add(preparePage());
                View appView = prepareApp(launchableApps.get(i));
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
            pager.addView(pages.get(i));
        }

        currentPageNumber = 0;
    }

    private void createWidgetsPage() {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.widgets_page,
                null);
        pager.addView(layout, 0);
        currentPageNumber = 1;
        pager.setCurrentPage(currentPageNumber);
        searchInput = layout.findViewById(R.id.search_input);
        layout.setOnDragListener(null);

        searchInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int action, KeyEvent keyEvent) {
                if (action == EditorInfo.IME_ACTION_SEARCH) {
                    runSearch(searchInput.getText().toString());
                    searchInput.setText("");
                }
                return false;
            }
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
        iconLayoutParams.setMargins(getResources().getDimensionPixelSize(R.dimen.app_col_margin),
                0,
                getResources().getDimensionPixelSize(R.dimen.app_col_margin),
                0);
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
        iconLayoutParams.setMargins(getResources().getDimensionPixelSize(R.dimen.app_col_margin),
                0,
                getResources().getDimensionPixelSize(R.dimen.app_col_margin),
                0);

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
        View output = null;
        if (currentItemData.getBoolean("isFolder")) {
            AppItem folderItem =
                    new AppItem(currentItemData.getString("folderName"),
                            "", getDrawable(R.mipmap.ic_folder), null, "FOLDER", false);
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
            //output = prepareApp(folderItem);
            return folderItem;
        } else {
            AppItem appItem = prepareAppItemFromComponent(componentName);
            return appItem;
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
     * the pages and the dock.
     *
     * The View object also has all the required listeners attached to it.
     */
    private View prepareApp(final AppItem app) {
        final View v = getLayoutInflater().inflate(R.layout.app_view, null);
        final SquareImageView icon = v.findViewById(R.id.app_icon);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) icon.getLayoutParams();
        int margin = 0;
        if (nRows == 4) {
            margin = getResources().getDimensionPixelSize(R.dimen.margin_inch1);
        } else if (nRows == 5) {
            margin = getResources().getDimensionPixelSize(R.dimen.margin_inch2);
        } else if (nRows == 6) {
            margin = getResources().getDimensionPixelSize(R.dimen.margin_inch3);
        }
        layoutParams.leftMargin = margin;
        layoutParams.rightMargin = margin;

        final TextView label = v.findViewById(R.id.app_label);
        final Intent intent = app.getIntent();
        icon.setImageDrawable(app.getIcon());
        label.setText(app.getLabel());
        List<Object> tags = new ArrayList<>();
        tags.add(icon);
        tags.add(label);
        tags.add(app);
        v.setTag(tags);

        if (IconPackUtil.iconPackPresent) {
            if (app.isIconFromIconPack()) {
                icon.setBackgroundResource(0);
            } else {
                if (!app.isFolder()) {
                    icon.setBackground(IconPackUtil.getIconBackground(app.getLabel().charAt(0)));
                } else {
                    icon.setBackground(IconPackUtil.folderBackground);
                }
            }
        }

        icon.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                View.DragShadowBuilder dragShadowBuilder = new BlissDragShadowBuilder(icon);
                v.startDrag(null, dragShadowBuilder, v, 0);
                if (v.getParent().getParent() instanceof HorizontalPager) {
                    parentPage = getCurrentAppsPageNumber();
                } else {
                    parentPage = -99;
                }
                v.setVisibility(View.INVISIBLE);
                movingApp = v;
                dragDropEnabled = true;

                if (!app.isBelongsToFolder()) {
                    handleWobbling(true);
                }

                return true;
            }
        });

        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Make outside apps unclickable when the folder window is visible
                if (!app.isBelongsToFolder() &&
                        folderWindowContainer.getVisibility() == View.VISIBLE) {
                    return;
                }

                if (!app.isFolder()) {
                    AppUtil.startActivityWithAnimation(getApplicationContext(), intent);
                } else {
                    folderFromDock = !(v.getParent().getParent() instanceof HorizontalPager);
                    displayFolder(app, v);
                }
            }
        });

        return v;
    }

    private AppItem activeFolder;
    private View activeFolderView;

    private void displayFolder(AppItem app, View v) {
        folderWindowContainer.setAlpha(0f);
        folderWindowContainer.setVisibility(View.VISIBLE);
        folderWindowContainer.animate().alpha(1.0f).setDuration(200);

        folderTitle.setText(app.getLabel());
        folderTitle.setCursorVisible(false);
        folderApps.removeAllViews();
        for (int i = 0; i < app.getSubApps().size(); i++) {
            View appView = prepareApp(app.getSubApps().get(i));
            GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
            GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
            appView.setLayoutParams(new GridLayout.LayoutParams(rowSpec, colSpec));
            appView.getLayoutParams().width = folderIconWidth;
            appView.getLayoutParams().height = iconHeight;
            folderApps.addView(appView);
        }
        activeFolder = app;
        activeFolderView = v;
    }

    /**
     * Toggles the wobbling animation
     */
    private void handleWobbling(boolean on) {
        for (int i = 0; i < pages.size(); i++) {
            for (int j = 0; j < getGridFromPage(pages.get(i)).getChildCount(); j++) {

                if (movingApp != null &&
                        movingApp == getGridFromPage(pages.get(i)).getChildAt(j)) {
                    continue;
                }

                if (on) {
                    if (getGridFromPage(pages.get(i)).getChildAt(j).getAnimation() == null) {
                        if (j % 2 == 0) {
                            getGridFromPage(pages.get(i)).getChildAt(j).startAnimation(
                                    wobbleAnimation);
                        } else {
                            getGridFromPage(pages.get(i)).getChildAt(j).startAnimation(
                                    wobbleReverseAnimation);
                        }
                    }
                } else {
                    getGridFromPage(pages.get(i)).getChildAt(j).setAnimation(null);
                }
            }
        }
        for (int i = 0; i < dock.getChildCount(); i++) {
            if (movingApp != null && movingApp == dock.getChildAt(i)) {
                continue;
            }
            if (on) {
                if (dock.getChildAt(i).getAnimation() == null) {
                    if (i % 2 == 0) {
                        dock.getChildAt(i).startAnimation(wobbleAnimation);
                    } else {
                        dock.getChildAt(i).startAnimation(wobbleReverseAnimation);
                    }
                }
            } else {
                dock.getChildAt(i).setAnimation(null);
            }
        }

        if (!on) {
            storage.save(pages, dock);
        }
    }

    /**
     * Creates drag listeners for the dock and pages, which are responsible for almost
     * all the drag and drop functionality present in this app.
     */
    private void createDragListener() {
        dock.setOnDragListener(new View.OnDragListener() {

            private int latestIndex;
            private long interestExpressedAt;
            private final int timeToSwap = 100;
            private boolean latestFolderInterest;

            private float lastX, lastY;

            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                if (dock.getChildCount() > nCols) {
                    Log.d(TAG, "Too many children in dock: " + dock.getChildCount());
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

                    lastX = dragEvent.getX();
                    lastY = dragEvent.getY();

                    int index = getIndex(dock, dragEvent.getX(), dragEvent.getY());
                    Log.i(TAG, "onDrag: Index: " + index);
                    // If hovering over self, ignore drag/drop
                    if (index == dock.indexOfChild(movingApp)) {
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
                        View latestCollidingApp = dock.getChildAt(index);
                        if (collidingApp != latestCollidingApp) {
                            makeAppCold(collidingApp, true);
                            collidingApp = dock.getChildAt(index);
                            folderInterest = false;
                        }

                        AppItem moveAppDetails = getAppDetails(movingApp);
                        if (moveAppDetails.isFolder()) {
                            folderInterest = false;
                        } else {
                            latestFolderInterest = checkIfFolderInterest(dock, index,
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

                        if (elapsedTime > timeToSwap && !folderInterest) {
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
                movingApp = (View) dragEvent.getLocalState();
                return true;
            }
        });

        pager.setOnDragListener(new View.OnDragListener() {
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
                                collidingApp = getGridFromPage(page).getChildAt(index);
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
                                    latestFolderInterest = checkIfFolderInterest(dock, index,
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
                                pager.scrollRight();
                            } else if (getCurrentAppsPageNumber() + 1 == pages.size()
                                    && getGridFromPage(page).getChildCount() > 1) {
                                GridLayout layout = preparePage();
                                pages.add(layout);
                                ImageView dot = new ImageView(DesktopActivity.this);
                                dot.setImageDrawable(getDrawable(R.drawable.dot_off));
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        getResources().getDimensionPixelSize(R.dimen.dotSize),
                                        getResources().getDimensionPixelSize(R.dimen.dotSize)
                                );
                                dot.setLayoutParams(params);
                                indicator.addView(dot);
                                pager.addView(layout);
                            }
                        } else if (dragEvent.getX() < scrollCorner) {
                            if (getCurrentAppsPageNumber() == 0) {
                                return true;
                            }
                            if (getCurrentAppsPageNumber() - 1 >= 0) {
                                //removeAppFromPage(page, movingApp);
                                pager.scrollLeft();
                            } else if (getCurrentAppsPageNumber() + 1 == pages.size() - 2
                                    && getGridFromPage(pages.get(pages.size() - 1)).getChildCount()
                                    <= 0) {
                                indicator.removeViewAt(pages.size());
                                pager.removeViewAt(pages.size());
                                pages.remove(pages.size() - 1);
                            }
                        }
                    }
                } else if (dragEvent.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    handleWobbling(false);
                    if (folderWindowContainer.getVisibility() != View.VISIBLE) {
                        // Drop functionality when the folder window container
                        // is not being shown -- default
                        if (!folderInterest) {
                            Log.i(TAG, "onDrag: here");
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
                        Rect bounds = new Rect((int) folderApps.getX(), (int) folderApps.getY(),
                                (int) (folderApps.getWidth() + folderApps.getX()),
                                (int) (folderApps.getHeight() + folderApps.getY()));
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
                        indicator.removeViewAt(getCurrentAppsPageNumber());
                        pager.removeViewAt(getCurrentAppsPageNumber());
                        pager.scrollLeft();
                    }


                    if (getCurrentAppsPageNumber() < pages.size() - 1 && getGridFromPage(
                            pages.get(getCurrentAppsPageNumber() + 1)).getChildCount() <= 0) {
                        pages.remove(getCurrentAppsPageNumber() + 1);
                        indicator.removeViewAt(getCurrentAppsPageNumber() + 2);
                        pager.removeViewAt(getCurrentAppsPageNumber() + 2);
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
            assert app != null;
            app.setBelongsToFolder(false);
            Log.i(TAG, "removeAppFromFolder: Size:" + activeFolder.getSubApps().size());

            if (activeFolder.getSubApps().size() == 1) {
                AppItem item = activeFolder.getSubApps().get(0);
                activeFolder.getSubApps().remove(item);
                item.setBelongsToFolder(false);
                View view = prepareApp(item);
                if (folderFromDock) {
                    addToDock(view, dock.indexOfChild(activeFolderView));
                } else {
                    GridLayout gridLayout = pages.get(getCurrentAppsPageNumber());
                    addAppToPage(gridLayout, view, gridLayout.indexOfChild(activeFolderView));
                }

                ((ViewGroup) activeFolderView.getParent()).removeView(activeFolderView);
            } else {
                updateIcon(activeFolderView, activeFolder,
                        GraphicsUtil.generateFolderIcon(this, activeFolder));
            }
            hideFolderWindowContainer();
            if (movingApp.getParent() != null) {
                ((ViewGroup) movingApp.getParent()).removeView(movingApp);
            }
            movingApp.setVisibility(View.VISIBLE);
            int current = getCurrentAppsPageNumber();
            addAppToPage(pages.get(current), movingApp);
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
     * Adds items to the dock making sure that the GridLayout's parameters are
     * not violated.
     */
    private void addToDock(View view, int index) {
        view.findViewById(R.id.app_label).setVisibility(View.GONE);
        GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED);
        GridLayout.LayoutParams iconLayoutParams = new GridLayout.LayoutParams(rowSpec, colSpec);
        iconLayoutParams.setMargins(getResources().getDimensionPixelSize(R.dimen.app_col_margin),
                0,
                getResources().getDimensionPixelSize(R.dimen.app_col_margin),
                0);
        iconLayoutParams.height = iconHeight;
        iconLayoutParams.width = iconWidth;
        iconLayoutParams.setGravity(Gravity.CENTER);
        view.setLayoutParams(iconLayoutParams);
        if (index != INVALID) {
            dock.addView(view, index);
        } else {
            dock.addView(view);
        }
    }

    /**
     * Creates/updates a folder using the tags associated with the app being dragged,
     * and the target app.
     */
    private void createFolder(boolean fromDock) {
        int index;

        if (fromDock) {
            index = dock.indexOfChild(
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
            folder = new AppItem("Untitled", "", folderIcon, null, "FOLDER", false);
            folder.setFolder(true);
            folder.setFolderID(UUID.randomUUID().toString());

            List<AppItem> subApps = folder.getSubApps();
            app1.setBelongsToFolder(true);
            app2.setBelongsToFolder(true);
            subApps.add(app1);
            subApps.add(app2);

            View folderView = prepareApp(folder);
            if (fromDock) {
                addToDock(folderView, index);
            } else {
                addAppToPage(pages.get(getCurrentAppsPageNumber()), folderView, index);
            }

            ((ViewGroup) collidingApp.getParent()).removeView(collidingApp);

        } else {
            app2.setBelongsToFolder(true);
            app1.getSubApps().add(app2);
            updateIcon(collidingApp, app1, GraphicsUtil.generateFolderIcon(this, app1));
        }


        if (movingApp.getParent() != null) {
            ((ViewGroup) movingApp.getParent()).removeView(movingApp);
        }

        makeAppCold(collidingApp, fromDock);
        makeAppCold(movingApp, fromDock);

        storage.save(pages, dock);
    }

    private void updateIcon(View appView, AppItem app, Drawable drawable) {
        app.setIcon(drawable);
        List<Object> tags = (List<Object>) appView.getTag();
        ImageView iv = (ImageView) tags.get(0);
        iv.setImageDrawable(drawable);
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
        ((View) views.get(1)).setVisibility(View.GONE);
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
            ((View) views.get(1)).setVisibility(View.GONE);
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
            Rect r2 = new Rect((int) (x - iconWidth / 2), (int) (y - iconWidth / 2),
                    (int) (x + iconWidth / 2), (int) (y + iconWidth / 2));
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
     * Creates a dots-based indicator using two simple drawables.
     */
    private void createIndicator() {
        if (indicator.getChildCount() != 0) {
            indicator.removeAllViews();
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.dotSize),
                getResources().getDimensionPixelSize(R.dimen.dotSize)
        );

        for (int i = 0; i < pages.size() + 1; i++) {
            ImageView dot = new ImageView(this);
            dot.setImageDrawable(getDrawable(R.drawable.dot_off));
            dot.setLayoutParams(params);
            indicator.addView(dot);
        }
        updateIndicator();
    }

    private int activeDot;

    private void updateIndicator() {

        Log.i(TAG, "activeDot: [" + activeDot + "] currentPageNumber: [" + currentPageNumber + "]");
        if (indicator.getChildAt(activeDot) != null) {
            ((ImageView) indicator.getChildAt(activeDot)).setImageResource(R.drawable.dot_off);
        }
        if (indicator.getChildAt(currentPageNumber) != null) {
            ((ImageView) indicator.getChildAt(currentPageNumber)).setImageResource(
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
        storage.save(pages, dock);
        folderTitle.clearFocus();
        folderFromDock = false;
        folderWindowContainer.animate().alpha(0f)
                .setDuration(200).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                folderWindowContainer.setVisibility(View.GONE);
                activeFolder = null;
                folderWindowContainer.animate().setListener(null);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        returnToHomeScreen();
    }

    private void returnToHomeScreen() {
        if (searchInput != null) {
            searchInput.setText("");
        }
        if (backAllowed && folderWindowContainer.getVisibility() == View.VISIBLE) {
            hideFolderWindowContainer();
        } else if (backAllowed) {
            if (currentPageNumber != 0 && currentPageNumber != 2) {
                pager.setCurrentPage(1);
                currentPageNumber = 1;
            } else {
                if (currentPageNumber == 0) {
                    pager.scrollRight();
                } else {
                    pager.scrollLeft();
                }
            }
        }
    }
}
