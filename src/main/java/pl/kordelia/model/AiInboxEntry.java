package pl.kordelia.model;

import java.time.Instant;

public record AiInboxEntry(
        String id,
        InboxEntryType type,
        double confidence,
        String proposedSummary,
        String sourceMailId,
        AiInboxStatus status,
        Instant createdAt,
        String decidedBy
) {
    public AiInboxEntry withStatus(AiInboxStatus newStatus, String decidedBy) {
        return new AiInboxEntry(id, type, confidence, proposedSummary, sourceMailId, newStatus, createdAt, decidedBy);
    }

    public boolean eligibleForSerialMode() {
        return type == InboxEntryType.NOWY_DOKUMENT && confidence >= 0.90;
    }
}
