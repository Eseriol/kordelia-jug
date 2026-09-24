package pl.kordelia.inbox;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import pl.kordelia.model.AiInboxEntry;
import pl.kordelia.model.AiInboxStatus;
import pl.kordelia.model.InboxEntryType;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AiInboxService {
    private final ConcurrentHashMap<String, AiInboxEntry> entries = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @PreAuthorize("hasRole('AGENT_AI')")
    public AiInboxEntry propose(InboxEntryType type, double confidence, String proposedSummary, String sourceMailId) {
        String id = "inbox-" + idSequence.getAndIncrement();
        AiInboxEntry entry = new AiInboxEntry(id, type, confidence, proposedSummary, sourceMailId,
                AiInboxStatus.OCZEKUJE, Instant.now(), null);
        entries.put(id, entry);
        return entry;
    }

    public List<AiInboxEntry> queueSortedByConfidenceDesc() {
        return entries.values().stream()
                .sorted(Comparator.comparingDouble(AiInboxEntry::confidence).reversed())
                .toList();
    }

    @PreAuthorize("hasAnyRole('SPECJALISTA', 'ADMIN')")
    public AiInboxEntry approve(String id, String decidedBy) {
        AiInboxEntry entry = require(id);
        AiInboxEntry approved = entry.withStatus(AiInboxStatus.ZATWIERDZONE, decidedBy);
        entries.put(id, approved);
        return approved;
    }

    @PreAuthorize("hasAnyRole('SPECJALISTA', 'ADMIN')")
    public AiInboxEntry reject(String id, String decidedBy) {
        AiInboxEntry entry = require(id);
        AiInboxEntry rejected = entry.withStatus(AiInboxStatus.ODRZUCONE, decidedBy);
        entries.put(id, rejected);
        return rejected;
    }

    public void clear() {
        entries.clear();
        idSequence.set(1);
    }

    private AiInboxEntry require(String id) {
        AiInboxEntry entry = entries.get(id);
        if (entry == null) {
            throw new NoSuchElementException("Nie znaleziono wpisu ai_inbox: " + id);
        }
        return entry;
    }
}
