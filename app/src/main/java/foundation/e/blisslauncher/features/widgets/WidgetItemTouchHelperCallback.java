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
    private final List<Widget> items;
    private final WidgetHost widgetHost;
    private final Context applicationContext;

    public WidgetItemTouchHelperCallback(AddedWidgetsAdapter adapter, List<Widget> items, WidgetHost widgetHost, Context applicationContext) {
        this.adapter = adapter;
        this.items = items;
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

        if (dragFrom == -1)
            dragFrom = fromPosition;

        if (toPosition < 0)
            toPosition = 0;

        if (reallyMoved(dragFrom, toPosition)) {
            adapter.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            adapter.setAppWidgetProviderInfos(items);
            Widget widget = items.get(toPosition);
            RoundedWidgetView hostView = (RoundedWidgetView) widgetHost.createView(
                    applicationContext, widget.id, widget.info);
            hostView.setOriginalContainerIndex(fromPosition);
            hostView.setNewContainerIndex(toPosition);
            WidgetManager.getInstance().enqueueMoveWidget(hostView);
        }

        dragFrom = -1;
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }

    private boolean reallyMoved(int dragFrom, int dragTo) {
        if (dragFrom < 0 || dragTo >= items.size())
            return false;

        Collections.swap(items, dragFrom, dragTo);
        return true;
    }
}
