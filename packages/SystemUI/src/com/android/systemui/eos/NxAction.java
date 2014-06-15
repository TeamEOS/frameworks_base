package com.android.systemui.eos;

import org.codefirex.utils.CFXConstants;

import android.os.Handler;
import android.text.TextUtils;

public class NxAction {
	public interface ActionReceiver {
		public void onActionDispatched(NxAction actionEvent, String task);
	}

	static final int EVENT_SINGLE_TAP = 1;
	static final int EVENT_DOUBLE_TAP = 2;
	static final int EVENT_LONG_PRESS = 3;
	static final int EVENT_FLING_SHORT_LEFT = 4;
	static final int EVENT_FLING_SHORT_RIGHT = 5;
	static final int EVENT_FLING_LONG_LEFT = 6;
	static final int EVENT_FLING_LONG_RIGHT = 7;

	private String mAction = "";
	private int mDelay = 0;
	private ActionReceiver mActionReceiver;
	private Handler mHandler;

	private final String mUri;

	private Runnable mActionThread = new Runnable() {
		@Override
		public void run() {
			mActionReceiver.onActionDispatched(NxAction.this, mAction);
		}
	};

	public NxAction(String uri, ActionReceiver receiver, Handler h, String action, int delay) {
		this.mUri = uri;
		this.mActionReceiver = receiver;
		this.mHandler = h;
		this.mAction = action;
		this.mDelay = delay;
	}

	public String getUri() {
		return mUri;
	}

	public void setAction(String action) {
		this.mAction = action;
	}

	public String getAction() {
		return mAction;
	}

	public void setDelay(int delay) {
		this.mDelay = delay;
	}

	public void fireAction() {
		mHandler.post(mActionThread);
	}

	public void queueAction() {
		mHandler.postDelayed(mActionThread, mDelay);
	}

	public void cancelAction() {
		mHandler.removeCallbacks(mActionThread);
	}

	public boolean isEnabled() {
		return !isActionEmpty(mAction) || !CFXConstants.SYSTEMUI_TASK_NO_ACTION.equals(mAction);
	}

	private boolean isActionEmpty(String action) {
		return TextUtils.isEmpty(action) 
				|| action.startsWith("empty")
				|| null == action;
	}

}
