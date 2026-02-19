package com.rift.model;

import lombok.Data;
import java.util.*;

@Data
public class FraudRing {
    private String ringId;
    private Set<String> memberAccounts = new HashSet<>();
    private String patternType;
    private double riskScore;
    private List<String> detectedPatterns = new ArrayList<>();
    private Map<String, Double> accountScores = new HashMap<>();

    public FraudRing(String ringId, String patternType) {
        this.ringId = ringId;
        this.patternType = patternType;
    }

    public void addAccountWithScore(String accountId, double score) {
        memberAccounts.add(accountId);
        accountScores.put(accountId, score);
    }

    public void calculateRiskScore() {
        if (accountScores.isEmpty()) {
            this.riskScore = 0.0;
            return;
        }

        // Use ACTUAL account scores (not scaled down)
        double totalScore = accountScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        double avgScore = totalScore / accountScores.size();

        // Pattern-based multiplier
        double multiplier = 1.0;
        if ("cycle".equals(patternType)) {
            multiplier = 1.2;
        } else if ("layered".equals(patternType)) {
            multiplier = 1.15;
        } else if ("smurfing".equals(patternType)) {
            multiplier = 1.1;
        }

        // Size multiplier
        double sizeMultiplier = 1.0 + (memberAccounts.size() * 0.03);

        // Calculate final score - keep in 0-100 range
        this.riskScore = Math.min(100.0, avgScore * multiplier * sizeMultiplier);

        // Debug
        System.out.println("Ring " + ringId + " avgScore=" + avgScore +
                " multiplier=" + multiplier +
                " final=" + riskScore);
    }
}