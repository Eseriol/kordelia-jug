package pl.kordelia.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.kordelia.inbox.AiInboxService;
import pl.kordelia.inbox.HumanTaskQueue;
import pl.kordelia.model.AiInboxStatus;
import pl.kordelia.model.TaskStatus;
import pl.kordelia.web.dto.StatsResponse;
import pl.kordelia.web.dto.TriageResultDto;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final TriageStatsStore statsStore;
    private final AiInboxService aiInboxService;
    private final HumanTaskQueue humanTaskQueue;

    public StatsController(TriageStatsStore statsStore, AiInboxService aiInboxService, HumanTaskQueue humanTaskQueue) {
        this.statsStore = statsStore;
        this.aiInboxService = aiInboxService;
        this.humanTaskQueue = humanTaskQueue;
    }

    @GetMapping
    public StatsResponse stats() {
        List<TriageResultDto> lastRun = statsStore.lastRun();

        long countA = countByCategory(lastRun, "A_OFERTA_SZKOLEN");
        long countB = countByCategory(lastRun, "B_FAQ");
        long countC = countByCategory(lastRun, "C_STATUS_SPRAWY");
        long countD = countByCategory(lastRun, "D_DOKUMENTACJA");
        long countE = countByCategory(lastRun, "E_INNE");

        long viaModel = countBySource(lastRun, "MODEL");
        long viaFallback = countBySource(lastRun, "REGULY_FALLBACK");
        long viaNoModel = countBySource(lastRun, "BEZ_MODELU");

        long inboxPending = aiInboxService.queueSortedByConfidenceDesc().stream()
                .filter(e -> e.status() == AiInboxStatus.OCZEKUJE).count();
        long inboxApproved = aiInboxService.queueSortedByConfidenceDesc().stream()
                .filter(e -> e.status() == AiInboxStatus.ZATWIERDZONE).count();
        long inboxRejected = aiInboxService.queueSortedByConfidenceDesc().stream()
                .filter(e -> e.status() == AiInboxStatus.ODRZUCONE).count();

        long tasksOpen = humanTaskQueue.newestFirst().stream()
                .filter(t -> t.status() == TaskStatus.OCZEKUJE).count();
        long tasksResolved = humanTaskQueue.newestFirst().stream()
                .filter(t -> t.status() == TaskStatus.OBSLUZONE).count();

        return new StatsResponse(lastRun.size(), countA, countB, countC, countD, countE,
                viaModel, viaFallback, viaNoModel, inboxPending, inboxApproved, inboxRejected,
                tasksOpen, tasksResolved);
    }

    private long countByCategory(List<TriageResultDto> results, String kategoria) {
        return results.stream().filter(r -> r.kategoria().equals(kategoria)).count();
    }

    private long countBySource(List<TriageResultDto> results, String zrodlo) {
        return results.stream().filter(r -> r.zrodlo().equals(zrodlo)).count();
    }
}
