package pl.kordelia.web;

import org.springframework.stereotype.Component;
import pl.kordelia.web.dto.TriageResultDto;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class TriageStatsStore {

    private final AtomicReference<List<TriageResultDto>> lastRun = new AtomicReference<>(List.of());

    public void recordRun(List<TriageResultDto> results) {
        lastRun.set(List.copyOf(results));
    }

    public List<TriageResultDto> lastRun() {
        return lastRun.get();
    }

    public void clear() {
        lastRun.set(List.of());
    }
}
