package com.meowcare.app.ui;

import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.meowcare.app.MainActivity;
import com.meowcare.app.R;
import com.meowcare.app.domain.CareTask;

public final class TaskForms {
    private TaskForms(){}
    public static void show(MainActivity host,CareTask original){
        ScrollView scroll=new ScrollView(host);LinearLayout form=new LinearLayout(host);form.setOrientation(LinearLayout.VERTICAL);int p=Ui.dp(host,24);form.setPadding(p,0,p,p);scroll.addView(form);
        EditText title=field(form,"任务名称",original==null?"":original.title(),false);
        EditText item=field(form,"物品 / 位置",original==null?"":original.item(),false);
        EditText days=field(form,"间隔天数（1–365）",original==null?"1":String.valueOf(original.intervalDays()),true);
        SwitchMaterial remind=new SwitchMaterial(host);remind.setText("参与每日提醒");remind.setChecked(original==null||original.remind());remind.setMinHeight(Ui.dp(host,48));form.addView(remind);
        SwitchMaterial active=new SwitchMaterial(host);active.setText("启用任务");active.setChecked(original==null||original.active());active.setMinHeight(Ui.dp(host,48));form.addView(active);
        form.addView(Ui.text(host,"停用后不显示在首页，也不再提醒；已有记录会保留。",14,R.color.muted));
        AlertDialog dialog=new MaterialAlertDialogBuilder(host).setTitle(original==null?"添加任务":"编辑任务").setView(scroll).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
        dialog.setOnShowListener(shown->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            CareTask task;
            try{task=new CareTask(original==null?0:original.id(),title.getText().toString(),item.getText().toString(),Integer.parseInt(days.getText().toString().trim()),remind.isChecked(),active.isChecked());}
            catch(NumberFormatException ex){days.setError("请输入 1–365 的整数");return;}
            catch(IllegalArgumentException ex){host.message(ex.getMessage());return;}
            v.setEnabled(false);host.work(()->host.repository().saveTask(task),id->{dialog.dismiss();host.refresh();host.message("任务已保存");},()->v.setEnabled(true));
        }));dialog.show();
    }
    private static EditText field(LinearLayout form,String label,String value,boolean numeric){
        form.addView(Ui.text(form.getContext(),label,14,R.color.muted));EditText edit=new EditText(form.getContext());edit.setText(value);edit.setTextSize(16);edit.setSingleLine(true);edit.setInputType(numeric?InputType.TYPE_CLASS_NUMBER:InputType.TYPE_CLASS_TEXT);edit.setMinHeight(Ui.dp(form.getContext(),48));edit.setContentDescription(label);form.addView(edit);return edit;
    }
}
