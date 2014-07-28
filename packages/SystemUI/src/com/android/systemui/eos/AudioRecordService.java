
package com.android.systemui.eos;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class AudioRecordService extends Service {
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";

    private static GlobalAudioRecord mAudioRecord;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    final Messenger callback = msg.replyTo;
                    toggleScreenrecord();

                    Message reply = Message.obtain(null, 1);
                    try {
                        callback.send(reply);
                    } catch (RemoteException e) {
                    }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction().equals(ACTION_START)) {
                startAudioRecord();
            } else if (intent.getAction().equals(ACTION_STOP)) {
                stopAudioRecord();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Messenger(mHandler).getBinder();
    }

    private void startAudioRecord() {
        if (mAudioRecord == null) {
            mAudioRecord = new GlobalAudioRecord(AudioRecordService.this);
        }
        mAudioRecord.startRecording();
    }

    private void stopAudioRecord() {
        if (mAudioRecord == null) {
            return;
        }
        mAudioRecord.stopRecording();
    }

    private void toggleScreenrecord() {
        if (mAudioRecord == null || !mAudioRecord.isRecording()) {
            startAudioRecord();
        } else {
            stopAudioRecord();
        }
    }
}
