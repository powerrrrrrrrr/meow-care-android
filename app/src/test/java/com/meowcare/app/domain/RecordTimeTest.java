package com.meowcare.app.domain;

import java.time.*;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RecordTimeTest {
    private final ZoneId zone = ZoneId.of("America/New_York");
    @Test public void unchangedOverlapRecordKeepsItsInstant() {
        Instant original = Instant.parse("2026-11-01T06:30:12Z");
        assertEquals(original, RecordTime.resolve(LocalDateTime.parse("2026-11-01T01:30:12"), zone, original));
    }
    @Test public void editedOverlapTimeKeepsOriginalOffset() {
        assertEquals(Instant.parse("2026-11-01T06:40:00Z"), RecordTime.resolve(
                LocalDateTime.parse("2026-11-01T01:40:00"), zone, Instant.parse("2026-11-01T06:30:00Z")));
    }
    @Test public void newRecordUsesNormalZoneResolution() {
        assertEquals(Instant.parse("2026-09-06T16:00:00Z"), RecordTime.resolve(
                LocalDateTime.parse("2026-09-06T12:00:00"), zone, null));
    }
}
