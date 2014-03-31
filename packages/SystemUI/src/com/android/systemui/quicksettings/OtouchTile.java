package com.android.systemui.quicksettings;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsController;

public class OtouchTile extends FileObserverTile {
	public static final String TAG = OtouchTile.class.getSimpleName();
    public static final String OT_PATH = "/proc/touchpad/enable";

	public OtouchTile(Context context, QuickSettingsController qsc) {
		super(context, qsc);
	}

	@Override
	protected String getFilePath() {
		return OT_PATH;
	}

	@Override
	protected TwoStateTileRes getTileRes() {
		return new TwoStateTileRes(R.string.quick_settings_otouch_on_label
				,R.string.quick_settings_otouch_off_label
				,R.drawable.ic_qs_otouch_on
				,R.drawable.ic_qs_otouch_off);
	}

	protected void onFileChanged(boolean featureState) {
		Intent i = new Intent();
		i.setAction("com.cfx.settings.device.N1Settings.feature_changed");
		i.putExtra("feature_otouch", featureState ? "1" : "0");
		mContext.sendBroadcast(i);
		Log.i(TAG, OT_PATH + " changed. Notify interested parties");
	}
}
