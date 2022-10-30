package ysports.app.widgets.recyclerview

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,
    gridSpacing: Int,
    private val headerCount: Int,
    private val includeEdge: Boolean,
    private val isReverse: Boolean // Reverse layout
) : RecyclerView.ItemDecoration() {

    private val displayDensity = Resources.getSystem().displayMetrics.density
    private val spacing = dpToPx(displayDensity, gridSpacing)

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) - headerCount

        if (position >= 0) {
            val column = position % spanCount

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount
                outRect.right = (column + 1) * spacing / spanCount

                if (position < spanCount) {
                    if (isReverse) outRect.bottom = spacing else outRect.top = spacing
                }
                if (isReverse) outRect.top = spacing else outRect.bottom = spacing
            } else {
                outRect.left = column * spacing / spanCount
                outRect.right = spacing - (column + 1) * spacing / spanCount

                if (position >= spanCount) {
                    if (isReverse) outRect.bottom = spacing else outRect.top = spacing
                }
            }
        } else {
            // Margin for header
            outRect.left = spacing
            outRect.right = spacing
            outRect.top = spacing
            outRect.bottom = 0
        }
    }

    private fun dpToPx(density: Float, dps: Int): Int {
        return (dps * density + 0.5f).toInt()
    }
}