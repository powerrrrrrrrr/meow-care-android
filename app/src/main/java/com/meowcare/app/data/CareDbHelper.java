package com.meowcare.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

final class CareDbHelper extends SQLiteOpenHelper {
    CareDbHelper(Context context, String name) { super(context, name, null, 1); }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,item TEXT NOT NULL,interval_days INTEGER NOT NULL CHECK(interval_days BETWEEN 1 AND 365),remind INTEGER NOT NULL,active INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE records (id INTEGER PRIMARY KEY AUTOINCREMENT,task_id INTEGER NOT NULL REFERENCES tasks(id),completed_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX records_task_time ON records(task_id,completed_at)");
        String[] titles = {"铲猫砂", "整盆更换猫砂", "清洗猫砂盆", "清洗饮水盆", "清洗饭盆"};
        String[] items = {"猫砂盆", "猫砂盆", "猫砂盆", "饮水盆", "饭盆"};
        int[] days = {1, 7, 14, 1, 1};
        for (int i = 0; i < titles.length; i++) {
            db.execSQL("INSERT INTO tasks(title,item,interval_days,remind,active) VALUES(?,?,?,1,1)", new Object[]{titles[i], items[i], days[i]});
        }
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("缺少数据库迁移：" + oldVersion + " → " + newVersion);
    }
}
