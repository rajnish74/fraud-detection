package com.rift.algorithms;

import com.rift.model.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class LayeredNetworkDetector {

    public void detectLayeredNetworks(DetectionResult result) {
        Set<Set<String>> uniqueLayers = new HashSet<>();
        Set<String> processedAccounts = new HashSet<>();

        // Find all chains
        for (Account account : result.getAccounts().values()) {
            if (!processedAccounts.contains(account.getAccountId())) {
                List<List<String>> chains = findTransactionChains(account, result, 2);

                for (List<String> chain : chains) {
                    if (chain.size() >= 2) {
                        Set<String> layerSet = new TreeSet<>(chain);
                        uniqueLayers.add(layerSet);
                        processedAccounts.addAll(chain);
                    }
                }
            }
        }

        // Also include single accounts that are part of patterns
        for (Account account : result.getAccounts().values()) {
            if (!account.getPatterns().isEmpty() && !processedAccounts.contains(account.getAccountId())) {
                Set<String> singleSet = new TreeSet<>();
                singleSet.add(account.getAccountId());
                uniqueLayers.add(singleSet);
            }
        }

        int ringCounter = result.getRings().size();

        for (Set<String> layerSet : uniqueLayers) {
            if (layerSet.size() >= 2) { // Only create rings for 2+ accounts
                String ringId = "RING_" + String.format("%03d", ++ringCounter);
                FraudRing ring = new FraudRing(ringId, "layered");

                for (String accountId : layerSet) {
                    Account account = result.getAccounts().get(accountId);
                    if (account != null) {
                        if (!account.getPatterns().contains("layered_network")) {
                            account.getPatterns().add("layered_network");
                        }
                        ring.addAccountWithScore(accountId, account.getSuspicionScore());
                    }
                }

                ring.calculateRiskScore();
                result.getRings().put(ringId, ring);
            }
        }
    }

    private List<List<String>> findTransactionChains(Account start,
                                                     DetectionResult result, int minLength) {
        List<List<String>> chains = new ArrayList<>();
        dfs(start.getAccountId(), new ArrayList<>(), chains, result, minLength,
                new HashSet<>());
        return chains;
    }

    private void dfs(String currentId, List<String> path,
                     List<List<String>> chains, DetectionResult result,
                     int minLength, Set<String> visited) {

        if (visited.contains(currentId)) {
            return;
        }

        path.add(currentId);
        visited.add(currentId);

        if (path.size() >= minLength) {
            chains.add(new ArrayList<>(path));
        }

        Account current = result.getAccounts().get(currentId);
        if (current != null && path.size() < 5) {
            for (String nextId : current.getOutgoingTo()) {
                if (!visited.contains(nextId)) {
                    dfs(nextId, path, chains, result, minLength, visited);
                }
            }
        }

        path.remove(path.size() - 1);
        visited.remove(currentId);
    }
}