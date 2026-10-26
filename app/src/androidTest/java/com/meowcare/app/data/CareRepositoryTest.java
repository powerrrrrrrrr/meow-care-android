package com.meowcare.app.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.meowcare.app.domain.CareRecord;
import com.meowcare.app.domain.CareTask;
import java.time.Instant;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CareRepositoryTest {
    private static final String DATABASE = "meow-care-instrumentation.db";
    private Context context;
    private CareRepository repository;

    @Before public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(DATABASE);
        repository = new CareRepository(context, DATABASE);
    }

    @After public void tearDown() {
        repository.close();
        context.deleteDatabase(DATABASE);
    }

    @Test public void newDatabaseHasFiveTasksAndNoCompletionHistory() {
        assertEquals(5, repository.getAllTasks().size());
        assertTrue(repository.getAllRecords().isEmpty());
    }

    @Test public void outOfOrderBackfillDoesNotReplaceLatestAndDeleteRecomputesIt() {
        long taskId = repository.getAllTasks().get(0).id();
        Instant now = Instant.parse("2026-09-06T10:00:00Z");
        Instant newer = Instant.parse("2026-09-06T09:00:00Z");
        Instant older = Instant.parse("2026-08-01T09:00:00Z");
        long newerId = repository.insertRecord(taskId, newer, now);
        repository.insertRecord(taskId, older, now);
        assertEquals(newer, repository.getLatestCompletion(taskId));

        repository.deleteRecord(newerId);
        assertEquals(older, repository.getLatestCompletion(taskId));
    }

    @Test public void deletingOnlyRecordLeavesTaskUnrecorded() {
        long taskId = repository.getAllTasks().get(0).id();
        Instant completed = Instant.parse("2026-09-06T09:00:00Z");
        long id = repository.insertRecord(taskId, completed, Instant.parse("2026-09-06T10:00:00Z"));
        repository.deleteRecord(id);
        assertNull(repository.getLatestCompletion(taskId));
    }

    @Test public void recordsAndTaskStateSurviveRepositoryReopen() {
        CareTask original = repository.getAllTasks().get(0);
        Instant completed = Instant.parse("2026-09-06T09:00:00Z");
        repository.insertRecord(original.id(), completed, Instant.parse("2026-09-06T10:00:00Z"));
        repository.setTaskActive(original.id(), false);
        repository.close();

        repository = new CareRepository(context, DATABASE);
        assertEquals(completed, repository.getLatestCompletion(original.id()));
        assertFalse(findTask(original.id()).active());

        repository.setTaskActive(original.id(), true);
        assertTrue(findTask(original.id()).active());
    }

    @Test public void editingRecordChangesTheStoredTimestamp() {
        long taskId = repository.getAllTasks().get(0).id();
        long recordId = repository.insertRecord(taskId, Instant.parse("2026-09-01T09:00:00Z"),
                Instant.parse("2026-09-06T10:00:00Z"));
        Instant edited = Instant.parse("2026-09-02T11:30:00Z");
        repository.updateRecord(recordId, edited, Instant.parse("2026-09-06T10:00:00Z"));
        List<CareRecord> records = repository.getAllRecords();
        assertEquals(1, records.size());
        assertEquals(edited, records.get(0).completedAt());
    }

    private CareTask findTask(long taskId) {
        for (CareTask task : repository.getAllTasks()) if (task.id() == taskId) return task;
        throw new AssertionError("Task not found: " + taskId);
    }
}
