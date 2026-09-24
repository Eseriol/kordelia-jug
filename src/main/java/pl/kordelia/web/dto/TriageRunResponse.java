package pl.kordelia.web.dto;

import java.util.List;

public record TriageRunResponse(int przetworzoneMaile, List<TriageResultDto> wyniki) {
}
