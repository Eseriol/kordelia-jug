package pl.kordelia.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.kordelia.inbox.CaseDirectory;
import pl.kordelia.inbox.HumanTaskQueue;
import pl.kordelia.triage.CaseStatusPipeline;
import pl.kordelia.web.dto.CaseStatusResponse;

@RestController
@RequestMapping("/api/case-status")
public class CaseStatusController {

    private final CaseStatusPipeline pipeline;
    private final HumanTaskQueue humanTaskQueue;

    public CaseStatusController(CaseDirectory caseDirectory, HumanTaskQueue humanTaskQueue) {
        this.pipeline = new CaseStatusPipeline(caseDirectory);
        this.humanTaskQueue = humanTaskQueue;
    }

    @PostMapping
    public CaseStatusResponse check(@RequestParam String sender, @RequestParam String tekst) {
        CaseStatusPipeline.CaseStatusReply reply = pipeline.resolve(sender, tekst);
        String wynik = CaseStatusReplyPresenter.describe(reply);
        String typ = reply instanceof CaseStatusPipeline.CaseStatusReply.Template ? "TEMPLATE" : "ESCALATE_TO_HUMAN";

        if (reply instanceof CaseStatusPipeline.CaseStatusReply.EscalateToHuman escalation) {
            String context = "„%s” — od %s".formatted(tekst, sender);
            humanTaskQueue.add(escalation.reason(), context, null);
        }

        return new CaseStatusResponse(sender, tekst, wynik, typ);
    }
}
