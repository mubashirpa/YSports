package ysports.app.widgets.recyclerview

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HorizontalSpacingItemDecoration(
    private val marginStart: Int, private val marginEnd: Int, private val horizontalSpacing: Int
) : RecyclerView.ItemDecoration() {

    private val displayDensity = Resources.getSystem().displayMetrics.density

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
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