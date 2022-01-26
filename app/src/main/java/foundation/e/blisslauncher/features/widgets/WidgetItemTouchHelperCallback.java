package foundation.e.blisslauncher.features.widgets;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import foundation.e.blisslauncher.core.customviews.RoundedWidgetView;
import foundation.e.blisslauncher.core.customviews.WidgetHost;

public class WidgetItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private int dragFrom = -1;
    private final AddedWidgetsAdapter adapter;
    private final WidgetHost widgetHost;
    private final Context applicationContext;

    public WidgetItemTouchHelperCallback(AddedWidgetsAdapter adapter, WidgetHost widgetHost, Context applicationContext) {
        this.adapter = adapter;
        this.widgetHost = widgetHost;
        this.applicationContext = applicationContext;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags,0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        if (viewHolder.getItemViewType() != target.getItemViewType())
            return false;

        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();
        List<Widget> items = adapter.getAppWidgetProviderInfos();

        if (dragFrom == -1)
            dragFrom = fromPosition;

        if (toPosition < 0)
            toPosition = 0;

        if (dragFrom != toPosition && reallyMoved(dragFrom, toPosition, items)) {
            Widget widget = items.get(toPosition);
            RoundedWidgetView hostView = (RoundedWidgetView) widgetHost.createView(
                    applicationContext, widget.id, widget.info);
            hostView.setNewContainerIndex(toPosition);
            WidgetManager.getInstance().enqueueMoveWidget(hostView);
            dragFrom = -1;
        }

        adapter.notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }

    private boolean reallyMoved(int dragFrom, int dragTo, List<Widget> items) {
        if (dragFrom < 0 || dragTo >= items.size())
            return false;

        Collections.swap(items, dragFrom, dragTo);
        Collections.swap(WidgetManager.getInstance().getCurrentWidgetIds(), dragFrom, dragTo);
        return true;
    }
}
