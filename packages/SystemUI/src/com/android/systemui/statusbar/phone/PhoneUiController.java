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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codefirex.utils.CFXConstants;
import org.codefirex.utils.CFXUtils;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.eos.Navigator;
import com.android.systemui.statusbar.policy.KeyButtonView;

public class PhoneUiController {
    private static final String TAG = PhoneUiController.class.getSimpleName();
    private static final boolean DEBUG = false;

    // load the gdx library
    static {
        try {
            System.loadLibrary("gdx");
            Log.i(TAG, "Loaded libgdx.so");
        } catch (Exception e) {
            Log.i(TAG, "Failed to load libgdx.so");
        }
    }

    // navigation management
    private static final int NAVBAR_LAYOUT = com.android.systemui.R.layout.navigation_bar;
    private static final int NX_LAYOUT = com.android.systemui.R.layout.nx_bar;
    private static final String NX_ENABLED_URI = "eos_nx_enabled";
    private int mCurrentNavLayout;
    private boolean mRecreating = false;
    private Navigator mNavigator;
    private NavbarObserver mNavbarObserver;
    private Runnable mAddNavbar;
    private Runnable mRemoveNavbar;

    // monitor package changes and clear actions on features
    // that launched the package, if one was assigned
    // we monitor softkeys, hardkeys, and NX here
    private PackageReceiver mPackageReceiver;

    // temporary: map softkey ID to it's long press action uri
    // softkey uris are passed to instances as xml attributes
    // so we have to to inflate first to get the uri
    private Map<Integer, Uri> mSoftkeyMap = new HashMap<Integer, Uri>();
    private boolean mHasHardkeys;
    private List<String> mHardkeyActions;
    private List<String> mNxActions = new ArrayList<String>();

    private ContentResolver mResolver;
    private Context mContext;
    private Handler mHandler = new Handler();

    public PhoneUiController(Context context, Runnable forceAddNavbar, Runnable removeNavbar) {
        mContext = context;
        mResolver = context.getContentResolver();
        mAddNavbar = forceAddNavbar;
        mRemoveNavbar = removeNavbar;
        mHasHardkeys = CFXUtils.isCapKeyDevice(context);
        mNavbarObserver = new NavbarObserver(mHandler);

        // iterate list check for packages and resolve
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

    public Navigator getNavigationBarView() {
        boolean isNxEnabled = Settings.System.getBoolean(mResolver,
                NX_ENABLED_URI, false);
        mCurrentNavLayout = isNxEnabled ? NX_LAYOUT : NAVBAR_LAYOUT;
        mNavigator = (Navigator) View.inflate(mContext, mCurrentNavLayout, null);

        if (!isNxEnabled) {
            // register softkey observers
            mSoftkeyMap.clear();
            for (View v : getAllChildren(mNavigator.getViewForWindowManager())) {
                if (v instanceof KeyButtonView) {
                    String uriString = ((KeyButtonView) v).getLpUri();
                    if (uriString != null) {
                        mSoftkeyMap.put(Integer.valueOf(v.getId()),
                                Settings.System.getUriFor(uriString));
                    }
                }
            }
            mNavbarObserver.observeSoftkeys();
        }

        mNavbarObserver.observeBarMode();
        if (mHasHardkeys)
            mNavbarObserver.observeForceBar();

        return mNavigator;
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

        void observeSoftkeys() {
            for (Uri uri : mSoftkeyMap.values()) {
                mResolver.registerContentObserver(uri, false, this);
            }
        }

        void observeBarMode() {
            mResolver.registerContentObserver(
                    Settings.System.getUriFor(NX_ENABLED_URI), false, this);
        }

        void observeForceBar() {
            mResolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.DEV_FORCE_SHOW_NAVBAR), false, this);
        }

        void unobserve() {
            mResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mRecreating)
                return;
            if (uri.equals(Settings.System.getUriFor(NX_ENABLED_URI))) {
                mNavbarObserver.unobserve();
                mHandler.post(mRemoveNavbar);
                mHandler.postDelayed(mAddNavbar, 500);
                return;
            } else if (uri.equals(Settings.System.getUriFor(Settings.System.DEV_FORCE_SHOW_NAVBAR))) {
                mNavbarObserver.unobserve();
                boolean visible = Settings.System.getIntForUser(mResolver,
                        Settings.System.DEV_FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) == 1;
                if (visible) {
                    mHandler.post(mAddNavbar);
                } else {
                    mHandler.post(mRemoveNavbar);
                }
                mNavbarObserver.observeForceBar();
                return;
            } else if (uri.toString().contains("systemui_softkey_lp")) {
                if (mNavigator != null) {
                    for (Map.Entry<Integer, Uri> entry : mSoftkeyMap.entrySet()) {
                        Uri val = entry.getValue();
                        if (uri.equals(val)) {
                            View holder = mNavigator.getViewForWindowManager();
                            for (View v : getAllChildren(holder)) {
                                if (v instanceof KeyButtonView) {
                                    if (Integer.valueOf(v.getId()).equals(entry.getKey())) {
                                        ((KeyButtonView) v).updateLpAction();
                                    }
                                }
                            }
                        }
                    }
                }
                return;
            }
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
        View holder = null;
        if (mNavigator != null) {
            holder = mNavigator.getViewForWindowManager();
        }
        if (holder != null) {
            for (View v : getAllChildren(holder)) {
                if (v instanceof KeyButtonView) {
                    String uri = ((KeyButtonView) v).getLpUri();
                    if (uri != null && !TextUtils.isEmpty(uri)) {
                        String action = Settings.System.getString(mResolver,
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
                resetActionUri(uri);
            }
        }
        for (String uri : mNxActions) {
            resetActionUri(uri);
        }
    }

    private void resetActionUri(String uri) {
        String action = Settings.System.getString(mResolver, uri);
        if (action != null) {
            if (action.startsWith("app:")) {
                if (!CFXUtils.isComponentResolved(mContext.getPackageManager(), action)) {
                    Settings.System.putString(mResolver, uri, "");
                }
            }
        }
    }

    /* utility to iterate a viewgroup and return a list of child views */
    public static ArrayList<View> getAllChildren(View v) {

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
}
