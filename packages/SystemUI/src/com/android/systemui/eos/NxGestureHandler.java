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
 * Fires actions based on detected motion. calculates long and short swipes
 * as well as double taps. User can set "long swipe thresholds" for custom
 * long swipe definition. 
 *
 */

package com.android.systemui.eos;

import org.codefirex.utils.ActionHandler;
import org.codefirex.utils.CFXUtils;

import com.android.systemui.eos.NxAction.ActionReceiver;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.GestureDetector.OnGestureListener;
import android.view.View;

public class NxGestureHandler implements OnGestureListener, ActionReceiver {
    private static final String LONG_SWIPE_URI_LEFT_H = "eos_nx_long_swipe_left_h_threshold";
    private static final String LONG_SWIPE_URI_RIGHT_H = "eos_nx_long_swipe_right_h_threshold";
    private static final String LONG_SWIPE_URI_LEFT_V = "eos_nx_long_swipe_left_v_threshold";
    private static final String LONG_SWIPE_URI_RIGHT_V = "eos_nx_long_swipe_right_v_threshold";

    // normal screens - bar goes vertical
    private static final String LONG_SWIPE_URI_UP = "eos_nx_long_swipe_up_threshold";
    private static final String LONG_SWIPE_URI_DOWN = "eos_nx_long_swipe_down_threshold";

    private Handler H = new Handler();

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

    // pass motion events to action handler
    private NxActionHandler mActionHandler;
    private GestureObserver mObserver;
    private Context mContext;

    // for width/height
    private View mHost;
    private boolean mVertical;

    public NxGestureHandler(Context context, NxActionHandler actionHandler, View host) {
        mContext = context;
        mActionHandler = actionHandler;
        mActionHandler.setActionReceiver(this);
        mHost = host;
        mObserver = new GestureObserver(H);
        mObserver.register();
        updateLPThreshold();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (isDoubleTapPending) {
            isDoubleTapPending = false;
            wasConsumed = true;
            mActionHandler.cancelAction(NxAction.EVENT_SINGLE_TAP);
            mActionHandler.fireAction(NxAction.EVENT_DOUBLE_TAP);
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
        if (mActionHandler.isDoubleTapEnabled()) {
            if (wasConsumed) {
                wasConsumed = false;
                return true;
            }
            isDoubleTapPending = true;
            mActionHandler.cancelAction(NxAction.EVENT_SINGLE_TAP);
            mActionHandler.queueAction(NxAction.EVENT_SINGLE_TAP);
        } else {
            mActionHandler.fireAction(NxAction.EVENT_SINGLE_TAP);
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
        mActionHandler.cancelAction(NxAction.EVENT_SINGLE_TAP);
        mActionHandler.fireAction(NxAction.EVENT_LONG_PRESS);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        mActionHandler.cancelAction(NxAction.EVENT_SINGLE_TAP);

        boolean isVertical = mVertical;
        boolean isLandscape = CFXUtils.isLandscape(mContext);

        final float deltaParallel = isVertical ? e2.getY() - e1.getY() : e2
                .getX() - e1.getX();

        boolean isLongSwipe = isLongSwipe(mHost.getWidth(), mHost.getHeight(),
                deltaParallel, isVertical, isLandscape);

        if (deltaParallel > 0) {
            if (isVertical) {
                mActionHandler.fireAction(isLongSwipe ? NxAction.EVENT_FLING_LONG_LEFT
                        : NxAction.EVENT_FLING_SHORT_LEFT);
            } else {
                mActionHandler.fireAction(isLongSwipe ? NxAction.EVENT_FLING_LONG_RIGHT
                        : NxAction.EVENT_FLING_SHORT_RIGHT);
            }
        } else {
            if (isVertical) {
                mActionHandler.fireAction(isLongSwipe ? NxAction.EVENT_FLING_LONG_RIGHT
                        : NxAction.EVENT_FLING_SHORT_RIGHT);
            } else {
                mActionHandler.fireAction(isLongSwipe ? NxAction.EVENT_FLING_LONG_LEFT
                        : NxAction.EVENT_FLING_SHORT_LEFT);
            }
        }
        return true;
    }

    private int getScreenSize() {
        return Resources.getSystem().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
    }

    // expose to NxBarView for config changes
    public void setIsVertical(boolean isVertical) {
        mVertical = isVertical;
    }

    public void unregister() {
        mObserver.unregister();
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

    private void updateLPThreshold() {
        // get default swipe thresholds based on screensize
        float leftDefH;
        float rightDefH;
        float leftDefV;
        float rightDefV;

        // vertical bar, bar can move (normal screen)
        float upDef = 0.40f;
        float downDef = 0.40f;

        int screenSize = getScreenSize();

        if (Configuration.SCREENLAYOUT_SIZE_NORMAL == screenSize) {
            leftDefH = 0.40f;
            rightDefH = 0.40f;
            leftDefV = 0.35f;
            rightDefV = 0.35f;
        } else if (Configuration.SCREENLAYOUT_SIZE_LARGE == screenSize) {
            leftDefH = 0.30f;
            rightDefH = 0.30f;
            leftDefV = 0.40f;
            rightDefV = 0.40f;
        } else if (Configuration.SCREENLAYOUT_SIZE_XLARGE == screenSize) {
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

    private class GestureObserver extends ContentObserver {

        public GestureObserver(Handler handler) {
            super(handler);
        }

        void register() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(LONG_SWIPE_URI_LEFT_H), false,
                    GestureObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(LONG_SWIPE_URI_RIGHT_H), false,
                    GestureObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(LONG_SWIPE_URI_LEFT_V), false,
                    GestureObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(LONG_SWIPE_URI_RIGHT_V), false,
                    GestureObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(LONG_SWIPE_URI_UP), false,
                    GestureObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(LONG_SWIPE_URI_DOWN), false,
                    GestureObserver.this);
        }

        void unregister() {
            mContext.getContentResolver().unregisterContentObserver(
                    GestureObserver.this);
        }

        public void onChange(boolean selfChange, Uri uri) {
                updateLPThreshold();
        }
    }

    @Override
    public void onActionDispatched(NxAction actionEvent, String task) {
        isDoubleTapPending = false;
        if (actionEvent.isEnabled()) {
            if (task.equals(ActionHandler.SYSTEMUI_TASK_SCREENOFF)) {
                wasConsumed = false;
            }
            mHost.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            mHost.playSoundEffect(SoundEffectConstants.CLICK);
        }
    }
}
