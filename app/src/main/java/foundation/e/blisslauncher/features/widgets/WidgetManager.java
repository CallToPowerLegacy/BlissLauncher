package foundation.e.blisslauncher.features.widgets;

import android.appwidget.AppWidgetHostView;

import java.util.LinkedList;
import java.util.Queue;

public class WidgetManager {
    private static final WidgetManager ourInstance = new WidgetManager();

    private Queue<Integer> removeWidgetIds = new LinkedList<>();
    private Queue<AppWidgetHostView> addWidgetViews = new LinkedList<>();

    public static WidgetManager getInstance() {
        return ourInstance;
    }

    private WidgetManager() {
    }

    public void enqueueRemoveId(int id){
        removeWidgetIds.add(id);
    }

    public void enqueueAddWidget(AppWidgetHostView view){
        addWidgetViews.add(view);
    }

    public Integer dequeRemoveId(){
        return removeWidgetIds.poll();
    }

    public AppWidgetHostView dequeAddWidgetView(){
        return addWidgetViews.poll();
    }
}
