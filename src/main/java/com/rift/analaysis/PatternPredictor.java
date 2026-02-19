package com.rift.analaysis;

import com.rift.model.Account;
import com.rift.model.Transaction;
import org.springframework.stereotype.Component;
import java.util.*;
import java.time.*;
import java.time.temporal.ChronoUnit;

@Component
public class PatternPredictor {

    public Map<String, Object> predictEmergingPatterns(List<Account> accounts) {
        Map<String, Object> predictions = new LinkedHashMap<>();

        // 1. Emerging cycles
        List<Map<String, Object>> emergingCycles = findEmergingCycles(accounts);

        // 2. Smurfing patterns
        List<Map<String, Object>> smurfingPatterns = findSmurfingPatterns(accounts);

        // 3. Layered networks
        List<Map<String, Object>> layeredNetworks = findLayeredNetworks(accounts);

        // 4. Risk forecast for next 24h
        Map<String, Object> forecast = generateForecast(accounts);

        predictions.put("emerging_cycles", emergingCycles);
        predictions.put("smurfing_patterns", smurfingPatterns);
        predictions.put("layered_networks", layeredNetworks);
        predictions.put("forecast", forecast);
        predictions.put("timestamp", LocalDateTime.now().toString());

        return predictions;
    }

    private List<Map<String, Object>> findEmergingCycles(List<Account> accounts) {
        List<Map<String, Object>> cycles = new ArrayList<>();

        // Look for potential cycles (A→B, B→C, C→? not yet A)
        for (Account a : accounts) {
            for (String b : a.getOutgoingTo()) {
                Account accB = findAccount(accounts, b);
                if (accB != null) {
                    for (String c : accB.getOutgoingTo()) {
                        Account accC = findAccount(accounts, c);
                        if (accC != null && !accC.getOutgoingTo().contains(a.getAccountId())) {
                            // Potential cycle forming
                            Map<String, Object> cycle = new LinkedHashMap<>();
                            cycle.put("accounts", List.of(a.getAccountId(), b, c));
                            cycle.put("completion_probability", calculateCompletionProbability(accC, a));
                            cycle.put("estimated_risk", 85.0);
                            cycle.put("pattern", "CYCLE_FORMING");

                            cycles.add(cycle);
                        }
                    }
                }
            }
        }

        return cycles;
    }

    private double calculateCompletionProbability(Account from, Account to) {
        // Probability that from will send to to
        if (from.getOutgoingTo().contains(to.getAccountId())) {
            return 1.0; // Already complete
        }

        // Check transaction patterns
        double probability = 0.3; // Base probability

        // If they've transacted before in opposite direction
        if (from.getIncomingFrom().contains(to.getAccountId())) {
            probability += 0.4;
        }

        // If amounts are similar
        double avgFromAmount = from.getTransactions().stream()
                .mapToDouble(Transaction::getAmount)
                .average()
                .orElse(0);
        double avgToAmount = to.getTransactions().stream()
                .mapToDouble(Transaction::getAmount)
                .average()
                .orElse(0);

        if (Math.abs(avgFromAmount - avgToAmount) < 100) {
            probability += 0.2;
        }

        return Math.min(1.0, probability);
    }

    private List<Map<String, Object>> findSmurfingPatterns(List<Account> accounts) {
        List<Map<String, Object>> patterns = new ArrayList<>();

        // Look for accounts receiving many small transactions
        for (Account account : accounts) {
            long smallTxns = account.getTransactions().stream()
                    .filter(tx -> tx.getReceiverId().equals(account.getAccountId()))
                    .filter(tx -> tx.getAmount() < 100)
                    .count();

            if (smallTxns > 3) {
                // Potential smurfing
                Map<String, Object> pattern = new LinkedHashMap<>();
                pattern.put("target_account", account.getAccountId());
                pattern.put("small_transactions", smallTxns);
                pattern.put("suspicious_senders", findSuspiciousSenders(account));
                pattern.put("pattern_type", "SMURFING");
                pattern.put("confidence", Math.min(100, smallTxns * 20));

                patterns.add(pattern);
            }
        }

        return patterns;
    }

    private List<String> findSuspiciousSenders(Account receiver) {
        return receiver.getTransactions().stream()
                .filter(tx -> tx.getReceiverId().equals(receiver.getAccountId()))
                .filter(tx -> tx.getAmount() < 100)
                .map(Transaction::getSenderId)
                .distinct()
                .limit(5)
                .toList();
    }

    private List<Map<String, Object>> findLayeredNetworks(List<Account> accounts) {
        List<Map<String, Object>> networks = new ArrayList<>();

        // Find chains of 3+ accounts
        for (Account start : accounts) {
            List<List<String>> chains = findChains(start, accounts, 3);

            for (List<String> chain : chains) {
                if (chain.size() >= 3) {
                    Map<String, Object> network = new LinkedHashMap<>();
                    network.put("chain", String.join(" → ", chain));
                    network.put("length", chain.size());
                    network.put("expansion_rate", calculateExpansionRate(chain, accounts));
                    network.put("estimated_risk", 75.0);

                    networks.add(network);
                }
            }
        }

        return networks;
    }

    private List<List<String>> findChains(Account start, List<Account> accounts, int maxLength) {
        List<List<String>> chains = new ArrayList<>();
        findChainsDFS(start.getAccountId(), new ArrayList<>(), chains, accounts, maxLength, new HashSet<>());
        return chains;
    }

    private void findChainsDFS(String current, List<String> path, List<List<String>> chains,
                               List<Account> accounts, int maxLength, Set<String> visited) {
        if (path.size() >= maxLength) return;
        if (visited.contains(current)) return;

        path.add(current);
        visited.add(current);

        if (path.size() >= 2) {
            chains.add(new ArrayList<>(path));
        }

        Account account = findAccount(accounts, current);
        if (account != null) {
            for (String next : account.getOutgoingTo()) {
                findChainsDFS(next, path, chains, accounts, maxLength, visited);
            }
        }

        path.remove(path.size() - 1);
        visited.remove(current);
    }

    private double calculateExpansionRate(List<String> chain, List<Account> accounts) {
        // How fast is this chain growing?
        Account last = findAccount(accounts, chain.get(chain.size() - 1));
        if (last == null) return 0;

        // New connections in last 24h
        LocalDateTime dayAgo = LocalDateTime.now().minus(24, ChronoUnit.HOURS);

        long newConnections = last.getTransactions().stream()
                .filter(tx -> tx.getTimestamp().isAfter(dayAgo))
                .filter(tx -> tx.getSenderId().equals(last.getAccountId()))
                .count();

        return newConnections / 24.0; // Per hour
    }

    private Account findAccount(List<Account> accounts, String id) {
        return accounts.stream()
                .filter(a -> a.getAccountId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> generateForecast(List<Account> accounts) {
        Map<String, Object> forecast = new LinkedHashMap<>();

        // Risk forecast for next 24 hours
        double currentRisk = accounts.stream()
                .mapToDouble(Account::getSuspicionScore)
                .average()
                .orElse(50);

        // Simple time series prediction
        List<Double> predictions = new ArrayList<>();
        double trend = 1.05; // 5% increase trend

        for (int i = 1; i <= 24; i++) {
            double predicted = currentRisk * Math.pow(trend, i / 24.0);
            predictions.add(Math.min(100, predicted));
        }

        forecast.put("current_avg_risk", currentRisk);
        forecast.put("predictions", predictions);
        forecast.put("peak_risk", predictions.stream().max(Double::compare).orElse(0.0));
        forecast.put("peak_time", findPeakRiskTime(predictions));
        forecast.put("confidence", 85.0);

        return forecast;
    }

    private String findPeakRiskTime(List<Double> predictions) {
        int peakHour = 0;
        double maxRisk = 0;

        for (int i = 0; i < predictions.size(); i++) {
            if (predictions.get(i) > maxRisk) {
                maxRisk = predictions.get(i);
                peakHour = i + 1;
            }
        }

        return peakHour + " hours from now";
    }
}