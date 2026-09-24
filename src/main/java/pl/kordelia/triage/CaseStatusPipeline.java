package pl.kordelia.triage;

import pl.kordelia.inbox.CaseDirectory;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

public final class CaseStatusPipeline {
    private final CaseDirectory caseDirectory;

    public CaseStatusPipeline(CaseDirectory caseDirectory) {
        this.caseDirectory = caseDirectory;
    }

    public CaseStatusReply resolve(String senderAddress, String mailText) {
        List<CaseDirectory.CaseRecord> authorizedCases = caseDirectory.findCasesForGuardian(senderAddress);

        if (authorizedCases.isEmpty()) {
            return CaseStatusReply.escalateToHuman("nadawca nie jest zarejestrowanym opiekunem żadnej sprawy w systemie");
        }

        if (authorizedCases.size() == 1) {
            return CaseStatusReply.template(authorizedCases.get(0));
        }

        List<CaseDirectory.CaseRecord> matchingByName = authorizedCases.stream()
                .filter(c -> mentionsName(mailText, c.patientFirstName()))
                .toList();

        if (matchingByName.size() == 1) {
            return CaseStatusReply.template(matchingByName.get(0));
        }

        return CaseStatusReply.escalateToHuman(matchingByName.isEmpty()
                ? "opiekun ma więcej niż jedno dziecko w systemie, mail nie wskazuje jednoznacznie którego dotyczy"
                : "mail wymienia więcej niż jedno dziecko tego opiekuna jednocześnie");
    }

    public CaseStatusReply handleByCode(String senderAddress, String caseCode) {
        if (!caseDirectory.isLegalRepresentative(senderAddress, caseCode)) {
            return CaseStatusReply.escalateToHuman("adres nadawcy nie jest zarejestrowanym przedstawicielem tej sprawy");
        }
        return caseDirectory.findByCode(caseCode)
                .map(CaseStatusReply::template)
                .orElseGet(() -> CaseStatusReply.escalateToHuman("niespójność danych"));
    }

    private boolean mentionsName(String text, String firstName) {
        String stem = nameStem(firstName);
        return Pattern.compile("(?i)\\b" + Pattern.quote(stem) + "\\w*").matcher(text).find();
    }

    private String nameStem(String firstName) {
        return firstName.length() <= 3 ? firstName : firstName.substring(0, firstName.length() - 2);
    }

    public sealed interface CaseStatusReply permits CaseStatusReply.Template, CaseStatusReply.EscalateToHuman {
        record Template(String caseCode, String patientFirstName, CaseDirectory.CaseStatusValue status, LocalDate expectedDate)
                implements CaseStatusReply {
            public String renderTemplate() {
                return switch (status) {
                    case OTWARTA -> "Sprawa dotycząca %s została przyjęta i oczekuje na rozpoczęcie diagnostyki."
                            .formatted(patientFirstName);
                    case W_TRAKCIE -> "Diagnostyka dotycząca %s jest w trakcie. Przewidywany termin zakończenia: %s."
                            .formatted(patientFirstName, expectedDate);
                    case OPINIA_GOTOWA -> "Opinia dla %s jest gotowa i oczekuje na wysyłkę.".formatted(patientFirstName);
                    case WYDANA -> "Opinia dla %s została wysłana %s.".formatted(patientFirstName, expectedDate);
                };
            }
        }

        record EscalateToHuman(String reason) implements CaseStatusReply {}

        static CaseStatusReply template(CaseDirectory.CaseRecord record) {
            return new Template(record.code(), record.patientFirstName(), record.status(), record.expectedDate());
        }

        static CaseStatusReply escalateToHuman(String reason) {
            return new EscalateToHuman(reason);
        }

        default String caseCodeForDisplay() {
            return switch (this) {
                case Template t -> t.caseCode();
                case EscalateToHuman e -> "?";
            };
        }
    }
}
