package mai.project.cameraxapp.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mai.project.cameraxapp.R
import mai.project.cameraxapp.base.BaseFragment
import mai.project.cameraxapp.databinding.FragmentTakeSpecificAreaPictureBinding
import mai.project.cameraxapp.utils.MediaType
import mai.project.cameraxapp.utils.Method
import mai.project.cameraxapp.utils.Method.showToast

class TakeSpecificAreaPictureFragment : BaseFragment<FragmentTakeSpecificAreaPictureBinding>(
    FragmentTakeSpecificAreaPictureBinding::inflate
) {
    private var imageCapture: ImageCapture? = null

    override fun FragmentTakeSpecificAreaPictureBinding.destroy() {
        imageCapture = null
    }

    override fun FragmentTakeSpecificAreaPictureBinding.initialize(view: View, savedInstanceState: Bundle?) {
        startCamera()
    }

    override fun FragmentTakeSpecificAreaPictureBinding.setListener() {
        imgCapture.setOnClickListener { takePhoto() }

        swOverlay.setOnCheckedChangeListener { _, isChecked ->
            overLay.isVisible = isChecked
        }
    }

    override fun FragmentTakeSpecificAreaPictureBinding.handleOnBackPressed() {
        if (imgPreview.isVisible) {
            imgPreview.clearBitmap()
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
     * 擷取照片
     */
    private fun takePhoto() {
        // 取得圖像擷取物件
        val imageCapture = imageCapture ?: return

        // 開始擷取並儲存檔案
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    // 當擷取成功時進行圖像處理
                    val bitmap = imageProxyToBitmap(image)
                    // 獲取圖像的旋轉角度
                    val rotationDegrees = image.imageInfo.rotationDegrees
                    // 關閉ImageProxy，釋放資源
                    image.close()

                    // 根據旋轉角度調整 Bitmap
                    val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)
                    // 裁剪調整後的 Bitmap
                    val croppedBitmap = cropToCenter(rotatedBitmap)

                    // 顯示 Dialog 提示操作
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.capture_success))
                        .setMessage(getString(R.string.savePhotoOrPreview))
                        .setNeutralButton(getString(R.string.cancel)) { _, _ -> }
                        .setNegativeButton(getString(R.string.save)) { _, _ ->
                            // 保存裁切後的圖片
                            saveCroppedImage(croppedBitmap)
                        }
                        .setPositiveButton(getString(R.string.preview)) { _, _ ->
                            // 預覽
                            binding.imgPreview.setBitmap(croppedBitmap)
                        }
                        .show()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    val msg = getString(R.string.photo_save_error)
                    Method.logE(msg, exception)
                    requireContext().showToast(msg)
                }
            }
        )
    }

    /**
     * 將 ImageProxy 轉換為 Bitmap
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * 旋轉 Bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 裁剪 Bitmap
     */
    private fun cropToCenter(bitmap: Bitmap): Bitmap {
        if (binding.swOverlay.isChecked) {
            // 從 OverlayView 取得裁剪區域相對於 PreviewView 的座標
            val overlay = binding.overLay
            val previewView = binding.cameraPreview

            // 計算 PreviewView 的實際顯示寬高
            val previewWidth = previewView.width.toFloat()
            val previewHeight = previewView.height.toFloat()

            // 計算 Bitmap 的實際寬高
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()

            // 計算寬高的比例
            val widthRatio = bitmapWidth / previewWidth
            val heightRatio = bitmapHeight / previewHeight

            // 將 OverlayView 的左上右下位置，轉換為 Bitmap 中的相應位置
            val left = (overlay.left * widthRatio).toInt()
            val top = (overlay.top * heightRatio).toInt()
            val right = (overlay.right * widthRatio).toInt()
            val bottom = (overlay.bottom * heightRatio).toInt()

            // 確定裁剪寬度和高度
            val cropWidth = right - left
            val cropHeight = bottom - top

            // 防止裁剪範圍超出 Bitmap 邊界
            val validLeft = left.coerceAtLeast(0)
            val validTop = top.coerceAtLeast(0)
            val validWidth = cropWidth.coerceAtMost(bitmap.width - validLeft)
            val validHeight = cropHeight.coerceAtMost(bitmap.height - validTop)

            // 進行裁剪
            return Bitmap.createBitmap(bitmap, validLeft, validTop, validWidth, validHeight)
        } else {
            return bitmap
        }
    }

    /**
     * 保存裁剪後的圖像
     */
    private fun saveCroppedImage(croppedBitmap: Bitmap) {
        // 設定輸出 Uri
        val uri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            Method.createMediaFileFormater(MediaType.IMAGE)
        )

        uri?.let {
            val outputStream = requireContext().contentResolver.openOutputStream(it)
            outputStream?.use { output ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
                output.close()
            }
            val msg = getString(R.string.photo_saved_successful, it.toString())
            Method.logD(msg)
            requireContext().showToast(msg)
        }
    }
}