package com.rift.ml;

import com.rift.model.Account;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class RiskPredictor {

    private final Map<String, Double> featureWeights = Map.of(
            "transaction_count", 0.15,
            "in_out_ratio", 0.20,
            "amount_ratio", 0.15,
            "velocity", 0.25,
            "connectivity", 0.25
    );

    public double predict(Account account) {
        double score = 0;

        double txScore = Math.min(1.0, account.getTransactionCount() / 10.0);
        score += txScore * featureWeights.get("transaction_count");

        double inOutScore = calculateInOutRatioScore(account);
        score += inOutScore * featureWeights.get("in_out_ratio");

        double amountScore = calculateAmountRatioScore(account);
        score += amountScore * featureWeights.get("amount_ratio");

        double velocityScore = calculateVelocityScore(account);
        score += velocityScore * featureWeights.get("velocity");

        double connectivityScore = calculateConnectivityScore(account);
        score += connectivityScore * featureWeights.get("connectivity");

        return Math.min(1.0, score);
    }

    // 🔥 THIS METHOD FIXES YOUR ERROR
    public double predictRiskScore(Account account, List<Account> allAccounts) {
        // Use existing logic
        double baseScore = predict(account);

        // Optional network comparison boost
        double avgTx = allAccounts.stream()
                .mapToDouble(Account::getTransactionCount)
                .average()
                .orElse(1);

        if (account.getTransactionCount() > avgTx * 2) {
            baseScore += 0.1;
        }

        return Math.min(100.0, baseScore * 100);
    }

    private double calculateInOutRatioScore(Account account) {
        if (account.getIncomingCount() == 0 || account.getOutgoingCount() == 0) {
            return 0.5;
        }

        double ratio = (double) account.getIncomingCount() / account.getOutgoingCount();
        if (ratio > 3 || ratio < 0.33) return 0.9;
        if (ratio > 2 || ratio < 0.5) return 0.6;
        return 0.2;
    }

    private double calculateAmountRatioScore(Account account) {
        if (account.getTotalReceived() == 0 || account.getTotalSent() == 0) {
            return 0.4;
        }

        double ratio = account.getTotalReceived() / account.getTotalSent();
        if (ratio > 5 || ratio < 0.2) return 0.9;
        if (ratio > 3 || ratio < 0.33) return 0.7;
        return 0.3;
    }

    private double calculateVelocityScore(Account account) {
        if (account.getTransactions().size() < 2) return 0.2;

        long totalMinutes = 0;
        for (int i = 1; i < account.getTransactions().size(); i++) {
            totalMinutes += java.time.Duration.between(
                    account.getTransactions().get(i - 1).getTimestamp(),
                    account.getTransactions().get(i).getTimestamp()
            ).toMinutes();
        }

        double avgMinutes = totalMinutes / (account.getTransactions().size() - 1);

        if (avgMinutes < 30) return 0.9;
        if (avgMinutes < 60) return 0.7;
        if (avgMinutes < 120) return 0.4;
        return 0.2;
    }

    private double calculateConnectivityScore(Account account) {
        int connections = account.getIncomingFrom().size() + account.getOutgoingTo().size();

        if (connections > 10) return 0.9;
        if (connections > 5) return 0.7;
        if (connections > 2) return 0.4;
        return 0.2;
    }
}
