package pl.kordelia.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pl.kordelia.inbox.ActivityLog;

import java.util.List;

@RestController
@RequestMapping("/api/log")
public class LogController {
    private final ActivityLog activityLog;

    public LogController(ActivityLog activityLog) {
        this.activityLog = activityLog;
    }

    @GetMapping
    public List<String> history() {
        return activityLog.snapshot();
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return activityLog.subscribe();
    }
}
