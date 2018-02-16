package org.indin.blisslaunchero;

import android.content.Context;
import android.util.DisplayMetrics;

public class Utils {
	public static float dp2Px(int dp, Context context){
	    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
	    return dp * (metrics.densityDpi / 160f);
	}

	public static float px2Dp(float px, Context context) {
	    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
	    return px / (metrics.densityDpi / 160f);
	}
}
