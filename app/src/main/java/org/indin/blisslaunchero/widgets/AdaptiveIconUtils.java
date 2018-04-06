package org.indin.blisslaunchero.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.math.MathUtils;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.util.SparseIntArray;

import org.indin.blisslaunchero.ui.LauncherActivity;

public class AdaptiveIconUtils {

    public static String getMaskPath() {
        return "M50,0L70,0 A30,30,0,0 1 100,30 L100,70 A30,30,0,0 1 70,100 L30,100 A30,30,0,0 1 "
                + "0,70 L 0,30 A30,30,0,0 1 30,0z";
    }
}
