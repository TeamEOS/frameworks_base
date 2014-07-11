/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2014 The TeamEos Project
 * 
 * Contributor: Randall Rushing aka Bigrushdog
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
 * AOSP based Softkey navigation implementation and action executor
 * 
 */

package com.android.systemui.statusbar.phone;

import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseNavigationBar;
import com.android.systemui.statusbar.policy.KeyButtonView;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class NavigationBarView extends BaseNavigationBar {
    final static String TAG = "PhoneStatusBar/NavigationBarView";

    private Drawable mBackIcon, mBackLandIcon, mBackAltIcon, mBackAltLandIcon;
    private Drawable mRecentIcon;
    private Drawable mRecentLandIcon;

    private final NavigationBarTransitions mBarTransitions;

    // performs manual animation in sync with layout transitions
    private final NavTransitionListener mTransitionListener = new NavTransitionListener();

    private boolean mShowMenu;

    private class NavTransitionListener implements TransitionListener {
        private boolean mBackTransitioning;
        private boolean mHomeAppearing;
        private long mStartDelay;
        private long mDuration;
        private TimeInterpolator mInterpolator;

        @Override
        public void startTransition(LayoutTransition transition, ViewGroup container,
                View view, int transitionType) {
            if (view.getId() == R.id.back) {
                mBackTransitioning = true;
            } else if (view.getId() == R.id.home && transitionType == LayoutTransition.APPEARING) {
                mHomeAppearing = true;
                mStartDelay = transition.getStartDelay(transitionType);
                mDuration = transition.getDuration(transitionType);
                mInterpolator = transition.getInterpolator(transitionType);
            }
        }

        @Override
        public void endTransition(LayoutTransition transition, ViewGroup container,
                View view, int transitionType) {
            if (view.getId() == R.id.back) {
                mBackTransitioning = false;
            } else if (view.getId() == R.id.home && transitionType == LayoutTransition.APPEARING) {
                mHomeAppearing = false;
            }
        }

        public void onBackAltCleared() {
            // When dismissing ime during unlock, force the back button to run the same appearance
            // animation as home (if we catch this condition early enough).
            if (!mBackTransitioning && getBackButton().getVisibility() == VISIBLE
                    && mHomeAppearing && getHomeButton().getAlpha() == 0) {
                getBackButton().setAlpha(0);
                ValueAnimator a = ObjectAnimator.ofFloat(getBackButton(), "alpha", 0, 1);
                a.setStartDelay(mStartDelay);
                a.setDuration(mDuration);
                a.setInterpolator(mInterpolator);
                a.start();
            }
        }
    }

    public NavigationBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final Resources res = mContext.getResources();
        mShowMenu = false;
        getIcons(res);
        mBarTransitions = new NavigationBarTransitions(this);
    }

    public BarTransitions getBarTransitions() {
        return mBarTransitions;
    }

    public View getRecentsButton() {
        return mCurrentView.findViewById(R.id.recent_apps);
    }

    public View getMenuButton() {
        return mCurrentView.findViewById(R.id.menu);
    }

    public View getBackButton() {
        return mCurrentView.findViewById(R.id.back);
    }

    public View getHomeButton() {
        return mCurrentView.findViewById(R.id.home);
    }

    private void getIcons(Resources res) {
        mBackIcon = res.getDrawable(R.drawable.ic_sysbar_back);
        mBackLandIcon = res.getDrawable(R.drawable.ic_sysbar_back_land);
        mBackAltIcon = res.getDrawable(R.drawable.ic_sysbar_back_ime);
        mBackAltLandIcon = res.getDrawable(R.drawable.ic_sysbar_back_ime);
        mRecentIcon = res.getDrawable(R.drawable.ic_sysbar_recent);
        mRecentLandIcon = res.getDrawable(R.drawable.ic_sysbar_recent_land);
    }

    @Override
	public void updateResources() {
	    super.updateResources();
		getIcons(mContext.getResources());

		// tablets/phablets don't have this
		ImageView camera = ((ImageView) findViewById(R.id.camera_button));
		if(camera != null) {
			camera.setImageDrawable(mContext.getResources()
					.getDrawable(R.drawable.ic_sysbar_camera));
		}
	}

    @Override
    public void setLayoutDirection(int layoutDirection) {
        getIcons(mContext.getResources());
        super.setLayoutDirection(layoutDirection);
    }

    @Override
    public void setNavigationIconHints(int hints) {
        setNavigationIconHints(hints, false);
    }

    public void setNavigationIconHints(int hints, boolean force) {
        if (!force && hints == mNavigationIconHints) return;
        final boolean backAlt = (hints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0;
        if ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0 && !backAlt) {
            mTransitionListener.onBackAltCleared();
        }
        if (DEBUG) {
            android.widget.Toast.makeText(mContext,
                "Navigation icon hints = " + hints,
                500).show();
        }

        mNavigationIconHints = hints;

        ((ImageView)getBackButton()).setImageDrawable(backAlt
                ? (mVertical ? mBackAltLandIcon : mBackAltIcon)
                : (mVertical ? mBackLandIcon : mBackIcon));

        ((ImageView)getRecentsButton()).setImageDrawable(mVertical ? mRecentLandIcon : mRecentIcon);        

        setDisabledFlags(mDisabledFlags, true);
    }

    protected void setDisabledFlags(int disabledFlags, boolean force) {
        super.setDisabledFlags(disabledFlags, force);

        final boolean disableHome = ((disabledFlags & View.STATUS_BAR_DISABLE_HOME) != 0);
        final boolean disableRecent = ((disabledFlags & View.STATUS_BAR_DISABLE_RECENT) != 0);
        final boolean disableBack = ((disabledFlags & View.STATUS_BAR_DISABLE_BACK) != 0)
                && ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) == 0);

        ViewGroup navButtons = (ViewGroup) mCurrentView.findViewById(R.id.nav_buttons);
        if (navButtons != null) {
            LayoutTransition lt = navButtons.getLayoutTransition();
            if (lt != null) {
                if (!lt.getTransitionListeners().contains(mTransitionListener)) {
                    lt.addTransitionListener(mTransitionListener);
                }
                if (!mScreenOn && mCurrentView != null) {
                    lt.disableTransitionType(
                            LayoutTransition.CHANGE_APPEARING |
                            LayoutTransition.CHANGE_DISAPPEARING |
                            LayoutTransition.APPEARING |
                            LayoutTransition.DISAPPEARING);
                }
            }
        }

        getBackButton().setVisibility(disableBack ? View.INVISIBLE : View.VISIBLE);
		getHomeButton().setVisibility(disableHome ? View.INVISIBLE : View.VISIBLE);
		getRecentsButton().setVisibility(disableRecent ? View.INVISIBLE : View.VISIBLE);

        mBarTransitions.applyBackButtonQuiescentAlpha(mBarTransitions.getMode(), true /*animate*/);
    }

    @Override
    public void setMenuVisibility(final boolean show) {
        setMenuVisibility(show, false);
    }

    public void setMenuVisibility(final boolean show, final boolean force) {
        if (!force && mShowMenu == show) return;
        mShowMenu = show;
        getMenuButton().setVisibility(mShowMenu ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void reorient() {
        super.reorient();

        final Resources res = mContext.getResources();

        // force the low profile & disabled states into compliance
        mBarTransitions.init(mVertical);
        setDisabledFlags(mDisabledFlags, true /* force */);
        setMenuVisibility(mShowMenu, true /* force */);

		// update additional button resources for theme change
		((ImageView) getHomeButton()).setImageDrawable(mVertical ? res
				.getDrawable(R.drawable.ic_sysbar_home_land) : res
				.getDrawable(R.drawable.ic_sysbar_home));

        setNavigationIconHints(mNavigationIconHints, true);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mDelegateHelper.setInitialTouchRegion(getHomeButton(), getBackButton(), getRecentsButton());
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.println(String.format("      disabled=0x%08x vertical=%s menu=%s",
                        mDisabledFlags,
                        mVertical ? "true" : "false",
                        mShowMenu ? "true" : "false"));

        dumpButton(pw, "back", getBackButton());
        dumpButton(pw, "home", getHomeButton());
        dumpButton(pw, "rcnt", getRecentsButton());
        dumpButton(pw, "menu", getMenuButton());

        pw.println("    }");
    }

    @Override
    public void setNavigatorListeners(OnClickListener recentsClickListener,
            OnTouchListener recentsPreloadOnTouchListener,
            OnTouchListener homeSearchActionListener,
            View.OnTouchListener userAutoHideListener) {
        super.setNavigatorListeners(recentsClickListener, recentsPreloadOnTouchListener,
                homeSearchActionListener,
                userAutoHideListener);

        setOnTouchListener(mUserAutoHideListener);
        getRecentsButton().setOnClickListener(mRecentsClickListener);
        getRecentsButton().setOnTouchListener(mRecentsPreloadOnTouchListener);
        ((KeyButtonView) getHomeButton()).setHomeSearchActionListener(mHomeSearchActionListener);
    }
}
