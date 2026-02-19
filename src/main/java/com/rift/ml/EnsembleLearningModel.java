package com.rift.ml;

import com.rift.model.Account;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class EnsembleLearningModel {

    // Four different ML models combined
    private final IsolationForest isolationForest = new IsolationForest();
    private final LocalOutlierFactor lof = new LocalOutlierFactor();
    private final RiskPredictor riskPredictor = new RiskPredictor();

    // Model weights (ensemble learning)
    private final Map<String, Double> modelWeights = Map.of(
            "isolation_forest", 0.35,
            "local_outlier_factor", 0.30,
            "risk_predictor", 0.35
    );

    public Map<String, Object> predictRisk(Account account, List<Account> allAccounts) {
        Map<String, Object> result = new HashMap<>();

        // Get predictions from each model
        double isolationScore = isolationForest.calculateScore(account, allAccounts);
        double lofScore = lof.calculateScore(account, allAccounts);
        double riskScore = riskPredictor.predict(account);

        // Ensemble score (weighted average)
        double ensembleScore =
                isolationScore * modelWeights.get("isolation_forest") +
                        lofScore * modelWeights.get("local_outlier_factor") +
                        riskScore * modelWeights.get("risk_predictor");

        // Model confidence levels
        result.put("ensemble_score", ensembleScore * 100);
        result.put("isolation_forest_score", isolationScore * 100);
        result.put("lof_score", lofScore * 100);
        result.put("risk_predictor_score", riskScore * 100);
        result.put("confidence", calculateConfidence(isolationScore, lofScore, riskScore));
        result.put("model_version", "v2.1");

        return result;
    }

    private double calculateConfidence(double... scores) {
        // Calculate how much models agree
        double mean = Arrays.stream(scores).average().orElse(0);
        double variance = Arrays.stream(scores)
                .map(s -> Math.pow(s - mean, 2))
                .average()
                .orElse(0);

        // Lower variance = higher confidence
        return Math.max(0, 1 - variance) * 100;
    }
}