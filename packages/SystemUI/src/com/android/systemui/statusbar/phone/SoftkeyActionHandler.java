/*
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
 * Manage KeyButtonView action states and action dispatch
 * 
 */

package com.android.systemui.statusbar.phone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.codefirex.utils.ActionHandler;
import org.codefirex.utils.CFXUtils;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.KeyButtonView;
import com.android.systemui.statusbar.policy.KeyButtonView.ButtonInfo;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

public class SoftkeyActionHandler extends ActionHandler {
    private static final String URI_SOFTKEY_LONGPRESS_TIMEOUT = "eos_softkey_longpress_timeout";
    private static final int DT_TIMEOUT = ViewConfiguration.getDoubleTapTimeout() - 100;
    private static final int LP_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    final int LP_TIMEOUT_MAX = LP_TIMEOUT;
    // no less than 25ms longer than single tap timeout
    final int LP_TIMEOUT_MIN = 25;

    private static final int LP_INDEX = 0;
    private static final int DT_INDEX = 1;

    public static final Map<Integer, String> singleTapMap;
    static {
        singleTapMap = new HashMap<Integer, String>();
        singleTapMap.put(Integer.valueOf(R.id.back), ActionHandler.SYSTEMUI_TASK_BACK);
        singleTapMap.put(Integer.valueOf(R.id.home), ActionHandler.SYSTEMUI_TASK_HOME);
        singleTapMap.put(Integer.valueOf(R.id.recent_apps), ActionHandler.SYSTEMUI_TASK_RECENTS);
        singleTapMap.put(Integer.valueOf(R.id.menu), ActionHandler.SYSTEMUI_TASK_MENU);
    }

    // temporary: map softkey ID to it's ButtonInfo for url monitoring
    // and action updating
    private Map<Integer, ButtonInfo> mSoftkeyMap = new HashMap<Integer, ButtonInfo>();
    private NavigationBarView mNavigationBarView;
    private ContentResolver mResolver;
    private Handler mHandler;
    private SoftkeyActionObserver mObserver;
    private boolean mRecreating;

    public SoftkeyActionHandler(Context context, Handler h) {
        super(context);
        mHandler = h;
        mResolver = context.getContentResolver();
        mObserver = new SoftkeyActionObserver(mHandler);
    }

    public void setNavigationBarView(NavigationBarView v) {
        mNavigationBarView = v;
        loadButtonActions();
        mObserver.observe();
    }

    public void setIsRecreating(boolean recreating) {
        mRecreating = recreating;
    }

    private void loadButtonActions() {
        if (mNavigationBarView == null)
            return;
        mSoftkeyMap.clear();
        int lpTimeout = getLongPressTimeout();
        for (View v : getAllChildren(mNavigationBarView)) {
            if (v instanceof KeyButtonView) {
                KeyButtonView button = ((KeyButtonView) v);
                button.setActionHandler(this);
                ButtonInfo info = getSoftkeyButtonInfo(button);
                mSoftkeyMap.put(button.getId(), info);
                button.setButtonActions(info);
                button.setLongPressTimeout(lpTimeout);
                button.setDoubleTapTimeout(DT_TIMEOUT);
            }
        }
    }

    private int getLongPressTimeout() {
        int lpTimeout = Settings.System
                .getInt(mResolver, URI_SOFTKEY_LONGPRESS_TIMEOUT, LP_TIMEOUT);
        if (lpTimeout > LP_TIMEOUT_MAX) {
            lpTimeout = LP_TIMEOUT_MAX;
        } else if (lpTimeout < LP_TIMEOUT_MIN) {
            lpTimeout = LP_TIMEOUT_MIN;
        }
        return lpTimeout;
    }

    private void setLongPressTimeout() {
        int lpTimeout = getLongPressTimeout();
        for (View v : getAllChildren(mNavigationBarView)) {
            if (v instanceof KeyButtonView) {
                KeyButtonView button = ((KeyButtonView) v);
                button.setLongPressTimeout(lpTimeout);
            }
        }
    }

    private String getActionFromUri(String uri) {
        String action = Settings.System.getString(mResolver, uri);
        if (action == null)
            action = ActionHandler.SYSTEMUI_TASK_NO_ACTION;
        return action;
    }

    private ButtonInfo getSoftkeyButtonInfo(KeyButtonView view) {
        String lpUri = view.getActionUris()[LP_INDEX];
        String dtUri = view.getActionUris()[DT_INDEX];
        String singleAction = singleTapMap.get(view.getId());
        if (singleAction == null)
            singleAction = ActionHandler.SYSTEMUI_TASK_NO_ACTION;
        String lpAction = getActionFromUri(lpUri);
        String dtAction = getActionFromUri(dtUri);
        ButtonInfo info = new ButtonInfo(singleAction, dtAction, lpAction, lpUri, dtUri);
        return info;
    }

    @Override
    public boolean handleAction(String action) {
        // TODO Auto-generated method stub
        return false;
    }

    public void onHandlePackageChanged() {
        if (mNavigationBarView != null) {
            for (View v : getAllChildren(mNavigationBarView)) {
                if (v instanceof KeyButtonView) {
                    KeyButtonView button = ((KeyButtonView) v);
                    ButtonInfo info = mSoftkeyMap.get(button.getId());
                    if (info == null) continue;
                    resolveOrClearActions(button);
                }
            }
        }
    }

    public void onTearDown() {
        if (mObserver != null) {
            mObserver.unobserve();
        }
        mSoftkeyMap.clear();
    }

    private void resolveOrClearActions(KeyButtonView button) {
        ButtonInfo info = mSoftkeyMap.get(button.getId());
        boolean didReset = false;
        if (resetActionUri(info.lpUri)) {
            if (mNavigationBarView != null && button.getId() == R.id.home) {
                button.setOnTouchListener(mNavigationBarView.getHomeSearchActionListener());
            }
            didReset = true;
        }
        if (resetActionUri(info.dtUri)) {
            didReset = true;
        }
        if (didReset) {
            ButtonInfo newInfo = getSoftkeyButtonInfo(button);
            mSoftkeyMap.remove(button.getId());
            mSoftkeyMap.put(button.getId(), newInfo);
        }
    }

    private boolean resetActionUri(String uri) {
        if (TextUtils.isEmpty(uri))
            return false;
        String action = Settings.System.getString(mResolver, uri);
        if (action != null) {
            if (action.startsWith("app:")) {
                if (!CFXUtils.isComponentResolved(mContext.getPackageManager(), action)) {
                    Settings.System
                            .putString(mResolver, uri, ActionHandler.SYSTEMUI_TASK_NO_ACTION);
                    return true;
                }
            }
        }
        return false;
    }

    private class SoftkeyActionObserver extends ContentObserver {
        SoftkeyActionObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mNavigationBarView == null || mRecreating)
                return;

            Uri longPressUri = Settings.System.getUriFor(URI_SOFTKEY_LONGPRESS_TIMEOUT);
            if (longPressUri != null && uri.equals(longPressUri)) {
                setLongPressTimeout();
                return;
            }

            for (Map.Entry<Integer, ButtonInfo> entry : mSoftkeyMap.entrySet()) {
                ButtonInfo info = entry.getValue();
                Uri lpUri = Settings.System.getUriFor(info.lpUri);
                Uri dtUri = Settings.System.getUriFor(info.dtUri);

                if (lpUri != null && lpUri.equals(uri)) {
                    for (View v : getAllChildren(mNavigationBarView)) {
                        if (v instanceof KeyButtonView) {
                            if (Integer.valueOf(v.getId()).equals(entry.getKey())) {
                                mSoftkeyMap.remove(entry.getKey());
                                ButtonInfo newInfo = getSoftkeyButtonInfo(((KeyButtonView) v));
                                mSoftkeyMap.put(v.getId(), newInfo);
                                ((KeyButtonView) v).setButtonActions(newInfo);
                                return;
                            }
                        }
                    }
                }

                if (dtUri != null && dtUri.equals(uri)) {
                    for (View v : getAllChildren(mNavigationBarView)) {
                        if (v instanceof KeyButtonView) {
                            if (Integer.valueOf(v.getId()).equals(entry.getKey())) {
                                mSoftkeyMap.remove(entry.getKey());
                                ButtonInfo newInfo = getSoftkeyButtonInfo(((KeyButtonView) v));
                                mSoftkeyMap.put(v.getId(), newInfo);
                                ((KeyButtonView) v).setButtonActions(newInfo);
                                return;
                            }
                        }
                    }
                }
            }
        }

        void observe() {
            for (ButtonInfo info : mSoftkeyMap.values()) {
                if (!TextUtils.isEmpty(info.lpUri)) {
                    mResolver.registerContentObserver(Settings.System.getUriFor(info.lpUri), false,
                            SoftkeyActionObserver.this);
                }
                if (!TextUtils.isEmpty(info.dtUri)) {
                    mResolver.registerContentObserver(Settings.System.getUriFor(info.dtUri), false,
                            SoftkeyActionObserver.this);
                }
            }
            mResolver.registerContentObserver(Settings.System.getUriFor(URI_SOFTKEY_LONGPRESS_TIMEOUT), false,
                    SoftkeyActionObserver.this);
        }

        void unobserve() {
            mResolver.unregisterContentObserver(SoftkeyActionObserver.this);
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
