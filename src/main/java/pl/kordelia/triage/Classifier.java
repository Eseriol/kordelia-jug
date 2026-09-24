package pl.kordelia.triage;

import pl.kordelia.model.IncomingMail;
import pl.kordelia.model.TriageResult;

public interface Classifier {
    TriageResult classify(IncomingMail mail);
}
