package com.meowcare.app.reminder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

public final class ReminderPolicy {
    private ReminderPolicy() {}
    public static boolean deliveryEligible(Instant now, Instant pending, ZoneId zone) {
        return !pending.isAfter(now)
                && pending.atZone(zone).toLocalDate().equals(now.atZone(zone).toLocalDate());
    }
    public static Instant scheduledOccurrence(Instant now, LocalTime time, ZoneId zone, Instant pending) {
        if (pending != null && pending.atZone(zone).toLocalDate().equals(now.atZone(zone).toLocalDate())) {
            return pending;
        }
        return nextOccurrence(now, time, zone);
    }
    public static Instant nextOccurrence(Instant now, LocalTime time, ZoneId zone) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(zone, "zone");
        LocalDate date = now.atZone(zone).toLocalDate();
        ZonedDateTime candidate = date.atTime(time).atZone(zone);
        if (candidate.toInstant().isAfter(now)) return candidate.toInstant();
        Instant later = candidate.withLaterOffsetAtOverlap().toInstant();
        if (later.isAfter(now)) return later;
        return date.plusDays(1).atTime(time).atZone(zone).toInstant();
    }
}
