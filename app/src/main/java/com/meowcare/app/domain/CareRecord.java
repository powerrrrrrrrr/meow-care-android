package com.meowcare.app.domain;

import java.time.Instant;
import java.util.Objects;

public final class CareRecord {
    private final long id;
    private final long taskId;
    private final Instant completedAt;

    public CareRecord(long id, long taskId, Instant completedAt) {
        this.id = id;
        this.taskId = taskId;
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt");
    }
    public long id() { return id; }
    public long taskId() { return taskId; }
    public Instant completedAt() { return completedAt; }
}
