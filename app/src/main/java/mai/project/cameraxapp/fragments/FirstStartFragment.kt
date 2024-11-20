package mai.project.cameraxapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import mai.project.cameraxapp.R
import mai.project.cameraxapp.databinding.FragmentFirstStartBinding

class FirstStartFragment : Fragment() {
    private var _binding: FragmentFirstStartBinding? = null
    private val binding get() = _binding!!

    /**
     * 取得 NavController
     */
    private val navController: NavController by lazy { findNavController() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstStartBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setListener()
    }

    /**
     * 設定監聽器
     */
    private fun setListener() = with(binding) {
        btnTakePhotoDemo.setOnClickListener {
            navController.navigate(R.id.action_firstStartFragment_to_takePhotoDemoFragment)
        }

        btnCaptureVideoDemo.setOnClickListener {
            navController.navigate(R.id.action_firstStartFragment_to_captureVideoDemoFragment)
        }
    }
}