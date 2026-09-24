package pl.kordelia.triage;

import pl.kordelia.model.IncomingMail;
import pl.kordelia.model.MailCategory;
import pl.kordelia.model.TriageResult;

public final class RuleBasedClassifier implements Classifier {
    @Override
    public TriageResult classify(IncomingMail mail) {
        String text = (mail.subject() + " " + mail.body()).toLowerCase();

        if (containsAny(text, "szkolenie", "kurs dla rodziców", "oferta szkoleniowa", "warsztaty")) {
            return new TriageResult(new MailCategory.TrainingOffer(mail.senderAddress()), confidenceFor(mail, 0.80, 0.95), false);
        }

        if (containsAny(text, "jak wygląda proces", "czy potrzebne jest skierowanie", "jak umówić", "ile trwa diagnoza")) {
            return new TriageResult(new MailCategory.Faq(mail.subject()), confidenceFor(mail, 0.75, 0.92), false);
        }

        if (containsAny(text, "w załączniku", "przesyłam dokument", "skierowanie", "wyniki badań", "opinia psychologiczna")) {
            return new TriageResult(new MailCategory.Documentation(extractFileNameHint(text)), confidenceFor(mail, 0.70, 0.98), false);
        }

        return new TriageResult(new MailCategory.Other(truncate(text, 80)), confidenceFor(mail, 0.30, 0.60), false);
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private double confidenceFor(IncomingMail mail, double min, double max) {
        int hash = Math.abs(mail.id().hashCode());
        double fraction = (hash % 1000) / 1000.0;
        return min + fraction * (max - min);
    }

    private String extractFileNameHint(String text) {
        if (text.contains("wyniki")) return "wyniki-badan.pdf";
        if (text.contains("skierowanie")) return "skierowanie.pdf";
        if (text.contains("opinia")) return "opinia-psychologiczna.pdf";
        return "dokument.pdf";
    }

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }
}
