package foundation.e.blisslauncher.features.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;

import java.util.ArrayList;
import java.util.List;

import foundation.e.blisslauncher.BlissLauncher;
import foundation.e.blisslauncher.R;

public class WidgetsActivity extends Activity implements AddedWidgetsAdapter.OnActionClickListener {

    private RecyclerView addedWidgets;
    private AddedWidgetsAdapter mAddedWidgetsAdapter;

    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;

    private static final int REQUEST_PICK_APPWIDGET = 455;
    private static final int REQUEST_CREATE_APPWIDGET = 189;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widgets);

        mAppWidgetManager = BlissLauncher.getApplication(this).getAppWidgetManager();
        mAppWidgetHost = BlissLauncher.getApplication(this).getAppWidgetHost();

        addedWidgets = findViewById(R.id.added_widgets_recycler_view);
        addedWidgets.setLayoutManager(new LinearLayoutManager(this));
        addedWidgets.setHasFixedSize(false);
        addedWidgets.setNestedScrollingEnabled(false);
        addedWidgets.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        mAddedWidgetsAdapter = new AddedWidgetsAdapter(this, metrics.densityDpi);
        addedWidgets.setAdapter(mAddedWidgetsAdapter);

        refreshRecyclerView();

        findViewById(R.id.add_widget_button).setOnClickListener(view -> {
            selectWidget();
        });
    }

    private void refreshRecyclerView() {
        List<Widget> widgets = new ArrayList<>();
        for (int id : mAppWidgetHost.getAppWidgetIds()) {
            AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(id);
            if (appWidgetInfo != null) {
                Widget widget = new Widget();
                widget.id = id;
                widget.info = appWidgetInfo;
                widgets.add(widget);
            }
        }
        mAddedWidgetsAdapter.setAppWidgetProviderInfos(widgets);
    }

    @Override
    public void removeWidget(int id) {
        mAppWidgetHost.deleteAppWidgetId(id);
        WidgetManager.getInstance().enqueueRemoveId(id);
    }

    void selectWidget() {
        int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        addEmptyData(pickIntent);
        startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
    }

    void addEmptyData(Intent pickIntent) {
        ArrayList customInfo = new ArrayList();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList customExtras = new ArrayList();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_APPWIDGET) {
            configureWidget(data);
        } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
            createWidget(data);
        } else if (resultCode == RESULT_CANCELED && data != null) {
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                removeWidget(appWidgetId);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void configureWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo != null && appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            createWidget(data);
        }
    }

    public void createWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        AppWidgetHostView hostView = mAppWidgetHost.createView(getApplicationContext(), appWidgetId,
                appWidgetInfo);
        hostView.setAppWidget(appWidgetId, appWidgetInfo);
        WidgetManager.getInstance().enqueueAddWidget(hostView);
        refreshRecyclerView();
    }
}
