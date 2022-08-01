package ysports.app.util

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RecyclerDecorationVertical(
    private var marginTop: Int,
    private var marginBottom: Int,
    private var verticalSpacing: Int
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

        outRect.bottom = dpToPx(displayDensity, verticalSpacing)
        if (position == 0) outRect.top = dpToPx(displayDensity, marginTop)
        if (position == total - 1) outRect.bottom = dpToPx(displayDensity, marginBottom)
    }

    private fun dpToPx(density: Float, dps: Int): Int {
        return (dps * density + 0.5f).toInt()
    }
}