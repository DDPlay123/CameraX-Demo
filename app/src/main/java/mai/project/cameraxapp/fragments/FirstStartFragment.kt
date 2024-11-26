package mai.project.cameraxapp.fragments

import android.os.Bundle
import android.view.View
import mai.project.cameraxapp.R
import mai.project.cameraxapp.base.BaseFragment
import mai.project.cameraxapp.databinding.FragmentFirstStartBinding

class FirstStartFragment : BaseFragment<FragmentFirstStartBinding>(
    FragmentFirstStartBinding::inflate
) {
    override val useActivityOnBackPressed: Boolean = true

    override fun FragmentFirstStartBinding.destroy() {}

    override fun FragmentFirstStartBinding.initialize(view: View, savedInstanceState: Bundle?) {}

    override fun FragmentFirstStartBinding.setListener() {
        btnTakePhotoDemo.setOnClickListener {
            navController.navigate(R.id.action_firstStartFragment_to_takePhotoDemoFragment)
        }

        btnTakePhotoSpecificArea.setOnClickListener {
            navController.navigate(R.id.action_firstStartFragment_to_takeSpecificAreaPictureFragment)
        }

        btnCaptureVideoDemo.setOnClickListener {
            navController.navigate(R.id.action_firstStartFragment_to_captureVideoDemoFragment)
        }
    }
}