package foundation.e.blisslauncher.features.widgets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import foundation.e.blisslauncher.core.customviews.RoundedWidgetView;

public class WidgetManager {
    private static final WidgetManager ourInstance = new WidgetManager();

    private List<Integer> currentWidgetIds = new ArrayList<>();
    private Queue<Integer> removeWidgetIds = new LinkedList<>();
    private Queue<RoundedWidgetView> addWidgetViews = new LinkedList<>();
    private Queue<RoundedWidgetView> moveWidgetViews = new LinkedList<>();

    public static WidgetManager getInstance() {
        return ourInstance;
    }

    private WidgetManager() {
    }

    public void enqueueRemoveId(int id) {
        // If the widget is not yet created but scheduled to be created we have to prevent the
        // creation, too.
        Iterator<RoundedWidgetView> it = addWidgetViews.iterator();
        while (it.hasNext()) {
            RoundedWidgetView view = it.next();
            if (id == view.getAppWidgetId()) {
                addWidgetViews.remove(view);
                break;
            }
        }
        removeWidgetIds.add(id);
    }

    public void enqueueRemoveId(int id, int index) {
        enqueueRemoveId(id);
        for (RoundedWidgetView view : moveWidgetViews) {
            if (view.getNewContainerIndex() == index)
                moveWidgetViews.remove(view);
            if (view.getNewContainerIndex() > index)
                view.setNewContainerIndex(view.getNewContainerIndex()-1);
        }
    }

    public void enqueueAddWidget(RoundedWidgetView view) {
        addWidgetViews.add(view);
    }

    public void enqueueMoveWidget(RoundedWidgetView view) {
        RoundedWidgetView contained = moveWidgetViews.stream().filter(v -> v.getAppWidgetId() == view.getAppWidgetId()).findFirst().orElse(null);
        if (contained == null)
            moveWidgetViews.add(view);
        else
            contained.setNewContainerIndex(view.getNewContainerIndex());
    }

    public Integer dequeRemoveId() {
        return removeWidgetIds.poll();
    }

    public RoundedWidgetView dequeAddWidgetView() {
        return addWidgetViews.poll();
    }

    public RoundedWidgetView dequeMoveWidgetView() {
        return moveWidgetViews.poll();
    }

    public List<Integer> getCurrentWidgetIds() {
        return currentWidgetIds;
    }
    public void setCurrentWidgetIds(List<Integer> widgetIds) {
        currentWidgetIds = widgetIds;
    }
}
