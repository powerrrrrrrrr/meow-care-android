package com.meowcare.app.appearance;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Build;
import com.meowcare.app.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/** Launcher state is the source of truth, including after process death and upgrades. */
public final class AppIcons {
    public enum Choice {
        DEFAULT("LauncherDefault", "喵洁日历", R.mipmap.ic_launcher),
        CLASSIC("LauncherClassic", "经典猫咪", R.mipmap.ic_launcher_classic);

        public final String alias;
        public final String label;
        public final int icon;
        Choice(String alias, String label, int icon) {
            this.alias=alias; this.label=label; this.icon=icon;
        }
    }

    private AppIcons() {}

    public static ComponentName component(Context context, Choice choice) {
        return new ComponentName(context.getPackageName(), context.getPackageName()+"."+choice.alias);
    }

    public static Choice current(Context context) {
        PackageManager manager=context.getPackageManager();
        for (Choice choice:Choice.values()) {
            int state=manager.getComponentEnabledSetting(component(context,choice));
            if (state==PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    (state==PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && choice==Choice.DEFAULT)) return choice;
        }
        return Choice.DEFAULT;
    }

    public static void select(Context context, Choice selected) {
        PackageManager manager=context.getPackageManager();
        Choice previous=current(context);
        ShortcutManager shortcuts=context.getSystemService(ShortcutManager.class);
        boolean pinned=shortcuts!=null&&shortcuts.getPinnedShortcuts().stream()
                .anyMatch(shortcut->CustomIcon.SHORTCUT_ID.equals(shortcut.getId()));
        if(pinned&&previous!=selected) {
            int original=manager.getComponentEnabledSetting(component(context,selected));
            manager.setComponentEnabledSetting(component(context,selected),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);
            try {
                // Pinned shortcuts belong to a launcher activity. Move ours before disabling its old alias.
                ShortcutInfo moved=new ShortcutInfo.Builder(context,CustomIcon.SHORTCUT_ID)
                        .setActivity(component(context,selected)).build();
                if(!shortcuts.updateShortcuts(Collections.singletonList(moved))) throw new IllegalStateException("Shortcut update rate limited");
            } catch(RuntimeException error) {
                manager.setComponentEnabledSetting(component(context,selected),original,PackageManager.DONT_KILL_APP);
                throw error;
            }
        }
        if (Build.VERSION.SDK_INT>=33) {
            List<PackageManager.ComponentEnabledSetting> changes=new ArrayList<>();
            for (Choice choice:Choice.values()) changes.add(new PackageManager.ComponentEnabledSetting(
                    component(context,choice), choice==selected?PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP));
            manager.setComponentEnabledSettings(changes);
        } else {
            // Enable first: even if the process stops, there is always a working launcher entry.
            manager.setComponentEnabledSetting(component(context,selected),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);
            for (Choice choice:Choice.values()) if (choice!=selected) manager.setComponentEnabledSetting(
                    component(context,choice),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
        }
    }
}
