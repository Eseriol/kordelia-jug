package pl.kordelia.web;

import org.springframework.stereotype.Component;
import pl.kordelia.inbox.ActivityLog;
import pl.kordelia.inbox.AiInboxService;
import pl.kordelia.inbox.HumanTaskQueue;
import pl.kordelia.inbox.MailStore;
import pl.kordelia.model.AiInboxEntry;
import pl.kordelia.model.HumanTask;
import pl.kordelia.model.IncomingMail;
import pl.kordelia.model.InboxEntryType;
import pl.kordelia.model.MailCategory;
import pl.kordelia.triage.CaseStatusPipeline;
import pl.kordelia.triage.MailRouter;
import pl.kordelia.web.dto.TriageResultDto;
import pl.kordelia.web.dto.TriageRunResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
public class TriageOrchestrator {

    private static final Duration RESULT_TIMEOUT = Duration.ofSeconds(120);

    private final MailStore mailStore;
    private final MailRouter mailRouter;
    private final AiInboxService aiInboxService;
    private final HumanTaskQueue humanTaskQueue;
    private final ActivityLog activityLog;
    private final TriageResultAssembler resultAssembler;
    private final TriageStatsStore statsStore;

    public TriageOrchestrator(MailStore mailStore, MailRouter mailRouter, AiInboxService aiInboxService,
                               HumanTaskQueue humanTaskQueue, ActivityLog activityLog,
                               TriageResultAssembler resultAssembler, TriageStatsStore statsStore) {
        this.mailStore = mailStore;
        this.mailRouter = mailRouter;
        this.aiInboxService = aiInboxService;
        this.humanTaskQueue = humanTaskQueue;
        this.activityLog = activityLog;
        this.resultAssembler = resultAssembler;
        this.statsStore = statsStore;
    }

    public TriageRunResponse runAll() throws InterruptedException {
        List<IncomingMail> pending = mailStore.pending();
        List<TriageResultDto> results = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<TriageResultDto>> futures = pending.stream()
                    .map(mail -> executor.submit(() -> processOne(mail)))
                    .toList();
            collectAll(pending, futures, results);
        }

        statsStore.recordRun(results);
        return new TriageRunResponse(results.size(), results);
    }

    private void collectAll(List<IncomingMail> pending, List<Future<TriageResultDto>> futures, List<TriageResultDto> results) {
        for (int i = 0; i < futures.size(); i++) {
            collectOne(pending.get(i).id(), futures.get(i), results);
        }
    }

    private void collectOne(String mailId, Future<TriageResultDto> future, List<TriageResultDto> results) {
        try {
            results.add(future.get(RESULT_TIMEOUT.toSeconds(), TimeUnit.SECONDS));
        } catch (TimeoutException e) {
            activityLog.add("⚠ " + mailId + " — przekroczono " + RESULT_TIMEOUT.toSeconds()
                    + "s, zadanie działa dalej w tle, nie trafi do wyniku tego uruchomienia");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            activityLog.add("⚠ " + mailId + " — błąd przetwarzania: "
                    + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private TriageResultDto processOne(IncomingMail mail) {
        MailRouter.RoutingOutcome[] outcomeHolder = new MailRouter.RoutingOutcome[1];
        AgentIdentity.runAs(() -> outcomeHolder[0] = routeAndRecord(mail));
        return resultAssembler.toDto(mail.id(), outcomeHolder[0]);
    }

    private MailRouter.RoutingOutcome routeAndRecord(IncomingMail mail) {
        MailRouter.RoutingOutcome outcome = mailRouter.route(mail);
        mailStore.markProcessed(mail.id());
        AiInboxEntry proposedEntry = proposeIfDocumentation(mail, outcome);
        HumanTask createdTask = createTaskIfNeeded(mail, outcome);
        activityLog.add(describeOutcome(mail, outcome, proposedEntry, createdTask));
        return outcome;
    }

    private AiInboxEntry proposeIfDocumentation(IncomingMail mail, MailRouter.RoutingOutcome outcome) {
        if (!(outcome.triageResult().category() instanceof MailCategory.Documentation doc)) {
            return null;
        }
        String summary = "„%s” — od %s, dokument: %s".formatted(mail.subject(), mail.senderAddress(), doc.fileName());
        return aiInboxService.propose(InboxEntryType.NOWY_DOKUMENT, outcome.triageResult().confidence(), summary, mail.id());
    }

    private HumanTask createTaskIfNeeded(IncomingMail mail, MailRouter.RoutingOutcome outcome) {
        String context = "„%s” — od %s".formatted(mail.subject(), mail.senderAddress());

        if (outcome.triageResult().category() instanceof MailCategory.CaseStatus
                && outcome.caseStatusReply() instanceof CaseStatusPipeline.CaseStatusReply.EscalateToHuman escalation) {
            return humanTaskQueue.add(escalation.reason(), context, mail.id());
        }

        if (outcome.triageResult().category() instanceof MailCategory.Other) {
            return humanTaskQueue.add("Kategoria E — nie pasuje do żadnej rozpoznanej sprawy, wymaga oceny", context, mail.id());
        }

        return null;
    }

    private String describeOutcome(IncomingMail mail, MailRouter.RoutingOutcome outcome, AiInboxEntry proposedEntry, HumanTask createdTask) {
        String thread = Thread.currentThread().toString();
        String source = outcome.triageResult().viaFallback() ? " [REGUŁY — fallback, nie model]" : " [model]";

        return switch (outcome.triageResult().category()) {
            case MailCategory.Documentation ignored -> "[%s] %s → kategoria D%s → ai_inbox: %s"
                    .formatted(thread, mail.id(), source, proposedEntry.id());
            case MailCategory.CaseStatus ignored -> "[%s] %s → kategoria C (BEZ MODELU, z definicji) → %s%s"
                    .formatted(thread, mail.id(), CaseStatusReplyPresenter.describe(outcome.caseStatusReply()),
                            createdTask != null ? " → zadanie: " + createdTask.id() : "");
            case MailCategory.TrainingOffer ignored -> "[%s] %s → kategoria A%s (oferta szkoleń)"
                    .formatted(thread, mail.id(), source);
            case MailCategory.Faq ignored -> "[%s] %s → kategoria B%s (FAQ)"
                    .formatted(thread, mail.id(), source);
            case MailCategory.Other ignored -> "[%s] %s → kategoria E%s → zadanie: %s"
                    .formatted(thread, mail.id(), source, createdTask.id());
        };
    }
}
