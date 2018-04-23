package org.indin.blisslaunchero.framework.util;

import java.util.Map;

public class ComponentKeyMapper<T> {

    protected final ComponentKey mComponentKey;

    public ComponentKeyMapper(ComponentKey key) {
        this.mComponentKey = key;
    }

    public T getItem(Map<ComponentKey, T> map) {
        return map.get(mComponentKey);
    }

    public String getPackage() {
        return mComponentKey.componentName.getPackageName();
    }

    public String getComponentClass() {
        return mComponentKey.componentName.getClassName();
    }

    @Override
    public String toString() {
        return mComponentKey.toString();
    }

}
