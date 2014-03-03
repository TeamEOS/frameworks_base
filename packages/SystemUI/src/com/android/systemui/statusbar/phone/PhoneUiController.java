
package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.app.ActivityManager;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.cfx.CfxObserver.FeatureListener;
import com.android.systemui.statusbar.BarUiController;
import com.android.systemui.statusbar.policy.KeyButtonView;

public class PhoneUiController extends BarUiController {

    static final String TAG = "CFXUiController";

    static final int STOCK_NAV_BAR = com.android.systemui.R.layout.navigation_bar;
//    static final int CFX_NAV_BAR = com.android.systemui.R.layout.CFX_navigation_bar;

    private View mStatusBarView;
    private PhoneStatusBar mService;
    private NavigationBarView mNavigationBarView;
    private StatusBarWindowView mStatusBarWindow;

    private int mCurrentNavLayout;

    public PhoneUiController(Context context) {
        super(context);
    }

    public WindowManager.LayoutParams getNavigationBarLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_NAVIGATION_BAR,
                0
                        | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);
        // this will allow the navbar to run in an overlay on devices that
        // support this
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        }

        lp.setTitle("NavigationBar");
        lp.windowAnimations = 0;
        return lp;
    }

    public NavigationBarView getNavigationBarView() {
    	mCurrentNavLayout = STOCK_NAV_BAR;
        mNavigationBarView = (NavigationBarView) View.inflate(mContext, mCurrentNavLayout, null);

        // register softkeys for features
        for (View v : getAllChildren(mNavigationBarView)) {
            if (v instanceof KeyButtonView) {
                mObserver.setOnFeatureChangedListener((FeatureListener) v);
            }
        }

        // give it back to SystemUI
        return mNavigationBarView;
    }

    public void setBar(PhoneStatusBar service) {
        mService = service;
    }

    public void setBarWindow(StatusBarWindowView window) {
        mStatusBarWindow = window;
    }

    @Override
    protected TextView getClockCenterView() {
        return (TextView) mStatusBarView.findViewById(R.id.clock_center);
    }

    @Override
    protected TextView getClockClusterView() {
        return (TextView) mStatusBarView.findViewById(R.id.system_icon_area).findViewById(
                R.id.clock);
    }

    @Override
    protected void registerBarView(View v) {
        mStatusBarView = v;
        notifyBarViewRegistered();
    }

	@Override
	protected View getSoftkeyHolder() {
		return mNavigationBarView;
	}
}
