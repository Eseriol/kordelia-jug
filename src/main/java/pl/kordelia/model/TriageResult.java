package pl.kordelia.model;

public record TriageResult(MailCategory category, double confidence, boolean viaFallback) {
    public TriageResult {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence musi być w przedziale [0,1], było: " + confidence);
        }
    }

    public static TriageResult ofCaseStatus(String caseCode) {
        return new TriageResult(new MailCategory.CaseStatus(caseCode), 1.0, false);
    }
}
