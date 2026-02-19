package com.rift.ml;

import com.rift.model.Account;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class LocalOutlierFactor {

    private static final int K_NEIGHBORS = 5;

    public double calculateScore(Account account, List<Account> allAccounts) {
        // Find k-nearest neighbors
        List<Account> neighbors = findKNearestNeighbors(account, allAccounts, K_NEIGHBORS);

        // Calculate local reachability density
        double lrd = calculateLocalReachabilityDensity(account, neighbors);

        // Calculate average LRD of neighbors
        double avgNeighborLRD = neighbors.stream()
                .mapToDouble(n -> calculateLocalReachabilityDensity(n,
                        findKNearestNeighbors(n, allAccounts, K_NEIGHBORS)))
                .average()
                .orElse(1.0);

        // LOF score = avg neighbor LRD / own LRD
        double lofScore = avgNeighborLRD / lrd;

        // Normalize to 0-1
        return Math.min(1.0, lofScore / 2);
    }

    private List<Account> findKNearestNeighbors(Account target, List<Account> accounts, int k) {
        return accounts.stream()
                .filter(a -> !a.getAccountId().equals(target.getAccountId()))
                .sorted((a, b) -> Double.compare(
                        calculateDistance(target, a),
                        calculateDistance(target, b)))
                .limit(k)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private double calculateDistance(Account a, Account b) {
        // Euclidean distance in feature space
        double txDiff = a.getTransactionCount() - b.getTransactionCount();
        double inDiff = a.getIncomingCount() - b.getIncomingCount();
        double outDiff = a.getOutgoingCount() - b.getOutgoingCount();
        double recDiff = a.getTotalReceived() - b.getTotalReceived();
        double sentDiff = a.getTotalSent() - b.getTotalSent();

        return Math.sqrt(
                txDiff * txDiff +
                        inDiff * inDiff +
                        outDiff * outDiff +
                        recDiff * recDiff / 1000 +  // Scale large numbers
                        sentDiff * sentDiff / 1000
        );
    }

    private double calculateLocalReachabilityDensity(Account account, List<Account> neighbors) {
        double sumReachDist = neighbors.stream()
                .mapToDouble(n -> Math.max(
                        calculateDistance(account, n),
                        getKthNeighborDistance(n, neighbors)))
                .sum();

        return 1 / (sumReachDist / neighbors.size());
    }

    private double getKthNeighborDistance(Account account, List<Account> neighbors) {
        return calculateDistance(account, neighbors.get(neighbors.size() - 1));
    }
}