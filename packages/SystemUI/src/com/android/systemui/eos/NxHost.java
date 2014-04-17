package com.android.systemui.eos;

import android.view.MotionEvent;
import android.view.View;

public interface NxHost {
	public void onStartNX(NxCallback callback);
	public void onStopNX();
    public NxLogoView getNxLogo();
    public View getHostView();
	public void onDispatchMotionEvent(MotionEvent event);
	public boolean isVertical();
}
