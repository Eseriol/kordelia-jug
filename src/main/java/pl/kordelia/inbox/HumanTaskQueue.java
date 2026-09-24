package pl.kordelia.inbox;

import org.springframework.stereotype.Component;
import pl.kordelia.model.HumanTask;
import pl.kordelia.model.TaskStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class HumanTaskQueue {

    private final ConcurrentHashMap<String, HumanTask> tasks = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    public HumanTask add(String reason, String context, String sourceMailId) {
        String id = "task-" + idSequence.getAndIncrement();
        HumanTask task = new HumanTask(id, reason, context, sourceMailId, Instant.now(), TaskStatus.OCZEKUJE);
        tasks.put(id, task);
        return task;
    }

    public List<HumanTask> newestFirst() {
        return tasks.values().stream()
                .sorted(Comparator.comparing(HumanTask::createdAt).reversed())
                .toList();
    }

    public HumanTask resolve(String id) {
        HumanTask task = require(id);
        HumanTask resolved = task.withStatus(TaskStatus.OBSLUZONE);
        tasks.put(id, resolved);
        return resolved;
    }

    public void clear() {
        tasks.clear();
        idSequence.set(1);
    }

    private HumanTask require(String id) {
        HumanTask task = tasks.get(id);
        if (task == null) {
            throw new NoSuchElementException("Nie znaleziono zadania: " + id);
        }
        return task;
    }
}
