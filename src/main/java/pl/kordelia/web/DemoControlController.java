package pl.kordelia.web;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.kordelia.control.AiKillSwitch;
import pl.kordelia.inbox.ActivityLog;
import pl.kordelia.inbox.AiInboxService;
import pl.kordelia.inbox.HumanTaskQueue;
import pl.kordelia.inbox.MailStore;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoControlController {

    private final AiKillSwitch killSwitch;
    private final MailStore mailStore;
    private final AiInboxService aiInboxService;
    private final HumanTaskQueue humanTaskQueue;
    private final ActivityLog activityLog;
    private final TriageStatsStore statsStore;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public DemoControlController(AiKillSwitch killSwitch, MailStore mailStore, AiInboxService aiInboxService,
                                  HumanTaskQueue humanTaskQueue, ActivityLog activityLog,
                                  TriageStatsStore statsStore, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.killSwitch = killSwitch;
        this.mailStore = mailStore;
        this.aiInboxService = aiInboxService;
        this.humanTaskQueue = humanTaskQueue;
        this.activityLog = activityLog;
        this.statsStore = statsStore;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @GetMapping("/ai-status")
    public Map<String, Object> status() {
        return Map.of("aiEnabled", !killSwitch.isDisabled());
    }

    @PostMapping("/ai-disable")
    public Map<String, Object> disable() {
        killSwitch.disable();
        return Map.of("aiEnabled", false);
    }

    @PostMapping("/ai-enable")
    public Map<String, Object> enable() {
        killSwitch.enable();
        circuitBreakerRegistry.circuitBreaker("aiProvider").transitionToClosedState();
        return Map.of("aiEnabled", true);
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        mailStore.reset();
        aiInboxService.clear();
        humanTaskQueue.clear();
        statsStore.clear();
        activityLog.clear();
        activityLog.add("--- Demo zresetowane — można uruchomić triage od nowa ---");
        return Map.of("status", "ok");
    }
}
