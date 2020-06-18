package foundation.e.blisslauncher.core.blur

import android.content.Context
import android.graphics.Bitmap
import com.hoko.blur.HokoBlur
import com.hoko.blur.task.AsyncBlurTask

class BlurWallpaperFilter(private val context: Context) : WallpaperFilter {

    private var blurRadius = 8

    override fun apply(wallpaper: Bitmap): WallpaperFilter.ApplyTask {
        return WallpaperFilter.ApplyTask.create { emitter ->
            HokoBlur.with(context)
                .scheme(HokoBlur.SCHEME_NATIVE)
                .mode(HokoBlur.MODE_STACK)
                .radius(blurRadius)
                .sampleFactor(8f)
                .forceCopy(false)
                .needUpscale(true)
                .processor()
                .asyncBlur(wallpaper, object : AsyncBlurTask.Callback {
                    override fun onBlurSuccess(bitmap: Bitmap) {
                        emitter.onSuccess(bitmap)
                    }

                    override fun onBlurFailed(error: Throwable?) {
                        emitter.onError(error!!)
                    }
                })
        }
    }
}