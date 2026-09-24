package pl.kordelia.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.kordelia.inbox.HumanTaskQueue;
import pl.kordelia.model.HumanTask;
import pl.kordelia.web.dto.HumanTaskView;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class HumanTaskController {

    private final HumanTaskQueue humanTaskQueue;

    public HumanTaskController(HumanTaskQueue humanTaskQueue) {
        this.humanTaskQueue = humanTaskQueue;
    }

    @GetMapping
    public List<HumanTaskView> list() {
        return humanTaskQueue.newestFirst().stream().map(this::toView).toList();
    }

    @PostMapping("/resolve")
    public HumanTaskView resolve(@RequestParam String id) {
        return toView(humanTaskQueue.resolve(id));
    }

    private HumanTaskView toView(HumanTask task) {
        return new HumanTaskView(task.id(), task.reason(), task.context(), task.sourceMailId(), task.status().name());
    }
}
