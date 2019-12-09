package foundation.e.blisslauncher.core.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.GridLayout;

import foundation.e.blisslauncher.BlissLauncher;
import foundation.e.blisslauncher.core.DeviceProfile;
import foundation.e.blisslauncher.core.KotlinUtilsKt;
import foundation.e.blisslauncher.core.blur.BlurWallpaperProvider;
import foundation.e.blisslauncher.core.blur.ShaderBlurDrawable;

public class DockGridLayout extends GridLayout implements Insettable, BlurWallpaperProvider.Listener {

    private Context mContext;

    private final BlurWallpaperProvider blurWallpaperProvider;
    private ShaderBlurDrawable fullBlurDrawable = null;
    private int blurAlpha = 255;

    private final Drawable.Callback blurDrawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(@NonNull Drawable who) {
            KotlinUtilsKt.runOnMainThread(() -> {
                invalidate();
                return null;
            });
        }

        @Override
        public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {

        }

        @Override
        public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {

        }
    };

    public DockGridLayout(Context context) {
        this(context, null);
    }

    public DockGridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DockGridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        setWillNotDraw(false);
        blurWallpaperProvider = BlurWallpaperProvider.Companion.getInstance(context);
        createBlurDrawable();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        BlurWallpaperProvider.Companion.getInstance(mContext).addListener(this);
        fullBlurDrawable.startListening();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        BlurWallpaperProvider.Companion.getInstance(mContext).removeListener(this);
        fullBlurDrawable.stopListening();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        fullBlurDrawable.setAlpha(blurAlpha);
        fullBlurDrawable.draw(canvas);
        super.onDraw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            fullBlurDrawable.setBounds(left, top, right, bottom);
        }
    }

    @Override
    public void setInsets(WindowInsets insets) {
        DeviceProfile deviceProfile = BlissLauncher.getApplication(mContext).getDeviceProfile();
        InsettableRelativeLayout.LayoutParams lp = (InsettableRelativeLayout.LayoutParams) getLayoutParams();
        lp.height = deviceProfile.hotseatCellHeightPx + insets.getSystemWindowInsetBottom();
        setPadding(deviceProfile.iconDrawablePaddingPx / 2, 0,
                deviceProfile.iconDrawablePaddingPx / 2, insets.getSystemWindowInsetBottom());
        setLayoutParams(lp);
    }

    private void createBlurDrawable() {
        if (isAttachedToWindow()) {
            fullBlurDrawable.stopListening();
        }
        fullBlurDrawable = blurWallpaperProvider.createDrawable();
        fullBlurDrawable.setCallback(blurDrawableCallback);
        fullBlurDrawable.setBounds(getLeft(), getTop(), getRight(), getBottom());
        if (isAttachedToWindow()) fullBlurDrawable.startListening();
    }

    @Override
    public void onEnabledChanged() {
        createBlurDrawable();
    }

    @Override
    public void onWallpaperChanged() {
    }
}