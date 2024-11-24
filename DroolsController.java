import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/drools")
public class DroolsController {

    @Autowired
    private DroolsRuleService droolsRuleService;

    // Endpoint to execute rules with facts
    @PostMapping("/execute")
    public String executeRules(@RequestBody FactModel factModel) {
        droolsRuleService.executeRules(factModel);
        return "Rules executed successfully";
    }

    // Endpoint to save a new rule
    @PostMapping("/saveRule")
    public String saveRule(@RequestBody DroolsRule droolsRule) {
        droolsRuleService.saveRule(droolsRule);
        return "Rule saved successfully";
    }

    // Endpoint to fetch all saved rules
    @GetMapping("/getRules")
    public List<DroolsRule> getAllRules() {
        return droolsRuleService.getAllRules();
    }
}
