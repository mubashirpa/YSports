package ysports.app.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import android.view.View;
import android.widget.RemoteViews.RemoteView;

@RemoteView
public class PlayerUnlockButton extends AppCompatImageButton {

    private Handler handler;
    private Runnable runnable;

    public PlayerUnlockButton(@NonNull Context context) {
        super(context);
    }

    public PlayerUnlockButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerUnlockButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        if (handler == null || runnable == null) {
            handler = new Handler(Looper.getMainLooper());
            runnable = () -> changedView.setVisibility(GONE);
            if (visibility == View.VISIBLE) {
                handler.postDelayed(runnable, 5000);
            } else {
                super.onVisibilityChanged(changedView, visibility);
            }
            return;
        }
        handler.removeCallbacksAndMessages(null);
        if (visibility == View.VISIBLE) {
            handler.postDelayed(runnable, 5000);
        } else {
            super.onVisibilityChanged(changedView, visibility);
        }
    }
}
