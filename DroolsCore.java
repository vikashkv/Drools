import java.util.List;

public interface IRuleRepository {
    void save(RuleEntity ruleEntity);
    void saveAll(List<RuleEntity> ruleEntities);
}

public interface IMetadataExtractor {
    String extractMetadata(String drl);
}


import java.util.List;

public interface IRuleExtractor {
    List<RuleEntity> extractRules(String drl);
}


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class RuleRepository implements IRuleRepository {

    private final DataSource dataSource;

    public RuleRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(RuleEntity ruleEntity) {
        String sql = "INSERT INTO rule_entity (entry_type, rule_name, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ruleEntity.getEntryType());
            ps.setString(2, ruleEntity.getRuleName());
            ps.setString(3, ruleEntity.getContent());
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(ruleEntity.getCreatedAt()));
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(ruleEntity.getUpdatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveAll(List<RuleEntity> ruleEntities) {
        String sql = "INSERT INTO rule_entity (entry_type, rule_name, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (RuleEntity ruleEntity : ruleEntities) {
                ps.setString(1, ruleEntity.getEntryType());
                ps.setString(2, ruleEntity.getRuleName());
                ps.setString(3, ruleEntity.getContent());
                ps.setTimestamp(4, java.sql.Timestamp.valueOf(ruleEntity.getCreatedAt()));
                ps.setTimestamp(5, java.sql.Timestamp.valueOf(ruleEntity.getUpdatedAt()));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}


import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;
import org.drools.decisiontable.InputType;
import org.drools.decisiontable.SpreadsheetCompiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class DecisionTableService {

    @Autowired
    private IRuleRepository ruleRepository;
    @Autowired
    private IMetadataExtractor metadataExtractor;
    @Autowired
    private IRuleExtractor ruleExtractor;

    public void processDecisionTableAndSaveToDatabase() throws Exception {
        try (InputStream decisionTableStream = getClass().getResourceAsStream("/rules/decision_table.xlsx")) {
            if (decisionTableStream == null) {
                throw new Exception("Decision table not found");
            }

            // Compile decision table into DRL
            SpreadsheetCompiler compiler = new SpreadsheetCompiler();
            String drl = compiler.compile(decisionTableStream, InputType.XLS);

            // Extract and save metadata
            String metadata = metadataExtractor.extractMetadata(drl);
            saveMetadataToDatabase(metadata);

            // Extract and save individual rules
            List<RuleEntity> rules = ruleExtractor.extractRules(drl);
            ruleRepository.saveAll(rules);
        }
    }

    private void saveMetadataToDatabase(String metadata) {
        RuleEntity metadataEntity = new RuleEntity();
        metadataEntity.setEntryType("METADATA");
        metadataEntity.setContent(metadata);
        metadataEntity.setCreatedAt(java.time.LocalDateTime.now());
        metadataEntity.setUpdatedAt(java.time.LocalDateTime.now());
        ruleRepository.save(metadataEntity);
    }
}


import java.util.List;

public interface IRuleExtractor {
    List<RuleData> extractRules(String drl);
}


public class RuleData {
    private String ruleName;
    private String ruleContent;

    // Getters and setters
    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleContent() {
        return ruleContent;
    }

    public void setRuleContent(String ruleContent) {
        this.ruleContent = ruleContent;
    }
}

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleExtractor implements IRuleExtractor {
    @Override
    public List<RuleData> extractRules(String drl) {
        List<RuleData> rules = new ArrayList<>();
        Pattern rulePattern = Pattern.compile("rule\\s+\"(.*?)\"\\s+when(.*?)then(.*?)end", Pattern.DOTALL);
        Matcher ruleMatcher = rulePattern.matcher(drl);

        while (ruleMatcher.find()) {
            String ruleName = ruleMatcher.group(1);
            String ruleContent = ruleMatcher.group();

            RuleData ruleData = new RuleData();
            ruleData.setRuleName(ruleName);
            ruleData.setRuleContent(ruleContent);
            rules.add(ruleData);
        }

        return rules;
    }
}


import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;
import org.drools.decisiontable.InputType;
import org.drools.decisiontable.SpreadsheetCompiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DecisionTableService {

    @Autowired
    private IRuleRepository ruleRepository;
    @Autowired
    private IMetadataExtractor metadataExtractor;
    @Autowired
    private IRuleExtractor ruleExtractor;

    public void processDecisionTableAndSaveToDatabase() throws Exception {
        try (InputStream decisionTableStream = getClass().getResourceAsStream("/rules/decision_table.xlsx")) {
            if (decisionTableStream == null) {
                throw new Exception("Decision table not found");
            }

            // Compile decision table into DRL
            SpreadsheetCompiler compiler = new SpreadsheetCompiler();
            String drl = compiler.compile(decisionTableStream, InputType.XLS);

            // Extract and save metadata
            String metadata = metadataExtractor.extractMetadata(drl);
            saveEntityToDatabase("METADATA", null, metadata);

            // Extract rules and convert to RuleEntity
            List<RuleData> ruleDataList = ruleExtractor.extractRules(drl);
            List<RuleEntity> ruleEntities = new ArrayList<>();
            for (RuleData ruleData : ruleDataList) {
                RuleEntity ruleEntity = new RuleEntity();
                ruleEntity.setEntryType("RULE");
                ruleEntity.setRuleName(ruleData.getRuleName());
                ruleEntity.setContent(ruleData.getRuleContent());
                ruleEntity.setCreatedAt(LocalDateTime.now());
                ruleEntity.setUpdatedAt(LocalDateTime.now());
                ruleEntities.add(ruleEntity);
            }

            // Save all rules
            ruleRepository.saveAll(ruleEntities);
        }
    }

    private void saveEntityToDatabase(String entryType, String ruleName, String content) {
        RuleEntity entity = new RuleEntity();
        entity.setEntryType(entryType);
        entity.setRuleName(ruleName);
        entity.setContent(content);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        ruleRepository.save(entity);
    }
}


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataExtractor implements IMetadataExtractor {
    @Override
    public String extractMetadata(String drl) {
        StringBuilder metadata = new StringBuilder();

        Pattern packagePattern = Pattern.compile("package\\s+.*?;");
        Pattern importPattern = Pattern.compile("import\\s+.*?;");
        Pattern globalPattern = Pattern.compile("global\\s+.*?;");

        Matcher packageMatcher = packagePattern.matcher(drl);
        if (packageMatcher.find()) {
            metadata.append(packageMatcher.group()).append("\n");
        }

        Matcher importMatcher = importPattern.matcher(drl);
        while (importMatcher.find()) {
            metadata.append(importMatcher.group()).append("\n");
        }

        Matcher globalMatcher = globalPattern.matcher(drl);
        while (globalMatcher.find()) {
            metadata.append(globalMatcher.group()).append("\n");
        }

        return metadata.toString();
    }
}


CREATE TABLE rule_entity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_type VARCHAR(50),   -- 'METADATA' or 'RULE'
    rule_name VARCHAR(255),   -- Name of the rule; NULL for metadata
    content TEXT,             -- DRL content or metadata content
    created_at TIMESTAMP,     -- Timestamp of creation
    updated_at TIMESTAMP      -- Timestamp of last update
);
