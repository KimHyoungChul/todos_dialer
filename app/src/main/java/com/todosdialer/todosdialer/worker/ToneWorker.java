package com.todosdialer.todosdialer.worker;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ToneWorker extends Thread {
    private static final int TONE_DURATION = 100;
    private static final String DIAL_0 = "0";
    private static final String DIAL_1 = "1";
    private static final String DIAL_2 = "2";
    private static final String DIAL_3 = "3";
    private static final String DIAL_4 = "4";
    private static final String DIAL_5 = "5";
    private static final String DIAL_6 = "6";
    private static final String DIAL_7 = "7";
    private static final String DIAL_8 = "8";
    private static final String DIAL_9 = "9";
    private static final String DIAL_STAR = "*";
    private static final String DIAL_SHOP = "#";

    private Queue<String> mDialerQueue;
    private AudioManager mAudioManager;

    private boolean mHasToSound = false;
    private boolean mIsRunning;

    public ToneWorker(Context context) {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mDialerQueue = new ConcurrentLinkedQueue<>();
    }

    public void addDialer(String dial) {
        mDialerQueue.add(dial);

    }

    public void release() {
        mIsRunning = false;
        mDialerQueue.clear();

    }

    @Override
    public void run() {
        mIsRunning = true;
        while (mIsRunning) {
            if (mAudioManager != null) {
                mHasToSound = mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL ||
                        mAudioManager.getMode() != AudioManager.MODE_IN_CALL ||
                        mAudioManager.getMode() != AudioManager.MODE_IN_COMMUNICATION;
            }
            ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_DTMF, ToneGenerator.MAX_VOLUME / 2);
            String dial = mDialerQueue.poll();

            generateTone(toneGenerator, dial);

            try {
                Thread.sleep(TONE_DURATION / 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                toneGenerator.release();
            } catch (Exception ignored) {

            }
        }
    }

    private void generateTone(ToneGenerator toneGenerator, String dial) {
        if (mHasToSound) {
            try {
                if (DIAL_0.equals(dial)) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_0, TONE_DURATION);
                } else if (DIAL_1.equals(dial)) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, TONE_DURATION);
                } else if (DIAL_2.equals(dial)) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_2, TONE_DURATION);
                } else if (DIAL_3.equals(dial)) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_3, TONE_DURATION);
                } else if (DIAL_4.equals(dial)) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_4, TONE_DURATION);
                } else if (DIAL_5.equals(dial)) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_5, TONE_DURATION);
                } else if (DIAL_6.equals(dial)) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_6, TONE_DURATION);
                } else if (DIAL_7.equals(dial)) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_7, TONE_DURATION);
                } else if (DIAL_8.equals(dial)) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_8, TONE_DURATION);
                } else if (DIAL_9.equals(dial)) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, TONE_DURATION);
                } else if (DIAL_STAR.equals(dial)) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_S, TONE_DURATION);
                } else if (DIAL_SHOP.equals(dial)) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_P, TONE_DURATION);
                }
            } catch (Exception ignored) {
            }
        }
    }
}