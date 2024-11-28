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
