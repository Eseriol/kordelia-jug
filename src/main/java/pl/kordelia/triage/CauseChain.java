package pl.kordelia.triage;

public final class CauseChain {

    private static final int MAX_DEPTH = 6;

    private CauseChain() {}

    public static String describe(Throwable throwable) {
        StringBuilder result = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < MAX_DEPTH) {
            if (depth > 0) {
                result.append(" ← ");
            }
            result.append(current.getClass().getSimpleName());
            if (current.getMessage() != null) {
                result.append(": ").append(current.getMessage());
            }
            current = current.getCause();
            depth++;
        }
        return result.toString();
    }
}
