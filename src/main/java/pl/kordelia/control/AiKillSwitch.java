package pl.kordelia.control;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AiKillSwitch {
    private final AtomicBoolean disabled = new AtomicBoolean(false);

    public boolean isDisabled() {
        return disabled.get();
    }

    public void disable() {
        disabled.set(true);
    }

    public void enable() {
        disabled.set(false);
    }
}
