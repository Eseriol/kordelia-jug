package pl.kordelia.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.kordelia.inbox.MailStore;
import pl.kordelia.model.IncomingMail;
import pl.kordelia.web.dto.TriageRunResponse;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MailController {
    private final MailStore mailStore;
    private final TriageOrchestrator triageOrchestrator;

    public MailController(MailStore mailStore, TriageOrchestrator triageOrchestrator) {
        this.mailStore = mailStore;
        this.triageOrchestrator = triageOrchestrator;
    }

    @GetMapping("/mails")
    public List<IncomingMail> mails() {
        return mailStore.all();
    }

    @PostMapping("/triage/run")
    public TriageRunResponse runTriage() throws InterruptedException {
        return triageOrchestrator.runAll();
    }
}
