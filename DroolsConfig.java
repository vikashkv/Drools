import org.drools.core.impl.KieServicesImpl;
import org.drools.core.impl.KieContainerImpl;
import org.drools.core.impl.KieBaseImpl;
import org.drools.core.spi.KieSession;
import org.drools.core.spi.StatelessKieSession;
import org.drools.compiler.compiler.DroolsParserException;
import org.drools.compiler.compiler.io.memory.MemoryFileSystem;
import org.drools.core.impl.KieBaseConfigurationImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@Configuration
public class DroolsConfig {

    @Autowired
    private DroolsRuleRepository droolsRuleRepository;

    @Bean
    public StatelessKieSession kieSession() throws DroolsParserException {
        KieServicesImpl kieServices = new KieServicesImpl();
        KieBaseConfigurationImpl config = new KieBaseConfigurationImpl();
        config.setOption(KieBaseConfiguration.ParallelExecutionOption.YES);

        // Create KieContainer
        KieContainerImpl kieContainer = new KieContainerImpl(kieServices.newReleaseId("com.example", "drools-rules", "1.0.0"));
        KieBaseImpl kieBase = new KieBaseImpl(config);

        // Create Stateless KieSession
        StatelessKieSession kieSession = kieContainer.newStatelessKieSession();

        // Fetch all rules from the database and load them dynamically into KieBase
        List<DroolsRule> droolsRules = droolsRuleRepository.findAll();
        for (DroolsRule droolsRule : droolsRules) {
            String drlContent = droolsRule.getDrlContent();
            String imports = droolsRule.getImports();
            String globals = droolsRule.getGlobals();

            // Add imports and globals dynamically (if needed)
            // Compile the DRL content and add it to the KieBase
            kieSession.getKieBase().addKnowledgePackages(drlContent);
        }

        return kieSession;
    }
}
