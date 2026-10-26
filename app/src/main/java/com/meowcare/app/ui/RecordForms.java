package com.meowcare.app.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.meowcare.app.MainActivity;
import com.meowcare.app.R;
import com.meowcare.app.domain.CareRecord;
import com.meowcare.app.domain.CareRules;
import com.meowcare.app.domain.CareTask;
import com.meowcare.app.domain.RecordTime;
import java.time.*;
import java.time.format.DateTimeFormatter;

public final class RecordForms {
    private RecordForms(){}
    public static void add(MainActivity host,CareTask task,LocalDate date){show(host,task,null,date.atTime(LocalTime.now().withSecond(0).withNano(0)));}
    public static void edit(MainActivity host,CareTask task,CareRecord record){show(host,task,record,record.completedAt().atZone(ZoneId.systemDefault()).toLocalDateTime());}
    private static void show(MainActivity host,CareTask task,CareRecord record,LocalDateTime initial){
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime[] chosen={initial};LinearLayout form=new LinearLayout(host);form.setOrientation(LinearLayout.VERTICAL);int padding=Ui.dp(host,24);form.setPadding(padding,0,padding,0);
        form.addView(Ui.text(host,task.title()+" · "+task.item(),16,R.color.muted));
        MaterialButton date=Ui.button(host,"",null),time=Ui.button(host,"",null);Ui.secondary(date);Ui.secondary(time);form.addView(date);form.addView(time);
        Runnable labels=()->{date.setText(chosen[0].toLocalDate().toString());time.setText(chosen[0].format(DateTimeFormatter.ofPattern("HH:mm")));};labels.run();
        date.setOnClickListener(v->{DatePickerDialog picker=new DatePickerDialog(host,(view,y,m,d)->{chosen[0]=LocalDate.of(y,m+1,d).atTime(chosen[0].toLocalTime());labels.run();},chosen[0].getYear(),chosen[0].getMonthValue()-1,chosen[0].getDayOfMonth());picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();});
        time.setOnClickListener(v->new TimePickerDialog(host,(view,h,m)->{chosen[0]=chosen[0].toLocalDate().atTime(h,m);labels.run();},chosen[0].getHour(),chosen[0].getMinute(),true).show());
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(host).setTitle(record==null?"补记清洁":"修改清洁记录").setView(form).setNegativeButton("取消",null).setPositiveButton("保存",null);
        if(record!=null)builder.setNeutralButton("删除",null);
        AlertDialog dialog=builder.create();dialog.setOnShowListener(shown->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                Instant instant=RecordTime.resolve(chosen[0],zone,record==null?null:record.completedAt());
                try{CareRules.validateCompletion(instant,Instant.now());}catch(IllegalArgumentException ex){host.message(ex.getMessage());return;}
                v.setEnabled(false);
                host.work(()->{if(record==null)host.repository().insertRecord(task.id(),instant,Instant.now());else host.repository().updateRecord(record.id(),instant,Instant.now());return null;},ignored->{dialog.dismiss();host.refresh();host.message("记录已保存");},()->v.setEnabled(true));
            });
            if(record!=null)dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->new MaterialAlertDialogBuilder(host).setTitle("删除这条记录？").setMessage("删除后将按其余记录重新计算上次完成时间。").setNegativeButton("取消",null).setPositiveButton("删除",(confirm,which)->{dialog.dismiss();host.mutate(()->{host.repository().deleteRecord(record.id());return null;},"记录已删除");}).show());
        });dialog.show();
    }
}
