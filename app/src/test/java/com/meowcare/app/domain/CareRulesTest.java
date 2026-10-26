package com.meowcare.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class CareRulesTest {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    @Test public void dueDateUsesCalendarDaysAcrossMonths() {
        assertEquals(LocalDate.of(2026, 9, 5),
                CareRules.dueDate(LocalDate.of(2026, 8, 29), 7));
        assertEquals(LocalDate.of(2026, 2, 1),
                CareRules.dueDate(LocalDate.of(2026, 1, 31), 1));
    }

    @Test public void dueDateHandlesLeapDay() {
        assertEquals(LocalDate.of(2024, 2, 29),
                CareRules.dueDate(LocalDate.of(2024, 2, 28), 1));
    }

    @Test public void evaluatesDueAndOverdueFromLocalDates() {
        assertEquals(TaskStatus.DUE_TODAY,
                CareRules.status(LocalDate.of(2026, 8, 29), 7, LocalDate.of(2026, 9, 5)));
        assertEquals(TaskStatus.OVERDUE,
                CareRules.status(LocalDate.of(2026, 8, 29), 7, LocalDate.of(2026, 9, 6)));
    }

    @Test public void nullHistoryIsUnrecorded() {
        assertEquals(TaskStatus.UNRECORDED,
                CareRules.status(null, 7, LocalDate.of(2026, 9, 6)));
    }

    @Test public void rejectsIntervalsOutsideOneToThreeHundredSixtyFive() {
        assertThrows(IllegalArgumentException.class, () -> CareRules.validateInterval(0));
        assertThrows(IllegalArgumentException.class, () -> CareRules.validateInterval(366));
    }

    @Test public void rejectsFutureCompletion() {
        Instant now = Instant.parse("2026-09-06T10:00:00Z");
        assertThrows(IllegalArgumentException.class,
                () -> CareRules.validateCompletion(Instant.parse("2026-09-06T10:00:01Z"), now));
    }

    @Test public void latestCompletionUsesTimestampRatherThanInsertionOrder() {
        Instant newer = Instant.parse("2026-09-06T08:00:00Z");
        Instant olderBackfill = Instant.parse("2026-08-01T08:00:00Z");
        assertEquals(newer, CareRules.latestCompletion(Arrays.asList(newer, olderBackfill)));
        assertEquals(null, CareRules.latestCompletion(Collections.emptyList()));
    }

    @Test public void localDateIsRecomputedInCurrentTimeZone() {
        Instant completion = Instant.parse("2026-09-05T16:30:00Z");
        assertEquals(LocalDate.of(2026, 9, 6), CareRules.localDate(completion, SHANGHAI));
        assertEquals(LocalDate.of(2026, 9, 5), CareRules.localDate(completion, ZoneId.of("UTC")));
    }
}
