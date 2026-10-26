package com.meowcare.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class TaskSnapshotTest {
    @Test public void validatesTaskFieldsAndInterval() {
        assertThrows(IllegalArgumentException.class, () -> new CareTask(0, " ", "猫砂盆", 7, true, true));
        assertThrows(IllegalArgumentException.class, () -> new CareTask(0, "铲猫砂", " ", 7, true, true));
        assertThrows(IllegalArgumentException.class, () -> new CareTask(0, "铲猫砂", "猫砂盆", 0, true, true));
    }

    @Test public void computesDaysAndStatusInCurrentZone() {
        CareTask task = new CareTask(8, "清洗", "饮水盆", 7, true, true);
        TaskSnapshot snapshot = TaskSnapshot.from(task, Instant.parse("2026-08-29T02:00:00Z"),
                LocalDate.of(2026, 9, 6), ZoneId.of("Asia/Shanghai"));
        assertEquals(8, snapshot.daysSince());
        assertEquals(TaskStatus.OVERDUE, snapshot.status());
        assertEquals(LocalDate.of(2026, 9, 5), snapshot.dueDate());
    }

    @Test public void orderingIsUnrecordedThenOverdueThenDueThenNormal() {
        CareTask task = new CareTask(1, "清洗", "盆", 7, true, true);
        LocalDate today = LocalDate.of(2026, 9, 6);
        ZoneId zone = ZoneId.of("UTC");
        List<TaskSnapshot> values = new ArrayList<>(Arrays.asList(
                TaskSnapshot.from(task, Instant.parse("2026-09-05T08:00:00Z"), today, zone),
                TaskSnapshot.from(task, Instant.parse("2026-08-30T08:00:00Z"), today, zone),
                TaskSnapshot.from(task, Instant.parse("2026-08-28T08:00:00Z"), today, zone),
                TaskSnapshot.from(task, null, today, zone)));
        values.sort(TaskSnapshot.DISPLAY_ORDER);
        assertEquals(Arrays.asList(TaskStatus.UNRECORDED, TaskStatus.OVERDUE,
                        TaskStatus.DUE_TODAY, TaskStatus.OK),
                Arrays.asList(values.get(0).status(), values.get(1).status(),
                        values.get(2).status(), values.get(3).status()));
    }
}
