package mai.project.cameraxapp.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import mai.project.cameraxapp.utils.Method

/**
 * 基礎繼承用 Fragment
 */
abstract class BaseFragment<VB : ViewBinding>(
    private val inflater: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    /**
     * 取得 NavController
     */
    lateinit var navController: NavController
        private set

    /**
     * 是否使用 Activity 的 onBackPressed
     */
    protected open val useActivityOnBackPressed: Boolean = false

    /**
     * 返回鍵監聽器
     */
    private var onBackPressedCallback: OnBackPressedCallback? = null

    /**
     * 銷毀階段
     *
     * - 接在 onDestroyView
     */
    protected abstract fun VB.destroy()

    /**
     * 初始化階段
     *
     * - 接在 onViewCreated
     */
    protected abstract fun VB.initialize(view: View, savedInstanceState: Bundle?)

    /**
     * 設置監聽器
     */
    protected abstract fun VB.setListener()

    /**
     * 返回鍵監聽器
     */
    protected open fun VB.handleOnBackPressed() {
        navigateUp()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = this.inflater(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()
        binding.initialize(view, savedInstanceState)
        binding.setListener()
    }

    override fun onResume() {
        super.onResume()
        // Setup OnBackPressed
        if (onBackPressedCallback == null) {
            onBackPressedCallback = object : OnBackPressedCallback(!useActivityOnBackPressed) {
                override fun handleOnBackPressed() {
                    binding.handleOnBackPressed()
                }
            }
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback!!)
        }
    }

    override fun onPause() {
        super.onPause()
        onBackPressedCallback?.remove()
        onBackPressedCallback = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 再次移除，避免未刪乾淨
        onBackPressedCallback?.remove()
        onBackPressedCallback = null
        binding.destroy()
        _binding = null
    }

    /**
     * 前往指定的目標
     *
     * @param resId [Int] 指定的目標
     * @param args [Bundle] 傳遞的參數
     * @param navOptions [NavOptions]
     * @param navigatorExtras [Navigator.Extras]
     * @param errorCallback [Function] 錯誤回調
     */
    protected fun navigate(
        @IdRes resId: Int,
        args: Bundle? = null,
        navOptions: NavOptions? = null,
        navigatorExtras: Navigator.Extras? = null,
        errorCallback: ((e: Exception) -> Unit)? = null
    ) {
        if (!isAdded) return
        try {
            navController.navigate(resId, args, navOptions, navigatorExtras)
        } catch (e: Exception) {
            Method.logE("navigate()", e)
            errorCallback?.invoke(e)
        }
    }

    /**
     * 前往指定的目標
     *
     * @param directions [NavDirections] 指定的目標
     * @param navOptions [NavOptions]
     * @param errorCallback [Function] 錯誤回調
     */
    protected fun navigate(
        directions: NavDirections,
        navOptions: NavOptions? = null,
        errorCallback: ((e: Exception) -> Unit)? = null
    ) {
        if (!isAdded) return
        try {
            navController.navigate(directions, navOptions)
        } catch (e: Exception) {
            Method.logE("navigate()", e)
            errorCallback?.invoke(e)
        }
    }

    /**
     * 前往指定的目標
     *
     * @param directions [NavDirections] 指定的目標
     * @param navigatorExtras [Navigator.Extras]
     * @param errorCallback [Function] 錯誤回調
     */
    protected fun navigate(
        directions: NavDirections,
        navigatorExtras: Navigator.Extras,
        errorCallback: ((e: Exception) -> Unit)? = null
    ) {
        if (!isAdded) return
        try {
            navController.navigate(directions, navigatorExtras)
        } catch (e: Exception) {
            Method.logE("navigate()", e)
            errorCallback?.invoke(e)
        }
    }

    /**
     * 移除當前的 Fragment
     *
     * @param errorCallback [Function] 錯誤回調
     */
    protected fun popBackStack(
        errorCallback: ((e: Exception) -> Unit)? = null
    ) {
        if (!isAdded) return
        try {
            navController.popBackStack()
        } catch (e: Exception) {
            Method.logE("popBackStack()", e)
            errorCallback?.invoke(e)
        }
    }

    /**
     * 移除當前的 Fragment
     *
     * @param destinationId [Int] 目標 ID
     * @param inclusive [Boolean] 是否包含當前的 Fragment
     * @param saveState [Boolean] 是否儲存狀態
     * @param errorCallback [Function] 錯誤回調
     */
    protected fun popBackStack(
        @IdRes destinationId: Int,
        inclusive: Boolean,
        saveState: Boolean = false,
        errorCallback: ((e: Exception) -> Unit)? = null
    ) {
        if (!isAdded) return
        try {
            navController.popBackStack(destinationId, inclusive, saveState)
        } catch (e: Exception) {
            Method.logE("popBackStack()", e)
            errorCallback?.invoke(e)
        }
    }

    /**
     * 移除當前的 Fragment
     *
     * @param route [String] 路徑
     * @param inclusive [Boolean] 是否包含當前的 Fragment
     * @param saveState [Boolean] 是否儲存狀態
     * @param errorCallback [Function] 錯誤回調
     */
    protected fun popBackStack(
        route: String,
        inclusive: Boolean,
        saveState: Boolean = false,
        errorCallback: ((e: Exception) -> Unit)? = null
    ) {
        if (!isAdded) return
        try {
            navController.popBackStack(route, inclusive, saveState)
        } catch (e: Exception) {
            Method.logE("popBackStack()", e)
            errorCallback?.invoke(e)
        }
    }

    /**
     * 移除當前的 Fragment ，遵循 destinationId 的路徑。
     *
     * @param errorCallback [Function] 錯誤回調
     */
    protected fun navigateUp(
        errorCallback: ((e: Exception) -> Unit)? = null
    ) {
        if (!isAdded) return
        try {
            navController.navigateUp()
        } catch (e: Exception) {
            Method.logE("navigate()", e)
            errorCallback?.invoke(e)
        }
    }
}