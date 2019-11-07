package foundation.e.blisslauncher.core.blur;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.hoko.blur.HokoBlur;
import com.hoko.blur.processor.BlurProcessor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import foundation.e.blisslauncher.core.Utilities;

public class BlurWallpaperProvider {

    private final Context context;
    private final WallpaperManager wallpaperManager;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;

    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private final BlurProcessor blurProcessor;

    private ExecutorService mDispatcher = Executors.newSingleThreadExecutor();

    private final Runnable updateRunnable = () -> updateWallpaper();

    private DisplayMetrics displayMetrics = new DisplayMetrics();

    private static final float SAMPLE_FACTOR = 4.0f;

    private static BlurWallpaperProvider sInstance;
    private Bitmap wallpaper;
    private volatile Future mFuture;
    private Listener listener;

    private BlurWallpaperProvider(Context context) {
        this.context = context;
        this.wallpaperManager = WallpaperManager.getInstance(context);
        blurProcessor = HokoBlur.with(context).sampleFactor(SAMPLE_FACTOR)
                .scheme(HokoBlur.SCHEME_OPENGL)
                .mode(HokoBlur.MODE_GAUSSIAN)
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
        updateAsync();
    }

    private void updateAsync() {
        THREAD_POOL_EXECUTOR.execute(updateRunnable);
    }

    private void updateWallpaper() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        wallpaper = Utilities.drawableToBitmap(wallpaperManager.getFastDrawable(), true);
        //wallpaper = scaleToScreenSize(wallpaper);
    }


    public void blur(int radius) {
        cancelPreTask(false);
        mFuture = mDispatcher.submit(new BlurTask(wallpaper, blurProcessor, radius) {
            @Override
            void onBlurSuccess(Bitmap bitmap) {
                if (bitmap != null && listener != null) {
                    listener.onBlurSuccess(bitmap);
                    //bitmap.recycle();
                }
            }
        });
    }

    public void cancelPreTask(boolean interrupt) {
        if (mFuture != null && !mFuture.isCancelled() && !mFuture.isDone()) {
            mFuture.cancel(interrupt);
            mFuture = null;
        }
    }

    private Bitmap scaleToScreenSize(Bitmap wallpaper) {
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

        return Bitmap.createScaledBitmap(wallpaper, scaledWidth, scaledHeight, false);
    }

    public interface Listener {
        void onBlurSuccess(Bitmap bitmap);
    }

    public void clear(){
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
            }
        }

        abstract void onBlurSuccess(Bitmap bitmap);
    }
}
