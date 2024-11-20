package mai.project.cameraxapp

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import mai.project.cameraxapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    /**
     * 導航控制器
     */
    private lateinit var navController: NavController

    /**
     * 導航基底 Fragment
     */
    private lateinit var navHostFragment: NavHostFragment

    /**
     * 記錄返回鍵按下時間
     */
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        setupWindowPadding()
        doInitialized()
        setBackPress()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
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
        navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navigationHost) as NavHostFragment
        navController = navHostFragment.navController

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    /**
     * 設定返回鍵事件
     *
     * 讓使用者在 3 秒內按下兩次返回鍵才能退出程式
     */
    private fun setBackPress() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (backPressedTime + 3000 > System.currentTimeMillis()) {
                        finish()
                    } else {
                        Snackbar.make(binding.root, getString(R.string.press_again_to_exit), Snackbar.LENGTH_SHORT).show()
                    }

                    backPressedTime = System.currentTimeMillis()
                }
            }
        )
    }

    /**
     * 檢查權限是否全部允許
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) ==
                PackageManager.PERMISSION_GRANTED
    }

    companion object {
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