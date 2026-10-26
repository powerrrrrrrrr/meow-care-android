package com.meowcare.app.ui;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.meowcare.app.MainActivity;
import com.meowcare.app.R;
import com.meowcare.app.domain.CareRecord;
import com.meowcare.app.domain.CareTask;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CalendarController {
    private YearMonth month=YearMonth.now();
    private LocalDate selected=LocalDate.now();

    public YearMonth month(){return month;}
    public LocalDate selected(){return selected;}
    public void restore(String date,String savedMonth){if(date!=null)selected=LocalDate.parse(date);if(savedMonth!=null)month=YearMonth.parse(savedMonth);}

    public void render(MainActivity host,LinearLayout parent){
        renderCalendar(host,parent);
        List<CareRecord> selectedRecords=new ArrayList<>();
        for(CareRecord record:host.records())if(record.completedAt().atZone(ZoneId.systemDefault()).toLocalDate().equals(selected))selectedRecords.add(record);
        renderRecordHeader(host,parent,selectedRecords.size());
        if(selectedRecords.isEmpty()){
            LinearLayout empty=Ui.panel(parent);
            empty.addView(Ui.text(host,selected.isAfter(LocalDate.now())?"这一天还没有到来。":"这一天还没有清洁记录。",15,R.color.muted));
            return;
        }
        LinearLayout timeline=Ui.panel(parent);
        for(int i=0;i<selectedRecords.size();i++){
            if(i>0)timeline.addView(Ui.divider(host));
            addRecordRow(host,timeline,selectedRecords.get(i));
        }
    }

    private void renderCalendar(MainActivity host,LinearLayout parent){
        LinearLayout panel=Ui.panel(parent);
        LinearLayout nav=new LinearLayout(host);nav.setGravity(Gravity.CENTER_VERTICAL);
        MaterialButton prev=Ui.iconButton(host,R.drawable.ic_chevron,"上个月",v->{changeMonth(-1);host.render();});prev.setRotation(180);
        MaterialButton next=Ui.iconButton(host,R.drawable.ic_chevron,"下个月",v->{changeMonth(1);host.render();});
        TextView title=Ui.text(host,month.getYear()+"年"+month.getMonthValue()+"月",18,R.color.ink);title.setTypeface(null,Typeface.BOLD);title.setGravity(Gravity.CENTER);
        nav.addView(prev,new LinearLayout.LayoutParams(Ui.dp(host,48),Ui.dp(host,48)));nav.addView(title,new LinearLayout.LayoutParams(0,-2,1));nav.addView(next,new LinearLayout.LayoutParams(Ui.dp(host,48),Ui.dp(host,48)));panel.addView(nav);

        Set<LocalDate> marked=new HashSet<>();for(CareRecord r:host.records())marked.add(r.completedAt().atZone(ZoneId.systemDefault()).toLocalDate());
        LinearLayout grid=new LinearLayout(host);grid.setOrientation(LinearLayout.VERTICAL);panel.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout weekdays=new LinearLayout(host);for(String label:new String[]{"一","二","三","四","五","六","日"}){TextView day=Ui.text(host,label,13,R.color.muted);day.setGravity(Gravity.CENTER);day.setMinHeight(Ui.dp(host,48));weekdays.addView(day,new LinearLayout.LayoutParams(0,-2,1));}grid.addView(weekdays);
        int offset=month.atDay(1).getDayOfWeek().getValue()-1;
        int cells=((offset+month.lengthOfMonth()+6)/7)*7;
        LinearLayout row=null;
        for(int i=0;i<cells;i++){
            if(i%7==0){row=new LinearLayout(host);grid.addView(row,new LinearLayout.LayoutParams(-1,Ui.dp(host,48)));}
            int day=i-offset+1;
            if(day<1||day>month.lengthOfMonth()){View blank=new View(host);blank.setMinimumWidth(Ui.dp(host,48));blank.setMinimumHeight(Ui.dp(host,48));row.addView(blank,new LinearLayout.LayoutParams(0,-2,1));continue;}
            LocalDate date=month.atDay(day);boolean selectedDay=date.equals(selected);boolean showDot=marked.contains(date)&&!selectedDay;AppCompatButton button=new AppCompatButton(host);
            button.setText(host.getString(showDot?R.string.day_number_marked:R.string.day_number,day));button.setTextSize(13);button.setSingleLine(true);button.setPadding(0,0,0,0);button.setMinWidth(Ui.dp(host,48));button.setMinimumWidth(Ui.dp(host,48));button.setMinHeight(Ui.dp(host,48));button.setMinimumHeight(Ui.dp(host,48));button.setGravity(Gravity.CENTER);
            button.setElevation(0);button.setStateListAnimator(null);
            button.setBackground(selectedDay?Ui.circle(host,R.color.primary):Ui.background(host,R.color.surface,23));button.setTextColor(host.getColor(selectedDay?R.color.on_primary:marked.contains(date)?R.color.primary:R.color.ink));
            button.setSelected(selectedDay);button.setContentDescription(date.getYear()+"年"+date.getMonthValue()+"月"+date.getDayOfMonth()+"日"+(marked.contains(date)?"，有记录":"")+(selectedDay?"，已选择":""));button.setOnClickListener(v->{selected=date;host.render();});row.addView(button,new LinearLayout.LayoutParams(0,-2,1));
        }
    }

    private void changeMonth(int months){month=month.plusMonths(months);selected=month.atDay(Math.min(selected.getDayOfMonth(),month.lengthOfMonth()));}

    private void renderRecordHeader(MainActivity host,LinearLayout parent,int count){
        LinearLayout row=new LinearLayout(host);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(Ui.dp(host,4),Ui.dp(host,10),0,Ui.dp(host,10));
        TextView title=Ui.text(host,selected.getMonthValue()+"月"+selected.getDayOfMonth()+"日 · "+count+" 条记录",18,R.color.ink);title.setTypeface(null,Typeface.BOLD);row.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        MaterialButton add=Ui.button(host,"＋ 补记",v->chooseTask(host));Ui.secondary(add);add.setMinWidth(Ui.dp(host,94));add.setMinimumWidth(Ui.dp(host,94));add.setPadding(Ui.dp(host,12),0,Ui.dp(host,12),0);add.setEnabled(!selected.isAfter(LocalDate.now()));row.addView(add,new LinearLayout.LayoutParams(-2,Ui.dp(host,48)));parent.addView(row);
    }

    private void chooseTask(MainActivity host){
        List<CareTask> active=new ArrayList<>();for(CareTask task:host.tasks())if(task.active())active.add(task);
        if(active.isEmpty()){host.message("先到设置添加或恢复任务");return;}
        String[] labels=active.stream().map(task->task.title()+" · "+task.item()).toArray(String[]::new);
        new MaterialAlertDialogBuilder(host).setTitle("选择清洁任务").setItems(labels,(dialog,index)->RecordForms.add(host,active.get(index),selected)).setNegativeButton("取消",null).show();
    }

    private void addRecordRow(MainActivity host,LinearLayout parent,CareRecord record){
        ZonedDateTime completed=record.completedAt().atZone(ZoneId.systemDefault());CareTask task=host.task(record.taskId());
        LinearLayout row=new LinearLayout(host);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Ui.dp(host,10),0,Ui.dp(host,10));
        TextView dot=Ui.text(host,"●",13,R.color.primary);dot.setGravity(Gravity.CENTER);row.addView(dot,new LinearLayout.LayoutParams(Ui.dp(host,28),-1));
        TextView time=Ui.text(host,completed.format(DateTimeFormatter.ofPattern("HH:mm")),15,R.color.ink);row.addView(time,new LinearLayout.LayoutParams(Ui.dp(host,64),-2));
        LinearLayout copy=new LinearLayout(host);copy.setOrientation(LinearLayout.VERTICAL);TextView name=Ui.text(host,task.title(),16,R.color.ink);name.setTypeface(null,Typeface.BOLD);copy.addView(name);copy.addView(Ui.text(host,task.item(),13,R.color.muted));row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(Ui.iconButton(host,R.drawable.ic_edit,"修改"+task.title()+"记录",v->RecordForms.edit(host,task,record)),new LinearLayout.LayoutParams(Ui.dp(host,48),Ui.dp(host,48)));parent.addView(row);
    }
}
