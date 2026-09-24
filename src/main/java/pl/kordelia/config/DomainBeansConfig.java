package pl.kordelia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.kordelia.inbox.CaseDirectory;
import pl.kordelia.triage.CaseStatusPipeline;
import pl.kordelia.triage.Classifier;
import pl.kordelia.triage.MailRouter;

@Configuration
public class DomainBeansConfig {
    @Bean
    public CaseDirectory caseDirectory() {
        return new CaseDirectory();
    }

    @Bean
    public CaseStatusPipeline caseStatusPipeline(CaseDirectory caseDirectory) {
        return new CaseStatusPipeline(caseDirectory);
    }

    @Bean
    public MailRouter mailRouter(Classifier classifier, CaseStatusPipeline caseStatusPipeline) {
        return new MailRouter(classifier, caseStatusPipeline);
    }
}
