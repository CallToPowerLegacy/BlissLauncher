/*
  Credit to Gopal Awasthi
  https://medium.com/@gopalawasthi383/android-recyclerview-drag-and-drop-a3f227cdb641
 */
package foundation.e.blisslauncher.features.widgets;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class WidgetItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private int dragFrom = -1;
    private final AddedWidgetsAdapter adapter;
    private final List<Widget> items;

    public WidgetItemTouchHelperCallback(AddedWidgetsAdapter adapter, List<Widget> items) {
        this.adapter = adapter;
        this.items = items;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags,0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        // get the viewHolder's and target's positions in your adapter data, swap them
        if (viewHolder.getItemViewType() != target.getItemViewType())
            return false;

        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();

        if (dragFrom == -1)
            dragFrom =  fromPosition;

        if (dragFrom != -1 && toPosition != -1 && dragFrom != toPosition) {
            reallyMoved(dragFrom, toPosition);
            dragFrom = -1;
        }

        // and notify the adapter that its dataset has changed
        adapter.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        //nestedScrollView.requestDisallowInterceptTouchEvent(false);
        //recyclerView.setNestedScrollingEnabled(false);

        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }

    private void reallyMoved(int dragFrom, int dragTo) {
        if (dragFrom <= 0 || dragTo >= items.size())
            return;

        if (dragTo < 1)
            dragTo = 1;

        Collections.swap(items, dragFrom-1, dragTo-1);
    }
}
