
package com.android.systemui.quicksettings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsController;

public class FastChargeTile extends FileObserverTile {
    public static final String TAG = FastChargeTile.class.getSimpleName();
    public static final String FFC_PATH = "/sys/kernel/fast_charge/force_fast_charge";
    private boolean mNeedsWriteOverride = false;

    public FastChargeTile(Context context, QuickSettingsController qsc) {
        super(context, qsc);
    }

    @Override
    void onPostCreate() {
        String device = SystemProperties.get("ro.product.device", "none");
        mNeedsWriteOverride = "m8".equals(device);
        super.onPostCreate();
    }

    @Override
    protected String getFilePath() {
        return FFC_PATH;
    }

    @Override
    protected TwoStateTileRes getTileRes() {
        return new TwoStateTileRes(R.string.quick_settings_fcharge_on_label
                , R.string.quick_settings_fcharge_off_label
                , R.drawable.ic_qs_fcharge_on
                , R.drawable.ic_qs_fcharge_off);
    }

    protected void onFileChanged(boolean featureState) {
        Log.i(TAG, FFC_PATH + " changed. Notify interested parties");
    }

    protected void setEnabled(boolean enabled) {
        if (mNeedsWriteOverride) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File(FFC_PATH)));
                String output = "" + (enabled ? "1\n" : "0\n");
                writer.write(output.toCharArray(), 0, output.toCharArray().length);
                writer.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            super.setEnabled(enabled);
        }
    }
}
