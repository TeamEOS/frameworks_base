
package com.android.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import org.codefirex.utils.CFXConstants;
import org.codefirex.utils.CFXUtils;

import com.android.systemui.eos.EosObserver;
import com.android.systemui.eos.EosObserver.FeatureListener;
import com.android.systemui.statusbar.policy.KeyButtonView;

import com.android.systemui.R;

/**
 * Common behavior of all "bar" ui modes Mostly common indicator controls
 *
 * @author bigrushdog
 */

public abstract class BarUiController implements FeatureListener {
	public static final int DEVICE_NORMAL_SCREEN = 1;
	public static final int DEVICE_LARGE_SCREEN = 2;
	public static final int DEVICE_XLARGE_SCREEN = 3;

	private List<String> mHardkeyActions;
    private int MSG_CLOCK_VISIBLE_SETTINGS;
    private int MSG_CLOCK_COLOR_SETTINGS;

    private boolean mIsClockVisible = true;
    protected int mCurrentBarSizeMode;
    private int mScreenSize;
    protected boolean mHasHardkeys;

    private PackageReceiver mPackageReceiver;

    private View mCurrentClockView;
    protected ContentResolver mResolver;
    protected Context mContext;
    protected EosObserver mObserver;

    public BarUiController(Context context) {
        mContext = context;
        mResolver = mContext.getContentResolver();

        if (CFXUtils.isNormalScreen()) {
        	mScreenSize = DEVICE_NORMAL_SCREEN;
        } else if (CFXUtils.isLargeScreen()) {
        	mScreenSize = DEVICE_LARGE_SCREEN;
        } else if (CFXUtils.isXLargeScreen()) {
        	mScreenSize = DEVICE_XLARGE_SCREEN;
        } else {
        	mScreenSize = DEVICE_NORMAL_SCREEN;
        }

        mHasHardkeys = CFXUtils.isCapKeyDevice(context);
        if (mHasHardkeys) {
        	mHardkeyActions = new ArrayList<String>();
        	mHardkeyActions.add(CFXConstants.INPUT_HARDKEY_BACK_DOUBLETAP);
        	mHardkeyActions.add(CFXConstants.INPUT_HARDKEY_BACK_LONGPRESS);
        	mHardkeyActions.add(CFXConstants.INPUT_HARDKEY_HOME_DOUBLETAP);
        	mHardkeyActions.add(CFXConstants.INPUT_HARDKEY_HOME_LONGPRESS);
        	mHardkeyActions.add(CFXConstants.INPUT_HARDKEY_RECENT_SINGLETAP);
        	mHardkeyActions.add(CFXConstants.INPUT_HARDKEY_RECENT_DOUBLETAP);
        	mHardkeyActions.add(CFXConstants.INPUT_HARDKEY_RECENT_LONGPRESS);
        	mHardkeyActions.add(CFXConstants.INPUT_HARDKEY_MENU_SINGLETAP);
        	mHardkeyActions.add(CFXConstants.INPUT_HARDKEY_MENU_DOUBLETAP);
        	mHardkeyActions.add(CFXConstants.INPUT_HARDKEY_MENU_LONGPRESS);
        	mHardkeyActions.add(CFXConstants.INPUT_HARDKEY_ASSIST_SINGLETAP);
        	mHardkeyActions.add(CFXConstants.INPUT_HARDKEY_ASSIST_DOUBLETAP);
        	mHardkeyActions.add(CFXConstants.INPUT_HARDKEY_ASSIST_LONGPRESS);
        }
        mObserver = new EosObserver(mContext);
        mPackageReceiver = new PackageReceiver();
        mPackageReceiver.registerReceiver(context);
    }

    protected abstract TextView getClockCenterView();

    protected abstract TextView getClockClusterView();

    protected abstract View getSoftkeyHolder();

    protected abstract void registerBarView(View v);

    public int getScreenSize() {
    	return mScreenSize;
    }

    protected void notifyBarViewRegistered() {
        mObserver.setOnFeatureChangedListener((FeatureListener) getClockClusterView());
        mObserver.setOnFeatureChangedListener((FeatureListener) getClockCenterView());
        mObserver.setOnFeatureChangedListener((FeatureListener) BarUiController.this);
        mObserver.setEnabled(true);
        handleClockChange();
    }

    @Override
    public ArrayList<String> onRegisterClass() {
        ArrayList<String> uris = new ArrayList<String>();
        uris.add(CFXConstants.SYSTEMUI_CLOCK_VISIBLE);
        uris.add(CFXConstants.SYSTEMUI_CLOCK_COLOR);
        return uris;
    }

    @Override
    public void onSetMessage(String uri, int msg) {
        if (uri.equals(CFXConstants.SYSTEMUI_CLOCK_VISIBLE)) {
            MSG_CLOCK_VISIBLE_SETTINGS = msg;
        } else if (uri.equals(CFXConstants.SYSTEMUI_CLOCK_COLOR)) {
            MSG_CLOCK_COLOR_SETTINGS = msg;
        }
    }

    @Override
    public void onFeatureStateChanged(int msg) {
        if (msg == MSG_CLOCK_VISIBLE_SETTINGS
                || msg == MSG_CLOCK_COLOR_SETTINGS) {
            handleClockChange();
            return;
        }
    }

	protected void onTearDown() {
		mPackageReceiver.unregister(mContext);
		mObserver.setEnabled(false);
	}

    public void showClock(boolean show) {
        final View clock = mCurrentClockView;
        if (clock != null) {
            if (mIsClockVisible) {
                clock.setVisibility(show ? View.VISIBLE : View.GONE);
            } else {
                clock.setVisibility(View.GONE);
            }
        }
    }

    protected int getBarSizeMode() {
        return mCurrentBarSizeMode;
    }

    private void handleClockChange() {
        if (mCurrentClockView == null)
            mCurrentClockView = getClockClusterView();

        int clock_state = Settings.System.getInt(mResolver,
                CFXConstants.SYSTEMUI_CLOCK_VISIBLE,
                CFXConstants.SYSTEMUI_CLOCK_CLUSTER);

        switch (clock_state) {
            case CFXConstants.SYSTEMUI_CLOCK_GONE:
                mIsClockVisible = false;
                getClockCenterView().setVisibility(View.GONE);
                getClockClusterView().setVisibility(View.GONE);
                break;
            case CFXConstants.SYSTEMUI_CLOCK_CLUSTER:
                mIsClockVisible = true;
                getClockCenterView().setVisibility(View.GONE);
                getClockClusterView().setVisibility(View.VISIBLE);
                mCurrentClockView = getClockClusterView();
                break;
            case CFXConstants.SYSTEMUI_CLOCK_CENTER:
                mIsClockVisible = true;
                getClockClusterView().setVisibility(View.GONE);
                getClockCenterView().setVisibility(View.VISIBLE);
                mCurrentClockView = getClockCenterView();
                break;
        }

        int color = Settings.System.getInt(mContext.getContentResolver(),
                CFXConstants.SYSTEMUI_CLOCK_COLOR,
                CFXConstants.SYSTEMUI_CLOCK_COLOR_DEF);
        if (color == -1) {
            color = mContext.getResources()
                    .getColor(R.color.status_bar_clock_color);
        }
        getClockClusterView().setTextColor(color);
        getClockCenterView().setTextColor(color);
    }

    // protects action based features
    private class PackageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_PACKAGE_REMOVED)
                    || action.equals(Intent.ACTION_PACKAGE_CHANGED)) {
                handlePackageChanged();
            }
        }

        IntentFilter getFilters() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            return filter;
        }

        void registerReceiver(Context ctx) {
            ctx.registerReceiver(this, getFilters());
        }

        void unregister(Context ctx) {
            ctx.unregisterReceiver(this);
        }
    }

    protected void handlePackageChanged() {
    	// first the navigation bar if we have one
        View holder = getSoftkeyHolder();
        if (holder != null) {
            for (View v : getAllChildren(holder)) {
                if (v instanceof KeyButtonView) {
                    String uri = ((KeyButtonView) v).getLpUri();
                    if (uri != null && !TextUtils.isEmpty(uri)) {
                        String action = Settings.System.getString(mContext.getContentResolver(),
                                uri);
                        if (action != null) {
                            ((KeyButtonView) v).checkLpAction();
                        }
                    }
                }
            }
        }
        // now the hardkeys, if we have them
        if (mHasHardkeys) {
        	for (String uri : mHardkeyActions) {
                String action = Settings.System.getString(mContext.getContentResolver(),uri);
                if (action != null) {
                	if (action.startsWith("app:")) {
                        if (!CFXUtils.isComponentResolved(mContext.getPackageManager(), action)) {
                            Settings.System.putString(mContext.getContentResolver(), uri , "");
                        }
                	}
                }
        	}
        }
    }

    /* utility to iterate a viewgroup and return a list of child views */
    public ArrayList<View> getAllChildren(View v) {

        if (!(v instanceof ViewGroup)) {
            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            return viewArrayList;
        }

        ArrayList<View> result = new ArrayList<View>();

        ViewGroup vg = (ViewGroup) v;
        for (int i = 0; i < vg.getChildCount(); i++) {

            View child = vg.getChildAt(i);

            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            viewArrayList.addAll(getAllChildren(child));

            result.addAll(viewArrayList);
        }
        return result;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
