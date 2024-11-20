package mai.project.cameraxapp.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import mai.project.cameraxapp.BuildConfig

/**
 * 常用方法
 */
object Method {

    /**
     * Debug Log
     */
    fun logD(
        message: String,
        tag: String? = null
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(tag ?: getClassName(), message)
        }
    }

    /**
     * Error Log
     */
    fun logE(
        message: String,
        tr: Throwable? = null,
        tag: String? = null
    ){
        if(BuildConfig.DEBUG) {
            Log.e(tag ?: getClassName(), message, tr)
        }
    }

    /**
     * 顯示 Toast
     */
    fun Context.showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * 取得 Drawable
     */
    fun Context.drawable(@DrawableRes resId: Int): Drawable? {
        return ContextCompat.getDrawable(this, resId)
    }

    /**
     * 取得類別名稱
     */
    private fun getClassName(): String {
        val className = Throwable().stackTrace[2].className
        return className.substringAfterLast('.')
    }
}