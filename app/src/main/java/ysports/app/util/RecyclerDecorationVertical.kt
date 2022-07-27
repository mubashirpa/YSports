package ysports.app.util

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RecyclerDecorationVertical(
    private var top: Int,
    private var bottom: Int,
    private var left: Int,
    private var right: Int,
    private var verticalSpacing: Int
) : RecyclerView.ItemDecoration() {

    private val displayDensity = Resources.getSystem().displayMetrics.density

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position: Int = parent.getChildAdapterPosition(view)
        if (position == 0) outRect.top = dpToPx(displayDensity, top)
        outRect.bottom = dpToPx(displayDensity, verticalSpacing)
        outRect.left = dpToPx(displayDensity, left)
        outRect.right = dpToPx(displayDensity, right)
    }

    private fun dpToPx(density: Float, dps: Int): Int {
        return (dps * density + 0.5f).toInt()
    }
}