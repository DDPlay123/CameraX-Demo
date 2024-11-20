package mai.project.cameraxapp.utils

/**
 * 固定參數
 */
object Constants {
    /**
     * 時間格式，用於檔案命名
     */
    const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    /**
     * 圖片儲存格式
     */
    const val IMAGE_MIME_TYPE = "image/jpeg"

    /**
     * 影片儲存格式
     */
    const val VIDEO_MIME_TYPE = "video/mp4"

    /**
     * 圖片儲存路徑
     *
     * - Android 9 以上使用 相簿儲存路徑
     */
    const val IMAGE_STORAGE_PATH = "Pictures/CameraX-Image"

    /**
     * 影片儲存路徑
     *
     * - Android 9 以上使用 相簿儲存路徑
     */
    const val VIDEO_STORAGE_PATH = "Movies/CameraX-Video"
}