package com.todosdialer.todosdialer.manager;

import android.os.Handler;
import android.os.Looper;

import com.squareup.otto.Bus;

public class BusManager {
    private static final MainThreadBus mBus = new MainThreadBus();

    public static MainThreadBus getInstance() {
        return mBus;
    }

    private BusManager() {
        // No instances.
    }

    public static void post(Object event) {
        getInstance().post(event);
    }

    public static class MainThreadBus extends Bus {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void post(final Object event) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                super.post(event);
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        MainThreadBus.super.post(event);
                    }
                });
            }
        }
    }
}
