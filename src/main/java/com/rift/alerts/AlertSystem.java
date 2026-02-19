package com.rift.alerts;

import com.rift.model.Account;
import com.rift.model.FraudRing;
import com.rift.model.DetectionResult;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class AlertSystem {

    private final Map<String, List<Alert>> alertHistory = new HashMap<>();

    public List<Alert> generateAlerts(DetectionResult result) {
        List<Alert> alerts = new ArrayList<>();

        // High risk rings
        for (FraudRing ring : result.getRings().values()) {
            if (ring.getRiskScore() > 80) {
                alerts.add(new Alert(
                        "HIGH_RISK_RING",
                        "Critical: High risk fraud ring detected",
                        ring.getRingId(),
                        ring.getRiskScore(),
                        LocalDateTime.now()
                ));
            }
        }

        // Suspicious accounts
        for (Account account : result.getAccounts().values()) {
            if (account.getSuspicionScore() > 90) {
                alerts.add(new Alert(
                        "CRITICAL_ACCOUNT",
                        "Critical: Highly suspicious account activity",
                        account.getAccountId(),
                        account.getSuspicionScore(),
                        LocalDateTime.now()
                ));
            } else if (account.getSuspicionScore() > 70) {
                alerts.add(new Alert(
                        "SUSPICIOUS_ACCOUNT",
                        "Warning: Suspicious account detected",
                        account.getAccountId(),
                        account.getSuspicionScore(),
                        LocalDateTime.now()
                ));
            }
        }

        // Pattern-based alerts
        Map<String, Integer> patternCount = countPatterns(result);
        if (patternCount.getOrDefault("cycle", 0) > 2) {
            alerts.add(new Alert(
                    "MULTIPLE_CYCLES",
                    "Multiple circular patterns detected in network",
                    "NETWORK",
                    85.0,
                    LocalDateTime.now()
            ));
        }

        // Store alerts in history
        alerts.forEach(alert -> {
            alertHistory.computeIfAbsent(alert.getTargetId(), k -> new ArrayList<>())
                    .add(alert);
        });

        return alerts;
    }

    private Map<String, Integer> countPatterns(DetectionResult result) {
        Map<String, Integer> counts = new HashMap<>();

        for (Account account : result.getAccounts().values()) {
            for (String pattern : account.getPatterns()) {
                if (pattern.contains("cycle")) {
                    counts.merge("cycle", 1, Integer::sum);
                } else if (pattern.contains("fan")) {
                    counts.merge("smurfing", 1, Integer::sum);
                } else if (pattern.contains("layered")) {
                    counts.merge("layered", 1, Integer::sum);
                }
            }
        }

        return counts;
    }

    public List<Alert> getAlertHistory(String accountId) {
        return alertHistory.getOrDefault(accountId, new ArrayList<>());
    }

    @lombok.Data
    public static class Alert {
        private final String type;
        private final String message;
        private final String targetId;
        private final double severity;
        private final LocalDateTime timestamp;

        public Alert(String type, String message, String targetId,
                     double severity, LocalDateTime timestamp) {
            this.type = type;
            this.message = message;
            this.targetId = targetId;
            this.severity = severity;
            this.timestamp = timestamp;
        }
    }
}