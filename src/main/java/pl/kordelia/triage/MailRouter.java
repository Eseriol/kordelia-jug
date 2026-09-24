package pl.kordelia.triage;

import pl.kordelia.model.IncomingMail;
import pl.kordelia.model.MailCategory;
import pl.kordelia.model.TriageResult;

import java.util.List;

public final class MailRouter {
    private static final List<String> STATUS_INTENT_PHRASES = List.of(
            "status sprawy", "na jakim etapie", "czy gotowa", "czy jest gotowa", "już gotowa",
            "kiedy będzie gotowa", "kiedy otrzymam", "opinię dla", "opinia dla", "opinii dla",
            "co słychać w sprawie", "jak sprawa", "czy opinia"
    );

    private final Classifier classifier;
    private final CaseStatusPipeline caseStatusPipeline;

    public MailRouter(Classifier classifier, CaseStatusPipeline caseStatusPipeline) {
        this.classifier = classifier;
        this.caseStatusPipeline = caseStatusPipeline;
    }

    public RoutingOutcome route(IncomingMail mail) {
        String fullText = (mail.subject() + " " + mail.body());

        if (looksLikeStatusInquiry(fullText)) {
            var reply = caseStatusPipeline.resolve(mail.senderAddress(), fullText);
            return new RoutingOutcome(TriageResult.ofCaseStatus(reply.caseCodeForDisplay()), reply);
        }

        TriageResult result = classifier.classify(mail);
        return new RoutingOutcome(result, null);
    }

    private boolean looksLikeStatusInquiry(String text) {
        String lower = text.toLowerCase();
        return STATUS_INTENT_PHRASES.stream().anyMatch(lower::contains);
    }

    public record RoutingOutcome(TriageResult triageResult, CaseStatusPipeline.CaseStatusReply caseStatusReply) {
        public boolean wentThroughModel() {
            return caseStatusReply == null;
        }
    }
}
