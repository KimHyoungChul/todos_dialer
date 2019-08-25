package com.todosdialer.todosdialer.manager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.todosdialer.todosdialer.MainActivity;
import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.service.TodosService;

public class PushManager {
    public static final String CHANNEL_SERVICE_ID = "com.todosdialer.todosdialer.service.TodosService";
    private static final String CHANNEL_MESSAGE_ID = "com.todosdialer.todosdialer.manager.PushManager.message";
    private static final String CHANNEL_CALL_ID = "com.todosdialer.todosdialer.manager.PushManager.call";


    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channelService = new NotificationChannel(CHANNEL_SERVICE_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            channelService.setDescription(context.getString(R.string.app_name));
            channelService.setLightColor(Color.BLUE);
            channelService.setShowBadge(false);
            channelService.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            getManager(context).createNotificationChannel(channelService);

            NotificationChannel channelMessage = new NotificationChannel(CHANNEL_MESSAGE_ID, context.getString(R.string.app_name), android.app.NotificationManager.IMPORTANCE_DEFAULT);
            channelMessage.setDescription(context.getString(R.string.notification_channel_message_description));
            channelMessage.setLightColor(Color.BLUE);
            channelMessage.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            getManager(context).createNotificationChannel(channelMessage);

            NotificationChannel channelCall = new NotificationChannel(CHANNEL_CALL_ID, context.getString(R.string.app_name), android.app.NotificationManager.IMPORTANCE_DEFAULT);
            channelCall.setDescription(context.getString(R.string.notification_channel_comment_description));
            channelCall.setLightColor(Color.BLUE);
            channelCall.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getManager(context).createNotificationChannel(channelCall);
        }
    }

    public static void sendMessageNotification(Context context, int id, String title, String body) {
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(context, CHANNEL_MESSAGE_ID)
                    .setContentIntent(makeIntent(context, MainActivity.TAB_SMS))
                    .setContentTitle(title)
                    .setLights(0xff00ff00, 300, 1000)
                    .setContentText(body)
                    .setSmallIcon(R.drawable.ic_send_white_24dp)
                    .setAutoCancel(true)
                    .build();
        } else {
            notification = new NotificationCompat.Builder(context)
                    .setContentIntent(makeIntent(context, MainActivity.TAB_SMS))
                    .setContentTitle(title)
                    .setLights(0xff00ff00, 300, 1000)
                    .setContentText(body)
                    .setSmallIcon(R.drawable.ic_send_white_24dp)
                    .setAutoCancel(true)
                    .build();
        }

        getManager(context).notify(id, notification);
    }

    public static void sendMissCallNotification(Context context, int id, String title, String body) {
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(context, CHANNEL_CALL_ID)
                    .setContentIntent(makeIntent(context, MainActivity.TAB_CALL_LOG))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setLights(0xff00ff00, 300, 1000)
                    .setSmallIcon(R.drawable.ic_call_white_24dp)
                    .setAutoCancel(true)
                    .build();
        } else {
            notification = new NotificationCompat.Builder(context)
                    .setContentIntent(makeIntent(context, MainActivity.TAB_CALL_LOG))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setLights(0xff00ff00, 300, 1000)
                    .setSmallIcon(R.drawable.ic_call_white_24dp)
                    .setAutoCancel(true)
                    .build();
        }

        getManager(context).notify(id, notification);
    }

    private static PendingIntent makeIntent(Context context, int tab) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(MainActivity.EXTRA_KEY_TAB, tab);

        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void sendOngoingCallPush(Context context,Intent intent, String phoneNumber, String msg) {
        BusManager.getInstance().post(new TodosService.Request("PushManager", TodosService.Request.REQUEST_UPDATE_FOREGROUND_TO_CALL, phoneNumber, intent));

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(context, CHANNEL_CALL_ID)
                    .setSmallIcon(R.drawable.ic_call_white_24dp)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setOngoing(true)
                    .setLights(0xff00ff00, 300, 1000)
                    .setAutoCancel(false)
                    .setContentText(msg)
                    .setContentIntent(contentIntent)
                    .build();
        } else {
            notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_call_white_24dp)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setLights(0xff00ff00, 300, 1000)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setAutoCancel(false)
                    .setContentText(msg)
                    .setContentIntent(contentIntent)
                    .build();
        }

        getManager(context).notify(TodosService.CALL_NOTI_ONGOING_ID, notification);
    }

    public static void clearOngoingCall(Context context) {
        BusManager.getInstance().post(new TodosService.Request("PushManager", TodosService.Request.REQUEST_UPDATE_FOREGROUND_TO_MAIN, ""));

        getManager(context).cancel(TodosService.CALL_NOTI_ONGOING_ID);
    }

    private static NotificationManager getManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static void clearAll(Context context) {
        getManager(context).cancelAll();
    }
}
