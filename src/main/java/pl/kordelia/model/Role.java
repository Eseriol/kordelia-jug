package pl.kordelia.model;

public enum Role {
    AGENT_AI,
    SPECJALISTA,
    ADMIN;

    public static Role fromHeader(String header) {
        if (header == null || header.isBlank()) {
            return AGENT_AI;
        }
        try {
            return Role.valueOf(header.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AGENT_AI;
        }
    }

    public boolean canApprove() {
        return this == SPECJALISTA || this == ADMIN;
    }
}
