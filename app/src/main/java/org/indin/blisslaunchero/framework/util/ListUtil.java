package org.indin.blisslaunchero.framework.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Amit Kumar
 * Email : mr.doc10jl96@gmail.com
 */

public class ListUtil {
    @SafeVarargs
    public static <T> List<T> asSafeList(T... tArr) {
        return (tArr == null || tArr.length == 0) ? new ArrayList() : Arrays.asList(tArr);
    }
}
