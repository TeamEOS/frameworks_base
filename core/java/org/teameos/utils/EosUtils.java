/*
 * Copyright (C) 2014 The TeamEos Project
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

package org.teameos.utils;

import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplayStatus;
import android.net.ConnectivityManager;
import android.nfc.NfcAdapter;
import android.os.BatteryManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RILConstants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class EosUtils {
    public static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";
    public static final String URI_GRAVEYARD = "cfx_graveyard_uri";

    public static final String S2W_PATH = "/sys/android_touch/sweep2wake";
    public static final String FFC_PATH = "/sys/kernel/fast_charge/force_fast_charge";
    public static final String OT_PATH = "/proc/touchpad/enable";

    // 10 inch tablets
    public static boolean isXLargeScreen() {
        int screenLayout = Resources.getSystem().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
        return screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    // 7 inch "phablets" i.e. grouper
    public static boolean isLargeScreen() {
        int screenLayout = Resources.getSystem().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
        return screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    // normal phones
    public static boolean isNormalScreen() {
        int screenLayout = Resources.getSystem().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
        return screenLayout == Configuration.SCREENLAYOUT_SIZE_NORMAL;
    }

    public static boolean isLandscape(Context context) {
        return Configuration.ORIENTATION_LANDSCAPE
                == context.getResources().getConfiguration().orientation;
    }

    public static boolean isKeyguardRestricted(Context ctx) {
        IWindowManager mWm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        KeyguardManager km = (KeyguardManager) ctx.getSystemService(Context.KEYGUARD_SERVICE);
        boolean isKeyguardShowing = true;
        boolean isKeyguardLocked = true;
        try {
            isKeyguardShowing = mWm.isKeyguardLocked();
        } catch (RemoteException e) {

        }
        if (km == null)
            return false;
        isKeyguardLocked = km.isKeyguardLocked();
        return isKeyguardShowing && isKeyguardLocked;
    }

    public static boolean deviceSupportsUsbTether(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getTetherableUsbRegexs().length != 0);
    }

    public static boolean deviceSupportsWifiDisplay(Context ctx) {
        DisplayManager dm = (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
        return (dm.getWifiDisplayStatus().getFeatureState() != WifiDisplayStatus.FEATURE_STATE_UNAVAILABLE);
    }

    public static boolean deviceSupportsMobileData(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);
    }

    public static boolean deviceSupportsBluetooth() {
        return (BluetoothAdapter.getDefaultAdapter() != null);
    }

    public static boolean systemProfilesEnabled(ContentResolver resolver) {
        return (Settings.System.getInt(resolver, Settings.System.SYSTEM_PROFILES_ENABLED, 1) == 1);
    }

    public static boolean deviceSupportsNfc(Context ctx) {
        return NfcAdapter.getDefaultAdapter(ctx) != null;
    }

    /**
     * For now, let's say it's safe to assume if we are a mobile data device
     * we get lte. After all, it's 2014 ;D
     **/
    public static boolean deviceSupportsLte(Context ctx) {
        //final TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        //return (tm.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) || tm.getLteOnGsmMode() != 0;
        return deviceSupportsMobileData(ctx);
    }

    /*
    public static boolean deviceSupportsDockBattery(Context ctx) {
        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        return bm.isDockBatterySupported();
    }
    */

    public static boolean deviceSupportsCamera() {
        return Camera.getNumberOfCameras() > 0;
    }

    public static boolean deviceSupportsGps(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    public static boolean deviceSupportsTorch(Context context) {
        return SystemProperties.getBoolean("ro.hardware.supports_torch", true);
    }

    public static boolean adbEnabled(ContentResolver resolver) {
        return (Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0)) == 1;
    }

    public static boolean hasData(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);
    }

    public static boolean isCdma(Context context) {
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return (tm.getCurrentPhoneType() == TelephonyManager.PHONE_TYPE_CDMA);
    }

    public static boolean isGSM(Context context) {
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return (tm.getCurrentPhoneType() == TelephonyManager.PHONE_TYPE_GSM);
    }

    public static boolean isCdmaLTE(Context context) {
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getLteOnCdmaMode() == RILConstants.LTE_ON_CDMA_TRUE;
    }

    public static boolean hasNavBar(Context context) {
        IWindowManager mWindowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            return mWindowManager.hasNavigationBar();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isCapKeyDevice(Context context) {
        return !context.getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);
    }

    public static boolean isComponentResolved(PackageManager pm, String component) {
        // empty or custom action, always true
        if (!component.startsWith("app:")) {
            return true;
        }
        ComponentName componentName = ComponentName.unflattenFromString(component.substring(4));
        ActivityInfo activityInfo = null;
        boolean noError = false;
        try {
            activityInfo = pm.getActivityInfo(componentName, PackageManager.GET_RECEIVERS);
            noError = true;
        } catch (Exception e) {
            e.printStackTrace();
            // i'm not sure if "noError = true" gets called before error is
            // thrown
            noError = false;
        }
        return noError;
    }

    public static String getLabelFromComponent(PackageManager pm, String component) {
        if (component.startsWith("app:")) {
            component = component.substring(4);
        }
        ComponentName componentName = ComponentName.unflattenFromString(component);
        ActivityInfo activityInfo = null;
        boolean noError = false;
        try {
            activityInfo = pm.getActivityInfo(componentName, PackageManager.GET_RECEIVERS);
            noError = true;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            // i'm not sure if "noError = true" gets called before error is thrown
            noError = false;
        }
        if (noError) {
            return activityInfo.loadLabel(pm).toString();
        }
        return null;
    }

    public static Drawable getIconFromComponent(PackageManager pm, String component) {
        if (component.startsWith("app:")) {
            component = component.substring(4);
        }
        ComponentName componentName = ComponentName.unflattenFromString(component);
        ActivityInfo activityInfo = null;
        boolean noError = false;
        try {
            activityInfo = pm.getActivityInfo(componentName, PackageManager.GET_RECEIVERS);
            noError = true;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            // i'm not sure if "noError = true" gets called before error is thrown
            noError = false;
        }
        if (noError) {
            return activityInfo.loadIcon(pm);
        }
        return null;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device
     * density.
     * 
     * @param dp A value in dp (density independent pixels) unit. Which we need
     *            to convert into pixels
     * @param context Context to get resources and device specific display
     *            metrics
     * @return A float value to represent px equivalent to dp depending on
     *         device density
     */
    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    public static int ConvertDpToPixelAsInt(float dp, Context context) {
        float px = convertDpToPixel(dp, context);
        if (px < 1)
            px = 1;
        return Math.round(px);
    }

    public static int ConvertDpToPixelAsInt(int dp, Context context) {
        float px = convertDpToPixel((float) dp, context);
        if (px < 1)
            px = 1;
        return Math.round(px);
    }

    /**
     * This method converts device specific pixels to density independent
     * pixels.
     * 
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display
     *            metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }

    public static boolean hasKernelFeature(String path) {
        return new File(path).exists();
    }

    public static void setKernelFeatureEnabled(String feature, boolean enabled) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(feature)));
            String output = "" + (enabled ? "1" : "0");
            writer.write(output.toCharArray(), 0, output.toCharArray().length);
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static boolean isKernelFeatureEnabled(String feature) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(feature).getAbsolutePath()));
            String input = reader.readLine();
            reader.close();
            return input.contains("1");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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

    /* utility to iterate a viewgroup and return a list of child views of type */
    public static <T extends View> ArrayList<T> getAllChildren(View root, Class<T> returnType) {
        if (!(root instanceof ViewGroup)) {
            ArrayList<T> viewArrayList = new ArrayList<T>();
            try {
                viewArrayList.add(returnType.cast(root));
            } catch (Exception e) {
                // handle all exceptions the same and silently fail
            }
            return viewArrayList;
        }
        ArrayList<T> result = new ArrayList<T>();
        ViewGroup vg = (ViewGroup) root;
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            ArrayList<T> viewArrayList = new ArrayList<T>();
            try {
                viewArrayList.add(returnType.cast(root));
            } catch (Exception e) {
                // handle all exceptions the same and silently fail
            }
            viewArrayList.addAll(getAllChildren(child, returnType));
            result.addAll(viewArrayList);
        }
        return result;
    }
}
