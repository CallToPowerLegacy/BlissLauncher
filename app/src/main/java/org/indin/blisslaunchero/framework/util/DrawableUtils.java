package org.indin.blisslaunchero.framework.util;

import android.content.res.Resources;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.RotateDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.graphics.drawable.VectorDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import org.indin.blisslaunchero.framework.customviews.AdaptiveIconDrawableCompat;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by falcon on 19/4/18.
 */

public class DrawableUtils {
    private static Method methodInflateFromXml;
    private static ClassLoader mClassLoader;
    private static Class classDrawableInflater;
    private static Field fieldClassLoader;

    private static final String TAG = "DrawableUtils";


    private static Method methodGetDrawableInflater;

    private static ClassLoader wrappedClassLoader;

    static {
        mClassLoader = DrawableUtils.class.getClassLoader();
        try {
            classDrawableInflater = mClassLoader.loadClass(
                    "android.graphics.drawable.DrawableInflater");
            methodInflateFromXml = classDrawableInflater.getDeclaredMethod("inflateFromXml",
                    String.class, XmlPullParser.class, AttributeSet.class, Resources.Theme.class);
            fieldClassLoader = classDrawableInflater.getDeclaredField("mClassLoader");
            methodGetDrawableInflater = Resources.class.getDeclaredMethod("getDrawableInflater");
            wrappedClassLoader = new ClassLoader() {
                @Override
                protected Class<?> loadClass(String name, boolean resolve)
                        throws ClassNotFoundException {
                    Log.i(TAG, "loadClass: "+name);
                    return mClassLoader.loadClass(name.equals("adaptive-icon") ?
                            AdaptiveIconDrawableCompat.class.getName() : name);
                }
            };
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static Object getDrawableInflater(Resources res)
            throws InvocationTargetException, IllegalAccessException {
        Object inflater = DrawableUtils.methodGetDrawableInflater.invoke(res);
        DrawableUtils.fieldClassLoader.setAccessible(true);
        DrawableUtils.fieldClassLoader.set(inflater, wrappedClassLoader);
        return inflater;
    }

    public static Drawable inflateFromXml(Object drawableInflater, XmlPullParser parser) {
        return inflateFromXml(drawableInflater, parser, null);
    }

    public static Drawable inflateFromXml(Object drawableInflater, XmlPullParser parser,
            Resources.Theme theme) {
        return inflateFromXml(drawableInflater, parser, Xml.asAttributeSet(parser), theme);
    }

    public static Drawable inflateFromXml(Object drawableInflater, XmlPullParser parser,
            AttributeSet attributeSet, Resources.Theme theme) {
        try {
            while (parser.next() != XmlPullParser.START_TAG) {
                if (parser.getEventType() == XmlPullParser.END_DOCUMENT) break;
            }

            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new XmlPullParserException("No start tag found");
            }

            return (Drawable) methodInflateFromXml
                    .invoke(drawableInflater, parser.getName(), parser, attributeSet, theme);
        } catch (XmlPullParserException | IllegalAccessException | IOException |
                InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*private Drawable inflateFromTag(String name) {
        switch (name) {
            case "selector":
                return new StateListDrawable();
            case "adaptive-icon":
                return new AdaptiveIconDrawableCompat();
            case "animated-selector":
                return new AnimatedStateListDrawable();
            case "level-list":
                return new LevelListDrawable();
            case "layer-list":
                return new LayerDrawable();
            case "transition":
                return new TransitionDrawable();
            case "ripple":
                return new RippleDrawable();
            case "color":
                return new ColorDrawable();
            case "shape":
                return new GradientDrawable();
            case "vector":
                return new VectorDrawable();
            case "animated-vector":
                return new AnimatedVectorDrawable();
            case "scale":
                return new ScaleDrawable();
            case "clip":
                return new ClipDrawable();
            case "rotate":
                return new RotateDrawable();
            case "animated-rotate":
                return new AnimatedRotateDrawable();
            case "animation-list":
                return new AnimationDrawable();
            case "inset":
                return new InsetDrawable();
            case "bitmap":
                return new BitmapDrawable();
            case "nine-patch":
                return new NinePatchDrawable();
            default:
                return null;
        }*/
}
