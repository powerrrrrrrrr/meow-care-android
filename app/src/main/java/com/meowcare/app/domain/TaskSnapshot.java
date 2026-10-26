package com.meowcare.app.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Objects;
import java.time.temporal.ChronoUnit;

public final class TaskSnapshot {
    public static final Comparator<TaskSnapshot> DISPLAY_ORDER = Comparator
            .comparingInt((TaskSnapshot value) -> rank(value.status))
            .thenComparing(value -> value.dueDate, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(value -> value.task.item())
            .thenComparingLong(value -> value.task.id());

    private final CareTask task;
    private final Instant latestCompletion;
    private final LocalDate latestDate;
    private final LocalDate dueDate;
    private final int daysSince;
    private final TaskStatus status;

    private TaskSnapshot(CareTask task, Instant latestCompletion, LocalDate latestDate,
                         LocalDate dueDate, int daysSince, TaskStatus status) {
        this.task = task;
        this.latestCompletion = latestCompletion;
        this.latestDate = latestDate;
        this.dueDate = dueDate;
        this.daysSince = daysSince;
        this.status = status;
    }

    public static TaskSnapshot from(CareTask task, Instant latest, LocalDate today, ZoneId zone) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(today, "today");
        Objects.requireNonNull(zone, "zone");
        LocalDate latestDate = latest == null ? null : CareRules.localDate(latest, zone);
        LocalDate due = latestDate == null ? null : CareRules.dueDate(latestDate, task.intervalDays());
        long elapsed = latestDate == null ? -1 : ChronoUnit.DAYS.between(latestDate, today);
        int days = elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) elapsed;
        return new TaskSnapshot(task, latest, latestDate, due, days,
                CareRules.status(latestDate, task.intervalDays(), today));
    }

    private static int rank(TaskStatus status) {
        switch (status) {
            case UNRECORDED: return 0;
            case OVERDUE: return 1;
            case DUE_TODAY: return 2;
            default: return 3;
        }
    }

    public CareTask task() { return task; }
    public Instant latestCompletion() { return latestCompletion; }
    public LocalDate latestDate() { return latestDate; }
    public LocalDate dueDate() { return dueDate; }
    public int daysSince() { return daysSince; }
    public TaskStatus status() { return status; }
}
