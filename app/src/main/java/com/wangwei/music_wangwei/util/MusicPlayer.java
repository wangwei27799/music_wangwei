package com.wangwei.music_wangwei.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

/**
 * 音乐播放器
 */
public class MusicPlayer implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener {

    private Context context;
    private MediaPlayer mediaPlayer;
    private OnCompletionListener completionListener;
    private OnPreparedListener preparedListener;
    private PowerManager.WakeLock wakeLock;
    private PowerManager powerManager;
    private static final String TAG = "MusicPlayer";

    public MusicPlayer(Context context) {
        this.context = context;
        initMediaPlayer();
        initWakeLock(context);
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    private void initWakeLock(Context context) {
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MusicPlayer:WakeLock");
    }

    public void playMusic(String musicUrl) {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(musicUrl);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "播放音乐失败", e);
        }
    }

    public void play() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    public void seekTo(int position) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(position);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public void release() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        if (preparedListener != null) {
            preparedListener.onPrepared(mp);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (completionListener != null) {
            completionListener.onCompletion();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer错误: what=" + what + ", extra=" + extra);
        if (completionListener != null) {
            completionListener.onError();
        }
        return true;
    }

    public interface OnCompletionListener {
        void onCompletion();
        void onError();
    }

    public interface OnPreparedListener {
        void onPrepared(MediaPlayer mp);
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        this.completionListener = listener;
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        this.preparedListener = listener;
    }
}