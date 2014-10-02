
package org.codefirex.utils;

import android.app.KeyguardManager;
import android.content.ComponentName;
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
import android.net.ConnectivityManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.view.ViewGroup;

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

public final class CFXUtils {
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

    public static boolean hasTorch() {
        return new File("/system/app/Torch.apk").exists();
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

    /* since tablet bar is officially gone, so is this call in WM
     * however, we may keep this call and and get bar mode state
     * directly from user defined settings. In other words, no device
     * will ever have a default tablet mode state

    public static boolean hasSystemBar(Context context) {
        IWindowManager mWindowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            return mWindowManager.hasSystemNavBar();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }
    */

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

    public static String getDevice() {
        try {
            Process process = Runtime.getRuntime().exec("/system/bin/getprop ro.goo.board");

            BufferedReader mBufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = mBufferedReader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            mBufferedReader.close();
            process.waitFor();

            return output.toString().trim();
        } catch (Exception e) {
            return "error getting device name";
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
