package pl.kordelia.inbox;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LogEmitterRegistry {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        return emitter;
    }

    public void broadcast(String line) {
        for (SseEmitter emitter : emitters) {
            send(emitter, line);
        }
    }

    private void send(SseEmitter emitter, String line) {
        try {
            emitter.send(SseEmitter.event().data(line));
        } catch (IOException | IllegalStateException ex) {
            emitters.remove(emitter);
        }
    }
}
