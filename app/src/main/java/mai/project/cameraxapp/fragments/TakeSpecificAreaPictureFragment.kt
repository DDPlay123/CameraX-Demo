package mai.project.cameraxapp.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import mai.project.cameraxapp.R
import mai.project.cameraxapp.base.BaseFragment
import mai.project.cameraxapp.databinding.FragmentTakeSpecificAreaPictureBinding
import mai.project.cameraxapp.utils.Method
import mai.project.cameraxapp.utils.Method.DP
import mai.project.cameraxapp.utils.Method.showToast
import java.io.File

class TakeSpecificAreaPictureFragment : BaseFragment<FragmentTakeSpecificAreaPictureBinding>(
    FragmentTakeSpecificAreaPictureBinding::inflate
) {
    private var imageCapture: ImageCapture? = null

    override fun FragmentTakeSpecificAreaPictureBinding.destroy() {
        imageCapture = null
    }

    override fun FragmentTakeSpecificAreaPictureBinding.initialize(view: View, savedInstanceState: Bundle?) {
        startCamera()
        calculateOverlayRegion()
    }

    override fun FragmentTakeSpecificAreaPictureBinding.setListener() {
        imgCapture.setOnClickListener { takePhoto() }

        swOverlay.setOnCheckedChangeListener { _, isChecked ->
            overlay.isVisible = isChecked
        }
    }

    override fun FragmentTakeSpecificAreaPictureBinding.handleOnBackPressed() {
        if (imgPreview.isVisible) {
            imgPreview.clearImage()
            return
        }
        navigateUp()
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
     * 計算 Overlay 的區域
     */
    private fun calculateOverlayRegion() = with(binding) {
        cameraPreview.post {
            // 確保寬高比為 7:10
            val aspectRatio = 7f / 10f

            // 預覽畫面的寬高
            val previewWidth = cameraPreview.width.toFloat()
            val previewHeight = cameraPreview.height.toFloat()

            // 擷取按鈕的尺寸（寬高是相同的）
            val imgCaptureSize = imgCapture.height.toFloat()

            // 擷取按鈕上下需要預留的空間
            val imgCaptureMargin = 72f.DP

            // 距離頂部的偏移量
            val topMargin = 70f.DP

            // 可用的最大高度：排除頂部偏移和底部的按鈕與邊距
            val maxAvailableHeight = previewHeight - topMargin - imgCaptureSize - imgCaptureMargin

            // 初步計算寬高，先以高度為基準
            var overlayHeight = maxAvailableHeight
            var overlayWidth = overlayHeight * aspectRatio

            // 如果寬度超出可用寬度，改用寬度為基準
            if (overlayWidth > previewWidth) {
                overlayWidth = previewWidth
                overlayHeight = overlayWidth / aspectRatio
            }

            // 計算 top 和 bottom
            val top = topMargin / previewHeight // 距離頂部的百分比
            val bottom = top + overlayHeight / previewHeight // 底部的百分比

            // 計算 left 和 right
            val left = ((previewWidth - overlayWidth) / 2) / previewWidth
            val right = 1 - left

            // 設置 Overlay 的初始區域
            overlay.setInitialRegion(
                leftRatio = left,
                topRatio = top,
                rightRatio = right,
                bottomRatio = bottom
            )
        }
    }

    /**
     * 擷取照片
     */
    private fun takePhoto() {
        // 取得圖像擷取物件
        val imageCapture = imageCapture ?: return

        // 定義輸出選項
        val cacheDir = requireContext().cacheDir.absolutePath
        val file = File(cacheDir, Method.createFileName())
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(file)
            .build()

        // 開始擷取並儲存檔案
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val msg = getString(R.string.photo_saved_successful, outputFileResults.savedUri.toString())
                    Method.logD(msg)
                    requireContext().showToast(msg)
                    if (binding.swOverlay.isChecked) {
                        // 裁切
                        outputFileResults.savedUri?.let(::cropImage)
                    } else {
                        // 預覽
                        outputFileResults.savedUri?.let(binding.imgPreview::setImage)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    val msg = getString(R.string.photo_save_error)
                    Method.logE(msg, exception)
                    requireContext().showToast(msg)
                }
            }
        )
    }

    /**
     * 裁切圖片
     */
    private fun cropImage(
        savedUri: Uri
    ) {
        // CameraX 的 Preview 元件
        val preview = binding.cameraPreview
        // Overlay 的 元件
        val overlay = binding.overlay

        // 從 savedUri 獲取 Bitmap
        val inputStream = requireContext().contentResolver.openInputStream(savedUri)
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return
        inputStream?.close()

        // Overlay 的定位點位置
        val cropRect = overlay.getCropRect()

        // 獲取 Camera Preview 實際顯示的大小和位置
        val previewWidth = preview.width.toFloat()
        val previewHeight = preview.height.toFloat()

        // 獲取 Bitmap 的實際大小
        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()

        // 計算相對於 Camera Preview 與 Bitmap 的比例
        val widthScale = bitmapWidth / previewWidth
        val heightScale = bitmapHeight / previewHeight

        // 計算相對於 Bitmap 的裁切區域
        val scaledCropRect = RectF(
            cropRect.left * widthScale,
            cropRect.top * heightScale,
            cropRect.right * widthScale,
            cropRect.bottom * heightScale
        )

        // 確保裁切區域在 Bitmap 的範圍內
        val safeCropRect = Rect(
            scaledCropRect.left.toInt().coerceIn(0, bitmap.width),
            scaledCropRect.top.toInt().coerceIn(0, bitmap.height),
            scaledCropRect.right.toInt().coerceIn(0, bitmap.width),
            scaledCropRect.bottom.toInt().coerceIn(0, bitmap.height)
        )

        // 裁切 Bitmap
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            safeCropRect.left,
            safeCropRect.top,
            safeCropRect.width(),
            safeCropRect.height()
        )

        // 顯示裁切後的圖片
        binding.imgPreview.setImage(croppedBitmap)
    }
}