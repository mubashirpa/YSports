package ysports.app.widgets.recyclerview

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class VerticalSpacingItemDecoration(
    private val margin: Int, private val verticalSpacing: Int
) : RecyclerView.ItemDecoration() {

    private val displayDensity = Resources.getSystem().displayMetrics.density

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val total: Int = state.itemCount
        val position: Int = parent.getChildAdapterPosition(view)

        outRect.bottom = dpToPx(displayDensity, verticalSpacing)
        if (position == 0) outRect.top = dpToPx(displayDensity, margin)
        if (position == total - 1) outRect.bottom = dpToPx(displayDensity, margin)
        outRect.left = dpToPx(displayDensity, margin)
        outRect.right = dpToPx(displayDensity, margin)
    }

    private fun dpToPx(density: Float, dps: Int): Int {
        return (dps * density + 0.5f).toInt()
    }
}