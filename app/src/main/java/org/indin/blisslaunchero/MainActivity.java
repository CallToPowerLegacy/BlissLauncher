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
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.horizontalpagerlibrary.HorizontalPager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private HorizontalPager pager;
    private View workspace;
    private GridLayout dock;
    private LinearLayout indicator;
    private ViewGroup folderWindowContainer;
    private GridLayout folderApps;
    private BlissInput folderTitle;
    private EditText searchInput;
    private View progressBar;

    private InputMethodManager inputMethodManager;
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

    private List<LinearLayout> pages;

    private int screenWidth;
    private Drawable hotBackground;
    private Drawable defaultBackground;
    private Drawable transparentBackground;
    private int iconSize;
    private boolean dragDropEnabled = true;

    private boolean minimumWidthDetermined = false;

    private View movingApp;
    private View collidingApp;
    private boolean folderInterest;

    private Animation wobbleAnimation;
    private Animation wobbleReverseAnimation;
    private static String TAG = "BLISSLAUNCHER_HOME";
    private boolean backAllowed = true;
    private int scrollCorner;

    private Storage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        workspace = findViewById(R.id.workspace);
        pager = findViewById(R.id.pages_container);

        LinearLayout dockContainer = findViewById(R.id.dock);
        dock = (GridLayout) dockContainer.getChildAt(0);

        indicator = findViewById(R.id.page_indicator);
        folderWindowContainer = findViewById(R.id.folder_window_container);
        folderApps = findViewById(R.id.folder_apps);
        folderTitle = findViewById(R.id.folder_title);
        progressBar = findViewById(R.id.progressbar);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        storage = new Storage(getApplicationContext());

        prepareBroadcastReceivers();

        createLauncher();
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
                String packageName = intent.getData().toString();
                if (packageName.indexOf(":") != -1) {
                    packageName = packageName.split(":")[1];
                }
                Log.i(TAG, "Installed " + packageName);
                addNewApp(packageName);
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
            if (getGridFromPage(pages.get(getCurrentAppsPageNumber())).getChildCount()
                    < maxAppsPerPage - 1) {
                addAppToPage(pages.get(getCurrentAppsPageNumber()), view);
            } else {
                addAppToPage(pages.get(pages.size() - 1), view);
            }
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
        nRows = getResources().getInteger(R.integer.row_count);
        nCols = getResources().getInteger(R.integer.col_count);
        maxAppsPerPage = nRows * nCols;
        screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        maxDistanceForFolderCreation = getResources()
                .getDimensionPixelSize(R.dimen.maxDistanceForFolderCreation);
        hotBackground = getResources().getDrawable(R.drawable.rounded_corners_icon_hot, null);
        defaultBackground = getResources().getDrawable(R.drawable.rounded_corners_icon, null);
        iconSize = getResources().getDimensionPixelSize(R.dimen.iconSize);
        scrollCorner = getResources().getDimensionPixelSize(R.dimen.scrollCorner);
        wobbleAnimation = AnimationUtils.loadAnimation(this, R.anim.wobble);
        wobbleReverseAnimation = AnimationUtils.loadAnimation(this, R.anim.wobble_reverse);
        transparentBackground = getResources().getDrawable(R.drawable.transparent, null);
        inputMethodManager =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
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
                        createDragListener();
                        createPageChangeListener();
                        createFolderTitleListener();
                        createWidgetsPage();
                        createIndicator();

                        fixMinimumGridWidth();
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
            WallpaperManager wm = WallpaperManager.getInstance(this);
            try {
                wm.setBitmap(IconPackUtil.getWallpaper());
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

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.reenter, R.anim.releave);

        if (!minimumWidthDetermined) {
            fixMinimumGridWidth();
        }
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
                currentPageNumber = page;

                // Remove indicator and dock from widgets page, and make them
                // reappear when user swipes to the first apps page
                if (currentPageNumber == 0) {
                    indicator.animate().alpha(0).setDuration(300).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            indicator.setVisibility(View.GONE);
                        }
                    });
                    ((View) dock.getParent()).animate().translationYBy(500).setDuration(
                            300).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            ((View) dock.getParent()).setVisibility(View.GONE);
                        }
                    });
                } else {
                    if (indicator.getAlpha() != 1.0f) {
                        indicator.setVisibility(View.VISIBLE);
                        ((View) dock.getParent()).setVisibility(View.VISIBLE);
                        indicator.animate().alpha(1).setDuration(300);
                        ((View) dock.getParent()).animate().translationY(0).setDuration(300);
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
        if (storage.isLayoutPresent()) {
            Log.i(TAG, "Preparing UI from storage");
            createUIFromStorage();
            return;
        }
        int nPages = (int) Math.ceil((float) launchableApps.size() / (float) maxAppsPerPage) + 1;
        Log.i(TAG, "createUI: npages: " + nPages + ", max: " + maxAppsPerPage);
        pages = new ArrayList<>();
        for (int i = 0; i < nPages; i++) {
            LinearLayout page = preparePage();
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

    }

    private void fixMinimumGridWidth() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            _fixMinimumGridWidth();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void _fixMinimumGridWidth() {
        // Find the grid with the maximum width
        int maxWidth = 0;
        for (int i = 0; i < pages.size(); i++) {
            GridLayout grid = getGridFromPage(pages.get(i));
            if (grid.getWidth() > maxWidth) {
                maxWidth = grid.getWidth();
            }
        }
        Log.i(TAG, "Setting minimum width to " + maxWidth);
        if (maxWidth < screenWidth / 3) {
            maxWidth = screenWidth - 30;
        }
        for (int i = 0; i < pages.size(); i++) {
            GridLayout grid = getGridFromPage(pages.get(i));
            grid.setMinimumWidth(maxWidth);
        }
        //dock.setMinimumWidth(maxWidth);
        minimumWidthDetermined = true;
    }

    private LinearLayout preparePage() {
        LinearLayout page = (LinearLayout) getLayoutInflater().inflate(R.layout.apps_page, null);
        GridLayout grid = getGridFromPage(page);
        grid.setLayoutTransition(getDefaultLayoutTransition());
        return page;
    }

    /**
     * Re-creates the launcher layout based on the data stored in the shared-preferences.
     */
    private void createUIFromStorage() {
        Storage.StorageData storageData = storage.load();
        int nPages = storageData.getNPages();
        pages = new ArrayList<>();
        for (int i = 0; i < nPages; i++) {
            LinearLayout page = preparePage();
            pages.add(page);

            try {
                JSONArray pageData = storageData.pages.getJSONArray(i);
                for (int j = 0; j < pageData.length(); j++) {
                    JSONObject currentItemData = pageData.getJSONObject(j);
                    View appView = prepareAppFromJSON(currentItemData);
                    if (appView != null) {
                        addAppToPage(page, appView);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading page data " + e);
            }

        }

        // Always keep an extra empty page handy
        if (pages.get(pages.size() - 1).getChildCount() > 2) {
            pages.add(preparePage());
        }

        for (int i = 0; i < pages.size(); i++) {
            pager.addView(pages.get(i));
        }

        currentPageNumber = 0;
        dock.setLayoutTransition(getDefaultLayoutTransition());

        for (int i = 0; i < storageData.getNDocked(); i++) {
            try {
                JSONObject currentDockItemData = storageData.dock.getJSONArray(0).getJSONObject(i);
                View appView = prepareAppFromJSON(currentDockItemData);
                if (appView != null) {
                    addToDock(appView, INVALID);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading dock data " + e);
            }
        }
    }

    private void createWidgetsPage() {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.widgets_page,
                null);
        pager.addView(layout, 0);
        currentPageNumber = 1;
        pager.setCurrentPage(currentPageNumber);
        searchInput = layout.findViewById(R.id.search_input);
        searchInput.setOnDragListener(null);

        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int action, KeyEvent keyEvent) {
                if (action == EditorInfo.IME_ACTION_GO) {
                    runSearch(searchInput.getText().toString());
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

    private void addAppToPage(LinearLayout page, View view) {
        view.findViewById(R.id.app_label).setVisibility(View.VISIBLE);
        getGridFromPage(page).addView(view);
    }

    private void addAppToPage(LinearLayout page, View view, int index) {
        view.findViewById(R.id.app_label).setVisibility(View.VISIBLE);
        getGridFromPage(page).addView(view, index);
    }

    private void removeAppFromPage(LinearLayout page, View view) {
        getGridFromPage(page).removeView(view);
    }

    /**
     * Creates a View that can be displayed by the launcher using just stored
     * JSON data.
     */
    private View prepareAppFromJSON(JSONObject currentItemData) throws Exception {
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
            output = prepareApp(folderItem);
        } else {
            AppItem appItem = prepareAppItemFromComponent(componentName);
            if (appItem != null) {
                output = prepareApp(appItem);
            }
        }
        return output;
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
        final ImageView icon = v.findViewById(R.id.app_icon);
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
                View.DragShadowBuilder dragShadowBuilder = new View.DragShadowBuilder(icon);
                v.startDrag(null, dragShadowBuilder, v, 0);
                v.setVisibility(View.INVISIBLE);
                movingApp = v;
                dragDropEnabled = true;

                if (!app.isBelongsToFolder()) {
                    handleWobbling(true);
                }

                return false;
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
        folderWindowContainer.animate().alpha(1.0f).setDuration(800);

        folderTitle.setText(app.getLabel());
        folderTitle.setCursorVisible(false);
        folderApps.removeAllViews();
        for (int i = 0; i < app.getSubApps().size(); i++) {
            View appView = prepareApp(app.getSubApps().get(i));
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
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                if (getAppDetails(movingApp).isBelongsToFolder()) {
                    return true;
                }

                if (dock.getChildCount() > nCols) {
                    Log.d(TAG, "Too many children in dock: " + dock.getChildCount());
                    return false;
                }

                if (dragEvent.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                    int index = getIndex(dock, dragEvent.getX(), dragEvent.getY());
                    if (index >= nCols || dock.getChildCount() >= nCols) {
                        Log.d(TAG, "Won't add");
                        return true;
                    }
                    if (movingApp.getParent() != null) {
                        ((ViewGroup) movingApp.getParent()).removeView(movingApp);
                    }
                    addToDock(movingApp, index);
                }

                movingApp = (View) dragEvent.getLocalState();
                return true;
            }
        });

        pager.setOnDragListener(new View.OnDragListener() {
            private int latestIndex;
            private long interestExpressedAt;
            private final int timeToSwap = 300;
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

                    LinearLayout page = pages.get(getCurrentAppsPageNumber());
                    if (dragEvent.getX() < screenWidth - scrollCorner
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
                                makeAppCold(collidingApp);
                                collidingApp = getGridFromPage(page).getChildAt(index);
                                folderInterest = false;
                            }

                            AppItem moveAppDetails = getAppDetails(movingApp);
                            if (moveAppDetails.isFolder()) {
                                folderInterest = false;
                            } else {
                                latestFolderInterest = checkIfFolderInterest(index,
                                        dragEvent.getX(), dragEvent.getY());
                                if (latestFolderInterest != folderInterest) {
                                    folderInterest = latestFolderInterest;
                                    interestExpressedAt = System.currentTimeMillis();
                                }
                                if (folderInterest) {
                                    makeAppHot(collidingApp);
                                } else {
                                    makeAppCold(collidingApp);
                                }
                            }
                        }

                        if (index == latestIndex) {
                            long elapsedTime = System.currentTimeMillis() - interestExpressedAt;
                            if (elapsedTime > timeToSwap && !folderInterest) {
                                if (movingApp.getParent() != null) {
                                    ((ViewGroup) movingApp.getParent()).removeView(movingApp);
                                }
                                if (index != INVALID) {
                                    addAppToPage(page, movingApp, index);
                                } else {
                                    addAppToPage(page, movingApp);
                                }
                            }
                        } else {
                            latestIndex = index;
                            interestExpressedAt = System.currentTimeMillis();
                        }
                    } else {
                        if (dragEvent.getX() > screenWidth - scrollCorner) {
                            if (getCurrentAppsPageNumber() + 1 < pages.size()) {
                                removeAppFromPage(page, movingApp);
                                pager.scrollRight();
                            }
                        } else if (dragEvent.getX() < scrollCorner) {
                            if (getCurrentAppsPageNumber() - 1 >= 0) {
                                removeAppFromPage(page, movingApp);
                                pager.scrollLeft();
                            }
                        }
                    }
                }

                if (dragEvent.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    System.out.println("Drag ended.");
                    handleWobbling(false);
                    if (folderWindowContainer.getVisibility() != View.VISIBLE) {
                        // Drop functionality when the folder window container
                        // is not being shown -- default
                        if (!folderInterest) {
                            if (movingApp.getParent() == null) {
                                if (view instanceof HorizontalPager) {
                                    addAppToPage(pages.get(getCurrentAppsPageNumber()), movingApp);
                                }
                                if (view instanceof GridLayout && view.getId() == R.id.dock) {
                                    addToDock(movingApp, INVALID);
                                }
                            }
                            movingApp.setVisibility(View.VISIBLE);
                        } else {
                            createFolder();
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
                }
                return true;
            }
        });
    }

    private void removeAppFromFolder() {
        AppItem app = getAppDetails(movingApp);
        activeFolder.getSubApps().remove(app);
        app.setBelongsToFolder(false);

        if (activeFolder.getSubApps().size() == 0) {
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
        addAppToPage(pages.get(getCurrentAppsPageNumber()), movingApp);
    }

    /**
     * Returns an app to normal if the user doesn't express move/folder-creation interests.
     */
    private void discardCollidingApp() {
        if (collidingApp != null) {
            makeAppCold(collidingApp);
            collidingApp = null;
            folderInterest = false;
        }
    }

    /**
     * Adds items to the dock making sure that the GridLayout's parameters are
     * not violated.
     */
    private void addToDock(View view, int index) {

        // Hide label in dock
        view.findViewById(R.id.app_label).setVisibility(View.GONE);

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
    private void createFolder() {
        int index = getGridFromPage(pages.get(getCurrentAppsPageNumber())).indexOfChild(
                collidingApp);

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
            addAppToPage(pages.get(getCurrentAppsPageNumber()), folderView, index);

            ((ViewGroup) collidingApp.getParent()).removeView(collidingApp);

        } else {
            app2.setBelongsToFolder(true);
            app1.getSubApps().add(app2);
            updateIcon(collidingApp, app1, GraphicsUtil.generateFolderIcon(this, app1));
        }


        if (movingApp.getParent() != null) {
            ((ViewGroup) movingApp.getParent()).removeView(movingApp);
        }

        makeAppCold(collidingApp);
        makeAppCold(movingApp);

        storage.save(pages, dock);
        Log.i(TAG, "Created folder");
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
    private void makeAppCold(View app) {
        if (app == null) {
            return;
        }

        List<Object> views = (List<Object>) app.getTag();
        ((View) (views.get(0))).setBackground(
                getDefaultBackground(((AppItem) views.get(2)))
        );
        ((View) views.get(1)).setVisibility(View.VISIBLE);
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
    private boolean checkIfFolderInterest(int index, float x, float y) {
        View v = getGridFromPage(pages.get(getCurrentAppsPageNumber())).getChildAt(index);
        double distance = getDistance(x, y, v.getX() + v.getWidth() / 2,
                v.getY() + v.getHeight() / 2);
        if (distance < maxDistanceForFolderCreation) {
            return true;
        }
        return false;
    }

    private GridLayout getGridFromPage(ViewGroup page) {
        return (GridLayout) page.getChildAt(0);
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
        GridLayout grid;
        if (page instanceof LinearLayout) {
            grid = getGridFromPage(page);
        } else {
            grid = (GridLayout) page;
        }

        for (int i = 0; i < grid.getChildCount(); i++) {
            View v = grid.getChildAt(i);
            Rect r = new Rect((int) v.getX(), (int) v.getY(), (int) v.getX() + v.getWidth(),
                    (int) v.getY() + v.getHeight());
            Rect r2 = new Rect((int) (x - iconSize / 2), (int) (y - iconSize / 2),
                    (int) (x + iconSize / 2), (int) (y + iconSize / 2));
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

        for (int i = 0; i < pages.size() + 1; i++) {
            ImageView dot = new ImageView(this);
            dot.setImageDrawable(getDrawable(R.drawable.dot_off));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.dotSize),
                    getResources().getDimensionPixelSize(R.dimen.dotSize)
            );
            dot.setLayoutParams(params);
            indicator.addView(dot);
        }
        updateIndicator();
    }

    private int activeDot;

    private void updateIndicator() {
        ((ImageView) indicator.getChildAt(activeDot)).setImageResource(R.drawable.dot_off);
        ((ImageView) indicator.getChildAt(currentPageNumber)).setImageResource(R.drawable.dot_on);
        activeDot = currentPageNumber;
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
        folderWindowContainer.animate().alpha(0f)
                .setDuration(800).setListener(new AnimatorListenerAdapter() {
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
