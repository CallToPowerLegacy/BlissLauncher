package org.indin.blisslaunchero.features.widgets;

import org.indin.blisslaunchero.core.customviews.RoundedWidgetView;

import java.util.LinkedList;
import java.util.Queue;

public class WidgetManager {
    private static final WidgetManager ourInstance = new WidgetManager();

    private Queue<Integer> removeWidgetIds = new LinkedList<>();
    private Queue<RoundedWidgetView> addWidgetViews = new LinkedList<>();

    public static WidgetManager getInstance() {
        return ourInstance;
    }

    private WidgetManager() {
    }

    public void enqueueRemoveId(int id){
        removeWidgetIds.add(id);
    }

    public void enqueueAddWidget(RoundedWidgetView view){
        addWidgetViews.add(view);
    }

    public Integer dequeRemoveId(){
        return removeWidgetIds.poll();
    }

    public RoundedWidgetView dequeAddWidgetView(){
        return addWidgetViews.poll();
    }
}
