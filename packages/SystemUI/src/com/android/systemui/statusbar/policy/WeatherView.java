package com.android.systemui.statusbar.policy;

import org.codefirex.utils.CFXConstants;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Bundle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.systemui.R;

public class WeatherView extends TextView {
	private static final String TAG = "WeatherView";

	private static final String WEATHER_ACTION = "cfx_weather_update";
	private static final String WEATHER_EXTRA = "weather";
	private static final String WEATHER_SERVICE_STATE = "cfx_weather_service_state";

	private static final String QUERY_ACTION = "cfx_query_weather_service";
	private static final String QUERY_ENABLED = "cfx_query_weather_service_enabled";

	String mWeatherString = "";
	boolean mServiceEnabled = false;
	boolean mViewEnabled = false;

	Context mContext;
	Handler mHandler;
	SettingsObserver mObserver;

	public WeatherView(Context context) {
		this(context, null);
	}

	public WeatherView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WeatherView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		mHandler = new Handler();
		mObserver = new SettingsObserver(mHandler);
		mObserver.observe();
		IntentFilter filter = new IntentFilter();
		filter.addAction(WEATHER_ACTION);
		context.registerReceiver(mWeatherReceiver, filter);
		context.sendBroadcast(new Intent().setAction(QUERY_ACTION).putExtra(
				QUERY_ENABLED, "enabled"));
	}

	class SettingsObserver extends ContentObserver {
		public SettingsObserver(Handler handler) {
			super(handler);
		}

		void observe() {
			ContentResolver resolver = mContext.getContentResolver();
			resolver.registerContentObserver(Settings.System.getUriFor(
					CFXConstants.SYSTEMUI_WEATHER_HEADER_VIEW), false, this);
			onChange(true);
		}

		@Override
		public void onChange(boolean selfChange) {
			ContentResolver resolver = mContext.getContentResolver();
			mViewEnabled = Settings.System.getBoolean(resolver,
					CFXConstants.SYSTEMUI_WEATHER_HEADER_VIEW, false);
			updateVisibility();
		}
	}

	BroadcastReceiver mWeatherReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (WEATHER_ACTION.equals(action)) {
				if (intent.getBundleExtra(WEATHER_EXTRA) != null) {
					handleIncomingBundle(intent.getBundleExtra(WEATHER_EXTRA));
				} else if (intent.getStringExtra(WEATHER_SERVICE_STATE) != null) {
					mServiceEnabled = Boolean.valueOf(intent
							.getStringExtra(WEATHER_SERVICE_STATE));
					mObserver.onChange(true);
				}
			}
		}
	};

	private void updateVisibility() {
		setVisibility((mServiceEnabled && mViewEnabled) ? VISIBLE : INVISIBLE);
	}

	private void handleIncomingBundle(Bundle b) {
		String temp = b.getString("temp");
		String weather = b.getString("weather");
		StringBuilder bb = new StringBuilder().append(weather).append(" ")
				.append(temp);
		mWeatherString = bb.toString();
		setText(mWeatherString);
	}
}
