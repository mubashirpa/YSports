package ysports.app.widgets

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class AutoHideTextView : AppCompatTextView {

    private var mHandler: Handler? = null
    private var runnable: Runnable? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, android.R.attr.textViewStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (mHandler == null) Handler(Looper.getMainLooper())
        if (runnable == null) Runnable { this.visibility = GONE }
        mHandler?.removeCallbacks(runnable!!)
        mHandler?.postDelayed(runnable!!, 1000)
    }
}