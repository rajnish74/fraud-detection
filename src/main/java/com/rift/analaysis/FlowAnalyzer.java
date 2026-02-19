package com.rift.analaysis;

import com.rift.model.Account;
import com.rift.model.Transaction;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FlowAnalyzer {

    public Map<String, Object> analyzeFlow(Map<String, Account> accounts) {
        Map<String, Object> analysis = new LinkedHashMap<>();

        // 1. Money Hubs (accounts with high flow)
        List<Map<String, Object>> hubs = findMoneyHubs(accounts);

        // 2. Suspicious Paths
        List<Map<String, Object>> paths = findSuspiciousPaths(accounts);

        // 3. Flow Velocity
        Map<String, Double> velocity = calculateFlowVelocity(accounts);

        // 4. Network Statistics
        Map<String, Object> networkStats = calculateNetworkStats(accounts);

        // 5. Money Laundering Risk Index
        double mlRiskIndex = calculateMLRiskIndex(accounts, hubs, paths);

        analysis.put("money_hubs", hubs);
        analysis.put("suspicious_paths", paths);
        analysis.put("flow_velocity", velocity);
        analysis.put("network_stats", networkStats);
        analysis.put("ml_risk_index", mlRiskIndex);
        analysis.put("total_flow", calculateTotalFlow(accounts));

        return analysis;
    }

    private List<Map<String, Object>> findMoneyHubs(Map<String, Account> accounts) {
        List<Map<String, Object>> hubs = new ArrayList<>();

        for (Account account : accounts.values()) {
            double totalFlow = account.getTotalSent() + account.getTotalReceived();
            int connections = account.getIncomingFrom().size() + account.getOutgoingTo().size();

            // Hub criteria: high flow or many connections
            if (totalFlow > 5000 || connections > 5) {
                Map<String, Object> hub = new LinkedHashMap<>();
                hub.put("account_id", account.getAccountId());
                hub.put("total_flow", totalFlow);
                hub.put("connections", connections);
                hub.put("incoming", account.getTotalReceived());
                hub.put("outgoing", account.getTotalSent());
                hub.put("hub_score", calculateHubScore(totalFlow, connections));
                hub.put("risk_level", getRiskLevel(totalFlow, connections));

                hubs.add(hub);
            }
        }

        return hubs.stream()
                .sorted((a, b) -> Double.compare(
                        (double) b.get("hub_score"),
                        (double) a.get("hub_score")))
                .limit(5)
                .collect(Collectors.toList());
    }

    private double calculateHubScore(double totalFlow, int connections) {
        return (totalFlow / 10000) * 0.6 + (connections / 10.0) * 0.4;
    }

    private String getRiskLevel(double totalFlow, int connections) {
        double score = calculateHubScore(totalFlow, connections);
        if (score > 0.8) return "CRITICAL";
        if (score > 0.6) return "HIGH";
        if (score > 0.4) return "MEDIUM";
        return "LOW";
    }

    private List<Map<String, Object>> findSuspiciousPaths(Map<String, Account> accounts) {
        List<Map<String, Object>> paths = new ArrayList<>();

        // Find all paths of length 2-4
        for (Account start : accounts.values()) {
            List<List<String>> foundPaths = findPaths(start, accounts, 3);

            for (List<String> path : foundPaths) {
                if (isPathSuspicious(path, accounts)) {
                    Map<String, Object> pathInfo = new LinkedHashMap<>();
                    pathInfo.put("path", String.join(" → ", path));
                    pathInfo.put("length", path.size());
                    pathInfo.put("total_amount", calculatePathAmount(path, accounts));
                    pathInfo.put("avg_risk", calculateAverageRisk(path, accounts));
                    pathInfo.put("pattern", detectPathPattern(path));

                    paths.add(pathInfo);
                }
            }
        }

        return paths.stream()
                .sorted((a, b) -> Double.compare(
                        (double) b.get("avg_risk"),
                        (double) a.get("avg_risk")))
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<List<String>> findPaths(Account start, Map<String, Account> accounts, int maxLength) {
        List<List<String>> paths = new ArrayList<>();
        findPathsDFS(start.getAccountId(), new ArrayList<>(), paths, accounts, maxLength, new HashSet<>());
        return paths;
    }

    private void findPathsDFS(String current, List<String> path, List<List<String>> paths,
                              Map<String, Account> accounts, int maxLength, Set<String> visited) {
        if (path.size() >= maxLength) return;
        if (visited.contains(current)) return;

        path.add(current);
        visited.add(current);

        if (path.size() >= 2) {
            paths.add(new ArrayList<>(path));
        }

        Account account = accounts.get(current);
        if (account != null) {
            for (String next : account.getOutgoingTo()) {
                findPathsDFS(next, path, paths, accounts, maxLength, visited);
            }
        }

        path.remove(path.size() - 1);
        visited.remove(current);
    }

    private boolean isPathSuspicious(List<String> path, Map<String, Account> accounts) {
        // Check if path has intermediate accounts with low activity
        for (int i = 1; i < path.size() - 1; i++) {
            Account intermediate = accounts.get(path.get(i));
            if (intermediate != null && intermediate.getTransactionCount() > 3) {
                return false; // Too active to be suspicious
            }
        }
        return true;
    }

    private double calculatePathAmount(List<String> path, Map<String, Account> accounts) {
        double total = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            Account from = accounts.get(path.get(i));
            if (from != null) {

                // 👇 FIX START
                final String nextAccount = path.get(i + 1);
                // 👆 FIX END

                Optional<Transaction> tx = from.getTransactions().stream()
                        .filter(t -> t.getReceiverId().equals(nextAccount))
                        .findFirst();

                if (tx.isPresent()) {
                    total += tx.get().getAmount();
                }
            }
        }
        return total;
    }


    private double calculateAverageRisk(List<String> path, Map<String, Account> accounts) {
        return path.stream()
                .mapToDouble(id -> accounts.get(id).getSuspicionScore())
                .average()
                .orElse(0);
    }

    private String detectPathPattern(List<String> path) {
        if (path.size() == 2) return "DIRECT_TRANSFER";
        if (path.size() == 3) return "TWO_HOP_LAYER";
        if (path.size() == 4) return "THREE_HOP_LAYER";
        return "MULTI_HOP";
    }

    private Map<String, Double> calculateFlowVelocity(Map<String, Account> accounts) {
        Map<String, Double> velocity = new HashMap<>();

        for (Account account : accounts.values()) {
            if (account.getTransactionCount() > 1) {
                // Transactions per hour
                double hours = 24.0; // Assume 24 hour window
                double txPerHour = account.getTransactionCount() / hours;
                velocity.put(account.getAccountId(), txPerHour);
            }
        }

        return velocity;
    }

    private Map<String, Object> calculateNetworkStats(Map<String, Account> accounts) {
        Map<String, Object> stats = new LinkedHashMap<>();

        int totalNodes = accounts.size();
        int totalEdges = accounts.values().stream()
                .mapToInt(a -> a.getOutgoingTo().size())
                .sum();

        stats.put("total_nodes", totalNodes);
        stats.put("total_edges", totalEdges);
        stats.put("density", totalNodes > 1 ? (double) totalEdges / (totalNodes * (totalNodes - 1)) : 0);
        stats.put("avg_connections", totalNodes > 0 ? (double) totalEdges / totalNodes : 0);
        stats.put("is_connected", isNetworkConnected(accounts));

        return stats;
    }

    private boolean isNetworkConnected(Map<String, Account> accounts) {
        if (accounts.isEmpty()) return false;

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        String start = accounts.keySet().iterator().next();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            Account account = accounts.get(current);

            if (account != null) {
                for (String next : account.getOutgoingTo()) {
                    if (!visited.contains(next)) {
                        visited.add(next);
                        queue.add(next);
                    }
                }
                for (String next : account.getIncomingFrom()) {
                    if (!visited.contains(next)) {
                        visited.add(next);
                        queue.add(next);
                    }
                }
            }
        }

        return visited.size() == accounts.size();
    }

    private double calculateTotalFlow(Map<String, Account> accounts) {
        return accounts.values().stream()
                .mapToDouble(a -> a.getTotalSent() + a.getTotalReceived())
                .sum();
    }

    private double calculateMLRiskIndex(Map<String, Account> accounts,
                                        List<Map<String, Object>> hubs,
                                        List<Map<String, Object>> paths) {
        double risk = 0.0;

        // Hub risk
        risk += hubs.size() * 0.1;

        // Path risk
        risk += paths.size() * 0.05;

        // Suspicious accounts
        long suspiciousCount = accounts.values().stream()
                .filter(a -> a.getSuspicionScore() > 70)
                .count();
        risk += suspiciousCount * 0.15;

        // Normalize to 0-100
        return Math.min(100, risk * 10);
    }
}