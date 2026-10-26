package com.meowcare.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.meowcare.app.data.CareRepository;
import com.meowcare.app.domain.*;
import com.meowcare.app.reminder.ReminderScheduler;
import com.meowcare.app.reminder.ReminderSettings;
import com.meowcare.app.ui.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_REQUEST = 7;
    private LinearLayout content;
    private int page=0;
    private int loadGeneration=0;
    private List<CareTask> tasks=new ArrayList<>();
    private List<CareRecord> records=new ArrayList<>();
    private final TodayController todayController=new TodayController();
    private final CalendarController calendarController=new CalendarController();
    private final SettingsController settingsController=new SettingsController();
    private final Handler clock=new Handler(Looper.getMainLooper());
    private LocalDate renderedDay=LocalDate.now();
    private final Runnable dayCheck=new Runnable() { public void run() {
        if(!LocalDate.now().equals(renderedDay)) refresh();
        clock.postDelayed(this,30000);
    }};
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_main);
        content=findViewById(R.id.content);
        if(state!=null) {page=state.getInt("page",0);calendarController.restore(state.getString("selectedDate"),state.getString("month"));}
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root),(view,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
        });
        boolean dark=(getResources().getConfiguration().uiMode & 48)==32;
        WindowCompat.getInsetsController(getWindow(),findViewById(R.id.root)).setAppearanceLightStatusBars(!dark);
        WindowCompat.getInsetsController(getWindow(),findViewById(R.id.root)).setAppearanceLightNavigationBars(!dark);
        int[] ids={R.id.nav_today,R.id.nav_calendar,R.id.nav_settings};
        for(int i=0;i<ids.length;i++){final int destination=i;findViewById(ids[i]).setOnClickListener(v->{page=destination;render();findViewById(R.id.scroll).scrollTo(0,0);});}
    }
    @Override protected void onResume(){super.onResume();refresh();ReminderScheduler.schedule(this);clock.postDelayed(dayCheck,30000);}
    @Override protected void onPause(){super.onPause();clock.removeCallbacks(dayCheck);}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);page=0;refresh();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putInt("page",page);state.putString("selectedDate",calendarController.selected().toString());state.putString("month",calendarController.month().toString());}
    @Override protected void onDestroy(){clock.removeCallbacksAndMessages(null);super.onDestroy();}
    public CareRepository repository(){return ((CareApplication)getApplication()).repository();}
    public List<CareTask> tasks(){return tasks;}
    public List<CareRecord> records(){return records;}
    public CareTask task(long id){for(CareTask t:tasks)if(t.id()==id)return t;throw new IllegalStateException("任务不存在");}
    public <T> void work(Callable<T> operation,Consumer<T> success,Runnable failure){
        ((CareApplication)getApplication()).io().execute(()->{
            try {T result=operation.call();runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())success.accept(result);});}
            catch(Exception e){android.util.Log.e("MeowCare","Operation failed",e);runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){message(e instanceof IllegalArgumentException?e.getMessage():"操作未完成，请重试");if(failure!=null)failure.run();}});}
        });
    }
    public void refresh(){
        int generation=++loadGeneration;findViewById(R.id.loading).setVisibility(View.VISIBLE);
        work(()->new Data(repository().getAllTasks(),repository().getAllRecords()),data->{
            if(generation!=loadGeneration)return;tasks=data.tasks;records=data.records;renderedDay=LocalDate.now();findViewById(R.id.loading).setVisibility(View.GONE);render();
        },()->{findViewById(R.id.loading).setVisibility(View.GONE);Snackbar.make(content,"读取失败",Snackbar.LENGTH_INDEFINITE).setAction("重试",v->refresh()).show();});
    }
    public void render(){
        content.removeAllViews();
        ((TextView)findViewById(R.id.page_title)).setText(new String[]{"今天","清洁月历","按你家的习惯"}[page]);
        ((TextView)findViewById(R.id.page_subtitle)).setText(new String[]{
                LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 · EEEE",Locale.CHINA)),
                "每一次照顾，都有迹可循", "让清洁有自己的节奏"}[page]);
        findViewById(R.id.header_art).setVisibility(page==0?View.VISIBLE:View.GONE);
        int[] ids={R.id.nav_today,R.id.nav_calendar,R.id.nav_settings};
        for(int i=0;i<ids.length;i++){MaterialButton b=findViewById(ids[i]);b.setSelected(i==page);b.setTextColor(getColor(i==page?R.color.primary:R.color.muted));b.setIconTint(android.content.res.ColorStateList.valueOf(getColor(i==page?R.color.primary:R.color.muted)));b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(i==page?R.color.soft:R.color.surface)));}
        if(page==0){
            Map<Long,Instant> latest=new HashMap<>();for(CareRecord r:records)latest.merge(r.taskId(),r.completedAt(),(a,b)->a.isAfter(b)?a:b);
            List<TaskSnapshot> values=new ArrayList<>();for(CareTask t:tasks)if(t.active())values.add(TaskSnapshot.from(t,latest.get(t.id()),LocalDate.now(),ZoneId.systemDefault()));
            values.sort(TaskSnapshot.DISPLAY_ORDER);todayController.render(this,content,values);
        } else if(page==1)calendarController.render(this,content);else settingsController.render(this,content);
    }
    public void complete(CareTask task,View button){
        button.setEnabled(false);Instant completed=Instant.now();
        work(()->repository().insertRecord(task.id(),completed,Instant.now()),id->{
            refresh();Snackbar.make(content,"已记录「"+task.title()+"」",Snackbar.LENGTH_LONG).setAnchorView(findViewById(R.id.nav)).setAction("撤销",v->mutate(()->{repository().deleteRecord(id);return null;},"已撤销")).show();
        },()->button.setEnabled(true));
    }
    public void mutate(Callable<?> operation,String message){work(operation,result->{refresh();ReminderScheduler.schedule(this);message(message);},null);}
    public void message(String text){Toast.makeText(this,text==null?"操作未完成":text,Toast.LENGTH_LONG).show();}
    public void enableNotifications(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATION_REQUEST);return;}
        ReminderSettings s=new ReminderSettings(this);s.setEnabled(true);ReminderScheduler.schedule(this);render();
        if(!ReminderScheduler.canNotify(this))message("系统通知尚未允许，请在系统设置中开启");
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==NOTIFICATION_REQUEST){boolean granted=results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED;new ReminderSettings(this).setEnabled(granted);ReminderScheduler.schedule(this);render();if(!granted)message("未开启通知。记录功能仍可使用，可稍后到系统设置开启。");}
    }
    private static final class Data {final List<CareTask> tasks;final List<CareRecord> records;Data(List<CareTask> t,List<CareRecord> r){tasks=t;records=r;}}
}
