package com.android.systemui.eos;

import android.graphics.Canvas;

public interface CanvasInterceptor {
	public Canvas onInterceptDraw(Canvas c);
}
