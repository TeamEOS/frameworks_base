/*
 * Copyright (C) 2015 The CyanogenMod Open Source Project
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
 */

package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

public class UsbTetherTile extends QSTile<QSTile.BooleanState> {

    private boolean mUsbTethered = false;
    private boolean mUsbConnected = false;
    private boolean mMassStorageActive = false;
    private String[] mUsbRegexs;
    private boolean mListening;
    private ConnectivityManager mCM;

    public UsbTetherTile(com.android.systemui.qs.QSTile.Host host) {
        super(host);
        mCM = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening)
            return;
        mListening = listening;
        if (listening) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
            filter.addAction(UsbManager.ACTION_USB_STATE);
            filter.addAction(Intent.ACTION_MEDIA_SHARED);
            filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
            mContext.registerReceiver(mReceiver, filter);
        } else {
            mContext.unregisterReceiver(mReceiver);
        }
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick() {
        setUsbTethering(!mUsbTethered);
        refreshState();
    }

    @Override
    public void handleLongClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
        mHost.startSettingsActivity(intent);
    }

    @Override
    protected String composeChangeAnnouncement() {
        return mContext.getString(mState.value ? R.string.accessibility_quick_settings_usb_tether_changed_on
                        : R.string.accessibility_quick_settings_usb_tether_changed_off);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        updateState();
        state.value = mUsbTethered;
        state.visible = mUsbConnected && !mMassStorageActive;
        state.label = mContext.getString(R.string.quick_settings_usb_tether_label);
        state.iconId = state.value ? R.drawable.ic_qs_usb_tether_on
                : R.drawable.ic_qs_usb_tether_off;
        int onOrOffId = state.value
                ? R.string.accessibility_quick_settings_usb_tether_on
                : R.string.accessibility_quick_settings_usb_tether_off;
        state.contentDescription = mContext.getString(onOrOffId);
    }

    private void updateState() {
        mUsbRegexs = mCM.getTetherableUsbRegexs();
        mUsbTethered = false;
        for (String s : mCM.getTetheredIfaces()) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex))
                    mUsbTethered = true;
            }
        }
    }

    private void setUsbTethering(boolean enabled) {
        if (mCM.setUsbTethering(enabled) != ConnectivityManager.TETHER_ERROR_NO_ERROR) {
            return;
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UsbManager.ACTION_USB_STATE)) {
                mUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
            } else if (intent.getAction().equals(Intent.ACTION_MEDIA_SHARED)) {
                mMassStorageActive = true;
            } else if (intent.getAction().equals(Intent.ACTION_MEDIA_UNSHARED)) {
                mMassStorageActive = false;
            }
            refreshState();
        }
    };
}
