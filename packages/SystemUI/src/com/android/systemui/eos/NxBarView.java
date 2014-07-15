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

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseNavigationBar;
import com.android.systemui.statusbar.phone.BarTransitions;

import android.animation.LayoutTransition;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class NxBarView extends BaseNavigationBar {
    final static String TAG = NxBarView.class.getSimpleName();

    private NxActionHandler mActionHandler;
    private NxGestureHandler mGestureHandler;
    private GestureDetector mGestureDetector;
    private final NxBarTransitions mBarTransitions;

    private final OnTouchListener mNxTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mUserAutoHideListener != null) {
                mUserAutoHideListener.onTouch(NxBarView.this, event);
            }
            return mGestureDetector.onTouchEvent(event);
        }
    };

    public NxBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBarTransitions = new NxBarTransitions(this);
        mActionHandler = new NxActionHandler(context);
        mGestureHandler = new NxGestureHandler(context, mActionHandler, this);
        mGestureDetector = new GestureDetector(context, mGestureHandler);
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
        mGestureHandler.setIsVertical(mVertical);
        setDisabledFlags(mDisabledFlags, true /* force */);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mDelegateHelper.setInitialTouchRegion(getMiddleStub(), getLeftStub(), getRightStub());
    }

    @Override
    public void setNavigationIconHints(int hints) {
        // TODO Auto-generated method stub

    }
}
