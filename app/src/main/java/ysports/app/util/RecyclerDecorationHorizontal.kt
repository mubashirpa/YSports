package ysports.app.util

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RecyclerDecorationHorizontal(
    private var marginStart: Int,
    private var marginEnd: Int,
    private var horizontalSpacing: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val total: Int = state.itemCount
        val position: Int = parent.getChildAdapterPosition(view)
        outRect.right = intToDp(horizontalSpacing)
        if (position == 0) outRect.left = intToDp(marginStart)
        if (position == total - 1) outRect.right = intToDp(marginEnd)
    }

    private fun intToDp(int: Int): Int {
        val density: Float = Resources.getSystem().displayMetrics.density
        return ((int * density) + 0.5f).toInt()
    }
}