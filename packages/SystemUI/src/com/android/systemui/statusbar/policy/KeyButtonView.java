/*
 * Copyright (C) 2008 The Android Open Source Project
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
 */

package com.android.systemui.statusbar.policy;

import android.animation.Animator;

import org.codefirex.utils.ActionHandler;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;

import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.R;

public class KeyButtonView extends ImageView {
    private static final String TAG = "StatusBar.KeyButtonView";
    private static final boolean DEBUG = false;

    final float GLOW_MAX_SCALE_FACTOR = 1.8f;
    public static final float DEFAULT_QUIESCENT_ALPHA = 0.70f;

    long mDownTime;
    long mUpTime;
    int mTouchSlop;
    Drawable mGlowBG;
    int mGlowWidth, mGlowHeight;
    float mGlowAlpha = 0f, mGlowScale = 1f;
    @ViewDebug.ExportedProperty(category = "drawing")
    float mDrawingAlpha = 1f;
    @ViewDebug.ExportedProperty(category = "drawing")
    float mQuiescentAlpha = DEFAULT_QUIESCENT_ALPHA;
    boolean mSupportsLongpress = true;
    RectF mRect = new RectF();
    AnimatorSet mPressedAnim;
    Animator mAnimateToQuiescent = new ObjectAnimator();
    View.OnTouchListener mHomeSearchActionListener;

    final int mSingleTapTimeout = ViewConfiguration.getTapTimeout();
    boolean mShouldClick = true;
    private int mLongPressTimeout;
    private int mDoubleTapTimeout;
    private String mLpUri;
    private String mDtUri;
    private ButtonInfo mActions;
    private ActionHandler mActionHandler;

    protected static IStatusBarService mBarService;

    public static synchronized void getStatusBarInstance() {
        if (mBarService == null) {
            mBarService = IStatusBarService.Stub.asInterface(
                    ServiceManager.getService(Context.STATUS_BAR_SERVICE));
        }
    }

    private boolean mHasSingleAction = true, mHasDoubleAction, mHasLongAction;
    private boolean mIsRecentsAction = false, mIsRecentsSingleAction, mIsRecentsLongAction,
            mIsRecentsDoubleTapAction;
    public boolean mHasBlankSingleAction = false;
    volatile boolean mRecentsPreloaded;

    Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (isPressed()) {
                removeCallbacks(mSingleTap);
                doLongPress();
            }
        }
    };

    private Runnable mSingleTap = new Runnable() {
        @Override
        public void run() {
            if (!isPressed()) {
                doSinglePress();
            }
        }
    };

    public KeyButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KeyButtonView,
                defStyle, 0);

        mLpUri = a.getString(R.styleable.KeyButtonView_longPressUri);
        mDtUri = a.getString(R.styleable.KeyButtonView_doubleTapUri);
        if (mLpUri == null)
            mLpUri = " ";
        if (mDtUri == null)
            mDtUri = " ";

        mGlowBG = a.getDrawable(R.styleable.KeyButtonView_glowBackground);
        setDrawingAlpha(mQuiescentAlpha);
        if (mGlowBG != null) {
            mGlowWidth = mGlowBG.getIntrinsicWidth();
            mGlowHeight = mGlowBG.getIntrinsicHeight();
        }

        a.recycle();
        setClickable(true);
        setLongClickable(false);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public String[] getActionUris() {
        return new String[] {
                mLpUri, mDtUri 
        };
    }

    public void setLongPressTimeout(int lpTimeout) {
        mLongPressTimeout = lpTimeout;
    }

    public void setDoubleTapTimeout(int dtTimeout) {
        mDoubleTapTimeout = dtTimeout;
    }

    public void setActionHandler(ActionHandler handler) {
        mActionHandler = handler;
    }

    public void setButtonActions(ButtonInfo actions) {
        this.mActions = actions;

        setTag(mActions.singleAction); // should be OK even if it's null

        mHasSingleAction = mActions != null
                && (mActions.singleAction != null && !mActions.singleAction
                        .equals(ActionHandler.SYSTEMUI_TASK_NO_ACTION));
        mHasLongAction = mActions != null && mActions.longPressAction != null
                && !mActions.longPressAction.equals(ActionHandler.SYSTEMUI_TASK_NO_ACTION);
        mHasDoubleAction = mActions != null && mActions.doubleTapAction != null
                && !mActions.doubleTapAction.equals(ActionHandler.SYSTEMUI_TASK_NO_ACTION);
        mHasBlankSingleAction = mHasSingleAction
                && mActions.singleAction.equals(ActionHandler.SYSTEMUI_TASK_NO_ACTION);

        mIsRecentsSingleAction = (mHasSingleAction && mActions.singleAction
                .equals(ActionHandler.SYSTEMUI_TASK_RECENTS));
        mIsRecentsLongAction = (mHasLongAction && mActions.longPressAction
                .equals(ActionHandler.SYSTEMUI_TASK_RECENTS));
        mIsRecentsDoubleTapAction = (mHasDoubleAction && mActions.doubleTapAction
                .equals(ActionHandler.SYSTEMUI_TASK_RECENTS));

        if (mIsRecentsSingleAction || mIsRecentsLongAction || mIsRecentsDoubleTapAction) {
            mIsRecentsAction = true;
            getStatusBarInstance();
        }

        setLongClickable(mHasLongAction);
        if (getId() == R.id.home && mHomeSearchActionListener != null) {
            setOnTouchListener(mHasLongAction ? null : mHomeSearchActionListener);
        }
        Log.e(TAG, "Adding a navbar button in landscape or portrait");
    }

    public void setHomeSearchActionListener(View.OnTouchListener homeListener) {
        if (this.getId() == R.id.home) {
            if (mHomeSearchActionListener == null) {
                mHomeSearchActionListener = homeListener;
            }
            if (mActions.longPressAction.equals(ActionHandler.SYSTEMUI_TASK_NO_ACTION)) {
                setOnTouchListener(homeListener);
            } else {
                setOnTouchListener(null);
            }
        }
    }

    public void updateResources(int rot) {
        if (mGlowBG == null)
            return;
        int res = rot == R.id.rot0 ? R.drawable.ic_sysbar_highlight
                : R.drawable.ic_sysbar_highlight_land;
        mGlowBG = getResources().getDrawable(res);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mGlowBG != null) {
            canvas.save();
            final int w = getWidth();
            final int h = getHeight();
            final float aspect = (float) mGlowWidth / mGlowHeight;
            final int drawW = (int) (h * aspect);
            final int drawH = h;
            final int margin = (drawW - w) / 2;
            canvas.scale(mGlowScale, mGlowScale, w * 0.5f, h * 0.5f);
            mGlowBG.setBounds(-margin, 0, drawW - margin, drawH);
            mGlowBG.setAlpha((int) (mDrawingAlpha * mGlowAlpha * 255));
            mGlowBG.draw(canvas);
            canvas.restore();
            mRect.right = w;
            mRect.bottom = h;
        }
        super.onDraw(canvas);
    }

    public void setQuiescentAlpha(float alpha, boolean animate) {
        mAnimateToQuiescent.cancel();
        alpha = Math.min(Math.max(alpha, 0), 1);
        if (alpha == mQuiescentAlpha && alpha == mDrawingAlpha)
            return;
        mQuiescentAlpha = alpha;
        if (DEBUG)
            Log.d(TAG, "New quiescent alpha = " + mQuiescentAlpha);
        if (mGlowBG != null && animate) {
            mAnimateToQuiescent = animateToQuiescent();
            mAnimateToQuiescent.start();
        } else {
            setDrawingAlpha(mQuiescentAlpha);
        }
    }

    private ObjectAnimator animateToQuiescent() {
        return ObjectAnimator.ofFloat(this, "drawingAlpha", mQuiescentAlpha);
    }

    public float getQuiescentAlpha() {
        return mQuiescentAlpha;
    }

    public float getDrawingAlpha() {
        return mDrawingAlpha;
    }

    public void setDrawingAlpha(float x) {
        // Calling setAlpha(int), which is an ImageView-specific
        // method that's different from setAlpha(float). This sets
        // the alpha on this ImageView's drawable directly
        setAlpha((int) (x * 255));
        mDrawingAlpha = x;
    }

    public float getGlowAlpha() {
        if (mGlowBG == null)
            return 0;
        return mGlowAlpha;
    }

    public void setGlowAlpha(float x) {
        if (mGlowBG == null)
            return;
        mGlowAlpha = x;
        invalidate();
    }

    public float getGlowScale() {
        if (mGlowBG == null)
            return 0;
        return mGlowScale;
    }

    public void setGlowScale(float x) {
        if (mGlowBG == null)
            return;
        mGlowScale = x;
        final float w = getWidth();
        final float h = getHeight();
        if (GLOW_MAX_SCALE_FACTOR <= 1.0f) {
            // this only works if we know the glow will never leave our bounds
            invalidate();
        } else {
            final float rx = (w * (GLOW_MAX_SCALE_FACTOR - 1.0f)) / 2.0f + 1.0f;
            final float ry = (h * (GLOW_MAX_SCALE_FACTOR - 1.0f)) / 2.0f + 1.0f;
            com.android.systemui.SwipeHelper.invalidateGlobalRegion(
                    this,
                    new RectF(getLeft() - rx,
                            getTop() - ry,
                            getRight() + rx,
                            getBottom() + ry));

            // also invalidate our immediate parent to help avoid situations
            // where nearby glows
            // interfere
            if (getParent() != null)
                ((View) getParent()).invalidate();
        }
    }

    public void setPressed(boolean pressed) {
        if (mGlowBG != null) {
            if (pressed != isPressed()) {
                if (mPressedAnim != null && mPressedAnim.isRunning()) {
                    mPressedAnim.cancel();
                }
                final AnimatorSet as = mPressedAnim = new AnimatorSet();
                if (pressed) {
                    if (mGlowScale < GLOW_MAX_SCALE_FACTOR)
                        mGlowScale = GLOW_MAX_SCALE_FACTOR;
                    if (mGlowAlpha < mQuiescentAlpha)
                        mGlowAlpha = mQuiescentAlpha;
                    setDrawingAlpha(1f);
                    as.playTogether(
                            ObjectAnimator.ofFloat(this, "glowAlpha", 1f),
                            ObjectAnimator.ofFloat(this, "glowScale", GLOW_MAX_SCALE_FACTOR)
                            );
                    as.setDuration(50);
                } else {
                    mAnimateToQuiescent.cancel();
                    mAnimateToQuiescent = animateToQuiescent();
                    as.playTogether(
                            ObjectAnimator.ofFloat(this, "glowAlpha", 0f),
                            ObjectAnimator.ofFloat(this, "glowScale", 1f),
                            mAnimateToQuiescent
                            );
                    as.setDuration(500);
                }
                as.start();
            }
        }
        super.setPressed(pressed);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (mHasBlankSingleAction) {
            Log.i(TAG, "Has blanking action");
            return true;
        }

        final int action = ev.getAction();
        int x, y;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mIsRecentsAction && mRecentsPreloaded == false)
                    preloadRecentApps();
                mDownTime = SystemClock.uptimeMillis();
                setPressed(true);
                if (mHasSingleAction) {
                    removeCallbacks(mSingleTap);
                }
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                long diff = mDownTime - mUpTime; // difference between last up
                                                 // and now
                if (mHasDoubleAction && diff <= mDoubleTapTimeout) {
                    doDoubleTap();
                } else {

                    if (mHasLongAction) {
                        removeCallbacks(mCheckLongPress);
                        postDelayed(mCheckLongPress, mLongPressTimeout);
                    }
                    if (mHasSingleAction) {
                        postDelayed(mSingleTap, mSingleTapTimeout);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                x = (int) ev.getX();
                y = (int) ev.getY();
                setPressed(x >= -mTouchSlop
                        && x < getWidth() + mTouchSlop
                        && y >= -mTouchSlop
                        && y < getHeight() + mTouchSlop);
                break;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                if (mHasSingleAction) {
                    removeCallbacks(mSingleTap);
                }
                if (mHasLongAction) {
                    removeCallbacks(mCheckLongPress);
                }
                if (mRecentsPreloaded == true)
                    cancelPreloadRecentApps();
                break;
            case MotionEvent.ACTION_UP:
                mUpTime = SystemClock.uptimeMillis();
                boolean playSound;

                if (mHasLongAction) {
                    removeCallbacks(mCheckLongPress);
                }
                playSound = isPressed();
                setPressed(false);

                if (playSound) {
                    playSoundEffect(SoundEffectConstants.CLICK);
                }

                if (!mHasDoubleAction && !mHasLongAction) {
                    removeCallbacks(mSingleTap);
                    doSinglePress();
                }
                break;
        }
        return true;
    }

    private void doSinglePress() {
        if (callOnClick()) {
            // cool
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
        } else if (mIsRecentsSingleAction) {
            try {
                mBarService.toggleRecentApps();
                sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                mRecentsPreloaded = false;
            } catch (RemoteException e) {
                Log.e(TAG, "RECENTS ACTION FAILED");
            }
            return;
        }

        if (mActions != null) {
            if (mActions.singleAction != null) {
                mActionHandler.performTask(mActions.singleAction);
                sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
            }
        }
    }

    private void doDoubleTap() {
        if (mHasDoubleAction) {
            removeCallbacks(mSingleTap);
            if (mIsRecentsDoubleTapAction) {
                try {
                    mBarService.toggleRecentApps();
                    mRecentsPreloaded = false;
                } catch (RemoteException e) {
                    Log.e(TAG, "RECENTS ACTION FAILED");
                }
            } else {
                mActionHandler.performTask(mActions.doubleTapAction);
            }
        }
    }

    private void doLongPress() {
        if (mHasLongAction) {
            removeCallbacks(mSingleTap);
            if (mIsRecentsLongAction) {
                try {
                    mBarService.toggleRecentApps();
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
                    mRecentsPreloaded = false;
                } catch (RemoteException e) {
                    Log.e(TAG, "RECENTS ACTION FAILED");
                }
            } else {
                mActionHandler.performTask(mActions.longPressAction);
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
            }
        }
    }

    private void cancelPreloadRecentApps() {
        if (mRecentsPreloaded == false)
            return;
        try {
            mBarService.cancelPreloadRecentApps();
        } catch (RemoteException e) {
            // use previous state
            return;
        }
        mRecentsPreloaded = false;
    }

    private void preloadRecentApps() {
        try {
            mBarService.preloadRecentApps();
        } catch (RemoteException e) {
            mRecentsPreloaded = false;
            return;
        }
        mRecentsPreloaded = true;
    }

    public static class ButtonInfo {
        public String singleAction, doubleTapAction, longPressAction, lpUri, dtUri;

        public ButtonInfo(String singleTap, String doubleTap, String longPress, String LpUri,
                String DtUri) {
            this.singleAction = singleTap;
            this.doubleTapAction = doubleTap;
            this.longPressAction = longPress;
            this.lpUri = LpUri;
            this.dtUri = DtUri;
        }
    }
}
