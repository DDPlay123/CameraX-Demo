package mai.project.cameraxapp.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import mai.project.cameraxapp.BuildConfig
import mai.project.cameraxapp.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

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
    ) {
        if (BuildConfig.DEBUG) {
            Log.e(tag ?: getClassName(), message, tr)
        }
    }

    /**
     * 建立檔案名稱
     */
    fun createFileName(): String {
        return SimpleDateFormat(Constants.FILENAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis())
    }

    /**
     * 建立媒體檔案格式
     */
    fun createMediaFileFormater(
        mediaType: MediaType
    ): ContentValues {
        val mimeType = when (mediaType) {
            MediaType.IMAGE -> Constants.IMAGE_MIME_TYPE
            MediaType.VIDEO -> Constants.VIDEO_MIME_TYPE
        }
        val paths = when (mediaType) {
            MediaType.IMAGE -> Constants.IMAGE_STORAGE_PATH
            MediaType.VIDEO -> Constants.VIDEO_STORAGE_PATH
        }
        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, createFileName())
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            // 大於 Android 9 使用 相簿儲存路徑
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
                put(MediaStore.MediaColumns.RELATIVE_PATH, paths)
        }
    }

    /**
     * 儲存 Bitmap 到暫存區
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap): String? {
        return try {
            val cacheDir = context.cacheDir.absolutePath
            val file = File(cacheDir, createFileName())
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
            }
            file.absolutePath
        } catch (e: Exception) {
            logE(context.getString(R.string.photo_save_error), e)
            context.showToast(context.getString(R.string.photo_save_error))
            null
        }
    }

    /**
     * 儲存 Bitmap 到相簿
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
        // 設定輸出 Uri
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            createMediaFileFormater(MediaType.IMAGE)
        )

        uri?.let {
            val outputStream = context.contentResolver.openOutputStream(it)
            outputStream?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
            }
            val msg = context.getString(R.string.photo_saved_successful, it.toString())
            logD(msg)
            context.showToast(msg)
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
        val className = Throwable().stackTrace[3].className
        return className.substringAfterLast('.')
    }
}