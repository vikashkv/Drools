import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DroolsRuleRepository extends JpaRepository<DroolsRule, Long> {
    List<DroolsRule> findByVersion(int version);
}
