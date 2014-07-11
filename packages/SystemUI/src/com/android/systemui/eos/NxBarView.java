/*
 * Copyright (C) 2014 The TeamEos Project
 * Author: Randall Rushing aka Bigrushdog
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Gesture based navigation implementation and action executor
 *
 */
package com.android.systemui.eos;

import java.util.HashMap;
import java.util.Map;

import org.codefirex.utils.ActionHandler;
import org.codefirex.utils.CFXUtils;

import com.android.systemui.R;
import com.android.systemui.eos.NxAction.ActionReceiver;
import com.android.systemui.statusbar.BaseNavigationBar;
import com.android.systemui.statusbar.phone.BarTransitions;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

public class NxBarView extends BaseNavigationBar implements ActionReceiver {
    final static String TAG = NxBarView.class.getSimpleName();

    // custom tuning - stock timeout feels a bit slow here
    private static final int DT_TIMEOUT = ViewConfiguration.getDoubleTapTimeout() - 100;

    private static final String LONG_SWIPE_URI_LEFT_H = "eos_nx_long_swipe_left_h_threshold";
    private static final String LONG_SWIPE_URI_RIGHT_H = "eos_nx_long_swipe_right_h_threshold";
    private static final String LONG_SWIPE_URI_LEFT_V = "eos_nx_long_swipe_left_v_threshold";
    private static final String LONG_SWIPE_URI_RIGHT_V = "eos_nx_long_swipe_right_v_threshold";

    // normal screens - bar goes vertical
    private static final String LONG_SWIPE_URI_UP = "eos_nx_long_swipe_up_threshold";
    private static final String LONG_SWIPE_URI_DOWN = "eos_nx_long_swipe_down_threshold";

    private Map<Integer, NxAction> mActionMap;
    private ActionObserver mObserver;
    private GestureDetector mGestureDetector;
    private NxActionHandler mActionHandler;

    private Handler H = new Handler();
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

    private final NxBarTransitions mBarTransitions;

    private class NxActionHandler extends ActionHandler {

        public NxActionHandler(Context context) {
            super(context);
            // TODO Auto-generated constructor stub
        }

        @Override
        public boolean handleAction(String action) {
            // TODO Auto-generated method stub
            return false;
        }

    };

    private final OnTouchListener mNxTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mUserAutoHideListener != null) {
                mUserAutoHideListener.onTouch(NxBarView.this, event);
            }
            return mGestureDetector.onTouchEvent(event);
        }
    };

    private final OnGestureListener mNxGestureListener = new OnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
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
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
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

            boolean isVertical = mVertical;
            boolean isLandscape = CFXUtils.isLandscape(mContext);

            final float deltaParallel = isVertical ? e2.getY() - e1.getY() : e2
                    .getX() - e1.getX();

            boolean isLongSwipe = isLongSwipe(getWidth(), getHeight(),
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
    };

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

    public NxBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBarTransitions = new NxBarTransitions(this);

        mScreenSize = getScreenSize();
        mActionMap = new HashMap<Integer, NxAction>();

        String action = "eos_nx_action_single_tap";
        mActionMap.put(NxAction.EVENT_SINGLE_TAP, new NxAction(action, this,
                H, getAction(action), DT_TIMEOUT));

        action = "eos_nx_action_double_tap";
        mActionMap.put(NxAction.EVENT_DOUBLE_TAP, new NxAction(action, this,
                H, getAction(action), 0));

        action = "eos_nx_action_long_press";
        mActionMap.put(NxAction.EVENT_LONG_PRESS, new NxAction(action, this,
                H, getAction(action), 0));

        action = "eos_nx_action_fling_short_left";
        mActionMap.put(NxAction.EVENT_FLING_SHORT_LEFT, new NxAction(action,
                this, H, getAction(action), 0));

        action = "eos_nx_action_fling_short_right";
        mActionMap.put(NxAction.EVENT_FLING_SHORT_RIGHT, new NxAction(action,
                this, H, getAction(action), 0));

        action = "eos_nx_action_fling_long_left";
        mActionMap.put(NxAction.EVENT_FLING_LONG_LEFT, new NxAction(action,
                this, H, getAction(action), 0));

        action = "eos_nx_action_fling_long_right";
        mActionMap.put(NxAction.EVENT_FLING_LONG_RIGHT, new NxAction(action,
                this, H, getAction(action), 0));

        isDoubleTapEnabled = isDoubleTapEnabled();

        updateLPThreshold();

        mObserver = new ActionObserver(H);
        mObserver.register();
        mGestureDetector = new GestureDetector(mContext, mNxGestureListener);
        mActionHandler = new NxActionHandler(mContext);
    }

    // total touch control, except on keyguard
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isKeyguardShowing())
            return true;
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!isKeyguardShowing())
            return false;
        return super.onInterceptTouchEvent(event);
    }

    private int getScreenSize() {
        return Resources.getSystem().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
    }

    private String getAction(String uri) {
        String action = Settings.System.getString(
                mContext.getContentResolver(), uri);
        if (TextUtils.isEmpty(action) || action.equals("empty")) {
            action = "";
        }
        return action;
    }

    @Override
    public void onActionDispatched(NxAction actionEvent, String task) {
        isDoubleTapPending = false;
        if (actionEvent.isEnabled()) {
            if (task.equals(ActionHandler.SYSTEMUI_TASK_SCREENOFF)) {
                wasConsumed = false;
            }
            mActionHandler.performTask(task);
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            playSoundEffect(SoundEffectConstants.CLICK);
        }
    }

    @Override
    public BarTransitions getBarTransitions() {
        return mBarTransitions;
    }

    public View getNxContainer() {
        return mCurrentView.findViewById(R.id.eos_nx_container);
    }

    // stub views used to initialize DelegateHelper
    private View getLeftStub() {
        return mCurrentView.findViewById(R.id.nx_stub_left);
    }

    private View getRightStub() {
        return mCurrentView.findViewById(R.id.nx_stub_right);
    }

    private View getMiddleStub() {
        return mCurrentView.findViewById(R.id.nx_stub_middle);
    }

    // we still receive all flags from service so we can be aware
    public void setDisabledFlags(int disabledFlags, boolean force) {
        super.setDisabledFlags(disabledFlags, force);

        ViewGroup navButtons = (ViewGroup) mCurrentView.findViewById(R.id.eos_nx_container);
        if (navButtons != null) {
            LayoutTransition lt = navButtons.getLayoutTransition();
            if (lt != null) {
                if (!mScreenOn && mCurrentView != null) {
                    lt.disableTransitionType(
                            LayoutTransition.CHANGE_APPEARING |
                                    LayoutTransition.CHANGE_DISAPPEARING |
                                    LayoutTransition.APPEARING |
                                    LayoutTransition.DISAPPEARING);
                }
            }
        }

        setOnTouchListener(!isKeyguardShowing() ? mNxTouchListener : null);
        mBarTransitions.applyBackButtonQuiescentAlpha(mBarTransitions.getMode(), true /* animate */);
    }

    @Override
    public void setMenuVisibility(boolean show) {
        // TODO Auto-generated method stub
    }

    @Override
    public void reorient() {
        super.reorient();

        // force the low profile & disabled states into compliance
        mBarTransitions.init(mVertical);
        setDisabledFlags(mDisabledFlags, true /* force */);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mDelegateHelper.setInitialTouchRegion(getMiddleStub(), getLeftStub(), getRightStub());
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

        if (Configuration.SCREENLAYOUT_SIZE_NORMAL == mScreenSize) {
            leftDefH = 0.40f;
            rightDefH = 0.40f;
            leftDefV = 0.35f;
            rightDefV = 0.35f;
        } else if (Configuration.SCREENLAYOUT_SIZE_LARGE == mScreenSize) {
            leftDefH = 0.30f;
            rightDefH = 0.30f;
            leftDefV = 0.40f;
            rightDefV = 0.40f;
        } else if (Configuration.SCREENLAYOUT_SIZE_XLARGE == mScreenSize) {
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
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor("nx_logo_visible"), false,
                    ActionObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor("nx_logo_animates"), false,
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
            String changed = uri.toString();
            if (changed.contains("eos_nx_long_swipe")) {
                updateLPThreshold();
                return;
            }
            if (changed.contains("eos_nx_action")) {
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
                return;
            }
        }
    }

    @Override
    public void setNavigationIconHints(int hints) {
        // TODO Auto-generated method stub
        
    }
}
