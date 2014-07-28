
package com.android.systemui.eos;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.android.systemui.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class GlobalAudioRecord {
    private static final String TAG = GlobalAudioRecord.class.getSimpleName();

    private static final int AUDIORECORD_NOTIFICATION_ID = 69;
    private static final String TMP_PATH = "/sdcard/__tmp_audiorecord.amr";

    private Context mContext;
    private NotificationManager mNotificationManager;

    private MediaRecorder mRecorder = null;

    public GlobalAudioRecord(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public boolean isRecording() {
        return (mRecorder != null);
    }

    public void startRecording() {
        if (mRecorder != null) {
            Log.e(TAG, "Audio recorder is already running, ignoring audio record start request");
            return;
        }

        File temp = new File(TMP_PATH);
        try {
            if (temp.exists()) {
                temp.delete();
            }
            temp.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "Cannot create temp file");
            return;
        }

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(temp.getAbsolutePath());

        // Bail out on any error
        try {
            mRecorder.prepare();
        } catch (Exception exception) {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return;
        }

        try {
            mRecorder.start();
        } catch (Exception exception) {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return;
        }

        final Resources r = mContext.getResources();

        // Display a notification
        Notification.Builder builder = new Notification.Builder(mContext)
                .setTicker(r.getString(R.string.audiorecord_notif_ticker))
                .setContentTitle(r.getString(R.string.audiorecord_notif_title))
                .setSmallIcon(R.drawable.ic_sysbar_voiceassist)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true);

        Intent stopIntent = new Intent(mContext, AudioRecordService.class)
                .setAction(AudioRecordService.ACTION_STOP);
        PendingIntent stopPendIntent = PendingIntent.getService(mContext, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(com.android.internal.R.drawable.ic_media_stop,
                r.getString(R.string.audiorecord_notif_stop), stopPendIntent);
        Notification notif = builder.build();
        mNotificationManager.notify(AUDIORECORD_NOTIFICATION_ID, notif);
    }

    public void stopRecording() {
        if (mRecorder == null)
            return;

        mNotificationManager.cancel(AUDIORECORD_NOTIFICATION_ID);

        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        File input = new File(TMP_PATH);

        if (!input.exists())
            return;

        String fileName = "AUDIO_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
                + ".amr";
        File audio = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File audiorecords = new File(audio, "Audiorecords");

        if (!audiorecords.exists()) {
            if (!audiorecords.mkdir()) {
                Log.e(TAG, "Cannot create Audiorecords directory");
                return;
            }
        }

        final File output = new File(audiorecords, fileName);

        Log.d(TAG, "Copying file to " + output.getAbsolutePath());

        try {
            copyFileUsingStream(input, output);
            input.delete();
        } catch (IOException e) {
            Log.e(TAG, "Unable to copy output file", e);
            return;
        }

        // Make it appear in audio player, run MediaScanner
        MediaScannerConnection.scanFile(mContext,
                new String[] {
                    output.getAbsolutePath()
                }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i(TAG, "MediaScanner done scanning " + path);
                    }
                });
    }

    private static void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }
}
