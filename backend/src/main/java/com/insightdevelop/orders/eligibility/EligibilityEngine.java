package com.insightdevelop.orders.eligibility;

import com.insightdevelop.orders.config.EligibilityProperties;
import com.insightdevelop.orders.domain.Order;
import com.insightdevelop.orders.eligibility.rule.EligibilityRule;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Runs every registered {@link EligibilityRule} and combines their findings.
 * <p>
 * The engine knows nothing about individual rules: it starts at 100, subtracts penalties,
 * and applies the final rule "eligible only if no hard rule fails and score >= minimum".
 */
@Service
public class EligibilityEngine {

    static final int MAX_SCORE = 100;
    static final String MINIMUM_SCORE_RULE_ID = "MINIMUM_SCORE";

    private final List<EligibilityRule> rules;
    private final EligibilityProperties properties;
    private final Clock clock;

    public EligibilityEngine(List<EligibilityRule> rules, EligibilityProperties properties, Clock clock) {
        this.rules = rules.stream().sorted(Comparator.comparing(EligibilityRule::id)).toList();
        this.properties = properties;
        this.clock = clock;
    }

    public EligibilityResult evaluate(Order order) {
        EvaluationContext context = new EvaluationContext(order, clock.instant(), properties);

        List<Finding> findings = new ArrayList<>();
        for (EligibilityRule rule : rules) {
            findings.addAll(rule.evaluate(context));
        }

        int totalPenalty = findings.stream().mapToInt(Finding::penalty).sum();
        int score = Math.max(0, MAX_SCORE - totalPenalty);

        if (score < properties.minimumScore()) {
            findings.add(Finding.blocking(MINIMUM_SCORE_RULE_ID,
                    "Score " + score + " is below the minimum of " + properties.minimumScore()));
        }

        findings.sort(Comparator.comparing(Finding::severity));

        List<String> blocking = messages(findings, Severity.BLOCKING);
        List<String> reasons = new ArrayList<>(blocking);
        reasons.addAll(messages(findings, Severity.PENALTY));
        List<String> warnings = messages(findings, Severity.WARNING);

        boolean eligible = blocking.isEmpty();
        Decision decision = !eligible
                ? Decision.NOT_ELIGIBLE
                : (score < properties.riskyScoreBelow() || !warnings.isEmpty())
                        ? Decision.ELIGIBLE_WITH_RISK
                        : Decision.ELIGIBLE;

        return new EligibilityResult(eligible, score, decision, reasons, blocking, warnings, findings, context.now());
    }

    public List<String> ruleIds() {
        return rules.stream().map(EligibilityRule::id).toList();
    }

    private static List<String> messages(List<Finding> findings, Severity severity) {
        return findings.stream().filter(f -> f.severity() == severity).map(Finding::message).toList();
    }
}
