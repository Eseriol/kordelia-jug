package pl.kordelia.web;

import org.springframework.stereotype.Component;
import pl.kordelia.model.MailCategory;
import pl.kordelia.triage.MailRouter;
import pl.kordelia.web.dto.TriageResultDto;

@Component
public class TriageResultAssembler {
    public TriageResultDto toDto(String mailId, MailRouter.RoutingOutcome outcome) {
        String kategoria = categoryCode(outcome.triageResult().category());
        String zrodlo = source(outcome);
        String szczegoly = outcome.caseStatusReply() != null
                ? CaseStatusReplyPresenter.describe(outcome.caseStatusReply())
                : null;
        double pewnosc = Math.round(outcome.triageResult().confidence() * 100) / 100.0;
        return new TriageResultDto(mailId, kategoria, pewnosc, zrodlo, szczegoly);
    }

    private String categoryCode(MailCategory category) {
        return switch (category) {
            case MailCategory.TrainingOffer ignored -> "A_OFERTA_SZKOLEN";
            case MailCategory.Faq ignored -> "B_FAQ";
            case MailCategory.CaseStatus ignored -> "C_STATUS_SPRAWY";
            case MailCategory.Documentation ignored -> "D_DOKUMENTACJA";
            case MailCategory.Other ignored -> "E_INNE";
        };
    }

    private String source(MailRouter.RoutingOutcome outcome) {
        if (!outcome.wentThroughModel()) {
            return "BEZ_MODELU";
        }
        return outcome.triageResult().viaFallback() ? "REGULY_FALLBACK" : "MODEL";
    }
}
