package org.codefirex.utils;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.SearchManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.input.InputManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import android.view.HapticFeedbackConstants;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.cm.TorchConstants;

public abstract class ActionHandler {
    protected ArrayList<String> mActions;
    protected Context mContext;
    static final String TAG = "ActionHandler";

    public static final String SYSTEMUI_TASK_NO_ACTION = "task_no_action";
    public static final String SYSTEMUI_TASK_SETTINGS_PANEL = "task_settings_panel";
    public static final String SYSTEMUI_TASK_NOTIFICATION_PANEL = "task_notification_panel";
    public static final String SYSTEMUI_TASK_SCREENSHOT = "task_screenshot";
    public static final String SYSTEMUI_TASK_SCREENRECORD = "task_screenrecord";
    public static final String SYSTEMUI_TASK_SCREENOFF = "task_screenoff";
    public static final String SYSTEMUI_TASK_KILL_PROCESS = "task_killcurrent";
    public static final String SYSTEMUI_TASK_ASSIST = "task_assist";
    public static final String SYSTEMUI_TASK_POWER_MENU = "task_powermenu";
    public static final String SYSTEMUI_TASK_TORCH = "task_torch";
    public static final String SYSTEMUI_TASK_CAMERA = "task_camera";
    public static final String SYSTEMUI_TASK_BT = "task_bt";
    public static final String SYSTEMUI_TASK_WIFI = "task_wifi";
    public static final String SYSTEMUI_TASK_WIFIAP = "task_wifiap";
    public static final String SYSTEMUI_TASK_RECENTS = "task_recents";
    public static final String SYSTEMUI_TASK_VOICE_SEARCH = "task_voice_search";
    public static final String SYSTEMUI_TASK_APP_SEARCH = "task_app_search";
    public static final String SYSTEMUI_TASK_MENU = "task_menu";
    public static final String SYSTEMUI_TASK_BACK = "task_back";
    public static final String SYSTEMUI_TASK_HOME = "task_home";

    public ActionHandler(Context context, ArrayList<String> actions) {
        if (context == null) throw new IllegalArgumentException("Context cannot be null");
        mContext = context;
        mActions = actions;
    }

    public ActionHandler(Context context, String actions) {
        if (context == null) throw new IllegalArgumentException("Context cannot be null");
        mContext = context;
        mActions = new ArrayList<String>();
        mActions.addAll(Arrays.asList(actions.split("\\|")));
    }

    public ActionHandler(Context context) {
        if (context == null) throw new IllegalArgumentException("Context cannot be null");
        mContext = context;
    }

    /**
     * Set the actions to perform.
     *
     * @param actions
     */
    public void setActions(List<String> actions) {
        if (actions == null) {
            mActions = null;
        } else {
            mActions = new ArrayList<String>();
            mActions.addAll(actions);
        }
    }

    /**
     * Event handler. This method must be called when the event should be triggered.
     *
     * @param location
     * @return
     */
    public final boolean handleEvent(int location) {
        if (mActions == null) {
            Log.d("ActionHandler", "Discarding event due to null actions");
            return false;
        }

        String action = mActions.get(location);
        if (action == null || action.equals("")) {
            return false;
        } else {
            performTask(action);
            return true;
        }
    }

    public void performTask(String action) {
        if (action.equals(SYSTEMUI_TASK_NO_ACTION)) {
            return;
        } else if (action.equals(SYSTEMUI_TASK_KILL_PROCESS)) {
            killProcess();
        } else if (action.equals(SYSTEMUI_TASK_SCREENSHOT)) {
            takeScreenshot();
        } else if (action.equals(SYSTEMUI_TASK_SCREENRECORD)) {
            takeScreenrecord();
        } else if (action.equals(SYSTEMUI_TASK_SCREENOFF)) {
            screenOff();
        } else if (action.equals(SYSTEMUI_TASK_ASSIST)) {
            launchAssistAction();
        } else if (action.equals(SYSTEMUI_TASK_POWER_MENU)) {
            showPowerMenu();
        } else if (action.equals(SYSTEMUI_TASK_TORCH)) {
            toggleTorch();
        } else if (action.equals(SYSTEMUI_TASK_CAMERA)) {
            launchCamera();
        } else if (action.equals(SYSTEMUI_TASK_WIFI)) {
            toggleWifi();
        } else if (action.equals(SYSTEMUI_TASK_WIFIAP)) {
            toggleWifiAP();
        } else if (action.equals(SYSTEMUI_TASK_BT)) {
            toggleBluetooth();
        } else if (action.equals(SYSTEMUI_TASK_RECENTS)) {
            callStatusbarStub(SYSTEMUI_TASK_RECENTS);
        } else if (action.equals(SYSTEMUI_TASK_SETTINGS_PANEL)) {
            callStatusbarStub(SYSTEMUI_TASK_SETTINGS_PANEL);
        } else if (action.equals(SYSTEMUI_TASK_NOTIFICATION_PANEL)) {
            callStatusbarStub(SYSTEMUI_TASK_NOTIFICATION_PANEL);
        } else if (action.equals(SYSTEMUI_TASK_VOICE_SEARCH)) {
            launchAssistLongPressAction();
        } else if (action.equals(SYSTEMUI_TASK_APP_SEARCH)) {
            triggerVirtualKeypress(KeyEvent.KEYCODE_SEARCH);
        } else if (action.equals(SYSTEMUI_TASK_MENU)) {
            triggerVirtualKeypress(KeyEvent.KEYCODE_MENU);
        } else if (action.equals(SYSTEMUI_TASK_BACK)) {
            triggerVirtualKeypress(KeyEvent.KEYCODE_BACK);
        } else if (action.equals(SYSTEMUI_TASK_HOME)) {
            triggerVirtualKeypress(KeyEvent.KEYCODE_HOME);
        } else if (action.startsWith("app:")) {
            launchActivity(action);
        }
    }

    public Handler getHandler() {
        return H;
    }

    private Handler H = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {

            }
        }
    };

    private void launchActivity(String action) {
        String activity = action;
        if (activity.startsWith("app:")) {
            activity = activity.substring(4);
        }
        ComponentName component = ComponentName.unflattenFromString(activity);

        /* Try to launch the activity from history, if available. */
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RecentTaskInfo task : activityManager.getRecentTasks(20,
                ActivityManager.RECENT_IGNORE_UNAVAILABLE)) {
            if (task != null && task.origActivity != null &&
                    task.origActivity.equals(component)) {
                activityManager.moveTaskToFront(task.id, ActivityManager.MOVE_TASK_WITH_HOME);
                postActionEventHandled(true);
                return;
            }
        }

        try {
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(component);
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
                    | Intent.FLAG_ACTIVITY_TASK_ON_HOME
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            postActionEventHandled(true);
        } catch (Exception e) {
            Log.i(TAG, "Unable to launch activity " + e);
            postActionEventHandled(false);
            handleAction(activity);
        }
    }

    private boolean callStatusbarStub(String action) {
        try {
            IStatusBarService barService = IStatusBarService.Stub.asInterface(
                    ServiceManager.getService("statusbar"));
            if (barService == null)
                return false;
            if (SYSTEMUI_TASK_RECENTS.equals(action)) {
                sendCloseSystemWindows("recentapps");
                barService.toggleRecentApps();
            } else if (SYSTEMUI_TASK_SETTINGS_PANEL.equals(action)) {
                barService.expandSettingsPanel();
            } else if (SYSTEMUI_TASK_NOTIFICATION_PANEL.equals(action)) {
                barService.expandNotificationsPanel();
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void sendCloseSystemWindows(String reason) {
        if (ActivityManagerNative.isSystemReady()) {
            try {
                ActivityManagerNative.getDefault().closeSystemDialogs(reason);
            } catch (RemoteException e) {
            }
        }
    }

    private void launchAssistLongPressAction() {
        sendCloseSystemWindows("assist");
        // launch the search activity
        Intent intent = new Intent(Intent.ACTION_SEARCH_LONG_PRESS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            // TODO: This only stops the factory-installed search manager.  
            // Need to formalize an API to handle others
            SearchManager searchManager = (SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE);
            if (searchManager != null) {
                searchManager.stopSearch();
            }
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (ActivityNotFoundException e) {
            Slog.w(TAG, "No activity to handle assist long press action.", e);
        }
    }

    private void triggerVirtualKeypress(final int keyCode) {
        InputManager im = InputManager.getInstance();
        long now = SystemClock.uptimeMillis();

        final KeyEvent downEvent = new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
                keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD);
        final KeyEvent upEvent = KeyEvent.changeAction(downEvent, KeyEvent.ACTION_UP);

        im.injectInputEvent(downEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        im.injectInputEvent(upEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    private void launchCamera() {
        Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        PackageManager pm = mContext.getPackageManager();

        final ResolveInfo mInfo = pm.resolveActivity(i, 0);

        String action = new ComponentName(mInfo.activityInfo.packageName,
                mInfo.activityInfo.name).flattenToString();
        launchActivity(action);
    }

    private void toggleWifi() {
        WifiManager wfm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        wfm.setWifiEnabled(!wfm.isWifiEnabled());
    }

    private void toggleBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean enabled = bluetoothAdapter.isEnabled();
        if (enabled) {
            bluetoothAdapter.disable();
        } else {
            bluetoothAdapter.enable();
        }
    }

    private void toggleWifiAP() {
        WifiManager wfm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        int state = wfm.getWifiApState();
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
            case WifiManager.WIFI_AP_STATE_ENABLED:
                setSoftapEnabled(wfm, false);
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
            case WifiManager.WIFI_AP_STATE_DISABLED:
                setSoftapEnabled(wfm, true);
                break;
        }
    }

    private void setSoftapEnabled(WifiManager wm, boolean enable) {
        final ContentResolver cr = mContext.getContentResolver();
        /**
         * Disable Wifi if enabling tethering
         */
        int wifiState = wm.getWifiState();
        if (enable && ((wifiState == WifiManager.WIFI_STATE_ENABLING) ||
                (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
            wm.setWifiEnabled(false);
            Settings.Global.putInt(cr, Settings.Global.WIFI_SAVED_STATE, 1);
        }

        // Turn on the Wifi AP
        wm.setWifiApEnabled(null, enable);

        /**
         * If needed, restore Wifi on tether disable
         */
        if (!enable) {
            int wifiSavedState = 0;
            try {
                wifiSavedState = Settings.Global.getInt(cr, Settings.Global.WIFI_SAVED_STATE);
            } catch (Settings.SettingNotFoundException e) {
                // Do nothing here
            }
            if (wifiSavedState == 1) {
                wm.setWifiEnabled(true);
                Settings.Global.putInt(cr, Settings.Global.WIFI_SAVED_STATE, 0);
            }
        }
    }

    private void toggleTorch() {
        Intent i = new Intent(TorchConstants.ACTION_TOGGLE_STATE);
        mContext.sendBroadcast(i);
    }

    /**
     * functions needed for taking screenhots. This leverages the built in ICS
     * screenshot functionality
     */
    final Object mScreenshotLock = new Object();
    static ServiceConnection mScreenshotConnection = null;

    final Runnable mScreenshotTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (mScreenshotLock) {
                if (mScreenshotConnection != null) {
                    mContext.unbindService(mScreenshotConnection);
                    mScreenshotConnection = null;
                }
            }
        }
    };

    private void takeScreenshot() {
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return;
            }
            ComponentName cn = new ComponentName("com.android.systemui",
                    "com.android.systemui.screenshot.TakeScreenshotService");
            Intent intent = new Intent();
            intent.setComponent(cn);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, 1);
                        final ServiceConnection myConn = this;
                        Handler h = new Handler(H.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotConnection == myConn) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        H.removeCallbacks(mScreenshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg1 = msg.arg2 = 0;

                        /*
                         * remove for the time being if (mStatusBar != null &&
                         * mStatusBar.isVisibleLw()) msg.arg1 = 1; if
                         * (mNavigationBar != null &&
                         * mNavigationBar.isVisibleLw()) msg.arg2 = 1;
                         */

                        /* wait for the dialog box to close */
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                        }

                        /* take the screenshot */
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };
            if (mContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)) {
                mScreenshotConnection = conn;
                H.postDelayed(mScreenshotTimeout, 10000);
            }
        }
    }

    final Object mScreenrecordLock = new Object();
    ServiceConnection mScreenrecordConnection = null;

    final Runnable mScreenrecordTimeout = new Runnable() {
        @Override public void run() {
            synchronized (mScreenrecordLock) {
                if (mScreenrecordConnection != null) {
                    mContext.unbindService(mScreenrecordConnection);
                    mScreenrecordConnection = null;
                }
            }
        }
    };

    // Assume this is called from the Handler thread.
    private void takeScreenrecord() {
        synchronized (mScreenrecordLock) {
            if (mScreenrecordConnection != null) {
                return;
            }
            ComponentName cn = new ComponentName("com.android.systemui",
                    "com.android.systemui.omni.screenrecord.TakeScreenrecordService");
            Intent intent = new Intent();
            intent.setComponent(cn);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenrecordLock) {
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, 1);
                        final ServiceConnection myConn = this;
                        Handler h = new Handler(H.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenrecordLock) {
                                    if (mScreenrecordConnection == myConn) {
                                        mContext.unbindService(mScreenrecordConnection);
                                        mScreenrecordConnection = null;
                                        H.removeCallbacks(mScreenrecordTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg1 = msg.arg2 = 0;
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                        }
                    }
                }
                @Override
                public void onServiceDisconnected(ComponentName name) {}
            };
            if (mContext.bindServiceAsUser(
                    intent, conn, Context.BIND_AUTO_CREATE, UserHandle.CURRENT)) {
                mScreenrecordConnection = conn;
                // Screenrecord max duration is 30 minutes. Allow 31 minutes before killing
                // the service.
                H.postDelayed(mScreenrecordTimeout, 31 * 60 * 1000);
            }
        }
    }

	private void killProcess() {
		if (mContext
				.checkCallingOrSelfPermission(android.Manifest.permission.FORCE_STOP_PACKAGES) == PackageManager.PERMISSION_GRANTED) {
			try {
				final Intent intent = new Intent(Intent.ACTION_MAIN);
				String defaultHomePackage = "com.android.launcher";
				intent.addCategory(Intent.CATEGORY_HOME);
				final ResolveInfo res = mContext.getPackageManager()
						.resolveActivity(intent, 0);
				if (res.activityInfo != null
						&& !res.activityInfo.packageName.equals("android")) {
					defaultHomePackage = res.activityInfo.packageName;
				}
				IActivityManager am = ActivityManagerNative.getDefault();
				List<RunningAppProcessInfo> apps = am.getRunningAppProcesses();
				for (RunningAppProcessInfo appInfo : apps) {
					int uid = appInfo.uid;
					// Make sure it's a foreground user application (not system,
					// root, phone, etc.)
					if (uid >= Process.FIRST_APPLICATION_UID
							&& uid <= Process.LAST_APPLICATION_UID
							&& appInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
						if (appInfo.pkgList != null
								&& (appInfo.pkgList.length > 0)) {
							for (String pkg : appInfo.pkgList) {
								if (!pkg.equals("com.android.systemui")
										&& !pkg.equals(defaultHomePackage)) {
									am.forceStopPackage(pkg,
											UserHandle.USER_CURRENT);
									postActionEventHandled(true);
									break;
								}
							}
						} else {
							Process.killProcess(appInfo.pid);
							postActionEventHandled(true);
							break;
						}
					}
				}
			} catch (RemoteException remoteException) {
				Log.d("ActionHandler", "Caller cannot kill processes, aborting");
				postActionEventHandled(false);
			}
		} else {
			Log.d("ActionHandler", "Caller cannot kill processes, aborting");
			postActionEventHandled(false);
		}
	}

    private void screenOff() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        pm.goToSleep(SystemClock.uptimeMillis());
    }

    private void launchAssistAction() {
        sendCloseSystemWindows("assist");
        Intent intent = ((SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE))
                .getAssistIntent(mContext, true, UserHandle.USER_CURRENT);
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try {
                mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            } catch (ActivityNotFoundException e) {
                Slog.w(TAG, "No activity to handle assist action.", e);
            }
        }
    }

    private void showPowerMenu() {
        Intent i = new Intent(CFXConstants.ACTION_CFX_INTERNAL_ACTIVITY);
        i.putExtra(CFXConstants.INTENT_EXTRA_INTERNAL_ACTIVITY, SYSTEMUI_TASK_POWER_MENU);
        mContext.sendBroadcastAsUser(i, new UserHandle(UserHandle.USER_ALL));
    }

    /**
     * This method is called after an action is performed. This is useful for subclasses to
     * override, such as the one in the lock screen. As you need to unlock the device after
     * performing an action.
     * 
     * @param actionWasPerformed
     */
    protected boolean postActionEventHandled(boolean actionWasPerformed) {
        return actionWasPerformed;
    }

    /**
     * This the the fall over method that is called if this base class cannot process an action. You
     * do not need to manually call {@link postActionEventHandled}
     * 
     * @param action
     * @return
     */
    public abstract boolean handleAction(String action);
}
