package foundation.e.blisslauncher.features.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.Bundle;

import foundation.e.blisslauncher.BlissLauncher;
import foundation.e.blisslauncher.core.customviews.RoundedWidgetView;

public class WidgetViewBuilder {

    private static final String TAG = "WidgetViewBuilder";

    public static RoundedWidgetView create(Context context, RoundedWidgetView roundedWidgetView) {
        if (BlissLauncher.getApplication(context).getAppWidgetHost() == null) return null;
        roundedWidgetView.post(() -> updateWidgetOption(context, roundedWidgetView,
                roundedWidgetView.getAppWidgetInfo()));

        /*final FrameLayout widgetContainer = (FrameLayout) LayoutInflater.from(context).inflate(
                R.layout.layout_widget_host_container, null);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = context.getResources().getDimensionPixelSize(R.dimen.widget_margin);
        lp.bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.widget_margin);
        roundedWidgetView.setLayoutParams(lp);
        roundedWidgetView.setPadding(0, 0, 0, 0);
        widgetContainer.addView(roundedWidgetView);

        final ImageView border = widgetContainer.findViewById(R.id.border);
        border.bringToFront();
        FrameLayout.LayoutParams borderLp = (FrameLayout.LayoutParams) border.getLayoutParams();
        borderLp.topMargin = context.getResources().getDimensionPixelSize(R.dimen.widget_margin);
        borderLp.bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.widget_margin);
        final ImageView topHandle = widgetContainer.findViewById(R.id.topHandle);
        topHandle.bringToFront();
        final ImageView bottomHandle = widgetContainer.findViewById(R.id.bottomHandle);
        bottomHandle.bringToFront();*/

        /*roundedWidgetView.setOnLongClickListener(v -> {
            FrameLayout frameLayout = (FrameLayout) roundedWidgetView.getParent().getParent();
            frameLayout.addView(new AppWidgetResizeFrame(context, roundedWidgetView));
            return true;
        });*/
        return roundedWidgetView;
    }

    private static void updateWidgetOption(Context context,
            RoundedWidgetView roundedWidgetView,
            AppWidgetProviderInfo info) {
        Bundle newOps = new Bundle();
        newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, BlissLauncher.getApplication(
                context).getDeviceProfile().getMaxWidgetWidth());
        newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, BlissLauncher.getApplication(
                context).getDeviceProfile().getMaxWidgetWidth());
        newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, info.minHeight);
        newOps.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, BlissLauncher.getApplication(
                context).getDeviceProfile().getMaxWidgetHeight());
        roundedWidgetView.updateAppWidgetOptions(newOps);
    }
}
