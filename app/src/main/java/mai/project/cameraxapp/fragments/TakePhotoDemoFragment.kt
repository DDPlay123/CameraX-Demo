package mai.project.cameraxapp.fragments

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import mai.project.cameraxapp.R
import mai.project.cameraxapp.databinding.FragmentTakePhotoDemoBinding
import mai.project.cameraxapp.utils.Constants
import mai.project.cameraxapp.utils.LuminosityAnalyzer
import mai.project.cameraxapp.utils.Method
import mai.project.cameraxapp.utils.Method.showToast
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TakePhotoDemoFragment : Fragment() {
    private var _binding: FragmentTakePhotoDemoBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTakePhotoDemoBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 強制停止 ExecutorService
        cameraExecutor.shutdown()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 使用單一執行緒來擷取圖像
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
        setListener()
    }

    /**
     * 開啟相機以進行預覽
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // 用於將 相機的生命週期 與 生命週期擁有者 綁定
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 設定預覽對象元件
            val preview = Preview.Builder()
                .build()
                .also { it.surfaceProvider = binding.cameraPreview.surfaceProvider }

            // 設定圖片擷取物件
            imageCapture = ImageCapture.Builder()
                .build()

            // 設定相機圖像分析工具
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        // Fixed 返回事件時，Fragment消失，造成的 IllegalStateException
                        try {
                            Method.logD(getString(R.string.photo_average_luma, luma))
                        } catch (ignored: Exception) {
                        }
                    })
                }

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
                    preview, imageCapture, imageAnalyzer
                )
            } catch (e: Exception) {
                Method.logE(getString(R.string.camera_launch_error), e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * 設定監聽器
     */
    private fun setListener() = with(binding) {
        imgCapture.setOnClickListener { takePhoto() }
    }

    /**
     * 擷取照片
     */
    private fun takePhoto() {
        // 取得圖像擷取物件
        val imageCapture = imageCapture ?: return

        // 命名檔案
        val name = SimpleDateFormat(Constants.FILENAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis())

        // 定義檔案格式
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, Constants.IMAGE_MIME_TYPE)
            // 大於 Android 9 使用 相簿儲存路徑
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Constants.IMAGE_STORAGE_PATH)
        }

        // 定義輸出選項
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // 開始擷取並儲存檔案
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val msg = getString(R.string.photo_saved_successful, outputFileResults.savedUri.toString())
                    Method.logD(msg)
                    requireContext().showToast(msg)
                }

                override fun onError(exception: ImageCaptureException) {
                    val msg = getString(R.string.photo_save_error)
                    Method.logE(msg, exception)
                    requireContext().showToast(msg)
                }
            }
        )
    }
}