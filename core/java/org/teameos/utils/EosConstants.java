
package org.teameos.utils;

public final class EosConstants {
    /* Eos SETTINGS STRINGS */

	/**
	 * @hide
	 * EOS intent actions and keys
	 */	

    /**
     * @hide
     */
    public static final String ACTION_EOS_INTERNAL_ACTIVITY = "eos_internal_activity";

    /**
     * @hide
     */
    public static final String INTENT_EXTRA_INTERNAL_ACTIVITY = "eos_intent_extra_internal_activity";

    /**
     * @hide
     */
    public static final int SYSTEMUI_NAVBAR_SIZE_DEF_INDEX = 8;

    /**
     * @hide
     */
    public static final String SYSTEMUI_NAVBAR_SIZE_DP = "eos_reason_navbar_size_dp";

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
     *
     * @hide
     */
    public static final String SYSTEMUI_STATUSBAR_DRAG_BRIGHTNESS = "eos_systemui_statusbar_drag_brightness";

    /**
     * custom color for status bar
     * default is -1, indicating default color or to check for drawable
     *
     * @hide
     */
    public static final String SYSTEMUI_STATUSBAR_COLOR = "eos_systemui_statusbar_color";

    /**
     * custom color for navigation bar or system bar
     * default is -1
     *
     * @hide
     */
    public static final String SYSTEMUI_BOTTOM_BAR_COLOR = "eos_systemui_bottom_bar_color";

    /**
     * @hide
     */
    public static final String SYSTEMUI_BATTERY_ICON_VISIBLE = "eos_systemui_battery_icon_visible";

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
    public static final String SYSTEMUI_BATTERY_TEXT_COLOR = "eos_systemui_battery_text_color";

    /**
     * @hide
     */
    public static final int SYSTEMUI_BATTERY_TEXT_COLOR_DEF = -1;

    /**
     * @hide
     */
    public static final String SYSTEMUI_CLOCK_VISIBLE = "eos_systemui_clock_visible";

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
    public static final String SYSTEMUI_CLOCK_COLOR = "eos_systemui_clock_color";

    /**
     * @hide
     */
    public static final int SYSTEMUI_CLOCK_COLOR_DEF = -1;

    /**
     * @hide
     */
    public static final String SYSTEMUI_CLOCK_AMPM = "eos_systemui_clock_ampm";

    /**
     * @hide
     */
    public static final int SYSTEMUI_CLOCK_AMPM_DEF = 2;

    /**
     * @hide
     */
    public static final String SYSTEMUI_CLOCK_SIGNAL_CLUSTER_TAG = "eos_systemui_clock_cluster_tag";

    /**
     * @hide
     */
    public static final String SYSTEMUI_CLOCK_CENTER_TAG = "eos_systemui_clock_center_tag";

    /**
     * @hide
     */
    public static final String SYSTEMUI_WEATHER_HEADER_VIEW = "eos_systemui_header_weather_view";

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
    public static final String DEVICE_SETTINGS_TOUCHPAD_STATUS = "eos_device_settings_touchpad_status";

    /**
     * The touchpad gesture mode. (0 = spots, 1 = pointer)
     *
     * @hide
     */
    public static final String DEVICE_SETTINGS_TOUCHPAD_MODE = "eos_device_settings_touchpad_mode";

    /**
     * Value for {@link #eos_TOUCHPAD_STATUS} to use the touchpad located on the
     * hardware keyboard dock.
     *
     * @hide
     */
    public static final int DEVICE_SETTINGS_TOUCHPAD_DISABLED = 0;

    /**
     * Value for {@link #eos_TOUCHPAD_STATUS} to use the touchpad located on the
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
