package pl.kordelia.model;

public sealed interface MailCategory
        permits MailCategory.TrainingOffer, MailCategory.Faq, MailCategory.CaseStatus,
                MailCategory.Documentation, MailCategory.Other {
    record TrainingOffer(String to) implements MailCategory {}

    record Faq(String q) implements MailCategory {}

    record CaseStatus(String code) implements MailCategory {}

    record Documentation(String fileName) implements MailCategory {}

    record Other(String raw) implements MailCategory {}
}
