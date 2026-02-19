package com.rift.analaysis;

import com.rift.model.Account;
import com.rift.model.Transaction;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class NetworkFlowAnalyzer {

    public Map<String, Object> analyzeFlow(Map<String, Account> accounts) {
        Map<String, Object> flowAnalysis = new LinkedHashMap<>();

        // Find money hubs
        List<Map<String, Object>> hubs = findMoneyHubs(accounts);

        // Find flow paths
        List<Map<String, Object>> paths = findSuspiciousPaths(accounts);

        // Calculate flow velocity
        Map<String, Double> velocity = calculateFlowVelocity(accounts);

        flowAnalysis.put("money_hubs", hubs);
        flowAnalysis.put("suspicious_paths", paths);
        flowAnalysis.put("flow_velocity", velocity);
        flowAnalysis.put("network_density", calculateNetworkDensity(accounts));

        return flowAnalysis;
    }

    private List<Map<String, Object>> findMoneyHubs(Map<String, Account> accounts) {
        List<Map<String, Object>> hubs = new ArrayList<>();

        for (Account account : accounts.values()) {
            double totalFlow = account.getTotalSent() + account.getTotalReceived();
            int connectionCount = account.getIncomingFrom().size() + account.getOutgoingTo().size();

            if (totalFlow > 10000 || connectionCount > 10) {
                Map<String, Object> hub = new LinkedHashMap<>();
                hub.put("account_id", account.getAccountId());
                hub.put("total_flow", totalFlow);
                hub.put("connections", connectionCount);
                hub.put("incoming_ratio", (double) account.getIncomingCount() /
                        (account.getIncomingCount() + account.getOutgoingCount()));
                hub.put("suspicion_score", account.getSuspicionScore());

                hubs.add(hub);
            }
        }

        return hubs;
    }

    private List<Map<String, Object>> findSuspiciousPaths(Map<String, Account> accounts) {
        List<Map<String, Object>> paths = new ArrayList<>();

        // Find all paths of length 3-4
        for (Account start : accounts.values()) {
            List<List<String>> foundPaths = findPaths(start, accounts, 3);

            for (List<String> path : foundPaths) {
                if (isPathSuspicious(path, accounts)) {
                    Map<String, Object> pathInfo = new LinkedHashMap<>();
                    pathInfo.put("path", path);
                    pathInfo.put("length", path.size());
                    pathInfo.put("total_amount", calculatePathAmount(path, accounts));
                    pathInfo.put("risk_score", calculatePathRisk(path, accounts));

                    paths.add(pathInfo);
                }
            }
        }

        return paths;
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
        // Check if intermediate accounts have low transaction counts
        for (int i = 1; i < path.size() - 1; i++) {
            Account intermediate = accounts.get(path.get(i));
            if (intermediate != null && intermediate.getTransactionCount() > 3) {
                return false;
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


    private double calculatePathRisk(List<String> path, Map<String, Account> accounts) {
        double totalRisk = 0.0;
        for (String accountId : path) {
            Account account = accounts.get(accountId);
            if (account != null) {
                totalRisk += account.getSuspicionScore();
            }
        }
        return totalRisk / path.size();
    }

    private Map<String, Double> calculateFlowVelocity(Map<String, Account> accounts) {
        Map<String, Double> velocity = new HashMap<>();

        for (Account account : accounts.values()) {
            if (account.getTransactionCount() > 0) {
                // Transactions per hour
                double txPerHour = account.getTransactionCount() / 24.0;
                velocity.put(account.getAccountId(), txPerHour);
            }
        }

        return velocity;
    }

    private double calculateNetworkDensity(Map<String, Account> accounts) {
        int totalPossibleEdges = accounts.size() * (accounts.size() - 1);
        int actualEdges = 0;

        for (Account account : accounts.values()) {
            actualEdges += account.getOutgoingTo().size();
        }

        return totalPossibleEdges > 0 ? (double) actualEdges / totalPossibleEdges : 0;
    }
}