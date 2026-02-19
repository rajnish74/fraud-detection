package com.rift.service;

import com.rift.model.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SuspicionScoreCalculator {

    private static final Map<String, Double> PATTERN_WEIGHTS = Map.of(
            "cycle_length_3", 0.95,
            "cycle_length_4", 0.90,
            "cycle_length_5", 0.85,
            "layered_network", 0.92,
            "fan_in_aggregator", 0.88,
            "fan_out_disperser", 0.88,
            "high_velocity", 0.75,
            "unusual_timing", 0.60,
            "round_tripping", 0.82
    );

    public void calculateScores(DetectionResult result) {
        for (Account account : result.getAccounts().values()) {
            double score = calculateAccountScore(account, result);
            // Scale to 0-100 range
            account.setSuspicionScore(Math.min(100.0, Math.round(score * 100 * 10) / 10.0));
        }
    }

    private double calculateAccountScore(Account account, DetectionResult result) {
        double score = 0.0;

        // Pattern-based score (50% weight)
        double patternScore = calculatePatternScore(account);
        score += patternScore * 0.5;

        // Transaction behavior score (30% weight)
        double behaviorScore = calculateBehaviorScore(account);
        score += behaviorScore * 0.3;

        // Network centrality score (20% weight)
        double centralityScore = calculateCentralityScore(account, result);
        score += centralityScore * 0.2;

        // Ensure minimum score for accounts with patterns
        if (!account.getPatterns().isEmpty()) {
            score = Math.max(score, 0.5); // At least 50% for patterned accounts
        }

        return score;
    }

    private double calculatePatternScore(Account account) {
        if (account.getPatterns().isEmpty()) return 0.0;

        Set<String> uniquePatterns = new HashSet<>(account.getPatterns());

        double maxWeight = uniquePatterns.stream()
                .mapToDouble(pattern -> PATTERN_WEIGHTS.getOrDefault(pattern, 0.5))
                .max()
                .orElse(0.0);

        // Boost for multiple patterns
        double patternCountBoost = Math.min(0.3, uniquePatterns.size() * 0.1);

        return Math.min(1.0, maxWeight + patternCountBoost);
    }

    private double calculateBehaviorScore(Account account) {
        double score = 0.0;

        // Transaction count (max 0.3)
        if (account.getTransactionCount() >= 5) {
            score += 0.3;
        } else if (account.getTransactionCount() >= 3) {
            score += 0.25;
        } else if (account.getTransactionCount() >= 2) {
            score += 0.2;
        } else if (account.getTransactionCount() >= 1) {
            score += 0.1;
        }

        // Balance between incoming and outgoing (max 0.4)
        if (account.getIncomingCount() > 0 && account.getOutgoingCount() > 0) {
            double ratio = (double) account.getIncomingCount() / account.getOutgoingCount();
            if (ratio > 2.0 || ratio < 0.5) {
                score += 0.35;
            } else {
                score += 0.2;
            }
        } else if (account.getIncomingCount() > 0 || account.getOutgoingCount() > 0) {
            score += 0.15; // Only one direction
        }

        // Amount patterns (max 0.3)
        if (account.getTotalReceived() > 0 && account.getTotalSent() > 0) {
            double amountRatio = account.getTotalReceived() / account.getTotalSent();
            if (amountRatio > 3.0 || amountRatio < 0.33) {
                score += 0.3;
            } else if (amountRatio > 2.0 || amountRatio < 0.5) {
                score += 0.2;
            } else {
                score += 0.1;
            }
        }

        return Math.min(1.0, score);
    }

    private double calculateCentralityScore(Account account, DetectionResult result) {
        double score = 0.0;

        // Unique connections
        Set<String> uniqueConnections = new HashSet<>();
        uniqueConnections.addAll(account.getIncomingFrom());
        uniqueConnections.addAll(account.getOutgoingTo());

        int totalAccounts = result.getAccounts().size();
        if (totalAccounts > 0) {
            // Degree centrality (max 0.6)
            double degreeScore = (double) uniqueConnections.size() / totalAccounts;
            score += Math.min(0.6, degreeScore * 2); // Multiply for better scaling

            // Transaction frequency (max 0.4)
            double freqScore = (double) account.getTransactionCount() / totalAccounts;
            score += Math.min(0.4, freqScore * 3); // Multiply for better scaling
        }

        // Bonus for being in a ring
        if (account.getRingId() != null) {
            score += 0.2;
        }

        return Math.min(1.0, score);
    }
}