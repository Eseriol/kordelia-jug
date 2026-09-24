package pl.kordelia;

import pl.kordelia.inbox.CaseDirectory;
import pl.kordelia.model.IncomingMail;
import pl.kordelia.model.MailCategory;
import pl.kordelia.model.MailProcessingStatus;
import pl.kordelia.triage.CaseStatusPipeline;
import pl.kordelia.triage.MailRouter;
import pl.kordelia.triage.RuleBasedClassifier;

import java.util.ArrayList;
import java.util.List;

public final class SelfTest {
    private static int passed = 0;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) {
        testFaqClassifiedAboveConfidenceThreshold();
        testTrainingOfferClassifiedCorrectly();
        testCaseStatusNeverGoesThroughModel();
        testCaseStatusEscalatesOnSenderMismatch();
        testCaseStatusRevealsNothingOnMismatch();
        testGuardianOfTwoChildrenResolvedByNameOnly();
        testLowConfidenceOtherNeverQualifiesForSerialMode();

        System.out.println();
        System.out.println("=== WYNIK: " + passed + " testów przeszło, " + failures.size() + " nie przeszło ===");
        if (!failures.isEmpty()) {
            failures.forEach(f -> System.out.println("  ✗ " + f));
            System.exit(1);
        }
    }

    private static void testFaqClassifiedAboveConfidenceThreshold() {
        var classifier = new RuleBasedClassifier();
        var mail = new IncomingMail("test-1", "rodzic@example.com", "Pytanie",
                "Jak wygląda proces umówienia pierwszej wizyty diagnostycznej?",
                MailProcessingStatus.OCZEKUJE_NA_TRIAGE);

        var result = classifier.classify(mail);

        check("FAQ powinno być rozpoznane jako Faq",
                result.category() instanceof MailCategory.Faq);
        check("Pewność FAQ powinna być >= 0.70",
                result.confidence() >= 0.70);
    }

    private static void testTrainingOfferClassifiedCorrectly() {
        var classifier = new RuleBasedClassifier();
        var mail = new IncomingMail("test-2", "rodzic@example.com", "Oferta szkoleniowa",
                "Czy mają Państwo szkolenie dla rodziców w tym miesiącu?",
                MailProcessingStatus.OCZEKUJE_NA_TRIAGE);

        var result = classifier.classify(mail);

        check("Oferta szkoleń powinna być rozpoznana jako TrainingOffer",
                result.category() instanceof MailCategory.TrainingOffer);
    }

    private static void testCaseStatusNeverGoesThroughModel() {
        var caseDirectory = new CaseDirectory();
        var router = new MailRouter(new RuleBasedClassifier(), new CaseStatusPipeline(caseDirectory));
        var mail = new IncomingMail("test-3", "opiekun4471@example.com", "Pytanie o opinię",
                "Czy opinia dla Natalki jest już gotowa?", MailProcessingStatus.OCZEKUJE_NA_TRIAGE);

        var outcome = router.route(mail);

        check("Kategoria C nie powinna przechodzić przez model (LLM)",
                !outcome.wentThroughModel());
        check("Kategoria powinna być CaseStatus",
                outcome.triageResult().category() instanceof MailCategory.CaseStatus);
        check("Pewność kategorii C zawsze wynosi dokładnie 1.0 (nie jest to 'model', tylko fakt architektoniczny)",
                outcome.triageResult().confidence() == 1.0);
    }

    private static void testCaseStatusEscalatesOnSenderMismatch() {
        var caseDirectory = new CaseDirectory();
        var pipeline = new CaseStatusPipeline(caseDirectory);

        var reply = pipeline.resolve("nieuprawniony@example.com", "Czy opinia dla Natalki jest gotowa?");

        check("Niewłaściwy nadawca powinien skutkować eskalacją do człowieka, nie odpowiedzią",
                reply instanceof CaseStatusPipeline.CaseStatusReply.EscalateToHuman);
    }

    private static void testCaseStatusRevealsNothingOnMismatch() {
        var caseDirectory = new CaseDirectory();
        var pipeline = new CaseStatusPipeline(caseDirectory);

        var replyUnknownSender = pipeline.resolve("ktos@example.com", "Czy opinia dla Natalki jest gotowa?");
        var replyNoNameGiven = pipeline.resolve("ktos@example.com", "Czy nasza sprawa jest już gotowa?");

        check("Nieznany nadawca: eskalacja, nie błąd 'nie znaleziono'",
                replyUnknownSender instanceof CaseStatusPipeline.CaseStatusReply.EscalateToHuman);
        check("Nieznany nadawca bez i z imieniem dziecka: taka sama eskalacja",
                replyNoNameGiven.getClass() == replyUnknownSender.getClass());
    }

    private static void testGuardianOfTwoChildrenResolvedByNameOnly() {
        var caseDirectory = new CaseDirectory();
        var pipeline = new CaseStatusPipeline(caseDirectory);

        var replyWithName = pipeline.resolve("opiekun.dwojga@example.com",
                "Chciałabym zapytać o opinię dla Julii — na jakim etapie jest sprawa?");
        var replyWithoutName = pipeline.resolve("opiekun.dwojga@example.com",
                "Czy nasza sprawa jest już gotowa?");

        check("Opiekun dwojga dzieci + podane imię: powinno rozpoznać właściwe dziecko",
                replyWithName instanceof CaseStatusPipeline.CaseStatusReply.Template t
                        && t.patientFirstName().equals("Julia"));
        check("Opiekun dwojga dzieci BEZ imienia w treści: eskalacja, system nie zgaduje którego dziecka dotyczy",
                replyWithoutName instanceof CaseStatusPipeline.CaseStatusReply.EscalateToHuman);
    }

    private static void testLowConfidenceOtherNeverQualifiesForSerialMode() {
        var classifier = new RuleBasedClassifier();
        var mail = new IncomingMail("test-4", "ktos@example.com", "cześć",
                "mam pytanie w sprawie mojego dziecka", MailProcessingStatus.OCZEKUJE_NA_TRIAGE);

        var result = classifier.classify(mail);

        check("Niejednoznaczny mail powinien wylądować w kategorii 'Inne'",
                result.category() instanceof MailCategory.Other);
        check("Kategoria 'Inne' powinna mieć niską górną granicę pewności (< 0.90)",
                result.confidence() < 0.90);
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  ✓ " + description);
        } else {
            failures.add(description);
        }
    }
}
