
package com.android.systemui.statusbar.phone;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.app.ActivityManager;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.eos.EosObserver.FeatureListener;
import com.android.systemui.eos.NxCallback;
import com.android.systemui.eos.NxController;
import com.android.systemui.statusbar.BarUiController;
import com.android.systemui.statusbar.policy.KeyButtonView;

public class PhoneUiController extends BarUiController {

    static final String TAG = PhoneUiController.class.getSimpleName();

    static final int STOCK_NAV_BAR = com.android.systemui.R.layout.navigation_bar;
    static final String NX_ENABLED_URI = "eos_nx_enabled";

    private View mStatusBarView;
    private PhoneStatusBar mService;
    private NavigationBarView mNavigationBarView;
    private StatusBarWindowView mStatusBarWindow;
    private NxController mNx;
    private Handler mHandler;
    private NxObserver mNxObserver;

    private int mCurrentNavLayout;

    public PhoneUiController(Context context, Handler handler) {
        super(context);
        mHandler = handler;
        mNxObserver = new NxObserver(handler);
        mNxObserver.observe();
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
        // if enabled, bring it up, else do nothing
        updateNx();

        // give it back to SystemUI
        return mNavigationBarView;
    }

    public void setBar(PhoneStatusBar service) {
        mService = service;
    }

    public void setBarWindow(StatusBarWindowView window) {
        mStatusBarWindow = window;
    }

    public boolean isNxEnabled() {
        if (mNavigationBarView == null)
            return false;
        return mNavigationBarView.isNxEnabled();
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

    void onScreenStateChanged(boolean screenOn) {
        if (mNx != null) {
            mNx.onScreenStateChanged(screenOn);
        }
    }

    private void updateNx() {
        boolean isNxEnabled = Settings.System.getBoolean(mContext.getContentResolver(),
                NX_ENABLED_URI, false);
        if (isNxEnabled) {
            startNX();
        } else {
            stopNX();
        }
    }

    private void startNX() {
        stopNX();
        mNx = new NxController(mContext, mHandler, mNavigationBarView, getScreenSize());
        mNavigationBarView.onStartNX((NxCallback) mNx);
    }

    protected void onTearDown() {
        stopNX();
        mResolver.unregisterContentObserver(mNxObserver);
        super.onTearDown();
    }

    void stopNX() {
        if (mNx != null && mNavigationBarView != null) {
            mNavigationBarView.onStopNX();
            mNx.tearDown();
            mNx = null;
        }
    }

    void updateResources() {
        if (mNx != null)
            mNx.updateResources();
    }

    class NxObserver extends ContentObserver {
        NxObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mResolver.registerContentObserver(
                    Settings.System.getUriFor(NX_ENABLED_URI), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateNx();
        }
    }
}
