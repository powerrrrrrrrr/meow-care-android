package com.meowcare.app.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import com.meowcare.app.MainActivity;
import com.meowcare.app.R;
import com.meowcare.app.domain.TaskSnapshot;
import com.meowcare.app.domain.TaskStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class TodayController {
    public void render(MainActivity host, LinearLayout parent, List<TaskSnapshot> tasks) {
        long due=tasks.stream().filter(t->t.status()==TaskStatus.OVERDUE||t.status()==TaskStatus.DUE_TODAY).count();
        long unrecorded=tasks.stream().filter(t->t.status()==TaskStatus.UNRECORDED).count();
        LinearLayout banner=Ui.panel(parent); banner.setBackground(Ui.background(host,R.color.soft,18));
        TextView headline=Ui.text(host,due==0?"今天没有到期任务":"今天有 "+due+" 项到期",18,R.color.ink);headline.setTypeface(null,android.graphics.Typeface.BOLD);banner.addView(headline);
        banner.addView(Ui.text(host,unrecorded==0?"照顾都有记录，继续保持":"还有 "+unrecorded+" 项尚未记录",14,R.color.muted));
        if(tasks.isEmpty()) { parent.addView(Ui.text(host,"还没有启用的任务。去设置添加或恢复一项吧。",16,R.color.muted)); return; }
        for(TaskSnapshot snapshot:tasks) {
            View card=host.getLayoutInflater().inflate(R.layout.item_task,parent,false);
            ((TextView)card.findViewById(R.id.task_title)).setText(snapshot.task().title());
            ((TextView)card.findViewById(R.id.task_item)).setText(host.getString(R.string.item_interval,snapshot.task().item(),snapshot.task().intervalDays()));
            ((ImageView)card.findViewById(R.id.task_icon)).setImageResource(Ui.taskIcon(snapshot.task().title()));
            TextView status=card.findViewById(R.id.task_status); status.setText(statusLabel(snapshot));
            int foreground=snapshot.status()==TaskStatus.OK?R.color.success:snapshot.status()==TaskStatus.UNRECORDED?R.color.neutral:snapshot.status()==TaskStatus.OVERDUE?R.color.danger:R.color.warning;
            int background=snapshot.status()==TaskStatus.OK?R.color.success_bg:snapshot.status()==TaskStatus.UNRECORDED?R.color.neutral_bg:snapshot.status()==TaskStatus.OVERDUE?R.color.danger_bg:R.color.warning_bg;
            status.setTextColor(host.getColor(foreground));status.setBackground(Ui.background(host,background,10));
            ((TextView)card.findViewById(R.id.task_last)).setText(snapshot.latestDate()==null?"记下第一次清洁":"上次 "+snapshot.latestDate()+" · 已过 "+snapshot.daysSince()+" 天");
            View done=card.findViewById(R.id.task_done);
            done.setContentDescription("完成"+snapshot.task().title());
            done.setOnClickListener(v->host.complete(snapshot.task(),v));
            View backfill=card.findViewById(R.id.task_backfill);backfill.setContentDescription("补记"+snapshot.task().title());backfill.setOnClickListener(v->RecordForms.add(host,snapshot.task(),LocalDate.now()));
            parent.addView(card);
        }
    }
    private String statusLabel(TaskSnapshot s) {
        switch(s.status()) {
            case UNRECORDED:return "尚未记录";
            case OVERDUE:return "已超 "+ChronoUnit.DAYS.between(s.dueDate(),LocalDate.now())+" 天";
            case DUE_TODAY:return "今天到期";
            default:return s.daysSince()==0?"今天已完成":"还有 "+ChronoUnit.DAYS.between(LocalDate.now(),s.dueDate())+" 天到期";
        }
    }
}
