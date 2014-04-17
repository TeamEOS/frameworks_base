package com.android.systemui.eos;

import android.graphics.Canvas;
import android.view.View;

public interface NxCallback {
	public View.OnTouchListener getNxGestureListener();
	public Canvas onInterceptDraw(Canvas c);
	public void onSizeChanged(View v, int width, int height);
}
