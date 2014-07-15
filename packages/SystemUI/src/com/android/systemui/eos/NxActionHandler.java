/*
 * Copyright (C) 2014 The TeamEos Project
 * Author: Randall Rushing aka Bigrushdog
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
 * Handles binding actions to events, and a simple public api for firing
 * events. Also handles observing user changes to actions and a callback
 * that's called action pre-execution. Let's motion handler know if double
 * tap is enabled in case of different touch handling
 *
 */

package com.android.systemui.eos;

import java.util.HashMap;
import java.util.Map;

import org.codefirex.utils.ActionHandler;

import com.android.systemui.eos.NxAction.ActionReceiver;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.ViewConfiguration;

public class NxActionHandler extends ActionHandler implements ActionReceiver {
    final static String TAG = NxActionHandler.class.getSimpleName();
    private static final int DT_TIMEOUT = ViewConfiguration.getDoubleTapTimeout() - 100;

    private Map<Integer, NxAction> mActionMap = new HashMap<Integer, NxAction>();
    private ActionObserver mObserver;
    private ActionReceiver mReceiver; // let anyone else we're getting ready to fire
    private Handler H = new Handler();
    private boolean isDoubleTapEnabled;

    public NxActionHandler(Context context) {
        super(context);
        mActionMap = new HashMap<Integer, NxAction>();
        loadActionMap();
        mObserver = new ActionObserver(H);
        mObserver.register();
    }

    private void loadActionMap() {
        mActionMap.clear();

        String action = "eos_nx_action_single_tap";
        mActionMap.put(NxAction.EVENT_SINGLE_TAP, new NxAction(action, this,
                H, getAction(action), DT_TIMEOUT));

        action = "eos_nx_action_double_tap";
        mActionMap.put(NxAction.EVENT_DOUBLE_TAP, new NxAction(action, this,
                H, getAction(action), 0));

        action = "eos_nx_action_long_press";
        mActionMap.put(NxAction.EVENT_LONG_PRESS, new NxAction(action, this,
                H, getAction(action), 0));

        action = "eos_nx_action_fling_short_left";
        mActionMap.put(NxAction.EVENT_FLING_SHORT_LEFT, new NxAction(action,
                this, H, getAction(action), 0));

        action = "eos_nx_action_fling_short_right";
        mActionMap.put(NxAction.EVENT_FLING_SHORT_RIGHT, new NxAction(action,
                this, H, getAction(action), 0));

        action = "eos_nx_action_fling_long_left";
        mActionMap.put(NxAction.EVENT_FLING_LONG_LEFT, new NxAction(action,
                this, H, getAction(action), 0));

        action = "eos_nx_action_fling_long_right";
        mActionMap.put(NxAction.EVENT_FLING_LONG_RIGHT, new NxAction(action,
                this, H, getAction(action), 0));

        isDoubleTapEnabled = ((NxAction) mActionMap.get(NxAction.EVENT_DOUBLE_TAP))
                .isEnabled();
    }

    private String getAction(String uri) {
        String action = Settings.System.getString(
                mContext.getContentResolver(), uri);
        if (TextUtils.isEmpty(action) || action.equals("empty")) {
            action = "";
        }
        return action;
    }

    public void setActionReceiver(ActionReceiver receiver) {
        mReceiver = receiver;
    }

    public boolean isDoubleTapEnabled() {
        return isDoubleTapEnabled;
    }

    public void fireAction(int type) {
        ((NxAction) mActionMap.get(type)).fireAction();
    }

    public void cancelAction(int type) {
        ((NxAction) mActionMap.get(type)).cancelAction();
    }

    public void queueAction(int type) {
        ((NxAction) mActionMap.get(type)).queueAction();
    }

    public void unregister() {
        mObserver.unregister();
    }

    private class ActionObserver extends ContentObserver {

        public ActionObserver(Handler handler) {
            super(handler);
        }

        void register() {
            for (int i = 1; i < mActionMap.size() + 1; i++) {
                NxAction action = mActionMap.get(i);
                mContext.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(action.getUri()), false,
                        ActionObserver.this);
            }
        }

        void unregister() {
            mContext.getContentResolver().unregisterContentObserver(
                    ActionObserver.this);
        }

        public void onChange(boolean selfChange, Uri uri) {
            loadActionMap();
        }
    }

    @Override
    public boolean handleAction(String action) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onActionDispatched(NxAction actionEvent, String task) {
        if (mReceiver != null)
            mReceiver.onActionDispatched(actionEvent, task);
        if (actionEvent.isEnabled())
            performTask(task);
    }
}
