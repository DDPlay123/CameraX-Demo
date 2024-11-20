package mai.project.cameraxapp.fragments

import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import mai.project.cameraxapp.R
import mai.project.cameraxapp.base.BaseFragment
import mai.project.cameraxapp.databinding.FragmentTakePhotoDemoBinding
import mai.project.cameraxapp.utils.LuminosityAnalyzer
import mai.project.cameraxapp.utils.MediaType
import mai.project.cameraxapp.utils.Method
import mai.project.cameraxapp.utils.Method.showToast
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TakePhotoDemoFragment : BaseFragment<FragmentTakePhotoDemoBinding>(
    FragmentTakePhotoDemoBinding::inflate
) {
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun FragmentTakePhotoDemoBinding.destroy() {
        // 強制停止 ExecutorService
        cameraExecutor.shutdown()
    }

    override fun FragmentTakePhotoDemoBinding.initialize(view: View, savedInstanceState: Bundle?) {
        // 使用單一執行緒來擷取圖像
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
    }

    override fun FragmentTakePhotoDemoBinding.setListener() {
        imgCapture.setOnClickListener { takePhoto() }
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
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // 加快拍攝時間，縮減畫質
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
                // 獲取 PreviewView 的尺寸，來設置 ViewPort
                val viewPort = binding.cameraPreview.viewPort ?: return@addListener
                // 使用 UseCaseGroup 綁定 ViewPort，以確保所有用例保持一致的比例
                val useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(imageCapture!!)
                    .addUseCase(imageAnalyzer)
                    .setViewPort(viewPort)
                    .build()
                // 綁定用例
                cameraProvider.bindToLifecycle(
                    lifecycleOwner = this,
                    cameraSelector = cameraSelector,
                    useCaseGroup
                )
            } catch (e: Exception) {
                Method.logE(getString(R.string.camera_launch_error), e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * 擷取照片
     */
    private fun takePhoto() {
        // 取得圖像擷取物件
        val imageCapture = imageCapture ?: return

        // 定義輸出選項
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                Method.createMediaFileFormater(MediaType.IMAGE)
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