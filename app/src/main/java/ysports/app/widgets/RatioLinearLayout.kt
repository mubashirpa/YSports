package ysports.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import ysports.app.R

class RatioLinearLayout : LinearLayout {

    private var ratio: Int = 1

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context, attrs, defStyleAttr, 0
    )

    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.RatioView, defStyleAttr, defStyleRes
        )
        try {
            ratio = typedArray.getInteger(R.styleable.RatioView_ratio, 0)
        } finally {
            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth
        val height = if (ratio == 0) width * 9 / 16 else width * 3 / 4
        setMeasuredDimension(width, height)
    }
}