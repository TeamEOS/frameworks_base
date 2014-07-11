package com.android.systemui.eos;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.KeyButtonView;

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
	}

	// generate a sprite for trails
	public Bitmap cloneBitmap() {
		Bitmap src = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.eos_logo);
        Bitmap dest = Bitmap.createBitmap(src.getWidth(),src.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(dest);
        Paint p = new Paint();
        p.setColorFilter(new PorterDuffColorFilter(mLogoColor, PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(src, 0, 0, p);
        return dest;
	}
}
