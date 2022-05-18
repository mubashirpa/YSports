package ysports.app.util

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RecyclerDecorationVertical(
    private var verticalSpacing: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val total: Int = state.itemCount
        val position: Int = parent.getChildAdapterPosition(view)
        outRect.bottom = intToDp(verticalSpacing)
        if (position == total - 1) outRect.bottom = 0
    }

    private fun intToDp(int: Int): Int {
        val density: Float = Resources.getSystem().displayMetrics.density
        return ((int * density) + 0.5f).toInt()
    }
}