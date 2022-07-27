package ysports.app.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import android.view.View;
import android.widget.RemoteViews.RemoteView;

@RemoteView
public class PlayerUnlockButton extends AppCompatImageButton {

    private Handler handler;
    private Runnable runnable;

    public PlayerUnlockButton(Context context) {
        super(context);
    }

    public PlayerUnlockButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerUnlockButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
            handler = null;
            runnable = null;
        }
        if (visibility == View.VISIBLE) {
            handler = new Handler(Looper.getMainLooper());
            runnable = () -> {
                changedView.setVisibility(GONE);
                handler = null;
                runnable = null;
            };
            handler.postDelayed(runnable, 5000);
        } else {
            super.onVisibilityChanged(changedView, visibility);
        }
    }
}