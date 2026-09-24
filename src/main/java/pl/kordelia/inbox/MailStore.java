package pl.kordelia.inbox;

import org.springframework.stereotype.Component;
import pl.kordelia.model.IncomingMail;
import pl.kordelia.model.MailProcessingStatus;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MailStore {
    private final ConcurrentHashMap<String, IncomingMail> mails = new ConcurrentHashMap<>();

    public MailStore() {
        SeedData.mails().forEach(mail -> mails.put(mail.id(), mail));
    }

    public List<IncomingMail> all() {
        return mails.values().stream().sorted(Comparator.comparing(IncomingMail::id)).toList();
    }

    public List<IncomingMail> pending() {
        return mails.values().stream()
                .filter(m -> m.status() == MailProcessingStatus.OCZEKUJE_NA_TRIAGE)
                .sorted(Comparator.comparing(IncomingMail::id))
                .toList();
    }

    public void markProcessed(String mailId) {
        mails.computeIfPresent(mailId, (id, mail) -> mail.withStatus(MailProcessingStatus.PRZETWORZONY));
    }

    public void reset() {
        mails.clear();
        SeedData.mails().forEach(mail -> mails.put(mail.id(), mail));
    }
}
