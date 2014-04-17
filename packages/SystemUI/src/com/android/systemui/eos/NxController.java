package com.android.systemui.eos;

import java.util.HashMap;
import java.util.Map;

import org.codefirex.utils.ActionHandler;
import org.codefirex.utils.CFXConstants;
import org.codefirex.utils.CFXUtils;

import com.android.systemui.eos.NxAction.ActionReceiver;
import com.android.systemui.statusbar.BaseStatusBar;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.GestureDetector.OnGestureListener;

public class NxController extends ActionHandler implements ActionReceiver, View.OnTouchListener, OnGestureListener, CanvasInterceptor {
	private static final boolean DEBUG = true;

	// custom tuning - stock timeout feels a bit slow here
	private static final int DT_TIMEOUT = ViewConfiguration.getDoubleTapTimeout() - 100;

	// minimum percent of bar width (or height if vertical)
	// user must swipe to trigger long swipe
	private static final float LONG_SWIPE_THRESHOLD = 0.40f;
	private static final String LONG_SWIPE_URI = "eos_nx_long_swipe_threshold";

	private static final String BACK = "task_back";
	private static final String HOME = "task_home";
	private static final String RECENTS = CFXConstants.SYSTEMUI_TASK_RECENTS;
	private static final String MENU = CFXConstants.SYSTEMUI_TASK_MENU;
	private static final String ASSIST = CFXConstants.SYSTEMUI_TASK_ASSIST;

	private Map<Integer, NxAction> mActionMap;
	private Context mContext;
	private View mHost;
	private GestureDetector mGestureDetector;
	private ActionObserver mObserver;
	private NxCallbacks mCallback;
	private BaseStatusBar mBar;

	private boolean isPressed;
	private boolean isDoubleTapEnabled;
	private boolean isDoubleTapPending;
	private boolean wasConsumed;
	private float longPressThreshold;

	public NxController(Context context, Handler handler, BaseStatusBar bar, NxCallbacks callbacks) {
		super(context);
		mContext = context;
		mBar = bar;
		mCallback = callbacks;
		mActionMap = new HashMap<Integer, NxAction>();

		String action = "eos_nx_action_single_tap";
		mActionMap.put(NxAction.EVENT_SINGLE_TAP, new NxAction(action, this,
				handler, getAction(action, HOME), DT_TIMEOUT));

		action = "eos_nx_action_double_tap";
		mActionMap.put(NxAction.EVENT_DOUBLE_TAP, new NxAction(action, this,
				handler, getAction(action), 0));

		action = "eos_nx_action_long_press";
		mActionMap.put(NxAction.EVENT_LONG_PRESS, new NxAction(action, this,
				handler, getAction(action, MENU), 0));

		action = "eos_nx_action_fling_short_left";
		mActionMap.put(NxAction.EVENT_FLING_SHORT_LEFT, new NxAction(action,
				this, handler, getAction(action, BACK), 0));

		action = "eos_nx_action_fling_short_right";
		mActionMap.put(NxAction.EVENT_FLING_SHORT_RIGHT, new NxAction(action,
				this, handler, getAction(action, RECENTS), 0));

		action = "eos_nx_action_fling_long_left";
		mActionMap.put(NxAction.EVENT_FLING_LONG_LEFT, new NxAction(action,
				this, handler, getAction(action), 0));

		action = "eos_nx_action_fling_long_right";
		mActionMap.put(NxAction.EVENT_FLING_LONG_RIGHT, new NxAction(action,
				this, handler, getAction(action, ASSIST), 0));

		isDoubleTapEnabled = isDoubleTapEnabled();

		mObserver = new ActionObserver(handler);
		mObserver.register();

		longPressThreshold = Settings.System.getFloat(
				mContext.getContentResolver(), LONG_SWIPE_URI,
				LONG_SWIPE_THRESHOLD);

		mGestureDetector = new GestureDetector(context, this);
	}

	public void tearDown() {
		mObserver.unregister();
	}

	public void setHostView(View v) {
		mHost = v;
	}

	private String getAction(String uri) {
		String action = Settings.System.getString(
				mContext.getContentResolver(), uri);
		if (TextUtils.isEmpty(action) || action.equals("empty")) {
			action = "";
		}
		return action;
	}

	private String getAction(String uri, String def) {
		String action = Settings.System.getString(
				mContext.getContentResolver(), uri);
		if (TextUtils.isEmpty(action) || action.equals("empty")) {
			action = def;
		}
		return action;
	}

	class ActionObserver extends ContentObserver {

		public ActionObserver(Handler handler) {
			super(handler);
		}

		void register() {
			mContext.getContentResolver().registerContentObserver(
					Settings.System.getUriFor(LONG_SWIPE_URI), false,
					ActionObserver.this);
			for (int i = 1; i < mActionMap.size() + 1; i++) {
				NxAction action = mActionMap.get(i);
				mContext.getContentResolver().registerContentObserver(
						Settings.System.getUriFor(action.getUri()), false,
						ActionObserver.this);
			}
		}

		void unregister() {
			mContext.getContentResolver().unregisterContentObserver(
					ActionObserver.this);
		}

		public void onChange(boolean selfChange, Uri uri) {
			if (uri.equals(Settings.System.getUriFor(LONG_SWIPE_URI))) {
				longPressThreshold = Settings.System.getFloat(
						mContext.getContentResolver(), LONG_SWIPE_URI,
						LONG_SWIPE_THRESHOLD);
			} else {
				for (int i = 1; i < mActionMap.size() + 1; i++) {
					NxAction action = mActionMap.get(i);
					Uri testUri = Settings.System.getUriFor(action.getUri());
					if (testUri.equals(uri)) {
						action.setAction(getAction(action.getUri()));
						mActionMap.put(i, action);
						break;
					}
				}
				isDoubleTapEnabled = isDoubleTapEnabled();
			}
		}
	}

	@Override
	public void onActionDispatched(NxAction actionEvent, String task) {
		isDoubleTapPending = false;
		if (task.equals(BACK)) {
			injectKey(KeyEvent.KEYCODE_BACK);
		} else if (task.equals(HOME)) {
			injectKey(KeyEvent.KEYCODE_HOME);
		} else if (task.equals(RECENTS)) {
			toggleRecents();
		} else if (task.equals(MENU)) {
			injectKey(KeyEvent.KEYCODE_MENU);
		} else {
			if (task.equals(CFXConstants.SYSTEMUI_TASK_SCREENOFF)) {
				wasConsumed = false;
				isDoubleTapPending = false;
				isPressed = false;
			}
			mHost.invalidate();
			performTask(task);
		}
		if (actionEvent.isEnabled()) {
			mHost.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			mHost.playSoundEffect(SoundEffectConstants.CLICK);
		}
	}

	private boolean isDoubleTapEnabled() {
		return ((NxAction) mActionMap.get(NxAction.EVENT_DOUBLE_TAP))
				.isEnabled();
	}

	private void fireAction(int type) {
		((NxAction)mActionMap.get(type)).fireAction();
	}

	private void cancelAction(int type) {
		((NxAction)mActionMap.get(type)).cancelAction();
	}

	private void queueAction(int type) {
		((NxAction)mActionMap.get(type)).queueAction();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		mCallback.onDispatchMotionEvent(event);
		// disabled on secure keyguard
		if (CFXUtils.isKeyguardRestricted(mContext)) return true;
		if (event.getAction() == MotionEvent.ACTION_UP) {
			isPressed = false;
			mHost.invalidate();
		}
		return mGestureDetector.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		isPressed = true;
		mHost.invalidate();
		if(isDoubleTapPending) {
			isDoubleTapPending = false;
			wasConsumed = true;
			cancelAction(NxAction.EVENT_SINGLE_TAP);
			fireAction(NxAction.EVENT_DOUBLE_TAP);
			return true;
		}
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (isDoubleTapEnabled) {
           if(wasConsumed) {
        	   wasConsumed = false;
        	   return true;
           }
           isDoubleTapPending = true;
           cancelAction(NxAction.EVENT_SINGLE_TAP);
           queueAction(NxAction.EVENT_SINGLE_TAP);
		} else {
			fireAction(NxAction.EVENT_SINGLE_TAP);
		}
		return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
        cancelAction(NxAction.EVENT_SINGLE_TAP);
		fireAction(NxAction.EVENT_LONG_PRESS);
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
        cancelAction(NxAction.EVENT_SINGLE_TAP);

		boolean result = false;
		boolean isVertical = mCallback.isVertical();

		final float deltaParallel = isVertical ? e2.getY() - e1.getY() : e2
				.getX() - e1.getX();
		boolean isLongSwipe = isLongSwipe(mHost.getWidth(), mHost.getHeight(),
				deltaParallel);

		if (deltaParallel > 0) {
			if (isVertical) {
				fireAction(isLongSwipe ? NxAction.EVENT_FLING_LONG_LEFT
						: NxAction.EVENT_FLING_SHORT_LEFT);
			} else {
				fireAction(isLongSwipe ? NxAction.EVENT_FLING_LONG_RIGHT
						: NxAction.EVENT_FLING_SHORT_RIGHT);
			}
			result = true;
		} else if (deltaParallel < 0) {
			if (isVertical) {
				fireAction(isLongSwipe ? NxAction.EVENT_FLING_LONG_RIGHT
						: NxAction.EVENT_FLING_SHORT_RIGHT);
			} else {
				fireAction(isLongSwipe ? NxAction.EVENT_FLING_LONG_LEFT
						: NxAction.EVENT_FLING_SHORT_LEFT);
			}
			result = true;
		}
		return result;
	}

	private boolean isLongSwipe(float width, float height, float distance) {
		float size = width > height ? width : height;
		return Math.abs(distance) > size * longPressThreshold;
	}

	private void injectKey(int keycode) {
		final long eventTime = SystemClock.uptimeMillis();
		KeyEvent keyEvent = new KeyEvent(eventTime, eventTime,
				KeyEvent.ACTION_DOWN, keycode, 0);

		InputManager.getInstance().injectInputEvent(keyEvent,
				InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);

		keyEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP);
		InputManager.getInstance().injectInputEvent(keyEvent,
				InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
	}

	private void toggleRecents() {
		mBar.toggleRecentApps();
	}

	@Override
	public boolean handleAction(String action) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Canvas onInterceptDraw(Canvas c) {
		if (isPressed && !DEBUG) {
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setColor(mContext.getResources().getColor(
					com.android.systemui.R.color.status_bar_clock_color));
			paint.setStrokeWidth(1.5f);
			paint.setStyle(Style.STROKE);
			c.drawRect(0, 0, mHost.getWidth(), mHost.getHeight(), paint);
		}
		return c;
	}
}

