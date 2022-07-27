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

    private val displayDensity = Resources.getSystem().displayMetrics.density

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val total: Int = state.itemCount
        val position: Int = parent.getChildAdapterPosition(view)
        outRect.right = dpToPx(displayDensity, horizontalSpacing)
        if (position == 0) outRect.left = dpToPx(displayDensity, marginStart)
        if (position == total - 1) outRect.right = dpToPx(displayDensity, marginEnd)
    }

    private fun dpToPx(density: Float, dps: Int): Int {
        return (dps * density + 0.5f).toInt()
    }
}