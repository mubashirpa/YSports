package ysports.app.widgets

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import ysports.app.R

class AutoHideTextView : AppCompatTextView {

    private var mHandler: Handler? = null
    private var runnable: Runnable? = null
    private var hideMillis: Long = 1000

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context, attrs, android.R.attr.textViewStyle
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.AutoHideTextView, defStyleAttr, 0
        )
        try {
            hideMillis = typedArray.getInt(R.styleable.AutoHideTextView_hide_millis, 1000).toLong()
        } finally {
            typedArray.recycle()
        }
    }

    override fun onTextChanged(
        text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (mHandler == null) mHandler = Handler(Looper.getMainLooper())
        if (runnable == null) runnable = Runnable { this.visibility = GONE }
        mHandler?.removeCallbacks(runnable!!)
        mHandler?.postDelayed(runnable!!, hideMillis)
    }
}