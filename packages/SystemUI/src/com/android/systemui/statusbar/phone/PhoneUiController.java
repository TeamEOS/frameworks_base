
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
import com.android.systemui.eos.Navigator;
import com.android.systemui.statusbar.BarUiController;
import com.android.systemui.statusbar.policy.KeyButtonView;

public class PhoneUiController extends BarUiController {
    static final String TAG = PhoneUiController.class.getSimpleName();

    private static final int MSG_BAR_MODE_CHANGED = 14673;

    static final int NAVBAR_LAYOUT = com.android.systemui.R.layout.navigation_bar;
    static final int NX_LAYOUT = com.android.systemui.R.layout.nx_bar;
    static final String NX_ENABLED_URI = "eos_nx_enabled";

    private View mStatusBarView;
    private Navigator mNavigator;
    private NavModeObserver mNavModeObserver;
    private Handler mHandler;

    private int mCurrentNavLayout;

    public PhoneUiController(Context context, Handler h) {
        super(context);
        mHandler = h;
        mNavModeObserver = new NavModeObserver(new Handler());
        mNavModeObserver.observe();
    }

    public Navigator getNavigationBarView() {
        boolean isNxEnabled = Settings.System.getBoolean(mContext.getContentResolver(),
                NX_ENABLED_URI, false);
        mCurrentNavLayout = isNxEnabled ? NX_LAYOUT : NAVBAR_LAYOUT;
        mNavigator = (Navigator) View.inflate(mContext, mCurrentNavLayout, null);

        if (!isNxEnabled) {
            for (View v : getAllChildren(mNavigator.getViewForWindowManager())) {
                if (v instanceof KeyButtonView) {
                    mObserver.setOnFeatureChangedListener((FeatureListener) v);
                }
            }
        }

        return mNavigator;
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
        if (mNavigator != null) {
            return mNavigator.getViewForWindowManager();
        } else {
            return null;
        }
    }

    protected void onTearDown() {
        mResolver.unregisterContentObserver(mNavModeObserver);
        super.onTearDown();
    }

    class NavModeObserver extends ContentObserver {
        NavModeObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mResolver.registerContentObserver(
                    Settings.System.getUriFor(NX_ENABLED_URI), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            mHandler.sendEmptyMessage(MSG_BAR_MODE_CHANGED);
        }
    }
}
