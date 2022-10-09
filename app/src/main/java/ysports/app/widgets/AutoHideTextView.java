package ysports.app.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import androidx.annotation.Nullable;

public class AutoHideTextView extends androidx.appcompat.widget.AppCompatTextView {

    private Handler handler;
    private Runnable runnable;

    public AutoHideTextView(Context context) {
        super(context);
    }

    public AutoHideTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoHideTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        } else {
            handler.removeCallbacksAndMessages(null);
        }
        if (runnable == null) runnable = () -> this.setVisibility(GONE);
        handler.postDelayed(runnable, 1000);
    }
}
