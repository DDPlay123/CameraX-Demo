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
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import mai.project.cameraxapp.R
import mai.project.cameraxapp.databinding.FragmentCaptureVideoDemoBinding
import mai.project.cameraxapp.utils.Constants
import mai.project.cameraxapp.utils.Method
import mai.project.cameraxapp.utils.Method.drawable
import mai.project.cameraxapp.utils.Method.showToast
import java.text.SimpleDateFormat
import java.util.Locale

class CaptureVideoDemoFragment : Fragment() {
    private var _binding: FragmentCaptureVideoDemoBinding? = null
    private val binding get() = _binding!!

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCaptureVideoDemoBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                    preview, videoCapture
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
        imgCapture.setOnClickListener { captureVideo() }
    }

    /**
     * 擷取影片
     */
    private fun captureVideo() {
        // 取得影片擷取物件
        val videoCapture = videoCapture ?: return

        // 先禁止按鈕點擊事件
        binding.imgCapture.isEnabled = false

        // 取得錄影物件
        val curRecording = recording

        // 如果正在錄影，則強制關閉並返回
        curRecording?.let {
            it.stop()
            recording = null
            binding.imgCapture.isEnabled = true
            return
        }

        // 命名檔案
        val name = SimpleDateFormat(Constants.FILENAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis())

        // 定義檔案格式
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, Constants.VIDEO_MIME_TYPE)
            // 大於 Android 9 使用 相簿儲存路徑
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Constants.VIDEO_STORAGE_PATH)
        }

        // 定義輸出選項
        val outputOptions = MediaStoreOutputOptions
            .Builder(requireContext().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        // 開始錄影並監聽事件，最後儲存檔案
        recording = videoCapture.output
            .prepareRecording(requireContext(), outputOptions)
            .apply {
                // 如果有錄音權限，啟用音頻
                if (PermissionChecker.checkSelfPermission(
                        requireContext(), android.Manifest.permission.RECORD_AUDIO
                    ) == PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Status -> {
                        binding.imgCapture.apply {
                            setImageDrawable(requireContext().drawable(R.drawable.bg_camera_recording))
                            isEnabled = true
                        }
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = getString(R.string.video_captured_successful, recordEvent.outputResults.outputUri)
                            Method.logD(msg)
                            requireContext().showToast(msg)
                        } else {
                            recording?.close()
                            recording = null
                            val msg = getString(R.string.video_capture_error)
                            Method.logE(msg, recordEvent.cause)
                            requireContext().showToast(msg)
                        }
                        binding.imgCapture.apply {
                            setImageDrawable(requireContext().drawable(R.drawable.bg_camera_button))
                            isEnabled = true
                        }
                    }
                }
            }
    }
}