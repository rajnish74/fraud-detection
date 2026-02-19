package com.rift.algorithms;

import com.rift.model.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CycleDetector {

    public void detectCycles(DetectionResult result) {
        Map<String, Set<String>> graph = buildGraph(result.getAccounts());
        Set<Set<String>> allCycles = findCycles(graph);

        // Filter cycles and remove overlaps
        Set<Set<String>> uniqueCycles = new HashSet<>();

        for (Set<String> cycle : allCycles) {
            if (cycle.size() >= 3 && cycle.size() <= 5) {
                // Check if this cycle is already covered by a larger pattern
                boolean isSubset = false;
                for (Set<String> existing : uniqueCycles) {
                    if (existing.containsAll(cycle)) {
                        isSubset = true;
                        break;
                    }
                }
                if (!isSubset) {
                    uniqueCycles.add(cycle);
                }
            }
        }

        // Create rings
        for (Set<String> cycle : uniqueCycles) {
            FraudRing ring = new FraudRing(null, "cycle");

            for (String accountId : cycle) {
                Account account = result.getAccounts().get(accountId);
                if (account != null) {
                    account.getPatterns().add("cycle_length_" + cycle.size());
                    ring.addAccountWithScore(accountId, account.getSuspicionScore());
                }
            }

            ring.calculateRiskScore();

            // Only add if this combination doesn't exist
            String memberKey = ring.getMemberAccounts().stream()
                    .sorted()
                    .collect(Collectors.joining("-"));

            boolean exists = false;
            for (FraudRing existing : result.getRings().values()) {
                String existingKey = existing.getMemberAccounts().stream()
                        .sorted()
                        .collect(Collectors.joining("-"));
                if (existingKey.equals(memberKey)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                result.getRings().put(UUID.randomUUID().toString(), ring);
            }
        }
    }

    private Map<String, Set<String>> buildGraph(Map<String, Account> accounts) {
        Map<String, Set<String>> graph = new HashMap<>();
        for (Account account : accounts.values()) {
            graph.put(account.getAccountId(), new HashSet<>(account.getOutgoingTo()));
        }
        return graph;
    }

    private Set<Set<String>> findCycles(Map<String, Set<String>> graph) {
        Set<Set<String>> cycles = new HashSet<>();
        Set<String> visited = new HashSet<>();

        for (String node : graph.keySet()) {
            findCyclesDFS(node, node, new ArrayList<>(), graph, cycles, visited);
        }

        return cycles;
    }

    private void findCyclesDFS(String start, String current, List<String> path,
                               Map<String, Set<String>> graph,
                               Set<Set<String>> cycles, Set<String> visited) {
        path.add(current);
        visited.add(current);

        for (String neighbor : graph.getOrDefault(current, new HashSet<>())) {
            if (neighbor.equals(start) && path.size() >= 3) {
                cycles.add(new TreeSet<>(path));
            } else if (!visited.contains(neighbor) && path.size() < 5) {
                findCyclesDFS(start, neighbor, path, graph, cycles, visited);
            }
        }

        path.remove(path.size() - 1);
        visited.remove(current);
    }
}