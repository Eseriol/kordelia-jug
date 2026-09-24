package pl.kordelia.inbox;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ActivityLog {
    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());
    private final LogEmitterRegistry emitterRegistry;

    public ActivityLog(LogEmitterRegistry emitterRegistry) {
        this.emitterRegistry = emitterRegistry;
    }

    public void add(String message) {
        String line = LocalTime.now().withNano(0) + " " + message;
        lines.add(line);
        System.out.println(line);
        emitterRegistry.broadcast(line);
    }

    public List<String> snapshot() {
        synchronized (lines) {
            return new ArrayList<>(lines);
        }
    }

    public void clear() {
        lines.clear();
    }

    public SseEmitter subscribe() {
        return emitterRegistry.register();
    }
}
