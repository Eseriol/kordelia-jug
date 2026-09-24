package pl.kordelia.model;

import java.time.Instant;

public record HumanTask(String id, String reason, String context, String sourceMailId, Instant createdAt, TaskStatus status) {
    public HumanTask withStatus(TaskStatus newStatus) {
        return new HumanTask(id, reason, context, sourceMailId, createdAt, newStatus);
    }
}
