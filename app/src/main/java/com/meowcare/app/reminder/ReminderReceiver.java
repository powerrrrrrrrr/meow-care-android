package com.meowcare.app.reminder;

import android.app.*;
import android.content.*;
import android.util.Log;
import com.meowcare.app.CareApplication;
import com.meowcare.app.MainActivity;
import com.meowcare.app.R;
import com.meowcare.app.domain.*;
import java.time.*;
import java.util.*;

public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(!ReminderScheduler.ACTION.equals(intent.getAction())){
            if(Intent.ACTION_TIME_CHANGED.equals(intent.getAction())||Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction()))ReminderScheduler.reset(context);
            else ReminderScheduler.schedule(context);
            return;
        }
        if(!ReminderScheduler.consume(context,intent))return;
        PendingResult result=goAsync();CareApplication app=(CareApplication)context.getApplicationContext();
        app.io().execute(()->{
            try{
                ReminderSettings settings=new ReminderSettings(context);LocalDate today=LocalDate.now();
                if(!settings.enabled()||settings.sentOn(today)||!ReminderScheduler.canNotify(context))return;
                List<String> due=new ArrayList<>();Map<Long,Instant> latest=new HashMap<>();
                for(CareRecord r:app.repository().getAllRecords())latest.merge(r.taskId(),r.completedAt(),(a,b)->a.isAfter(b)?a:b);
                for(CareTask task:app.repository().getAllTasks())if(task.active()&&task.remind()){
                    TaskSnapshot snapshot=TaskSnapshot.from(task,latest.get(task.id()),today,ZoneId.systemDefault());
                    if(snapshot.status()!=TaskStatus.OK)due.add(task.title()+" · "+task.item());
                }
                if(due.isEmpty())return;
                // Recheck settings after IO, because the user may have disabled reminders meanwhile.
                if(!settings.enabled()||!ReminderScheduler.canNotify(context))return;
                PendingIntent open=PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                Notification notification=new Notification.Builder(context,ReminderScheduler.CHANNEL).setSmallIcon(R.drawable.ic_care)
                        .setContentTitle("有 "+due.size()+" 件清洁值得看一眼").setContentText(due.get(0))
                        .setStyle(new Notification.BigTextStyle().bigText(String.join("\n",due)))
                        .setContentIntent(open).setAutoCancel(true).build();
                context.getSystemService(NotificationManager.class).notify(1,notification);
                if(!settings.markSent(today))Log.w("MeowCare","Could not persist reminder deduplication date");
            }catch(Exception e){Log.e("MeowCare","Reminder failed",e);}
            finally{try{ReminderScheduler.schedule(context);}finally{result.finish();}}
        });
    }
}
