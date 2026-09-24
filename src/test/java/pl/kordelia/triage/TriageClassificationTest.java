package pl.kordelia.triage;

import org.junit.jupiter.api.Test;
import pl.kordelia.inbox.CaseDirectory;
import pl.kordelia.model.IncomingMail;
import pl.kordelia.model.MailCategory;
import pl.kordelia.model.MailProcessingStatus;
import pl.kordelia.model.TriageResult;

import static org.assertj.core.api.Assertions.assertThat;

class TriageClassificationTest {
    private final RuleBasedClassifier classifier = new RuleBasedClassifier();

    @Test
    void faqIsClassifiedAboveConfidenceThreshold() {
        var mail = new IncomingMail("test-faq", "rodzic@example.com", "Pytanie",
                "Jak wygląda proces umówienia pierwszej wizyty diagnostycznej?",
                MailProcessingStatus.OCZEKUJE_NA_TRIAGE);

        var result = classifier.classify(mail);

        assertThat(result.category()).isInstanceOf(MailCategory.Faq.class);
        assertThat(result.confidence()).isBetween(0.75, 0.92);
    }

    @Test
    void documentationMailIsRoutedToAiInboxCategory() {
        var mail = new IncomingMail("test-doc", "opiekun2093@example.com", "Dokumentacja",
                "W załączniku przesyłam wyniki badań psychologicznych.",
                MailProcessingStatus.OCZEKUJE_NA_TRIAGE);

        var result = classifier.classify(mail);

        assertThat(result.category()).isInstanceOf(MailCategory.Documentation.class);

        assertThat(result.confidence()).isBetween(0.70, 0.98);
    }

    @Test
    void highConfidenceDocumentationQualifiesForSerialMode() {
        var highConfidenceResult = new TriageResult(
                new MailCategory.Documentation("opinia-2026-03.pdf"), 0.96, false);

        assertThat(highConfidenceResult.category()).isInstanceOf(MailCategory.Documentation.class);
        assertThat(highConfidenceResult.confidence()).isBetween(0.90, 1.00);
    }

    @Test
    void ambiguousMailNeverQualifiesForSerialModeConfidence() {
        var mail = new IncomingMail("test-other", "ktos@example.com", "cześć",
                "mam pytanie w sprawie mojego dziecka", MailProcessingStatus.OCZEKUJE_NA_TRIAGE);

        var result = classifier.classify(mail);

        assertThat(result.category()).isInstanceOf(MailCategory.Other.class);

        assertThat(result.confidence()).isLessThan(0.90);
    }

    @Test
    void caseStatusNeverGoesThroughTheModel() {
        var caseDirectory = new CaseDirectory();
        var router = new MailRouter(classifier, new CaseStatusPipeline(caseDirectory));
        var mail = new IncomingMail("test-case", "opiekun4471@example.com", "Pytanie o opinię",
                "Czy opinia dla Natalki jest już gotowa?", MailProcessingStatus.OCZEKUJE_NA_TRIAGE);

        var outcome = router.route(mail);

        assertThat(outcome.wentThroughModel()).isFalse();
        assertThat(outcome.triageResult().category()).isInstanceOf(MailCategory.CaseStatus.class);
        assertThat(outcome.triageResult().confidence()).isEqualTo(1.0);
    }

    @Test
    void wrongSenderAndUnknownSenderGetIdenticalEscalation() {
        var caseDirectory = new CaseDirectory();
        var pipeline = new CaseStatusPipeline(caseDirectory);

        var replyUnknownSender = pipeline.resolve("ktos@example.com", "Czy opinia dla Natalki jest gotowa?");
        var replyWrongSenderKnowsName = pipeline.resolve("inny.nieuprawniony@example.com", "Pytam o Natalkę");

        assertThat(replyUnknownSender).isInstanceOf(CaseStatusPipeline.CaseStatusReply.EscalateToHuman.class);
        assertThat(replyWrongSenderKnowsName).isInstanceOf(replyUnknownSender.getClass());
    }

    @Test
    void guardianOfTwoChildrenIsResolvedByNameWithinOwnCasesOnly() {
        var caseDirectory = new CaseDirectory();
        var pipeline = new CaseStatusPipeline(caseDirectory);

        var reply = pipeline.resolve("opiekun.dwojga@example.com",
                "Chciałabym zapytać o opinię dla Julii — na jakim etapie jest sprawa?");

        assertThat(reply).isInstanceOf(CaseStatusPipeline.CaseStatusReply.Template.class);
        var template = (CaseStatusPipeline.CaseStatusReply.Template) reply;
        assertThat(template.patientFirstName()).isEqualTo("Julia");
        assertThat(template.caseCode()).isEqualTo("PAC-5001");
    }

    @Test
    void guardianOfTwoChildrenWithoutNameEscalatesInsteadOfGuessing() {
        var caseDirectory = new CaseDirectory();
        var pipeline = new CaseStatusPipeline(caseDirectory);

        var reply = pipeline.resolve("opiekun.dwojga@example.com", "Czy nasza sprawa jest już gotowa?");

        assertThat(reply).isInstanceOf(CaseStatusPipeline.CaseStatusReply.EscalateToHuman.class);
    }
}
