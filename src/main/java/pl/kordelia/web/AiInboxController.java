package pl.kordelia.web;

import org.springframework.web.bind.annotation.*;
import pl.kordelia.inbox.AiInboxService;
import pl.kordelia.model.AiInboxEntry;
import pl.kordelia.web.dto.AiInboxEntryView;

import java.util.List;

@RestController
@RequestMapping("/api/ai-inbox")
public class AiInboxController {
    private final AiInboxService aiInboxService;

    public AiInboxController(AiInboxService aiInboxService) {
        this.aiInboxService = aiInboxService;
    }

    @GetMapping
    public List<AiInboxEntryView> list() {
        return aiInboxService.queueSortedByConfidenceDesc().stream().map(this::toView).toList();
    }

    @PostMapping("/approve")
    public AiInboxEntryView approve(@RequestParam String id) {
        return toView(aiInboxService.approve(id, "demo-user"));
    }

    @PostMapping("/reject")
    public AiInboxEntryView reject(@RequestParam String id) {
        return toView(aiInboxService.reject(id, "demo-user"));
    }

    private AiInboxEntryView toView(AiInboxEntry entry) {
        return new AiInboxEntryView(
                entry.id(),
                entry.type().name(),
                Math.round(entry.confidence() * 100) / 100.0,
                entry.proposedSummary(),
                entry.status().name(),
                entry.eligibleForSerialMode(),
                entry.decidedBy()
        );
    }
}
