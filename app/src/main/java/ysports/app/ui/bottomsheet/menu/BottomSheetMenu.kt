package ysports.app.ui.bottomsheet.menu

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ysports.app.R
import ysports.app.databinding.ViewBottomSheetMenuBinding
import ysports.app.util.AppUtil

class BottomSheetMenu : BottomSheetDialogFragment() {

    private var _binding: ViewBottomSheetMenuBinding? = null
    private val binding get() = _binding!!
    private var screenWidth: Int = 0
    private lateinit var listView: ListView
    private var listViewAdapter: ListViewAdapter? = null
    private var itemClickListener: AdapterView.OnItemClickListener? = null
    private val bottomSheetMenuItem = ArrayList<BottomSheetMenuItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ViewBottomSheetMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sheetBehavior = BottomSheetBehavior.from(view.parent as View)
        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        screenWidth = AppUtil(requireContext()).minScreenWidth()
        if (screenWidth > 640) screenWidth = 640
        listView = binding.listView
        if (listViewAdapter != null) {
            listView.adapter = listViewAdapter
        }
        if (itemClickListener != null) {
            listView.onItemClickListener = itemClickListener
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        val configuration = requireActivity().resources.configuration
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            dialog?.window?.setLayout(dpToPx(screenWidth), -1)
        } else {
            dialog?.window?.setLayout(-1, -1)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            dialog?.window?.setLayout(dpToPx(screenWidth), -1)
        } else {
            dialog?.window?.setLayout(-1, -1)
        }
    }

    fun setMenuItem(title: String) {
        bottomSheetMenuItem.add(BottomSheetMenuItem(title, null, null))
    }

    fun setMenuItem(title: String, subtitle: String?) {
        bottomSheetMenuItem.add(BottomSheetMenuItem(title, subtitle, null))
    }

    fun setMenuItem(title: String, icon: Int?) {
        bottomSheetMenuItem.add(BottomSheetMenuItem(title, null, icon))
    }

    fun setMenuItem(title: String, subtitle: String?, icon: Int?) {
        bottomSheetMenuItem.add(BottomSheetMenuItem(title, subtitle, icon))
    }

    fun setAdapter(context: Context) {
        listViewAdapter = ListViewAdapter(context, bottomSheetMenuItem)
    }

    fun setOnItemClickListener(clickListener: AdapterView.OnItemClickListener) {
        this.itemClickListener = clickListener
    }

    private fun dpToPx(dps: Int): Int {
        val density = Resources.getSystem().displayMetrics.density
        return (dps * density + 0.5f).toInt()
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }

    inner class ListViewAdapter(context: Context, arrayList: ArrayList<BottomSheetMenuItem>) : ArrayAdapter<BottomSheetMenuItem>(context, 0, arrayList) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val menuItem: BottomSheetMenuItem? = getItem(position)
            var view = convertView
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.list_item_bottom_sheet_menu, parent, false)
            }

            val icon: ImageView = view!!.findViewById(R.id.list_item_icon)
            val textTitle: TextView = view.findViewById(R.id.list_item_text)
            val textSubtitle: TextView = view.findViewById(R.id.list_item_secondary_text)

            textTitle.text = menuItem?.title
            val subtitle = menuItem?.subtitle
            if (subtitle != null) {
                textSubtitle.text = subtitle
                textSubtitle.visibility = View.VISIBLE
            } else {
                textSubtitle.visibility = View.GONE
            }
            val iconID = menuItem?.icon
            if (iconID != null) {
                icon.setImageResource(iconID)
                icon.visibility = View.VISIBLE
            } else {
                icon.setImageResource(0)
                icon.visibility = View.GONE
            }

            return view
        }
    }
}