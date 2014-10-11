package com.android.systemui.nx;

import com.android.systemui.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Bitmap.Config;
import android.util.AttributeSet;
import android.widget.ImageView;

public class NxLogoView extends ImageView {
    public static final String TAG = NxLogoView.class.getSimpleName();
    public static final float DEFAULT_ALPHA = 0.50f;
	private int mLogoColor;

	public NxLogoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public NxLogoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		updateResources(0);
	}

	public void updateResources(int rot) {
		mLogoColor = getContext().getResources().getColor(
				R.color.status_bar_clock_color);
		setColorFilter(new PorterDuffColorFilter(mLogoColor, PorterDuff.Mode.SRC_ATOP));
		setDrawingAlpha(DEFAULT_ALPHA);
	}

	public void setDrawingAlpha(int alpha) {
	    setAlpha((int) alpha);
	}

    public void setDrawingAlpha(float alpha) {
        setAlpha((int)Math.round(alpha * 255));
    }

    public void setViewAlpha(float alpha) {
        setAlpha(alpha);
    }

	// generate a sprite for trails
	public Bitmap cloneBitmap() {
		Bitmap src = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_eos_nx);
        Bitmap dest = Bitmap.createBitmap(src.getWidth(),src.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(dest);
        Paint p = new Paint();
        p.setColorFilter(new PorterDuffColorFilter(mLogoColor, PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(src, 0, 0, p);
        return dest;
	}
}
