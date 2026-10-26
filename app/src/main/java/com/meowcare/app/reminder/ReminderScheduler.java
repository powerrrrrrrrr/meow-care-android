package com.meowcare.app.reminder;

import android.Manifest;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import java.time.*;

public final class ReminderScheduler {
    public static final String CHANNEL="cleaning_reminders";
    public static final String ACTION="com.meowcare.app.DAILY_REMINDER";
    private ReminderScheduler(){}
    public static void ensureChannel(Context context){
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        NotificationChannel channel=new NotificationChannel(CHANNEL,"每日清洁提醒",NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("汇总到期的猫咪清洁任务");manager.createNotificationChannel(channel);
    }
    public static boolean canNotify(Context context){
        if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
        NotificationManager manager=context.getSystemService(NotificationManager.class);NotificationChannel channel=manager.getNotificationChannel(CHANNEL);
        return manager.areNotificationsEnabled()&&(channel==null||channel.getImportance()!=NotificationManager.IMPORTANCE_NONE);
    }
    private static String config(ReminderSettings settings, ZoneId zone){return settings.hour()+":"+settings.minute()+"@"+zone.getId();}
    public static synchronized void reset(Context context){new ReminderSettings(context).clearPending();schedule(context);}
    public static synchronized boolean consume(Context context,Intent intent){
        ReminderSettings settings=new ReminderSettings(context);ZoneId zone=ZoneId.systemDefault();
        Instant pending=settings.pending(config(settings,zone));
        Instant now=Instant.now();
        if(!settings.enabled()||pending==null||intent.getLongExtra("occurrence",Long.MIN_VALUE)!=pending.toEpochMilli()||pending.isAfter(now))return false;
        boolean eligible=ReminderPolicy.deliveryEligible(now,pending,zone);
        // Consume even when permission is denied or no chores are due; otherwise it would loop.
        settings.clearPending();schedule(context);return eligible;
    }
    public static synchronized void schedule(Context context){
        ensureChannel(context);ReminderSettings settings=new ReminderSettings(context);
        AlarmManager alarms=context.getSystemService(AlarmManager.class);
        Intent intent=new Intent(context,ReminderReceiver.class).setAction(ACTION);
        if(!settings.enabled()){
            PendingIntent pending=PendingIntent.getBroadcast(context,1,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            settings.clearPending();alarms.cancel(pending);context.getSystemService(NotificationManager.class).cancel(1);return;
        }
        ZoneId zone=ZoneId.systemDefault();String config=config(settings,zone);
        Instant next=ReminderPolicy.scheduledOccurrence(Instant.now(),LocalTime.of(settings.hour(),settings.minute()),zone,settings.pending(config));
        settings.savePending(next,config);
        PendingIntent pending=PendingIntent.getBroadcast(context,1,intent.putExtra("occurrence",next.toEpochMilli()),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.toEpochMilli(),pending);
    }
}
