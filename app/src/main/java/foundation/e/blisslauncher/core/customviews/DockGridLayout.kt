package foundation.e.blisslauncher.core.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.WindowInsets
import android.widget.GridLayout
import foundation.e.blisslauncher.BlissLauncher
import foundation.e.blisslauncher.core.blur.BlurWallpaperProvider
import foundation.e.blisslauncher.core.blur.ShaderBlurDrawable
import foundation.e.blisslauncher.core.runOnMainThread

class DockGridLayout @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GridLayout(mContext, attrs, defStyleAttr), Insettable,
    BlurWallpaperProvider.Listener {
    private val blurWallpaperProvider: BlurWallpaperProvider
    private var fullBlurDrawable: ShaderBlurDrawable? = null
    private val blurAlpha = 255
    private val blurDrawableCallback: Drawable.Callback = object : Drawable.Callback {
        override fun invalidateDrawable(who: Drawable) {
            runOnMainThread {
                invalidate()
                null
            }
        }

        override fun scheduleDrawable(
            who: Drawable,
            what: Runnable,
            `when`: Long
        ) {
        }

        override fun unscheduleDrawable(
            who: Drawable,
            what: Runnable
        ) {
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        BlurWallpaperProvider.getInstance(mContext).addListener(this)
        fullBlurDrawable!!.startListening()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        BlurWallpaperProvider.getInstance(mContext).removeListener(this)
        fullBlurDrawable!!.stopListening()
    }

    override fun onDraw(canvas: Canvas) {
        fullBlurDrawable!!.alpha = blurAlpha
        fullBlurDrawable!!.draw(canvas)
        super.onDraw(canvas)
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            fullBlurDrawable!!.setBounds(left, top, right, bottom)
        }
    }

    override fun setInsets(insets: WindowInsets) {
        val deviceProfile = BlissLauncher.getApplication(mContext).deviceProfile
        val lp =
            layoutParams as InsettableRelativeLayout.LayoutParams
        lp.height = deviceProfile.hotseatCellHeightPx + insets.systemWindowInsetBottom
        setPadding(
            deviceProfile.iconDrawablePaddingPx / 2, 0,
            deviceProfile.iconDrawablePaddingPx / 2, insets.systemWindowInsetBottom
        )
        layoutParams = lp
    }

    private fun createBlurDrawable() {
        if (isAttachedToWindow) {
            fullBlurDrawable!!.stopListening()
        }
        fullBlurDrawable = blurWallpaperProvider.createDrawable()
        fullBlurDrawable!!.callback = blurDrawableCallback
        fullBlurDrawable!!.setBounds(left, top, right, bottom)
        if (isAttachedToWindow) fullBlurDrawable!!.startListening()
    }

    override fun onEnabledChanged() {
        createBlurDrawable()
    }

    override fun onWallpaperChanged() {}

    init {
        setWillNotDraw(false)
        blurWallpaperProvider = BlurWallpaperProvider.getInstance(mContext)
        createBlurDrawable()
    }
}