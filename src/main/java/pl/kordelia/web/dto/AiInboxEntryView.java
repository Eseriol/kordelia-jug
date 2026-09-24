package pl.kordelia.web.dto;

public record AiInboxEntryView(
        String id,
        String typ,
        double pewnosc,
        String opis,
        String status,
        boolean trybSeryjnyMozliwy,
        String decydent
) {
}
