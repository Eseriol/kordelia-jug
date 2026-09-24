package pl.kordelia.web.dto;

public record StatsResponse(
        int przetworzoneOstatnioRazem,
        long kategoriaA,
        long kategoriaB,
        long kategoriaC,
        long kategoriaD,
        long kategoriaE,
        long przezModel,
        long przezReguly,
        long bezModelu,
        long inboxOczekuje,
        long inboxZatwierdzone,
        long inboxOdrzucone,
        long zadaniaOtwarte,
        long zadaniaObsluzone
) {
}
