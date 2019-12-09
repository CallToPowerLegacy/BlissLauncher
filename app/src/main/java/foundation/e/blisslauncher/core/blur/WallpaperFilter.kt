package foundation.e.blisslauncher.core.blur

import android.graphics.Bitmap

interface WallpaperFilter {

    fun apply(wallpaper: Bitmap): ApplyTask

    class ApplyTask {

        val emitter = Emitter()

        private var result: Bitmap? = null
        private var error: Throwable? = null

        private var callback: ((Bitmap?, Throwable?) -> Unit)? = null

        fun setCallback(callback: (Bitmap?, Throwable?) -> Unit): ApplyTask {
            result?.let {
                callback(it, null)
                return this
            }
            error?.let {
                callback(null, it)
                return this
            }
            this.callback = callback
            return this
        }

        inner class Emitter {

            fun onSuccess(result: Bitmap) {
                callback?.let {
                    it(result, null)
                    return
                }
                this@ApplyTask.result = result
            }

            fun onError(error: Throwable) {
                callback?.let {
                    it(null, error)
                    return
                }
                this@ApplyTask.error = error
            }
        }

        companion object {

            inline fun create(source: (Emitter) -> Unit): ApplyTask {
                return ApplyTask().also { source(it.emitter) }
            }
        }
    }
}