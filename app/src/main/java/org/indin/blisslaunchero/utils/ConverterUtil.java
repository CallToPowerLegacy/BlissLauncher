package org.indin.blisslaunchero.utils;

import android.content.Context;
import android.util.DisplayMetrics;

public class ConverterUtil {
	public static float dp2Px(int dp, Context context){
	    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
	    return dp * (metrics.densityDpi / 160f);
	}

	public static float px2Dp(float px, Context context) {
	    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
	    return px / (metrics.densityDpi / 160f);
	}
}
