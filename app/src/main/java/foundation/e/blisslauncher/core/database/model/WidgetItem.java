package foundation.e.blisslauncher.core.database.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "widget_items")
public class WidgetItem {

    @PrimaryKey
    public int id;
    public int height;

    public WidgetItem(){

    }

    public WidgetItem(int id, int height){
        this.id = id;
        this.height = height;
    }
}
