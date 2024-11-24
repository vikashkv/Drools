import org.drools.core.spi.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DroolsRuleService {

    @Autowired
    private DroolsRuleRepository droolsRuleRepository;

    @Autowired
    private StatelessKieSession kieSession;

    // Save rule to the database dynamically
    public void saveRule(DroolsRule droolsRule) {
        droolsRuleRepository.save(droolsRule);
    }

    // Fetch and execute rules dynamically for a given fact model
    public void executeRules(FactModel factModel) {
        // Create a new StatelessKieSession each time
        StatelessKieSession session = kieSession.getKieBase().newStatelessKieSession();

        // Insert the facts and fire the rules
        session.execute(factModel);
    }

}