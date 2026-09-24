package pl.kordelia.inbox;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CaseDirectory {
    public enum CaseStatusValue { OTWARTA, W_TRAKCIE, OPINIA_GOTOWA, WYDANA }

    public record CaseRecord(String code, String patientFirstName, CaseStatusValue status, LocalDate expectedDate) {}

    private final Map<String, CaseRecord> casesByCode = Map.of(
            "PAC-4471", new CaseRecord("PAC-4471", "Natalka", CaseStatusValue.OPINIA_GOTOWA, LocalDate.now().plusDays(2)),
            "PAC-2093", new CaseRecord("PAC-2093", "Kacper", CaseStatusValue.W_TRAKCIE, LocalDate.now().plusDays(10)),
            "PAC-3312", new CaseRecord("PAC-3312", "Zosia", CaseStatusValue.WYDANA, LocalDate.now().minusDays(5)),
            "PAC-5001", new CaseRecord("PAC-5001", "Julia", CaseStatusValue.W_TRAKCIE, LocalDate.now().plusDays(14)),
            "PAC-5002", new CaseRecord("PAC-5002", "Mikołaj", CaseStatusValue.OPINIA_GOTOWA, LocalDate.now().plusDays(1))
    );

    private final Map<String, List<String>> caseCodesByGuardianEmail = Map.of(
            "opiekun4471@example.com", List.of("PAC-4471"),
            "opiekun2093@example.com", List.of("PAC-2093"),
            "opiekun3312@example.com", List.of("PAC-3312"),
            "opiekun.dwojga@example.com", List.of("PAC-5001", "PAC-5002")
    );

    public List<CaseRecord> findCasesForGuardian(String senderAddress) {
        List<String> codes = caseCodesByGuardianEmail.getOrDefault(normalizeEmail(senderAddress), List.of());
        return codes.stream().map(casesByCode::get).filter(c -> c != null).toList();
    }

    public Optional<CaseRecord> findByCode(String caseCode) {
        return Optional.ofNullable(casesByCode.get(caseCode));
    }

    public boolean isLegalRepresentative(String senderAddress, String caseCode) {
        return findCasesForGuardian(senderAddress).stream().anyMatch(c -> c.code().equals(caseCode));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
