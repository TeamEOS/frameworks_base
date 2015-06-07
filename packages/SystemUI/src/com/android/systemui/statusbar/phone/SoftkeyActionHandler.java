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
import java.util.Map.Entry;
import java.util.Set;

import com.android.internal.util.actions.ActionConstants;
import com.android.internal.util.actions.ActionHandler;
import com.android.internal.util.actions.ActionUtils;
import com.android.internal.util.actions.Config;
import com.android.internal.util.actions.Config.ButtonConfig;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.KeyButtonView;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.ViewConfiguration;

public class SoftkeyActionHandler {
    private static final int DT_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    private static final int LP_TIMEOUT = ViewConfiguration.getLongPressTimeout();

    private static Map<Integer, String> softkeyMap = new HashMap<Integer, String>();

    static {
        softkeyMap.put(Integer.valueOf(R.id.back), ActionConstants.Navbar.BUTTON1_TAG);
        softkeyMap.put(Integer.valueOf(R.id.home), ActionConstants.Navbar.BUTTON2_TAG);
        softkeyMap.put(Integer.valueOf(R.id.recent_apps), ActionConstants.Navbar.BUTTON3_TAG);
        softkeyMap.put(Integer.valueOf(R.id.menu), ActionConstants.Navbar.BUTTON4_TAG);
    }

    final int LP_TIMEOUT_MAX = LP_TIMEOUT;
    // no less than 25ms longer than single tap timeout
    final int LP_TIMEOUT_MIN = 25;

    private NavigationBarView mNavigationBarView;
    private Context mContext;
    private ContentResolver mResolver;
    private SoftkeyActionObserver mObserver;
    private boolean mRecreating;
    private boolean mKeyguardShowing;

    public SoftkeyActionHandler(NavigationBarView v) {
        mNavigationBarView = v;
        mContext = v.getContext();
        mResolver = v.getContext().getContentResolver();
        mObserver = new SoftkeyActionObserver(new Handler());
        mObserver.observe();
    }

    public void setKeyguardShowing(boolean showing) {
        if (mKeyguardShowing != showing) {
            mKeyguardShowing = showing;
        }
    }

    public boolean isSecureToFire(String action) {
        return action == null
                || !mKeyguardShowing
                || (mKeyguardShowing && ActionHandler.SYSTEMUI_TASK_BACK.equals(action));
    }

    public void setIsRecreating(boolean recreating) {
        mRecreating = recreating;
    }

    public void assignButtonInfo() {
        int lpTimeout = getLongPressTimeout();
        ArrayList<ButtonConfig> configs = Config.getConfig(mContext,
                ActionConstants.getDefaults(ActionConstants.NAVBAR));
        for (KeyButtonView v : ActionUtils.getAllChildren(mNavigationBarView, KeyButtonView.class)) {
            KeyButtonView button = ((KeyButtonView) v);
            button.setLongPressTimeout(lpTimeout);
            button.setDoubleTapTimeout(DT_TIMEOUT);
            String configTag = (String) softkeyMap.get(button.getId());
            if (configTag == null) {
                continue;
            }
            ButtonConfig config = Config.getButtonConfigFromTag(configs, configTag);
            if (config != null) {
                button.setActionHandler(this);
                button.setButtonInfo(config);
            }
        }
    }

    private int getLongPressTimeout() {
        int lpTimeout = Settings.System
                .getIntForUser(mResolver, Settings.System.SOFTKEY_LONGPRESS_TIMEOUT, LP_TIMEOUT,
                        UserHandle.USER_CURRENT);
        if (lpTimeout > LP_TIMEOUT_MAX) {
            lpTimeout = LP_TIMEOUT_MAX;
        } else if (lpTimeout < LP_TIMEOUT_MIN) {
            lpTimeout = LP_TIMEOUT_MIN;
        }
        return lpTimeout;
    }

    public void onDispose() {
        if (mObserver != null) {
            mObserver.unobserve();
        }
    }

    private class SoftkeyActionObserver extends ContentObserver {
        SoftkeyActionObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mNavigationBarView == null) {
                return;
            }
            assignButtonInfo();
        }

        void observe() {
            mResolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SOFTKEY_LONGPRESS_TIMEOUT), false,
                    SoftkeyActionObserver.this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(
                    Settings.System.getUriFor(ActionConstants.getDefaults(ActionConstants.NAVBAR)
                            .getUri()), false,
                    SoftkeyActionObserver.this, UserHandle.USER_ALL);
        }

        void unobserve() {
            mResolver.unregisterContentObserver(SoftkeyActionObserver.this);
        }
    }
}
