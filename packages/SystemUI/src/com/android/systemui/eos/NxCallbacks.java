package com.android.systemui.eos;

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

public interface NxCallbacks {
	public View onStartNX(View.OnTouchListener listener, CanvasInterceptor remoteCanvas);
	public void onStopNX();
	public void onDispatchMotionEvent(MotionEvent event);
	public boolean isVertical();
	public void onDraw(Canvas canvas);
}
