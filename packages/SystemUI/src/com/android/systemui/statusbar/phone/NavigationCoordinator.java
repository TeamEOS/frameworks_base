/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2014 The TeamEos Project
 * 
 * Author: Randall Rushing aka Bigrushdog (randall.rushing@gmail.com)
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
 * PhoneUiController is a state manager class to handle custom navigation
 * implementation. Functions include adding/removing the navigation view,
 * changing the navigation mode, register/unregister feature observers,
 * and monitor package changed broadcasts and update action based features
 * We also load the GDX shared library here as it seems a prudent spot
 * 
 */

package com.android.systemui.statusbar.phone;

import java.util.ArrayList;
import java.util.List;

import org.teameos.utils.EosConstants;
import org.teameos.utils.EosUtils;

import com.android.systemui.statusbar.BaseNavigationBar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

public class NavigationCoordinator {
    private static final String TAG = NavigationCoordinator.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int NAVBAR_LAYOUT = com.android.systemui.R.layout.navigation_bar;
    private static final int NX_LAYOUT = com.android.systemui.R.layout.nx_bar;
    private static final String NX_ENABLED_URI = "eos_nx_enabled";
    private boolean mRecreating = false;
    private PhoneStatusBar mBar;
    private NavbarObserver mNavbarObserver;
    private Runnable mAddNavbar;
    private Runnable mRemoveNavbar;
    private int mLastBarMode = -1;

    // monitor package changes and clear actions on features
    // that launched the package, if one was assigned
    // we monitor softkeys, hardkeys, and NX here
    private PackageReceiver mPackageReceiver;

    private boolean mHasHardkeys;
    private List<String> mHardkeyActions;
    private List<String> mNxActions = new ArrayList<String>();

    private Context mContext;
    private Handler mHandler = new Handler();

    public NavigationCoordinator(Context context, PhoneStatusBar statusBar,
            Runnable forceAddNavbar, Runnable removeNavbar) {
        mContext = context;
        mBar = statusBar;
        mAddNavbar = forceAddNavbar;
        mRemoveNavbar = removeNavbar;
        mHasHardkeys = EosUtils.isCapKeyDevice(context);
        mNavbarObserver = new NavbarObserver(mHandler);

        // iterate list check for packages and resolve
        if (mHasHardkeys) {
            mHardkeyActions = new ArrayList<String>();
            mHardkeyActions.add(EosConstants.INPUT_HARDKEY_BACK_DOUBLETAP);
            mHardkeyActions.add(EosConstants.INPUT_HARDKEY_BACK_LONGPRESS);
            mHardkeyActions.add(EosConstants.INPUT_HARDKEY_HOME_DOUBLETAP);
            mHardkeyActions.add(EosConstants.INPUT_HARDKEY_HOME_LONGPRESS);
            mHardkeyActions.add(EosConstants.INPUT_HARDKEY_RECENT_SINGLETAP);
            mHardkeyActions.add(EosConstants.INPUT_HARDKEY_RECENT_DOUBLETAP);
            mHardkeyActions.add(EosConstants.INPUT_HARDKEY_RECENT_LONGPRESS);
            mHardkeyActions.add(EosConstants.INPUT_HARDKEY_MENU_SINGLETAP);
            mHardkeyActions.add(EosConstants.INPUT_HARDKEY_MENU_DOUBLETAP);
            mHardkeyActions.add(EosConstants.INPUT_HARDKEY_MENU_LONGPRESS);
            mHardkeyActions.add(EosConstants.INPUT_HARDKEY_ASSIST_SINGLETAP);
            mHardkeyActions.add(EosConstants.INPUT_HARDKEY_ASSIST_DOUBLETAP);
            mHardkeyActions.add(EosConstants.INPUT_HARDKEY_ASSIST_LONGPRESS);
            mNavbarObserver.observeForceBar();
        }

        // add nx actions for package resolve
        mNxActions.add("eos_nx_action_single_tap");
        mNxActions.add("eos_nx_action_single_left_tap");
        mNxActions.add("eos_nx_action_double_tap");
        mNxActions.add("eos_nx_action_double_left_tap");
        mNxActions.add("eos_nx_action_long_press");
        mNxActions.add("eos_nx_action_long_left_press");
        mNxActions.add("eos_nx_action_fling_short_left");
        mNxActions.add("eos_nx_action_fling_short_right");
        mNxActions.add("eos_nx_action_fling_long_left");
        mNxActions.add("eos_nx_action_fling_long_right");

        mPackageReceiver = new PackageReceiver();
        mPackageReceiver.registerBootReceiver(context);
    }

    public BaseNavigationBar getNavigationBarView() {
        boolean isNxEnabled = Settings.System.getInt(mContext.getContentResolver(),
                NX_ENABLED_URI, 0) == 1;
        BaseNavigationBar navBar = (BaseNavigationBar) View.inflate(mContext,
                isNxEnabled ? NX_LAYOUT : NAVBAR_LAYOUT, null);

        if (mLastBarMode != -1) {
            navBar.getBarTransitions().setMode(mLastBarMode);
            mLastBarMode = -1;
        }

        mNavbarObserver.observeBarMode();
        if (mHasHardkeys)
            mNavbarObserver.observeForceBar();

        return navBar;
    }

    // hook theme change from PhoneStatusBar
    public void setRecreating(boolean recreating) {
        mRecreating = recreating;
    }

    // for now, it makes sense to let PhoneStatusBar add/remove navbar view
    // from window manager. Define the add/remove runnables in PSB then pass
    // to us for handling
    class NavbarObserver extends ContentObserver {
        NavbarObserver(Handler handler) {
            super(handler);
        }

        void observeBarMode() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(NX_ENABLED_URI), false, this);
        }

        void observeForceBar() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.DEV_FORCE_SHOW_NAVBAR), false, this);
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mRecreating)
                return;
            if (uri.equals(Settings.System.getUriFor(NX_ENABLED_URI))) {
                mNavbarObserver.unobserve();
                if (mBar.getNavigationBarView() != null) {
                    mLastBarMode = mBar.getNavigationBarView().getBarTransitions().getMode();
                    mBar.getNavigationBarView().onStop();
                }
                mHandler.post(mRemoveNavbar);
                mHandler.postDelayed(mAddNavbar, 500);
                return;
            } else if (uri.equals(Settings.System.getUriFor(Settings.System.DEV_FORCE_SHOW_NAVBAR))) {
                mNavbarObserver.unobserve();
                boolean visible = Settings.System.getIntForUser(mContext.getContentResolver(),
                        Settings.System.DEV_FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) == 1;
                if (visible) {
                    mHandler.post(mAddNavbar);
                } else {
                    if (mBar.getNavigationBarView() != null) {
                        mBar.getNavigationBarView().onStop();
                    }
                    mHandler.post(mRemoveNavbar);
                }
                mNavbarObserver.observeForceBar();
                return;
            }
            return;
        }
    }

    /*
     * Initially register for boot completed, as PackageManager is likely not
     * online yet. Once boot is completed, reregister for package changes and
     * handle as needed
     */
    private class PackageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                registerPackageReceiver(mContext);
            } else if (action.equals(Intent.ACTION_PACKAGE_REMOVED)
                    || action.equals(Intent.ACTION_PACKAGE_CHANGED)) {
                handlePackageChanged();
            }
        }

        void registerPackageReceiver(Context ctx) {
            Log.i(TAG, "Boot completed received, registering package receiver");
            ctx.unregisterReceiver(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            ctx.registerReceiver(this, filter);
        }

        void registerBootReceiver(Context ctx) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BOOT_COMPLETED);
            ctx.registerReceiver(this, filter);
        }
    }

    private void handlePackageChanged() {
        // first the navigation bar if we have one
        if (mBar.getNavigationBarView() != null) {
            mBar.getNavigationBarView().onHandlePackageChanged();
        }
        // now the hardkeys, if we have them
        if (mHasHardkeys) {
            for (String uri : mHardkeyActions) {
                resetActionUri(uri);
            }
        }
        for (String uri : mNxActions) {
            resetActionUri(uri);
        }
    }

    private void resetActionUri(String uri) {
        String action = Settings.System.getString(mContext.getContentResolver(), uri);
        if (action != null) {
            if (action.startsWith("app:")) {
                if (!EosUtils.isComponentResolved(mContext.getPackageManager(), action)) {
                    Settings.System.putString(mContext.getContentResolver(), uri, "");
                }
            }
        }
    }
}
