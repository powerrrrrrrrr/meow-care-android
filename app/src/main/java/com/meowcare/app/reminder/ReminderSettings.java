package com.meowcare.app.reminder;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;
import java.time.Instant;

public final class ReminderSettings {
    private final SharedPreferences preferences;
    public ReminderSettings(Context context){preferences=context.getSharedPreferences("reminders",Context.MODE_PRIVATE);}
    public boolean enabled(){return preferences.getBoolean("enabled",false);}
    public int hour(){return preferences.getInt("hour",20);}
    public int minute(){return preferences.getInt("minute",0);}
    public void setEnabled(boolean enabled){synchronized(ReminderScheduler.class){
        if(enabled()!=enabled)preferences.edit().putBoolean("enabled",enabled).remove("pending_at").remove("pending_config").apply();
    }}
    public void setTime(int hour,int minute){java.time.LocalTime.of(hour,minute);synchronized(ReminderScheduler.class){
        if(hour()!=hour||minute()!=minute)preferences.edit().putInt("hour",hour).putInt("minute",minute).remove("pending_at").remove("pending_config").apply();
    }}
    public Instant pending(String config){
        return config.equals(preferences.getString("pending_config",null))&&preferences.contains("pending_at")
                ?Instant.ofEpochMilli(preferences.getLong("pending_at",0)):null;
    }
    public void savePending(Instant instant,String config){preferences.edit().putLong("pending_at",instant.toEpochMilli()).putString("pending_config",config).apply();}
    public void clearPending(){preferences.edit().remove("pending_at").remove("pending_config").apply();}
    public boolean sentOn(LocalDate date){return date.toString().equals(preferences.getString("last_sent",null));}
    public boolean markSent(LocalDate date){return preferences.edit().putString("last_sent",date.toString()).commit();}
}
