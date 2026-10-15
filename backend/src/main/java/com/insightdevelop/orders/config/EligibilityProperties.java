package com.insightdevelop.orders.config;

import com.insightdevelop.orders.domain.CustomerTier;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Tunable values used by the eligibility rules (bound from {@code eligibility.*} in application.yml).
 *
 * @param minimumScore          an order needs at least this score to be eligible
 * @param riskyScoreBelow       eligible orders scoring below this are reported as "eligible but risky"
 * @param inventoryMaxAge       inventory checks older than this are considered stale
 * @param orderAgePenaltyAfter  orders older than this lose points
 * @param maxQuantityWithoutPenalty total quantity above this loses points
 * @param tierAmountThresholds  expected maximum order amount per tier
 * @param supportedCountries    ISO country codes eligible for expedited shipping
 * @param blockingRiskFlags     risk flags that block expedited processing outright
 * @param amountTolerance       allowed difference between totalAmount and the sum of items
 */
@ConfigurationProperties(prefix = "eligibility")
public record EligibilityProperties(
        int minimumScore,
        int riskyScoreBelow,
        Duration inventoryMaxAge,
        Duration orderAgePenaltyAfter,
        int maxQuantityWithoutPenalty,
        Map<CustomerTier, BigDecimal> tierAmountThresholds,
        Set<String> supportedCountries,
        Set<String> blockingRiskFlags,
        BigDecimal amountTolerance) {

    public EligibilityProperties {
        tierAmountThresholds = Map.copyOf(tierAmountThresholds);
        supportedCountries = Set.copyOf(supportedCountries);
        blockingRiskFlags = Set.copyOf(blockingRiskFlags);
    }
}
