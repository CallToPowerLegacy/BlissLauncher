package foundation.e.blisslauncher.core.blur;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.hoko.blur.HokoBlur;
import com.hoko.blur.processor.BlurProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import foundation.e.blisslauncher.BlissLauncher;
import foundation.e.blisslauncher.core.Utilities;

public class BlurWallpaperProvider {

    private final Context context;
    private final WallpaperManager wallpaperManager;

    private final BlurProcessor blurProcessor;

    private ExecutorService mDispatcher = Executors.newSingleThreadExecutor();

    private final Runnable updateRunnable = () -> updateWallpaper();

    private DisplayMetrics displayMetrics = new DisplayMetrics();

    private static final float SAMPLE_FACTOR = 8.0f;

    private static BlurWallpaperProvider sInstance;
    private Bitmap wallpaper;
    private volatile Future mFuture;
    private Listener listener;

    private BlurWallpaperProvider(Context context) {
        this.context = context;
        this.wallpaperManager = WallpaperManager.getInstance(context);
        blurProcessor = HokoBlur.with(context).sampleFactor(SAMPLE_FACTOR)
                .scheme(HokoBlur.SCHEME_OPENGL)
                .mode(HokoBlur.MODE_STACK)
                .forceCopy(false)
                .needUpscale(true)
                .processor();
        init();
        listener = (Listener) context;
    }

    public static BlurWallpaperProvider getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BlurWallpaperProvider(context);
        }
        return sInstance;
    }

    private void init() {
        updateWallpaper();
    }

    private void updateWallpaper() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (wallpaperManager.getWallpaperInfo() != null) {
            // Wallpaper is live wallpaper so can't support blur effect.
            return;
        }
        wallpaper = Utilities.drawableToBitmap(wallpaperManager.getDrawable(), true);
        wallpaper = scaleAndCropToScreenSize(wallpaper);
    }


    public void blur(int radius) {
        cancelPreTask(false);
        mFuture = mDispatcher.submit(new BlurTask(wallpaper, blurProcessor, radius) {
            @Override
            void onBlurSuccess(Bitmap bitmap) {
                if (bitmap != null && listener != null) {
                    listener.blurBackgroundLayer(bitmap);
                }
            }

            @Override
            void onBlurFailed(float factor) {
                listener.fallbackToDimBackground(factor);
            }
        });
    }

    public Bitmap mergeLauncherView(Bitmap launcherView) {
        updateWallpaper();
        if (wallpaper == null) {  // possibly we don't have access to read the wallpaper or the wallpaper is live wallpaper.
            return null;
        }
        int wallpaperWidth = wallpaper.getWidth();
        int wallpaperHeight = wallpaper.getHeight();
        int overlayWidth = launcherView.getWidth();
        int overlayHeight = launcherView.getHeight();

        // Hack for removing soft navigation bar
        if(overlayHeight > wallpaperHeight) {
            overlayHeight = wallpaperHeight;
            launcherView = Bitmap.createBitmap(launcherView, 0, 0, overlayWidth, overlayHeight);
        }

        float marginLeft = (float) (wallpaperWidth * 0.5 - overlayWidth * 0.5);
        float marginTop = (float) (wallpaperHeight * 0.5 - overlayHeight * 0.5);

        Bitmap finalBitmap = Bitmap.createBitmap(wallpaperWidth, wallpaperHeight, wallpaper.getConfig());
        Canvas canvas = new Canvas(finalBitmap);
        canvas.drawBitmap(wallpaper, new Matrix(), null);
        canvas.drawBitmap(launcherView, marginLeft, marginTop, null);
        return finalBitmap;
    }

    public void blurWithLauncherView(Bitmap view, int radius) {
        cancelPreTask(false);
        mFuture = mDispatcher.submit(new BlurTask(view, blurProcessor, radius) {
            @Override
            void onBlurSuccess(Bitmap bitmap) {
                if (bitmap != null && listener != null) {
                    listener.blurFrontLayer(bitmap);
                }
            }

            @Override
            void onBlurFailed(float factor) {
                listener.fallbackToDimBackground(factor);
            }
        });
    }

    public void cancelPreTask(boolean interrupt) {
        if (mFuture != null && !mFuture.isCancelled() && !mFuture.isDone()) {
            mFuture.cancel(interrupt);
            mFuture = null;
        }
    }

    private Bitmap scaleAndCropToScreenSize(Bitmap wallpaper) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getRealMetrics(displayMetrics);

        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        float widthFactor = ((float) width) / wallpaper.getWidth();
        float heightFactor = ((float) height) / wallpaper.getHeight();

        float upscaleFactor = Math.max(widthFactor, heightFactor);

        if (upscaleFactor <= 0) {
            return wallpaper;
        }

        int scaledWidth = (int) Math.max(width, (wallpaper.getWidth() * upscaleFactor));
        int scaledHeight = (int) Math.max(height, (wallpaper.getHeight() * upscaleFactor));

        wallpaper = Bitmap.createScaledBitmap(wallpaper, scaledWidth, scaledHeight, false);

        int navigationBarHeight = 0;
        if (BlissLauncher.getApplication(context).getDeviceProfile().hasSoftNavigationBar(context)) {

            int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navigationBarHeight = context.getResources().getDimensionPixelSize(resourceId);
            }
        }

        int y;
        if (wallpaper.getHeight() > height) {
            y = (wallpaper.getHeight() - height) / 2;
        } else y = 0;

        return Bitmap.createBitmap(wallpaper, 0, y, width, height - navigationBarHeight);
    }

    public interface Listener {
        void blurBackgroundLayer(Bitmap bitmap);

        void blurFrontLayer(Bitmap bitmap);

        void fallbackToDimBackground(float dimAlpha);
    }

    public void clear() {
        listener = null;
        cancelPreTask(true);
        sInstance = null;
    }

    private abstract static class BlurTask implements Runnable {
        private Bitmap bitmap;
        private BlurProcessor blurProcessor;
        private int radius;

        BlurTask(Bitmap bitmap, BlurProcessor blurProcessor, int radius) {
            this.bitmap = bitmap;
            this.blurProcessor = blurProcessor;
            this.radius = radius;
        }

        @Override
        public void run() {
            if (bitmap != null && !bitmap.isRecycled() && blurProcessor != null) {
                blurProcessor.radius(radius);
                onBlurSuccess(blurProcessor.blur(bitmap));
            } else {
                onBlurFailed((float) radius / 15);
            }
        }

        abstract void onBlurSuccess(Bitmap bitmap);

        abstract void onBlurFailed(float factor);
    }
}
