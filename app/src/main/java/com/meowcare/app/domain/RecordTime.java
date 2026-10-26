package com.meowcare.app.domain;

import java.time.*;

public final class RecordTime {
    private RecordTime() {}
    public static Instant resolve(LocalDateTime chosen, ZoneId zone, Instant original) {
        ZoneOffset preferred = original == null ? null : original.atZone(zone).getOffset();
        return ZonedDateTime.ofLocal(chosen, zone, preferred).toInstant();
    }
}
