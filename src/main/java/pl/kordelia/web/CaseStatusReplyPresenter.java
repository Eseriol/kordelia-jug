package pl.kordelia.web;

import pl.kordelia.triage.CaseStatusPipeline;

public final class CaseStatusReplyPresenter {
    private CaseStatusReplyPresenter() {}

    public static String describe(CaseStatusPipeline.CaseStatusReply reply) {
        return switch (reply) {
            case CaseStatusPipeline.CaseStatusReply.Template t -> t.renderTemplate();
            case CaseStatusPipeline.CaseStatusReply.EscalateToHuman e ->
                    "Eskalacja do człowieka (" + e.reason() + ") — żadna informacja nie została ujawniona nadawcy.";
        };
    }
}
