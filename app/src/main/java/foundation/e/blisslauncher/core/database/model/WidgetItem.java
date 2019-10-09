package foundation.e.blisslauncher.core.database.model;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

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
