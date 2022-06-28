package ysports.app.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

public class ResizableImageView extends AppCompatImageView {

    public ResizableImageView(Context context) {
        super(context);
    }

    public ResizableImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public ResizableImageView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
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