
package com.android.systemui.eos;

import java.util.HashMap;
import java.util.Map;

import org.codefirex.utils.ActionHandler;
import org.codefirex.utils.CFXUtils;

import com.android.systemui.eos.NxAction.ActionReceiver;
import com.android.systemui.statusbar.BarUiController;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.GestureDetector.OnGestureListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;

public class NxController extends ActionHandler
        implements ActionReceiver, OnTouchListener, OnGestureListener, NxCallback,
        NxAnimator.BufferListener {

    // custom tuning - stock timeout feels a bit slow here
    private static final int DT_TIMEOUT = ViewConfiguration.getDoubleTapTimeout() - 100;

    // phablets and tablets - horizontal and vertical views the bar width
    // changes
    // user may need different thresholds
    private static final String LONG_SWIPE_URI_LEFT_H = "eos_nx_long_swipe_left_h_threshold";
    private static final String LONG_SWIPE_URI_RIGHT_H = "eos_nx_long_swipe_right_h_threshold";
    private static final String LONG_SWIPE_URI_LEFT_V = "eos_nx_long_swipe_left_v_threshold";
    private static final String LONG_SWIPE_URI_RIGHT_V = "eos_nx_long_swipe_right_v_threshold";

    // normal screens - bar goes vertical
    private static final String LONG_SWIPE_URI_UP = "eos_nx_long_swipe_up_threshold";
    private static final String LONG_SWIPE_URI_DOWN = "eos_nx_long_swipe_down_threshold";

    private static final String BACK = "task_back";
    private static final String HOME = "task_home";

    private static final int HIDE_LOGO_DURATION = 100;
    private static final int SHOW_LOGO_DURATION = 150;

    private Map<Integer, NxAction> mActionMap;
    private Context mContext;
    private GestureDetector mGestureDetector;
    private ActionObserver mObserver;
    private NxHost mHost;
    // private NxAnimator mTrails; // disable for now

    private int mScreenSize;

    private boolean isDoubleTapEnabled;
    private boolean isDoubleTapPending;
    private boolean wasConsumed;

    // long swipe thresholds
    private float mLeftLandVal;
    private float mRightLandVal;
    private float mLeftPortVal;
    private float mRightPortVal;

    // vertical navbar (usually normal screen size)
    private float mUpVal;
    private float mDownVal;

    public NxController(Context context, Handler handler, NxHost host, int screenSize) {
        super(context);
        mContext = context;
        mHost = host;
        mScreenSize = screenSize;

        mActionMap = new HashMap<Integer, NxAction>();

        String action = "eos_nx_action_single_tap";
        mActionMap.put(NxAction.EVENT_SINGLE_TAP, new NxAction(action, this,
                handler, getAction(action), DT_TIMEOUT));

        action = "eos_nx_action_double_tap";
        mActionMap.put(NxAction.EVENT_DOUBLE_TAP, new NxAction(action, this,
                handler, getAction(action), 0));

        action = "eos_nx_action_long_press";
        mActionMap.put(NxAction.EVENT_LONG_PRESS, new NxAction(action, this,
                handler, getAction(action), 0));

        action = "eos_nx_action_fling_short_left";
        mActionMap.put(NxAction.EVENT_FLING_SHORT_LEFT, new NxAction(action,
                this, handler, getAction(action), 0));

        action = "eos_nx_action_fling_short_right";
        mActionMap.put(NxAction.EVENT_FLING_SHORT_RIGHT, new NxAction(action,
                this, handler, getAction(action), 0));

        action = "eos_nx_action_fling_long_left";
        mActionMap.put(NxAction.EVENT_FLING_LONG_LEFT, new NxAction(action,
                this, handler, getAction(action), 0));

        action = "eos_nx_action_fling_long_right";
        mActionMap.put(NxAction.EVENT_FLING_LONG_RIGHT, new NxAction(action,
                this, handler, getAction(action), 0));

        isDoubleTapEnabled = isDoubleTapEnabled();

        updateLPThreshold();

        mObserver = new ActionObserver(handler);
        mObserver.register();

        mGestureDetector = new GestureDetector(context, this);
    }

    private void animateLogo(boolean isDown) {
        final AnimationSet logoAnim = getLogoAnimator(isDown);
        mHost.getNxLogo().animate().cancel();
        mHost.getNxLogo().startAnimation(logoAnim);
    }

    private AnimationSet getLogoAnimator(boolean isDown) {
        final boolean down = isDown;
        final float from = isDown ? 1.0f : 0.1f;
        final float to = isDown ? 0.1f : 1.0f;
        final float fromDeg = isDown ? 0.0f : 360.0f;
        final float toDeg = isDown ? 360.0f : 0.0f;

        Animation scale = new ScaleAnimation(from, to, from, to, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        RotateAnimation rotate = new RotateAnimation(fromDeg, toDeg, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        AnimationSet animSet = new AnimationSet(true);
        animSet.setInterpolator(new LinearInterpolator());
        animSet.setDuration(150);
        animSet.setFillAfter(true);
        animSet.addAnimation(scale);
        animSet.addAnimation(rotate);
        animSet.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (!down)
                    mHost.getNxLogo().setAlpha(NxLogoView.DEFAULT_NX_ALPHA);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (down)
                    mHost.getNxLogo().setAlpha(0.0f);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // TODO Auto-generated method stub
            }

        });
        return animSet;
    }

    public void tearDown() {
        mObserver.unregister();
    }

    public void onScreenStateChanged(boolean screenOn) {
    }

    public void updateResources() {
        /*
         * int dimen; int width = mHost.getHostView().getWidth(); int height =
         * mHost.getHostView().getHeight(); if (mHost.isVertical()) { dimen =
         * Math.round(width * 0.65f); } else { dimen = Math.round(height *
         * 0.65f); } Bitmap bm =
         * Bitmap.createScaledBitmap(mHost.getNxLogo().cloneBitmap(), dimen,
         * dimen, false); if (mTrails != null) { mTrails.stopAnimation();
         * mTrails = null; } mTrails = new NxAnimator(mContext, width, height,
         * bm, NxController.this);
         */
    }

    private String getAction(String uri) {
        String action = Settings.System.getString(
                mContext.getContentResolver(), uri);
        if (TextUtils.isEmpty(action) || action.equals("empty")) {
            action = "";
        }
        return action;
    }

    class ActionObserver extends ContentObserver {

        public ActionObserver(Handler handler) {
            super(handler);
        }

        void register() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(LONG_SWIPE_URI_LEFT_H), false,
                    ActionObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(LONG_SWIPE_URI_RIGHT_H), false,
                    ActionObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(LONG_SWIPE_URI_LEFT_V), false,
                    ActionObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(LONG_SWIPE_URI_RIGHT_V), false,
                    ActionObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(LONG_SWIPE_URI_UP), false,
                    ActionObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(LONG_SWIPE_URI_DOWN), false,
                    ActionObserver.this);
            /*
             * mContext.getContentResolver().registerContentObserver(
             * Settings.System.getUriFor("eos_nx_trails_alpha_decay"), false,
             * ActionObserver.this);
             * mContext.getContentResolver().registerContentObserver(
             * Settings.System.getUriFor("eos_nx_trails_alpha_level"), false,
             * ActionObserver.this);
             * mContext.getContentResolver().registerContentObserver(
             * Settings.System.getUriFor("eos_nx_trails_max_trails"), false,
             * ActionObserver.this);
             * mContext.getContentResolver().registerContentObserver(
             * Settings.System.getUriFor("eos_nx_trails_touch_slop"), false,
             * ActionObserver.this);
             * mContext.getContentResolver().registerContentObserver(
             * Settings.System.getUriFor("eos_nx_trails_enabled"), false,
             * ActionObserver.this);
             */

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
            updateLPThreshold();
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
            updateResources();
        }
    }

    private void updateLPThreshold() {
        // get default swipe thresholds based on screensize
        float leftDefH;
        float rightDefH;
        float leftDefV;
        float rightDefV;

        // vertical bar, bar can move (normal screen)
        float upDef = 0.40f;
        float downDef = 0.40f;

        if (BarUiController.DEVICE_NORMAL_SCREEN == mScreenSize) {
            leftDefH = 0.40f;
            rightDefH = 0.40f;
            leftDefV = 0.35f;
            rightDefV = 0.35f;
        } else if (BarUiController.DEVICE_LARGE_SCREEN == mScreenSize) {
            leftDefH = 0.30f;
            rightDefH = 0.30f;
            leftDefV = 0.40f;
            rightDefV = 0.40f;
        } else if (BarUiController.DEVICE_XLARGE_SCREEN == mScreenSize) {
            leftDefH = 0.25f;
            rightDefH = 0.25f;
            leftDefV = 0.30f;
            rightDefV = 0.30f;
        } else {
            leftDefH = 0.40f;
            rightDefH = 0.40f;
            leftDefV = 0.40f;
            rightDefV = 0.40f;
        }

        mLeftLandVal = Settings.System.getFloat(
                mContext.getContentResolver(), LONG_SWIPE_URI_LEFT_H,
                leftDefH);

        mRightLandVal = Settings.System.getFloat(
                mContext.getContentResolver(), LONG_SWIPE_URI_RIGHT_H,
                rightDefH);

        mLeftPortVal = Settings.System.getFloat(
                mContext.getContentResolver(), LONG_SWIPE_URI_LEFT_V,
                leftDefV);

        mRightPortVal = Settings.System.getFloat(
                mContext.getContentResolver(), LONG_SWIPE_URI_RIGHT_V,
                rightDefV);

        mUpVal = Settings.System.getFloat(
                mContext.getContentResolver(), LONG_SWIPE_URI_UP,
                upDef);

        mDownVal = Settings.System.getFloat(
                mContext.getContentResolver(), LONG_SWIPE_URI_DOWN,
                downDef);
    }

    @Override
    public void onActionDispatched(NxAction actionEvent, String task) {
        isDoubleTapPending = false;
        if (actionEvent.isEnabled()) {
            if (task.equals(ActionHandler.SYSTEMUI_TASK_SCREENOFF)) {
                wasConsumed = false;
                animateLogo(false);
            }
            performTask(task);
            mHost.getHostView().performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY);
            mHost.getHostView().playSoundEffect(SoundEffectConstants.CLICK);
        }
    }

    private boolean isDoubleTapEnabled() {
        return ((NxAction) mActionMap.get(NxAction.EVENT_DOUBLE_TAP))
                .isEnabled();
    }

    private void fireAction(int type) {
        ((NxAction) mActionMap.get(type)).fireAction();
    }

    private void cancelAction(int type) {
        ((NxAction) mActionMap.get(type)).cancelAction();
    }

    private void queueAction(int type) {
        ((NxAction) mActionMap.get(type)).queueAction();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mHost.onDispatchMotionEvent(event);
        // if (mTrails != null && mTrails.isEnabled()) {
        // mTrails.handleMotionEvent(event.getAction(), event.getX(),
        // event.getY());
        // }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            animateLogo(false);
        }
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        animateLogo(true);
        if (isDoubleTapPending) {
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
            if (wasConsumed) {
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

        boolean isVertical = mHost.isVertical();
        boolean isLandscape = CFXUtils.isLandscape(mContext);

        final float deltaParallel = isVertical ? e2.getY() - e1.getY() : e2
                .getX() - e1.getX();

        boolean isLongSwipe = isLongSwipe(mHost.getHostView().getWidth(), mHost.getHostView()
                .getHeight(),
                deltaParallel, isVertical, isLandscape);

        if (deltaParallel > 0) {
            if (isVertical) {
                fireAction(isLongSwipe ? NxAction.EVENT_FLING_LONG_LEFT
                        : NxAction.EVENT_FLING_SHORT_LEFT);
            } else {
                fireAction(isLongSwipe ? NxAction.EVENT_FLING_LONG_RIGHT
                        : NxAction.EVENT_FLING_SHORT_RIGHT);
            }
        } else {
            if (isVertical) {
                fireAction(isLongSwipe ? NxAction.EVENT_FLING_LONG_RIGHT
                        : NxAction.EVENT_FLING_SHORT_RIGHT);
            } else {
                fireAction(isLongSwipe ? NxAction.EVENT_FLING_LONG_LEFT
                        : NxAction.EVENT_FLING_SHORT_LEFT);
            }
        }
        return true;
    }

    private boolean isLongSwipe(float width, float height, float distance,
            boolean isVertical, boolean isLandscape) {
        float size;
        float longPressThreshold;

        // determine correct bar dimensions to calculate against
        if (isLandscape) {
            if (isVertical) {
                size = height;
            } else {
                size = width;
            }
        } else {
            size = width;
        }
        // determine right or left
        // greater than zero is either right or up
        if (distance > 0) {
            if (isLandscape) {
                // must be landscape for vertical bar
                if (isVertical) {
                    // landscape with vertical bar
                    longPressThreshold = mUpVal;
                } else {
                    // landscape horizontal bar
                    longPressThreshold = mRightLandVal;
                }
            } else {
                // portrait: can't have vertical navbar
                longPressThreshold = mRightPortVal;
            }
        } else {
            // left or down
            if (isLandscape) {
                // must be landscape for vertical bar
                if (isVertical) {
                    // landscape with vertical bar
                    longPressThreshold = mDownVal;
                } else {
                    // landscape horizontal bar
                    longPressThreshold = mLeftLandVal;
                }
            } else {
                // portrait: can't have vertical navbar
                longPressThreshold = mLeftPortVal;
            }
        }

        return Math.abs(distance) > (size * longPressThreshold);
    }

    @Override
    public boolean handleAction(String action) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Canvas onInterceptDraw(Canvas c) {
        // if (mTrails != null && mTrails.isEnabled() && mTrails.isAnimating())
        // {
        // c.drawBitmap(mTrails.getBuffer(), 0, 0, null);
        // }
        return c;
    }

    @Override
    public OnTouchListener getNxGestureListener() {
        return (OnTouchListener) NxController.this;
    }

    @Override
    public void onSizeChanged(View v, int width, int height) {
        /*
         * int dimen; if (mHost.isVertical()) { dimen = Math.round(width *
         * 0.65f); } else { dimen = Math.round(height * 0.65f); } Bitmap bm =
         * Bitmap.createScaledBitmap(mHost.getNxLogo().cloneBitmap(), dimen,
         * dimen, false); if (mTrails != null) { mTrails.stopAnimation();
         * mTrails = null; } mTrails = new NxAnimator(mContext, width, height,
         * bm, NxController.this);
         */
    }

    @Override
    public void onPrepareToDraw() {
        mHost.getHostView().invalidate();
    }

    @Override
    public void onBufferUpdated(Bitmap buffer) {
        // TODO Auto-generated method stub

    }
}
