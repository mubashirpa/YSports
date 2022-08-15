package ysports.app.widgets.imageview;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class ResizableImageView extends AppCompatImageView {

    public ResizableImageView(@NonNull Context context) {
        super(context);
    }

    public ResizableImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ResizableImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            widthMeasureSpec = MeasureSpec.getSize(widthMeasureSpec);
            setMeasuredDimension(widthMeasureSpec, (int) Math.ceil((((float) widthMeasureSpec) * ((float) drawable.getIntrinsicHeight())) / ((float) drawable.getIntrinsicWidth())));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}