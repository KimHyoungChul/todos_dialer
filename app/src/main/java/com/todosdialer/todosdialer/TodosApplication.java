package com.todosdialer.todosdialer;

import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.todosdialer.todosdialer.manager.PushManager;
import com.todosdialer.todosdialer.manager.RealmManager;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class TodosApplication extends MultiDexApplication {
    private static boolean mOnCalling = false;

    public static void onCalling(boolean onCalling) {
        TodosApplication.mOnCalling = onCalling;
    }

    public static boolean onCalling() {
        return TodosApplication.mOnCalling;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(getApplicationContext());

        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();

        Realm.setDefaultConfiguration(config);

        PushManager.createChannel(this);


        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                RealmManager.newInstance().writeLog("[Todos] Exception: " + Log.getStackTraceString(e));
            }
        });
    }
}
