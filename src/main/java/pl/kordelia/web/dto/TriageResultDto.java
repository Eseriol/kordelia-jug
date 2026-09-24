package pl.kordelia.web.dto;

public record TriageResultDto(String mailId, String kategoria, double pewnosc, String zrodlo, String szczegoly) {
}
