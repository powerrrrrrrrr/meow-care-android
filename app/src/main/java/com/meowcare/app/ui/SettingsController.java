package com.meowcare.app.ui;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.meowcare.app.MainActivity;
import com.meowcare.app.AppIconActivity;
import com.meowcare.app.appearance.AppIcons;
import com.meowcare.app.R;
import com.meowcare.app.domain.CareTask;
import com.meowcare.app.reminder.ReminderScheduler;
import com.meowcare.app.reminder.ReminderSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SettingsController {
    public void render(MainActivity host,LinearLayout parent){
        sectionHeader(host,parent,"清洁任务","＋ 添加",v->TaskForms.show(host,null));
        renderTasks(host,parent);
        Ui.heading(parent,"提醒设置");
        renderReminder(host,parent);
        Ui.heading(parent,"外观设置");
        LinearLayout appearance=Ui.panel(parent);
        AppIcons.Choice selected=AppIcons.current(host);
        addActionRow(host,appearance,R.drawable.ic_cat_resting,"App 图标",selected.label+" · 更换图标或使用自己的照片","自定义 App 图标",v->host.startActivity(new Intent(host,AppIconActivity.class)));
        Ui.heading(parent,"关于与数据");
        LinearLayout about=Ui.panel(parent);
        addStaticRow(host,about,R.drawable.ic_info,"喵洁日历  v0.1","记录保存在本机，卸载或清除数据后无法恢复");
    }

    private void renderTasks(MainActivity host,LinearLayout parent){
        LinearLayout panel=Ui.panel(parent);List<CareTask> ordered=new ArrayList<>();
        for(CareTask task:host.tasks())if(task.active())ordered.add(task);
        for(CareTask task:host.tasks())if(!task.active())ordered.add(task);
        for(int i=0;i<ordered.size();i++){
            if(i>0)panel.addView(Ui.divider(host));
            CareTask task=ordered.get(i);String detail="每 "+task.intervalDays()+" 天 · "+(task.remind()?"参与提醒":"不提醒")+(task.active()?"":" · 已停用");
            addActionRow(host,panel,Ui.taskIcon(task.title()),task.title(),detail,"编辑"+task.title(),v->TaskForms.show(host,task));
        }
    }

    private void renderReminder(MainActivity host,LinearLayout parent){
        ReminderSettings settings=new ReminderSettings(host);LinearLayout panel=Ui.panel(parent);
        LinearLayout enabledRow=baseRow(host,R.drawable.ic_bell,"每日提醒","汇总到期和未记录的任务");
        SwitchMaterial enabled=new SwitchMaterial(host);enabled.setChecked(settings.enabled());enabled.setMinWidth(Ui.dp(host,54));enabled.setContentDescription("开启每日提醒");enabledRow.addView(enabled);panel.addView(enabledRow);
        enabled.setOnCheckedChangeListener((button,checked)->{if(checked)host.enableNotifications();else{settings.setEnabled(false);ReminderScheduler.schedule(host);host.render();}});
        panel.addView(Ui.divider(host));
        addActionRow(host,panel,R.drawable.ic_clock,"提醒时间",String.format(Locale.CHINA,"%02d:%02d",settings.hour(),settings.minute()),"修改提醒时间",v->new TimePickerDialog(host,(picker,h,m)->{settings.setTime(h,m);ReminderScheduler.schedule(host);host.render();},settings.hour(),settings.minute(),true).show());
        panel.addView(Ui.divider(host));
        addActionRow(host,panel,R.drawable.ic_bell,"系统通知权限",settings.enabled()&&ReminderScheduler.canNotify(host)?"已允许":"查看系统设置","打开系统通知设置",v->{Intent intent=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,host.getPackageName());host.startActivity(intent);});
        if(settings.enabled()&&!ReminderScheduler.canNotify(host)){TextView warning=Ui.text(host,"系统通知尚未允许，当前无法收到提醒。",13,R.color.warning);warning.setPadding(0,Ui.dp(host,8),0,0);panel.addView(warning);}
        TextView note=Ui.text(host,"通知可能受系统和省电策略影响；强行停止应用后，请重新打开。",13,R.color.muted);note.setPadding(Ui.dp(host,4),Ui.dp(host,8),Ui.dp(host,4),0);parent.addView(note);
    }

    private void sectionHeader(MainActivity host,LinearLayout parent,String title,String action,View.OnClickListener listener){
        LinearLayout row=new LinearLayout(host);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Ui.dp(host,14),0,Ui.dp(host,8));TextView heading=Ui.text(host,title,20,R.color.ink);heading.setTypeface(null,Typeface.BOLD);row.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        MaterialButton button=Ui.button(host,action,listener);button.setMinWidth(Ui.dp(host,96));button.setMinimumWidth(Ui.dp(host,96));button.setPadding(Ui.dp(host,12),0,Ui.dp(host,12),0);button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(host.getColor(R.color.background)));button.setTextColor(host.getColor(R.color.primary));row.addView(button,new LinearLayout.LayoutParams(-2,Ui.dp(host,48)));parent.addView(row);
    }

    private void addActionRow(MainActivity host,LinearLayout parent,int icon,String title,String detail,String description,View.OnClickListener listener){
        LinearLayout row=baseRow(host,icon,title,detail);ImageView chevron=new ImageView(host);chevron.setImageResource(R.drawable.ic_chevron);chevron.setColorFilter(host.getColor(R.color.muted));row.addView(chevron,new LinearLayout.LayoutParams(Ui.dp(host,28),Ui.dp(host,48)));row.setContentDescription(title+"，"+detail+"，"+description);row.setClickable(true);row.setFocusable(true);row.setOnClickListener(listener);parent.addView(row);
    }

    private LinearLayout baseRow(MainActivity host,int icon,String title,String detail){
        LinearLayout row=new LinearLayout(host);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Ui.dp(host,7),0,Ui.dp(host,7));ImageView image=Ui.icon(host,icon);row.addView(image,new LinearLayout.LayoutParams(Ui.dp(host,42),Ui.dp(host,42)));
        LinearLayout copy=new LinearLayout(host);copy.setOrientation(LinearLayout.VERTICAL);copy.setPadding(Ui.dp(host,10),0,Ui.dp(host,8),0);TextView name=Ui.text(host,title,16,R.color.ink);name.setTypeface(null,Typeface.BOLD);copy.addView(name);copy.addView(Ui.text(host,detail,13,R.color.muted));row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));return row;
    }

    private void addStaticRow(MainActivity host,LinearLayout parent,int icon,String title,String detail){parent.addView(baseRow(host,icon,title,detail));}
}
