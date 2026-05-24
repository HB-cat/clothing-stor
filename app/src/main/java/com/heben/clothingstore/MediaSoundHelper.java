package com.heben.clothingstore;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;

public class MediaSoundHelper {

    private static MediaSoundHelper instance;
    private MediaPlayer mediaPlayer;

    private MediaSoundHelper() {}

    public static MediaSoundHelper getInstance() {
        if (instance == null) {
            instance = new MediaSoundHelper();
        }
        return instance;
    }

    public void init(Context context) {}

    public boolean isSoundEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("clothing_store_prefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("sound_enabled", true);
    }

    public void setSoundEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences("clothing_store_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("sound_enabled", enabled).apply();
    }

    private void playSound(Context context, int rawResId) {
        if (!isSoundEnabled(context)) return;
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = MediaPlayer.create(context, rawResId);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playSale(Context context) { playSound(context, R.raw.sale_success); }
    public void playPurchase(Context context) { playSound(context, R.raw.purchase_success); }
    public void playRefund(Context context) { playSound(context, R.raw.refund_success); }
    public void playDelete(Context context) { playSound(context, R.raw.delete_success); }
    public void playSuccess(Context context) { playSound(context, R.raw.general_success); }
    public void playError(Context context) { playSound(context, R.raw.error); }
    public void playConfirm(Context context) { playSound(context, R.raw.confirm_pop); }
    public void playCancel(Context context) { playSound(context, R.raw.cancel); }
    public void playClick(Context context) { playSound(context, R.raw.click); }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}