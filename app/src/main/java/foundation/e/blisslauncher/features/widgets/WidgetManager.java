package foundation.e.blisslauncher.features.widgets;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import foundation.e.blisslauncher.core.customviews.RoundedWidgetView;

public class WidgetManager {
    private static final WidgetManager ourInstance = new WidgetManager();

    private int[] currentWidgetIds = new int[0];
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
            if (view.getOriginalContainerIndex() > index)
                view.setOriginalContainerIndex(view.getOriginalContainerIndex()-1);
        }
    }

    public void enqueueAddWidget(RoundedWidgetView view) {
        addWidgetViews.add(view);
    }

    public void enqueueMoveWidget(RoundedWidgetView view) {
        moveWidgetViews.add(view);
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

    public int[] getCurrentWidgetIds() {
        return currentWidgetIds;
    }
    public void setCurrentWidgetIds(List<Integer> widgetIds) {
        int[] newWidgetIds = new int[widgetIds.size()];
        for (int i = 0; i < widgetIds.size(); i++) {
            newWidgetIds[i] = widgetIds.get(i);
        }
        currentWidgetIds = newWidgetIds;
    }
}
