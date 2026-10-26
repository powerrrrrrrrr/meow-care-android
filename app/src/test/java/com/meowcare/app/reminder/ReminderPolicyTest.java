package com.meowcare.app.reminder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.Test;

public class ReminderPolicyTest {
    @Test public void yesterdayDeliveryMustNotConsumeTodaysNotification() {
        assertFalse(ReminderPolicy.deliveryEligible(Instant.parse("2026-09-07T00:01:00Z"),
                Instant.parse("2026-09-06T20:00:00Z"), ZoneId.of("UTC")));
    }
    @Test public void delayedSameDayDeliveryIsEligible() {
        assertTrue(ReminderPolicy.deliveryEligible(Instant.parse("2026-09-06T20:01:00Z"),
                Instant.parse("2026-09-06T20:00:00Z"), ZoneId.of("UTC")));
    }
    @Test public void futureDeliveryIsNotEligible() {
        assertFalse(ReminderPolicy.deliveryEligible(Instant.parse("2026-09-06T19:00:00Z"),
                Instant.parse("2026-09-06T20:00:00Z"), ZoneId.of("UTC")));
    }
    @Test public void delayedSameDayAlarmSurvivesResume() {
        assertEquals(Instant.parse("2026-09-06T12:00:00Z"), ReminderPolicy.scheduledOccurrence(
                Instant.parse("2026-09-06T12:01:00Z"), LocalTime.of(20,0), ZoneId.of("Asia/Shanghai"),
                Instant.parse("2026-09-06T12:00:00Z")));
    }
    @Test public void consumedAlarmMovesToTomorrow() {
        assertEquals(Instant.parse("2026-09-07T12:00:00Z"), ReminderPolicy.scheduledOccurrence(
                Instant.parse("2026-09-06T12:01:00Z"), LocalTime.of(20,0), ZoneId.of("Asia/Shanghai"), null));
    }
    @Test public void previousDayAlarmDoesNotCatchUp() {
        assertEquals(Instant.parse("2026-09-07T12:00:00Z"), ReminderPolicy.scheduledOccurrence(
                Instant.parse("2026-09-07T10:00:00Z"), LocalTime.of(20,0), ZoneId.of("Asia/Shanghai"),
                Instant.parse("2026-09-06T12:00:00Z")));
    }
    @Test public void overlapAlwaysSchedulesAFutureInstant() {
        Instant now=Instant.parse("2026-11-01T06:10:00Z");
        Instant next=ReminderPolicy.nextOccurrence(now,LocalTime.of(1,30),ZoneId.of("America/New_York"));
        assertTrue("The alarm must not be in the past: "+next,next.isAfter(now));
        assertEquals(Instant.parse("2026-11-01T06:30:00Z"),next);
    }

    @Test public void schedulesTodayWhenTimeHasNotPassed() {
        assertEquals(Instant.parse("2026-09-06T12:30:00Z"),
                ReminderPolicy.nextOccurrence(
                        Instant.parse("2026-09-06T10:00:00Z"),
                        LocalTime.of(20, 30), ZoneId.of("Asia/Shanghai")));
    }

    @Test public void schedulesTomorrowWhenTimeHasPassed() {
        assertEquals(Instant.parse("2026-09-07T00:00:00Z"),
                ReminderPolicy.nextOccurrence(
                        Instant.parse("2026-09-06T12:30:00Z"),
                        LocalTime.of(8, 0), ZoneId.of("Asia/Shanghai")));
    }
}
