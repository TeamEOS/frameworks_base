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

package com.android.systemui.nx;

import com.android.systemui.R;
import com.android.systemui.nx.eyecandy.NxMediaController;
import com.android.systemui.nx.eyecandy.NxSurface;
import com.android.systemui.statusbar.BaseNavigationBar;
import com.android.systemui.statusbar.phone.BarTransitions;

import android.animation.LayoutTransition;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;

public class NxBarView extends BaseNavigationBar implements NxSurface {
    final static String TAG = NxBarView.class.getSimpleName();

    private NxActionHandler mActionHandler;
    private NxGestureHandler mGestureHandler;
    private NxGestureDetector mGestureDetector;
    private final NxBarTransitions mBarTransitions;
    private NxBarObserver mObserver = new NxBarObserver(new Handler());
    private AnimationSet mSpinOut;
    private AnimationSet mSpinIn;
    private boolean mIsAnimating;
    private boolean mLogoEnabled;
    private boolean mLogoAnimates;
    private NxMediaController mMC;

    private final class NxGestureDetector extends GestureDetector {
        final int LP_TIMEOUT = ViewConfiguration.getLongPressTimeout();
        // no more than default timeout
        final int LP_TIMEOUT_MAX = LP_TIMEOUT;
        // no less than 25ms longer than single tap timeout
        final int LP_TIMEOUT_MIN = 25;
        private int mLongPressTimeout = LP_TIMEOUT;

        public NxGestureDetector(Context context, OnGestureListener listener) {
            super(context, listener);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected int getLongPressTimeout() {
            Log.i(TAG, "LongPress timeout = " + String.valueOf(mLongPressTimeout));
            return mLongPressTimeout;
        }

        void setLongPressTimeout(int timeoutFactor) {
            if (timeoutFactor > LP_TIMEOUT_MAX) {
                timeoutFactor = LP_TIMEOUT_MAX;
            } else if (timeoutFactor < LP_TIMEOUT_MIN) {
                timeoutFactor = LP_TIMEOUT_MIN;
            }
            mLongPressTimeout = timeoutFactor;
        }
    }

    private class NxBarObserver extends ContentObserver {

        public NxBarObserver(Handler handler) {
            super(handler);
        }

        void register() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor("nx_logo_visible"), false,
                    NxBarObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor("nx_logo_animates"), false,
                    NxBarObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor("eos_nx_pulse"), false,
                    NxBarObserver.this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor("eos_nx_long_press_timeout"), false,
                    NxBarObserver.this);
        }

        void unregister() {
            mContext.getContentResolver().unregisterContentObserver(
                    NxBarObserver.this);
        }

        public void onChange(boolean selfChange, Uri uri) {
            boolean oldEnabled = mLogoEnabled;
            mLogoEnabled = Settings.System.getBoolean(mContext.getContentResolver(),
                    "nx_logo_visible", true);
            if (oldEnabled != mLogoEnabled) {
                findViewById(R.id.rot0).findViewById(R.id.nx_stub_middle).setAlpha(
                        mLogoEnabled ? 1.0f : 0.0f);
                findViewById(R.id.rot90).findViewById(R.id.nx_stub_middle).setAlpha(
                        mLogoEnabled ? 1.0f : 0.0f);
                setDisabledFlags(mDisabledFlags, true);
            }
            mLogoAnimates = Settings.System.getBoolean(mContext.getContentResolver(),
                    "nx_logo_animates", false);
            int lpTimeout = Settings.System.getInt(mContext.getContentResolver(),
                    "eos_nx_long_press_timeout", mGestureDetector.LP_TIMEOUT_MAX);
            mGestureDetector.setLongPressTimeout(lpTimeout);
            boolean doPulse = Settings.System.getBoolean(mContext.getContentResolver(),
                    "eos_nx_pulse", false);
            if (doPulse != mMC.isPulseEnabled()) {
                mMC.setPulseEnabled(doPulse);
            }
        }
    }

    private final OnTouchListener mNxTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            if (mUserAutoHideListener != null) {
                mUserAutoHideListener.onTouch(NxBarView.this, event);
            }
            if (action == MotionEvent.ACTION_DOWN) {
                if (!mIsAnimating)
                    animateLogo(true);
            } else if (action == MotionEvent.ACTION_UP) {
                animateLogo(false);
            }
            return mGestureDetector.onTouchEvent(event);
        }
    };

    public NxBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBarTransitions = new NxBarTransitions(this);
        mActionHandler = new NxActionHandler(context, this);
        mGestureHandler = new NxGestureHandler(context, mActionHandler, this);
        mGestureDetector = new NxGestureDetector(context, mGestureHandler);
        mObserver = new NxBarObserver(new Handler());
        mObserver.register();
        mLogoEnabled = Settings.System.getBoolean(mContext.getContentResolver(),
                "nx_logo_visible", true);
        mLogoAnimates = Settings.System.getBoolean(mContext.getContentResolver(),
                "nx_logo_animates", false);
        int lpTimeout = Settings.System.getInt(mContext.getContentResolver(),
                "eos_nx_long_press_timeout", mGestureDetector.LP_TIMEOUT_MAX);
        mGestureDetector.setLongPressTimeout(lpTimeout);
        mSpinIn = getLogoAnimator(false);
        mSpinOut = getLogoAnimator(true);
        mMC = new NxMediaController(context);
        boolean doPulse = Settings.System.getBoolean(mContext.getContentResolver(),
                "eos_nx_pulse", false);
        mMC.setPulseEnabled(doPulse);
        mMC.onSetNxSurface(this);
    }

    // total touch control, except on keyguard
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isKeyguardShowing())
            return true;
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!isKeyguardShowing())
            return false;
        return super.onInterceptTouchEvent(event);
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

    private NxLogoView getNxLogo() {
        return (NxLogoView) mCurrentView.findViewById(R.id.nx_stub_middle);
    }

    private void animateLogo(boolean isDown) {
        if (mLogoAnimates && mLogoEnabled) {
            getNxLogo().animate().cancel();
            getNxLogo().startAnimation(isDown ? mSpinOut : mSpinIn);
        }
    }

    @Override
    public void updateResources() {
        super.updateResources();
        for (View v : getAllChildren(findViewById(R.id.rot0))) {
            if (v instanceof NxLogoView) {
                ((NxLogoView) v).updateResources(R.id.rot0);
            }
        }

        for (View v : getAllChildren(findViewById(R.id.rot90))) {
            if (v instanceof NxLogoView) {
                ((NxLogoView) v).updateResources(R.id.rot90);
            }
        }
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

        mGestureHandler.onScreenStateChanged(mScreenOn);
        getNxLogo().setVisibility(!isKeyguardShowing() && mLogoEnabled && !mMC.shouldDrawPulse() ? View.VISIBLE : View.INVISIBLE);
        setOnTouchListener(!isKeyguardShowing() ? mNxTouchListener : null);
        mBarTransitions.applyBackButtonQuiescentAlpha(mBarTransitions.getMode(), true /* animate */);
        if (mLogoEnabled && (isKeyguardShowing() || mMC.shouldDrawPulse())) getNxLogo().setAlpha(0.0f);
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
        mGestureHandler.setIsVertical(mVertical);
        setDisabledFlags(mDisabledFlags, true /* force */);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mDelegateHelper.setInitialTouchRegion(getNxLogo(), getLeftStub(), getRightStub());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mMC.onSizeChanged();
    }

    @Override
    public void setNavigationIconHints(int hints) {
        // TODO Auto-generated method stub

    }

    private AnimationSet getLogoAnimator(boolean isDown) {
        final float from = isDown ? 1.0f : 0.0f;
        final float to = isDown ? 0.0f : 1.0f;
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
                mIsAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIsAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // TODO Auto-generated method stub
            }

        });
        return animSet;
    }

    @Override
    public Rect onGetSurfaceDimens() {
        Rect rect = new Rect();
        rect.set(0, 0, getWidth(), getHeight());
        return rect;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mMC.shouldDrawPulse() && !isKeyguardShowing()) {
            mMC.onDrawNx(canvas);
        }
    }

    @Override
    public void updateBar() {
        setDisabledFlags(mDisabledFlags, true /* force */);
    }
}
