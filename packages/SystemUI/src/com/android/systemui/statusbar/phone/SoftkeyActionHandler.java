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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.android.internal.utils.eos.ActionConstants;
import com.android.internal.utils.eos.ActionHandler;
import com.android.internal.utils.eos.ActionUtils;
import com.android.internal.utils.eos.Config;
import com.android.internal.utils.eos.Config.ButtonConfig;
import com.android.internal.navigation.utils.SmartObserver.SmartObservable;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.KeyButtonView;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.ViewConfiguration;

public class SoftkeyActionHandler implements SmartObservable {
    private static final int DT_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    private static final int LP_TIMEOUT = ViewConfiguration.getLongPressTimeout();

    private static Map<Integer, String> softkeyMap = new HashMap<Integer, String>();

    static {
        softkeyMap.put(Integer.valueOf(R.id.back), ActionConstants.Navbar.BUTTON1_TAG);
        softkeyMap.put(Integer.valueOf(R.id.home), ActionConstants.Navbar.BUTTON2_TAG);
        softkeyMap.put(Integer.valueOf(R.id.recent_apps), ActionConstants.Navbar.BUTTON3_TAG);
        softkeyMap.put(Integer.valueOf(R.id.menu), ActionConstants.Navbar.BUTTON4_TAG);
    }

    private static Set<Uri> sUris = new HashSet<Uri>();
    static {
        sUris.add(Settings.System.getUriFor(Settings.System.SOFTKEY_LONGPRESS_TIMEOUT));
        sUris.add(Settings.System.getUriFor(ActionConstants.getDefaults(ActionConstants.NAVBAR)
                .getUri()));
    }

    final int LP_TIMEOUT_MAX = LP_TIMEOUT;
    // no less than 25ms longer than single tap timeout
    final int LP_TIMEOUT_MIN = 25;

    private NavigationBarView mNavigationBarView;
    private Context mContext;
    private ContentResolver mResolver;
    private boolean mRecreating;
    private boolean mKeyguardShowing;

    public SoftkeyActionHandler(NavigationBarView v) {
        mNavigationBarView = v;
        mContext = v.getContext();
        mResolver = v.getContext().getContentResolver();
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

    @Override
    public Set<Uri> onGetUris() {
        return sUris;
    }

    @Override
    public void onChange(Uri uri) {
        if (mNavigationBarView != null) {
            assignButtonInfo();
        }
    }
}
