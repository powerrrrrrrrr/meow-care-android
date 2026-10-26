package com.meowcare.app;

import android.content.ActivityNotFoundException;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.meowcare.app.appearance.AppIcons;
import com.meowcare.app.appearance.CustomIcon;
import com.meowcare.app.ui.Ui;
import java.io.IOException;
import java.util.Collections;

public final class AppIconActivity extends AppCompatActivity {
    private LinearLayout content;
    private Bitmap custom;
    private boolean busy;
    private final ActivityResultLauncher<String> pickImage=registerForActivityResult(
            new ActivityResultContracts.GetContent(),uri->{ if (uri!=null) importImage(uri); });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(getColor(R.color.background));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);setContentView(root);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
        });
        boolean dark=(getResources().getConfiguration().uiMode&48)==32;
        WindowCompat.getInsetsController(getWindow(),scroll).setAppearanceLightStatusBars(!dark);
        WindowCompat.getInsetsController(getWindow(),scroll).setAppearanceLightNavigationBars(!dark);
        int padding=Ui.dp(this,20);content.setPadding(padding,padding,padding,padding);
        busy=true;render();
        ((CareApplication)getApplication()).io().execute(()->{
            try {
                Bitmap saved=new CustomIcon(this).load();
                runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){custom=saved;busy=false;render();}});
            } catch (IOException error) {
                android.util.Log.e("MeowCare","Load icon failed",error);
                runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){busy=false;render();message("已保存的图片无法读取，请重新选择");}});
            }
        });
    }

    @Override protected void onResume() { super.onResume();if(content!=null)render(); }

    private void render() {
        content.removeAllViews();
        MaterialButton back=Ui.button(this,"‹ 返回设置",v->finish());Ui.secondary(back);content.addView(back);
        Ui.heading(content,"让桌面也有一点喵意");
        content.addView(Ui.text(this,"选一个喜欢的图标，或放上自家猫咪的照片。",14,R.color.muted));
        Ui.heading(content,"应用图标 · 无快捷方式角标");
        LinearLayout presets=Ui.panel(content);
        AppIcons.Choice selected=AppIcons.current(this);
        for (AppIcons.Choice choice:AppIcons.Choice.values()) {
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,10));
            ImageView image=new ImageView(this);image.setImageResource(choice.icon);image.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(image,new LinearLayout.LayoutParams(Ui.dp(this,64),Ui.dp(this,64)));
            TextView label=Ui.text(this,choice.label,16,R.color.ink);
            label.setPadding(Ui.dp(this,14),0,Ui.dp(this,8),0);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            TextView status=Ui.text(this,choice==selected?"使用中":"使用",14,choice==selected?R.color.primary:R.color.muted);row.addView(status);
            row.setContentDescription(choice.label+(choice==selected?"，使用中":"，点击更换"));row.setClickable(true);row.setFocusable(true);row.setEnabled(!busy);
            row.setOnClickListener(v->select(choice));presets.addView(row);
            if(choice==AppIcons.Choice.DEFAULT)presets.addView(Ui.divider(this));
        }
        MaterialButton reset=Ui.button(this,"恢复默认图标",v->select(AppIcons.Choice.DEFAULT));Ui.secondary(reset);reset.setEnabled(!busy&&selected!=AppIcons.Choice.DEFAULT);presets.addView(reset);
        presets.addView(Ui.text(this,"切换后可能返回桌面，重新打开即可。图标可能需要几秒刷新；开启系统主题图标时，颜色由桌面主题决定。",13,R.color.muted));
        Ui.heading(content,"自定义桌面快捷方式");
        LinearLayout panel=Ui.panel(content);
        panel.addView(Ui.text(this,"图片会铺满图标，并随桌面裁成圆形或圆角方形。右下角的小图标是桌面添加的快捷方式标记，应用无法隐藏；不需要该标记时，请使用上方应用图标。",14,R.color.muted));
        if(custom!=null) {
            ImageView preview=new ImageView(this);preview.setImageDrawable(CustomIcon.icon(custom).loadDrawable(this));preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setContentDescription("自定义图标预览，按系统图标形状裁切");
            LinearLayout.LayoutParams previewParams=new LinearLayout.LayoutParams(Ui.dp(this,112),Ui.dp(this,112));previewParams.gravity=Gravity.CENTER_HORIZONTAL;previewParams.topMargin=Ui.dp(this,16);previewParams.bottomMargin=Ui.dp(this,12);panel.addView(preview,previewParams);
        }
        MaterialButton choose=Ui.button(this,busy?"处理中…":custom==null?"从相册选择图片":"重新选择图片",v->{
            try {pickImage.launch("image/*");} catch (ActivityNotFoundException error) {message("当前设备没有可用的图片选择器");}
        });choose.setEnabled(!busy);Ui.secondary(choose);panel.addView(choose);
        ShortcutManager manager=getSystemService(ShortcutManager.class);
        boolean supported=manager!=null&&manager.isRequestPinShortcutSupported();
        MaterialButton add=Ui.button(this,"添加到桌面",v->pin());add.setEnabled(!busy&&custom!=null&&supported);panel.addView(add);
        panel.addView(Ui.text(this,supported?"图片居中取景，边角按桌面形状裁切，不添加白色外框。系统确认后添加到桌面；应用列表仍使用上方图标。已添加的快捷方式可在桌面长按移除。":"当前桌面不支持添加自定义快捷方式，仍可切换上方应用图标。",13,R.color.muted));
    }

    private void select(AppIcons.Choice choice) {
        if(busy)return;
        busy=true;render();
        ((CareApplication)getApplication()).io().execute(()->{
            try {
                AppIcons.select(this,choice);
                runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){busy=false;render();message("已切换为「"+choice.label+"」，桌面稍后刷新");}});
            } catch (RuntimeException error) {
                android.util.Log.e("MeowCare","Switch icon failed",error);
                runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){busy=false;render();message("切换未完成，请稍后重试");}});
            }
        });
    }

    private void importImage(Uri uri) {
        busy=true;render();
        ((CareApplication)getApplication()).io().execute(()->{
            try {
                Bitmap image=CustomIcon.read(this,uri);new CustomIcon(this).save(image);
                runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){custom=image;busy=false;render();message("图片已保存，请预览后添加到桌面");}});
            } catch (IOException | RuntimeException error) {
                android.util.Log.e("MeowCare","Import icon failed",error);
                runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){busy=false;render();message("图片无法读取，请换一张图片重试");}});
            }
        });
    }

    private void pin() {
        try {
            ShortcutManager manager=getSystemService(ShortcutManager.class);
            if(manager==null||!manager.isRequestPinShortcutSupported()){message("当前桌面不支持自定义快捷方式");return;}
            ShortcutInfo shortcut=CustomIcon.shortcut(this,custom);
            // A stable ID updates existing pinned copies instead of leaving an obsolete photo behind.
            if(!manager.updateShortcuts(Collections.singletonList(shortcut))){message("操作过于频繁，请稍后再试");return;}
            if(manager.requestPinShortcut(shortcut,null))message("请在系统窗口确认添加；已添加的图标会更新");
            else message("桌面未接受添加请求，请稍后重试");
        } catch (RuntimeException error) {
            android.util.Log.e("MeowCare","Pin icon failed",error);message("添加未完成，请稍后重试");
        }
    }

    private void message(String text) { Toast.makeText(this,text,Toast.LENGTH_LONG).show(); }
}
