package com.rift.model;

import com.rift.model.Account;
import com.rift.model.FraudRing;
import lombok.Data;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class DetectionResult {
    private List<Map<String, Object>> suspiciousAccounts = new ArrayList<>();
    private List<Map<String, Object>> fraudRings = new ArrayList<>();
    private Map<String, Object> summary = new HashMap<>();
    private Map<String, Account> accounts = new ConcurrentHashMap<>();
    private Map<String, FraudRing> rings = new ConcurrentHashMap<>();
    private long processingTime;

    public void buildOutput() {
        // Sort suspicious accounts by score descending
        suspiciousAccounts.sort((a, b) ->
                Double.compare((double) b.get("suspicion_score"),
                        (double) a.get("suspicion_score")));

        summary.put("total_accounts_analyzed", accounts.size());
        summary.put("suspicious_accounts_flagged", suspiciousAccounts.size());
        summary.put("fraud_rings_detected", fraudRings.size());
        summary.put("processing_time_seconds", processingTime / 1000.0);
    }
}