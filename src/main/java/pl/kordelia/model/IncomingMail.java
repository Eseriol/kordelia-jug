package pl.kordelia.model;

public record IncomingMail(
        String id,
        String senderAddress,
        String subject,
        String body,
        MailProcessingStatus status
) {
    public IncomingMail withStatus(MailProcessingStatus newStatus) {
        return new IncomingMail(id, senderAddress, subject, body, newStatus);
    }
}
