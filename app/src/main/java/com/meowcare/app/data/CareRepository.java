package com.meowcare.app.data;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import com.meowcare.app.domain.CareRules;
import com.meowcare.app.domain.CareRecord;
import com.meowcare.app.domain.CareTask;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

public final class CareRepository implements AutoCloseable {
    public static final String DATABASE_NAME = "meow-care.db";
    public CareRepository(Context context) { this(context, DATABASE_NAME); }
    private final CareDbHelper helper;
    public CareRepository(Context context, String databaseName) { helper = new CareDbHelper(context.getApplicationContext(), databaseName); }
    public List<CareTask> getAllTasks() {
        List<CareTask> tasks = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT id,title,item,interval_days,remind,active FROM tasks ORDER BY id", null)) {
            while (c.moveToNext()) tasks.add(new CareTask(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4) != 0, c.getInt(5) != 0));
        }
        return tasks;
    }
    public List<CareRecord> getAllRecords() {
        List<CareRecord> records = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT id,task_id,completed_at FROM records ORDER BY completed_at DESC,id DESC", null)) {
            while (c.moveToNext()) records.add(new CareRecord(c.getLong(0), c.getLong(1), Instant.ofEpochMilli(c.getLong(2))));
        }
        return records;
    }
    public Instant getLatestCompletion(long taskId) {
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT MAX(completed_at) FROM records WHERE task_id=?", new String[]{String.valueOf(taskId)})) {
            return c.moveToFirst() && !c.isNull(0) ? Instant.ofEpochMilli(c.getLong(0)) : null;
        }
    }
    public long saveTask(CareTask task) {
        ContentValues v = new ContentValues();
        v.put("title", task.title()); v.put("item", task.item()); v.put("interval_days", task.intervalDays());
        v.put("remind", task.remind() ? 1 : 0); v.put("active", task.active() ? 1 : 0);
        if (task.id() == 0) return helper.getWritableDatabase().insertOrThrow("tasks", null, v);
        requireChanged(helper.getWritableDatabase().update("tasks", v, "id=?", new String[]{String.valueOf(task.id())}));
        return task.id();
    }
    public long insertRecord(long taskId, Instant completedAt, Instant now) {
        CareRules.validateCompletion(completedAt, now);
        ContentValues v = new ContentValues(); v.put("task_id", taskId); v.put("completed_at", completedAt.toEpochMilli());
        return helper.getWritableDatabase().insertOrThrow("records", null, v);
    }
    public void updateRecord(long recordId, Instant completedAt, Instant now) {
        CareRules.validateCompletion(completedAt, now);
        ContentValues v = new ContentValues(); v.put("completed_at", completedAt.toEpochMilli());
        requireChanged(helper.getWritableDatabase().update("records", v, "id=?", new String[]{String.valueOf(recordId)}));
    }
    public void deleteRecord(long recordId) { requireChanged(helper.getWritableDatabase().delete("records", "id=?", new String[]{String.valueOf(recordId)})); }
    public void setTaskActive(long taskId, boolean active) {
        ContentValues v = new ContentValues(); v.put("active", active ? 1 : 0);
        requireChanged(helper.getWritableDatabase().update("tasks", v, "id=?", new String[]{String.valueOf(taskId)}));
    }
    private static void requireChanged(int count) { if (count != 1) throw new IllegalStateException("这条记录已不存在，请刷新后重试"); }
    @Override public void close() { helper.close(); }
}
