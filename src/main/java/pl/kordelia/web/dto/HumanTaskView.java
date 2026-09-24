package pl.kordelia.web.dto;

public record HumanTaskView(String id, String reason, String context, String sourceMailId, String status) {
}
