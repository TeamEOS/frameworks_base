package com.android.systemui.quicksettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.codefirex.utils.CFXUtils;

import android.content.Context;
import android.os.FileObserver;
import android.util.Log;
import android.view.View;

import com.android.systemui.statusbar.phone.QuickSettingsController;

public abstract class FileObserverTile extends QuickSettingsTile {
	protected static String TAG = FileObserverTile.class.getSimpleName();
	protected TwoStateTileRes mTileRes;
	protected boolean mFeatureEnabled;
	protected String mFilePath;

	private FileObserver mObserver;

	// keep FileObserver onEvent() callback thread safe
	private final Runnable mFileChangedRunnable = new Runnable() {
		@Override
		public void run() {
			updateEnabled();
			updateResources();
			onFileChanged(mFeatureEnabled);
		}		
	};

	public FileObserverTile(Context context, QuickSettingsController qsc) {
		super(context, qsc);
		updateEnabled();
		mOnClick = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleState();
				updateEnabled();
				updateResources();
			}
		};
	}

	@Override
	void onPostCreate() {
		mFilePath = getFilePath();
		mObserver = new FileObserver(mFilePath, FileObserver.MODIFY) {
			@Override
			public void onEvent(int event, String file) {
				mStatusbarService.getHandler().post(mFileChangedRunnable);
			}
		};
		mObserver.startWatching();
		mTileRes = getTileRes();
		updateEnabled();
		updateTile();
        super.onPostCreate();
	}

    @Override
    public void onDestroy() {
    	mObserver.stopWatching();
    	super.onDestroy();
    }

	@Override
	public void updateResources() {
		updateTile();
		super.updateResources();
	}

	protected abstract String getFilePath();

	protected abstract TwoStateTileRes getTileRes();

	protected void setEnabled(boolean enabled) {
		CFXUtils.setKernelFeatureEnabled(mFilePath, enabled);
	}

	private synchronized void updateTile() {
		mLabel = mContext.getString(mFeatureEnabled ? mTileRes.mTileOnLabel
				: mTileRes.mTileOffLabel);
		mDrawable = mFeatureEnabled ? mTileRes.mTileOnDrawable
				: mTileRes.mTileOffDrawable;
	}

	protected void toggleState() {
		updateEnabled();
		setEnabled(!mFeatureEnabled);
	}

	/**
	 * subclasses can override onFileChanged() to hook
	 * into the FileObserver onEvent() callback
	 */

	protected void onFileChanged(boolean featureState){}

	protected void updateEnabled() {
		mFeatureEnabled = isFeatureOn();
	}

	protected boolean isFeatureOn() {
		if (mFilePath == null || mFilePath.isEmpty()) {
			return false;
		}
		File file = new File(mFilePath);
		if (!file.exists()) {
			return false;
		}
		String content = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			content = reader.readLine();
			Log.i(TAG, "isFeatureOn(): content: " + content);
			return "1".equals(content) || "Y".equalsIgnoreCase(content)
					|| "on".equalsIgnoreCase(content);
		} catch (Exception e) {
			Log.i(TAG, "exception reading feature file", e);
			return false;
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				// ignore
			}
		}
	}

	protected static class TwoStateTileRes {
		int mTileOnLabel;
		int mTileOffLabel;
		int mTileOnDrawable;
		int mTileOffDrawable;

		public TwoStateTileRes(int labelOn, int labelOff, int drawableOn,
				int drawableOff) {
			mTileOnLabel = labelOn;
			mTileOffLabel = labelOff;
			mTileOnDrawable = drawableOn;
			mTileOffDrawable = drawableOff;
		}
	}
}
