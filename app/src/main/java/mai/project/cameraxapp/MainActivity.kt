package mai.project.cameraxapp

import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import mai.project.cameraxapp.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        setupWindowPadding()
        doInitialized()
        setListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 強制停止 ExecutorService
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(baseContext, getString(R.string.permission_not_allow), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * 設定 Window 的間距
     */
    private fun setupWindowPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * 執行初始化
     */
    private fun doInitialized() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // 使用單一執行緒來擷取圖像
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    /**
     * 設定監聽器
     */
    private fun setListener() = with(binding) {
        imgTakePhoto.setOnClickListener { takePhoto() }

        imgCaptureVideo.setOnClickListener { captureVideo() }
    }

    /**
     * 擷取照片
     */
    private fun takePhoto() {
        // 取得圖像擷取物件
        val imageCapture = imageCapture ?: return

        // 命名檔案
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis())

        // 定義檔案格式
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, IMAGE_MIME_TYPE)
            // 大於 Android 9 使用 相簿儲存路徑
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
                put(MediaStore.MediaColumns.RELATIVE_PATH, IMAGE_STORAGE_PATH)
        }

        // 定義輸出選項
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // 開始擷取並儲存檔案
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val msg = getString(R.string.photo_saved_successful, outputFileResults.savedUri.toString())
                    Log.d(TAG, msg)
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    val msg = getString(R.string.photo_save_error)
                    Log.e(TAG, msg, exception)
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    /**
     * 擷取影片
     */
    private fun captureVideo() {
        // 取得影片擷取物件
        val videoCapture = videoCapture ?: return

        // 先禁止按鈕點擊事件
        binding.imgCaptureVideo.isEnabled = false

        // 取得錄影物件
        val curRecording = recording

        // 如果正在錄影，則強制關閉並返回
        curRecording?.let {
            it.stop()
            recording = null
            binding.imgCaptureVideo.isEnabled = true
            return
        }

        // 命名檔案
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis())

        // 定義檔案格式
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, VIDEO_MIME_TYPE)
            // 大於 Android 9 使用 相簿儲存路徑
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
                put(MediaStore.MediaColumns.RELATIVE_PATH, VIDEO_STORAGE_PATH)
        }

        // 定義輸出選項
        val outputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        // 開始錄影並監聽事件，最後儲存檔案
        recording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .apply {
                // 如果有錄音權限，啟用音頻
                if (PermissionChecker.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Status -> {
                        binding.imgCaptureVideo.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = getString(R.string.video_captured_successful, recordEvent.outputResults.outputUri)
                            Log.d(TAG, msg)
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            recording?.close()
                            recording = null
                            val msg = getString(R.string.video_capture_error)
                            Log.e(TAG, msg, recordEvent.cause)
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        }
                        binding.imgCaptureVideo.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }
                }
            }
    }

    /**
     * 開啟相機以進行預覽
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // 用於將 相機的生命週期 與 生命週期擁有者(MainActivity) 綁定
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 設定預覽對象元件
            val preview = Preview.Builder()
                .build()
                .also { it.surfaceProvider = binding.cameraPreview.surfaceProvider }

            // 設定圖片擷取物件
//            imageCapture = ImageCapture.Builder()
//                .build()
//
//            // 設定相機圖像分析工具
//            val imageAnalyzer = ImageAnalysis.Builder()
//                .build()
//                .also {
//                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
//                        Log.d(TAG, getString(R.string.photo_average_luma, luma))
//                    })
//                }

            // 建構錄影物件
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            // 設定影片擷取物件
            videoCapture = VideoCapture.withOutput(recorder)

            // 預設選擇後鏡頭
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // 先解除過去綁定的用例
                cameraProvider.unbindAll()
                // 綁定用例
                cameraProvider.bindToLifecycle(
                    lifecycleOwner = this,
                    cameraSelector = cameraSelector,
                    // useCases
                    preview,
//                    imageCapture,
//                    imageAnalyzer,
                    videoCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "啟動相機失敗", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 檢查權限是否全部允許
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) ==
                PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val IMAGE_MIME_TYPE = "image/jpeg"
        private const val VIDEO_MIME_TYPE = "video/mp4"
        private const val IMAGE_STORAGE_PATH = "Pictures/CameraX-Image"
        private const val VIDEO_STORAGE_PATH = "Movies/CameraX-Video"
        private const val REQUEST_CODE_PERMISSIONS = 10

        // 需要的權限 (低於 Android9 需要補上檔案寫入權限)
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}