package com.meowcare.app.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

public final class CareRules {
    private CareRules() {}
    public static LocalDate dueDate(LocalDate latest, int intervalDays) {
        Objects.requireNonNull(latest, "latest");
        validateInterval(intervalDays);
        return latest.plusDays(intervalDays);
    }

    public static TaskStatus status(LocalDate latest, int intervalDays, LocalDate today) {
        Objects.requireNonNull(today, "today");
        if (latest == null) return TaskStatus.UNRECORDED;
        int comparison = dueDate(latest, intervalDays).compareTo(today);
        if (comparison < 0) return TaskStatus.OVERDUE;
        if (comparison == 0) return TaskStatus.DUE_TODAY;
        return TaskStatus.OK;
    }

    public static void validateInterval(int intervalDays) {
        if (intervalDays < 1 || intervalDays > 365) {
            throw new IllegalArgumentException("周期必须是 1–365 天");
        }
    }

    public static void validateCompletion(Instant completedAt, Instant now) {
        Objects.requireNonNull(completedAt, "completedAt");
        Objects.requireNonNull(now, "now");
        if (completedAt.isAfter(now)) throw new IllegalArgumentException("不能记录未来时间");
    }

    public static Instant latestCompletion(List<Instant> completions) {
        Objects.requireNonNull(completions, "completions");
        return completions.stream().filter(Objects::nonNull).max(Instant::compareTo).orElse(null);
    }

    public static LocalDate localDate(Instant instant, ZoneId zone) {
        return Objects.requireNonNull(instant, "instant").atZone(Objects.requireNonNull(zone, "zone")).toLocalDate();
    }
}
