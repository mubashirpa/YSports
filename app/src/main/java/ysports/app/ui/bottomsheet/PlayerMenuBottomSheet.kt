package ysports.app.ui.bottomsheet

import android.app.Dialog
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ysports.app.databinding.ViewSettingsPlayerBinding

class PlayerMenuBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ViewSettingsPlayerBinding? = null
    private val binding get() = _binding!!
    var videoTrackClickListener: View.OnClickListener? = null
    var audioTrackClickListener: View.OnClickListener? = null
    var subTrackClickListener: View.OnClickListener? = null
    var settingsClickListener: View.OnClickListener? = null
    var playbackSpeedClickListener: View.OnClickListener? = null
    private val displayDensity = Resources.getSystem().displayMetrics.density

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ViewSettingsPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        val modalBottomSheetBehavior = (dialog as BottomSheetDialog).behavior
        modalBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.quality.setOnClickListener(videoTrackClickListener)
        binding.audioTrack.setOnClickListener(audioTrackClickListener)
        binding.subtitles.setOnClickListener(subTrackClickListener)
        binding.settings.setOnClickListener(settingsClickListener)
        binding.playbackSpeed.setOnClickListener(playbackSpeedClickListener)
    }

    override fun onResume() {
        super.onResume()
        val configuration = requireActivity().resources.configuration
        if (dialog != null) {
            if (configuration.screenWidthDp > 470) {
                dialog!!.window?.setLayout(dpToPx(displayDensity, 450), -1)
            } else {
                dialog!!.window?.setLayout(-1, -1)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (dialog != null) {
            if (newConfig.screenWidthDp > 470) {
                dialog!!.window?.setLayout(dpToPx(displayDensity, 450), -1)
            } else {
                dialog!!.window?.setLayout(-1, -1)
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun dpToPx(density: Float, dps: Int): Int {
        return (dps * density + 0.5f).toInt()
    }

    @Suppress("unused")
    private fun pxToDp(density: Float, px: Int): Int {
        return (px / density).toInt()
    }
}