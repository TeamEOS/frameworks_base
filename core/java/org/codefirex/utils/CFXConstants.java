
package org.codefirex.utils;

public final class CFXConstants {
    /* CFX SETTINGS STRINGS */

	/**
	 * @hide
	 * CFX intent actions and keys
	 */	

    /**
     * @hide
     */
    public static final String ACTION_CFX_UI_CHANGE = "cfx_ui_change";

    /**
     * @hide
     */
    public static final String ACTION_CFX_HOT_REBOOT = "cfx_hot_reboot";

    /**
     * @hide
     */
    public static final String ACTION_CFX_INTERNAL_ACTIVITY = "cfx_internal_activity";

    /**
     * @hide
     */
    public static final String INTENT_REASON_UI_CHANGE = "cfx_intent_reason_ui_change";

    /**
     * @hide
     */
    public static final String INTENT_REASON_UI_BAR_MODE = "cfx_intent_reason_ui_barmode";

    /**
     * @hide
     */
    public static final String INTENT_REASON_UI_BAR_SIZE = "cfx_intent_reason_ui_barsize";

    /**
     * @hide
     */
    public static final String INTENT_REASON_UI_CHANGE_GLASS_ENABLED = "cfx_intent_reason_glass_enabled";

    /**
     * @hide
     */
    public static final String INTENT_EXTRA_RESTART_SYSTEMUI = "cfx_intent_extra_kill_systemui";

    /**
     * @hide
     */
    public static final String INTENT_EXTRA_INTERNAL_ACTIVITY = "cfx_intent_extra_internal_activity";

    /**
     * @hide
     */
    public static final String SYSTEMUI_UI_MODE = "cfx_reason_systemui_ui_mode";

    /**
     * @hide
     */
    public static final int SYSTEMUI_UI_MODE_NO_NAVBAR = 0;

    /**
     * @hide
     */
    public static final int SYSTEMUI_UI_MODE_NAVBAR = 1;

    /**
     * @hide
     */
    public static final int SYSTEMUI_UI_MODE_NAVBAR_LEFT = 2;

    /**
     * @hide
     */
    public static final int SYSTEMUI_UI_MODE_SYSTEMBAR = 3;    

    /**
     * @hide
     */
    public static final int SYSTEMUI_NAVBAR_SIZE_DEF_INDEX = 8;

    /**
     * @hide
     */
    public static final String SYSTEMUI_NAVBAR_SIZE_DP = "cfx_reason_navbar_size_dp";

    /**
     * @hide
     */
    public static final int[] getNavbarDP = {
        32,
        34,
        36,
        38,
        40,
        42,
        44,
        46,
        48
    };

    /**
     * @hide
     */
    public static final int[] getNavbarDpWidth = {
        34,
        34,
        34,
        34,
        36,
        38,
        40,
        40,
        42
    };

    /**
     * @hide
     */
    public static final String SYSTEMUI_SOFTKEY_LP_BACK = "systemui_softkey_lp_back";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SOFTKEY_LP_HOME = "systemui_softkey_lp_home";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SOFTKEY_LP_RECENT = "systemui_softkey_lp_recent";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SOFTKEY_LP_MENU = "systemui_softkey_lp_menu";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SOFTKEY_LP_EXTRA1 = "systemui_softkey_lp_extra1";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SOFTKEY_LP_EXTRA2 = "systemui_softkey_lp_extra2";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_NO_ACTION = "task_no_action";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_SCREENSHOT = "task_screenshot";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_SCREENRECORD = "task_screenrecord";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_SCREENOFF = "task_screenoff";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_KILL_PROCESS = "task_killcurrent";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_ASSIST = "task_assist";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_POWER_MENU = "task_powermenu";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_TORCH = "task_torch";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_CAMERA = "task_camera";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_BT = "task_bt";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_WIFI = "task_wifi";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_WIFIAP = "task_wifiap";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_RECENTS = "task_recents";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_VOICE_SEARCH = "task_voice_search";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_APP_SEARCH = "task_app_search";

    /**
     * @hide
     */
    public static final String SYSTEMUI_TASK_MENU = "task_menu";

    /**
     * @hide
     */
    public static final String SETTINGS_GLOBAL_AOSP_MODE = "cfx_settings_aosp_mode";

    /**
     * SystemUI bars color and transparency
     *
     * @hide
     */
    public static final String SYSTEMUI_USE_GLASS = "cfx_systemui_use_glass";

    /**
     * @hide
     */
    public static final int SYSTEMUI_USE_GLASS_DEF = 0;

    /**
     * transparency level for navbar glass when launcher is showing
     *
     * @hide
     */
    public static final String SYSTEMUI_NAVBAR_GLASS_LEVEL = "cfx_systemui_navbar_glass_level";

    /**
     * transparency level for statusbar glass when launcher is showing
     *
     * @hide
     */
    public static final String SYSTEMUI_STATUSBAR_GLASS_LEVEL = "cfx_systemui_statusbar_glass_level";

    /**
     * default transparency enabled for navbar glass when launcher is showing
     *
     * @hide
     */
    public static final String SYSTEMUI_NAVBAR_GLASS_DEFAULT_ENABLED = "cfx_systemui_navbar_glass_default_enabled";

    /**
     * default transparency enabled for navbar glass when launcher is showing
     *
     * @hide
     */
    public static final String SYSTEMUI_STATUSBAR_GLASS_DEFAULT_ENABLED = "cfx_systemui_statusbar_glass_default_enabled";

    /**
     * default transparency level preset for navbar when launcher is showing
     * 
     * @hide
     */
    public static final int SYSTEMUI_NAVBAR_GLASS_PRESET = 125;

    /**
     * default transparency level preset for statusbar when launcher is showing
     *
     * @hide
     */

    public static final int SYSTEMUI_STATUSBAR_GLASS_PRESET = 120;

    /**
     *
     * @hide
     */
    public static final String SYSTEMUI_STATUSBAR_DRAG_BRIGHTNESS = "cfx_systemui_statusbar_drag_brightness";

    /**
     * custom color for status bar
     * default is -1, indicating default color or to check for drawable
     *
     * @hide
     */
    public static final String SYSTEMUI_STATUSBAR_COLOR = "cfx_systemui_statusbar_color";

    /**
     * custom color for navigation bar or system bar
     * default is -1
     *
     * @hide
     */
    public static final String SYSTEMUI_BOTTOM_BAR_COLOR = "cfx_systemui_bottom_bar_color";

    /**
     * @hide
     */
    public static final String SYSTEMUI_BATTERY_ICON_VISIBLE = "cfx_systemui_battery_icon_visible";

    /**
     * @hide
     */
    public static final int SYSTEMUI_BATTERY_ICON_VISIBLE_DEF = 1;

    /**
     * @hide
     * 
     */
    public static final String SYSTEMUI_STATUSBAR_BATTERY_TEXT_STATE = "systemui_statusbar_battery_text_state"; 

    /**
     * @hide
     */
    public static final String SYSTEMUI_BATTERY_TEXT_TAG = "systemui_statusbar_battery_text_tag";

    /**
     * @hide
     */
    public static final String SYSTEMUI_BATTERY_ICON_TAG = "systemui_statusbar_battery_icon_tag";

    /**
     * @hide
     * 
     * 0 = gone
     * 1 = text with percent
     * 2 = text no percent
     * 
     */
    public static final String SYSTEMUI_BATTERY_TEXT_COLOR = "cfx_systemui_battery_text_color";

    /**
     * @hide
     */
    public static final int SYSTEMUI_BATTERY_TEXT_COLOR_DEF = -1;

    /**
     * @hide
     */
    public static final String SYSTEMUI_CLOCK_VISIBLE = "cfx_systemui_clock_visible";

    /**
     * @hide
     */
    public static final int SYSTEMUI_CLOCK_GONE = 0;

    /**
     * @hide
     */
    public static final int SYSTEMUI_CLOCK_CLUSTER = 1;

    /**
     * @hide
     */
    public static final int SYSTEMUI_CLOCK_CENTER = 2;

    /**
     * @hide
     */
    public static final String SYSTEMUI_CLOCK_COLOR = "cfx_systemui_clock_color";

    /**
     * @hide
     */
    public static final int SYSTEMUI_CLOCK_COLOR_DEF = -1;

    /**
     * @hide
     */
    public static final String SYSTEMUI_CLOCK_AMPM = "cfx_systemui_clock_ampm";

    /**
     * @hide
     */
    public static final int SYSTEMUI_CLOCK_AMPM_DEF = 2;

    /**
     * @hide
     */
    public static final String SYSTEMUI_CLOCK_SIGNAL_CLUSTER_TAG = "cfx_systemui_clock_cluster_tag";

    /**
     * @hide
     */
    public static final String SYSTEMUI_CLOCK_CENTER_TAG = "cfx_systemui_clock_center_tag";

    /**
     * @hide
     */
    public static final String SYSTEMUI_WEATHER_HEADER_VIEW = "cfx_systemui_header_weather_view";

    /**
     * @hide
     */
    public static final String SYSTEM_POWER_DONT_WAKE_DEVICE_PLUGGED = "system_power_dont_wake_plugged";

    /**
     * @hide
     */
    public static final String SYSTEM_POWER_ENABLE_CRT_OFF = "system_power_crt_off";

    /**
     * @hide
     */
    public static final String SYSTEM_POWER_ENABLE_CRT_ON = "system_power_crt_on";

    /**
     * @hide
     */
    public static final String SYSTEMUI_SCREENSHOT_SCALE_INDEX = "screenshot_scale_factor";

    /**
     * @hide
     */
    public static final String SYSTEMUI_DISABLE_BATTERY_WARNING = "systemui_disable_battery_warning";

    /**
     * Whether or not the touchpad is enabled. (0 = false, 1 = true)
     *
     * @hide
     */
    public static final String DEVICE_SETTINGS_TOUCHPAD_STATUS = "cfx_device_settings_touchpad_status";

    /**
     * The touchpad gesture mode. (0 = spots, 1 = pointer)
     *
     * @hide
     */
    public static final String DEVICE_SETTINGS_TOUCHPAD_MODE = "cfx_device_settings_touchpad_mode";

    /**
     * Value for {@link #cfx_TOUCHPAD_STATUS} to use the touchpad located on the
     * hardware keyboard dock.
     *
     * @hide
     */
    public static final int DEVICE_SETTINGS_TOUCHPAD_DISABLED = 0;

    /**
     * Value for {@link #cfx_TOUCHPAD_STATUS} to use the touchpad located on the
     * hardware keyboard dock.
     *
     * @hide
     */
    public static final int DEVICE_SETTINGS_TOUCHPAD_ENABLED = 1;

    /**
     * Forces formal text input.  1 to replace emoticon key with enter key.
     * @hide
     */
    public static final String FORMAL_TEXT_INPUT = "formal_text_input";

    /**
     *
     * @hide
     */
    public static final String RECENTS_MEM_DISPLAY = "recents_mem_display";

    /**
     * Recent apps clear all button.
     *
     * @hide
     */
    public static final String RECENTS_CLEAR_ALL = "recents_clear_all";

    /** 
     * Whether to show the network status in the status bar
     * @hide
     */
    public static final String STATUS_BAR_NETWORK_STATS = "status_bar_network_stats";

    /**
     * Frequency at which stats are updated, in milliseconds
     * @hide
     */
    public static final String STATUS_BAR_NETWORK_STATS_UPDATE_INTERVAL = "status_bar_network_stats_update_frequency";

    /**
     * statusbar network stats text color
     *
     * @hide
     */
    public static final String STATUS_BAR_NETWORK_STATS_TEXT_COLOR = "status_bar_network_stats_text_color";

    /**
     * back key long press action
     *
     * @hide
     */
    public static final String INPUT_HARDKEY_BACK_LONGPRESS = "input_hardkey_back_longpress";

    /**
     * home key long press action
     *
     * @hide
     */
    public static final String INPUT_HARDKEY_HOME_LONGPRESS = "input_hardkey_home_longpress";

    /**
     * recent key long press action
     *
     * @hide
     */
    public static final String INPUT_HARDKEY_RECENT_LONGPRESS = "input_hardkey_recent_longpress";

    /**
     * menu key long press action
     *
     * @hide
     */
    public static final String INPUT_HARDKEY_MENU_LONGPRESS = "input_hardkey_menu_longpress";

    /**
     * search assistant key long press action
     *
     * @hide
     */
    public static final String INPUT_HARDKEY_ASSIST_LONGPRESS = "input_hardkey_assist_longpress";

    /**
     * back key double tap action
     *
     * @hide
     */
    public static final String INPUT_HARDKEY_BACK_DOUBLETAP = "input_hardkey_back_doubletap";

    /**
     * home key double tap action
     *
     * @hide
     */
    public static final String INPUT_HARDKEY_HOME_DOUBLETAP = "input_hardkey_home_doubletap";

    /**
     * recent key double tap action
     *
     * @hide
     */
    public static final String INPUT_HARDKEY_RECENT_DOUBLETAP = "input_hardkey_recent_doubletap";

    /**
     * menu key double tap action
     *
     * @hide
     */
    public static final String INPUT_HARDKEY_MENU_DOUBLETAP = "input_hardkey_menu_doubletap";

    /**
     * search assistant double tap action
     *
     * @hide
     */
    public static final String INPUT_HARDKEY_ASSIST_DOUBLETAP = "input_hardkey_assist_doubletap";

    /**
     * recent key double tap action
     *
     * @hide
     */
    public static final String INPUT_HARDKEY_RECENT_SINGLETAP = "input_hardkey_recent_singletap";

    /**
     * menu key double tap action
     *
     * @hide
     */
    public static final String INPUT_HARDKEY_MENU_SINGLETAP = "input_hardkey_menu_singletap";

    /**
     * search assistant double tap action
     *
     * @hide
     */
    public static final String INPUT_HARDKEY_ASSIST_SINGLETAP = "input_hardkey_assist_singletap";

}
