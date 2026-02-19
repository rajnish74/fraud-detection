package com.rift.algorithms;

import com.rift.model.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SmurfingDetector {

    private static final int FAN_IN_THRESHOLD = 10;
    private static final int FAN_OUT_THRESHOLD = 10;
    private static final int TIME_WINDOW_HOURS = 72;

    public void detectSmurfing(DetectionResult result) {
        int ringCounter = result.getRings().size();

        // Detect fan-in patterns (multiple senders to one receiver)
        detectFanInPatterns(result, ringCounter);

        // Detect fan-out patterns (one sender to multiple receivers)
        detectFanOutPatterns(result, ringCounter);
    }

    private void detectFanInPatterns(DetectionResult result, int ringCounter) {
        for (Account account : result.getAccounts().values()) {
            if (account.getIncomingCount() >= FAN_IN_THRESHOLD) {
                // Check temporal clustering
                List<Transaction> incomingTxs = account.getTransactions().stream()
                        .filter(tx -> tx.getReceiverId().equals(account.getAccountId()))
                        .collect(Collectors.toList());

                if (hasTemporalClustering(incomingTxs)) {
                    String ringId = "RING_" + String.format("%03d", ++ringCounter);
                    FraudRing ring = new FraudRing(ringId, "smurfing_fan_in");

                    // Add the aggregator account
                    ring.getMemberAccounts().add(account.getAccountId());
                    account.getPatterns().add("fan_in_aggregator");
                    account.setRingId(ringId);
                    ring.getAccountScores().put(account.getAccountId(),
                            account.getSuspicionScore());

                    // Add top suspicious senders
                    Set<String> suspiciousSenders = findSuspiciousSenders(incomingTxs);
                    for (String senderId : suspiciousSenders) {
                        Account sender = result.getAccounts().get(senderId);
                        if (sender != null) {
                            ring.getMemberAccounts().add(senderId);
                            sender.getPatterns().add("fan_in_sender");
                            sender.setRingId(ringId);
                            ring.getAccountScores().put(senderId,
                                    sender.getSuspicionScore());
                        }
                    }

                    ring.calculateRiskScore();
                    result.getRings().put(ringId, ring);
                }
            }
        }
    }

    private void detectFanOutPatterns(DetectionResult result, int ringCounter) {
        for (Account account : result.getAccounts().values()) {
            if (account.getOutgoingCount() >= FAN_OUT_THRESHOLD) {
                List<Transaction> outgoingTxs = account.getTransactions().stream()
                        .filter(tx -> tx.getSenderId().equals(account.getAccountId()))
                        .collect(Collectors.toList());

                if (hasTemporalClustering(outgoingTxs)) {
                    String ringId = "RING_" + String.format("%03d", ++ringCounter);
                    FraudRing ring = new FraudRing(ringId, "smurfing_fan_out");

                    // Add the disperser account
                    ring.getMemberAccounts().add(account.getAccountId());
                    account.getPatterns().add("fan_out_disperser");
                    account.setRingId(ringId);
                    ring.getAccountScores().put(account.getAccountId(),
                            account.getSuspicionScore());

                    // Add receivers
                    Set<String> receivers = outgoingTxs.stream()
                            .map(Transaction::getReceiverId)
                            .collect(Collectors.toSet());

                    for (String receiverId : receivers) {
                        Account receiver = result.getAccounts().get(receiverId);
                        if (receiver != null) {
                            ring.getMemberAccounts().add(receiverId);
                            receiver.getPatterns().add("fan_out_receiver");
                            receiver.setRingId(ringId);
                            ring.getAccountScores().put(receiverId,
                                    receiver.getSuspicionScore());
                        }
                    }

                    ring.calculateRiskScore();
                    result.getRings().put(ringId, ring);
                }
            }
        }
    }

    private boolean hasTemporalClustering(List<Transaction> transactions) {
        if (transactions.size() < 3) return false;

        transactions.sort(Comparator.comparing(Transaction::getTimestamp));

        // Check if transactions cluster within 72-hour windows
        LocalDateTime windowStart = transactions.get(0).getTimestamp();
        int txInWindow = 0;

        for (Transaction tx : transactions) {
            if (tx.getTimestamp().isBefore(windowStart.plusHours(TIME_WINDOW_HOURS))) {
                txInWindow++;
            } else {
                if (txInWindow >= 5) return true; // 5+ transactions in window
                windowStart = tx.getTimestamp();
                txInWindow = 1;
            }
        }

        return txInWindow >= 5;
    }

    private Set<String> findSuspiciousSenders(List<Transaction> transactions) {
        // Find senders with small amounts (smurfing)
        double avgAmount = transactions.stream()
                .mapToDouble(Transaction::getAmount)
                .average()
                .orElse(0);

        double threshold = avgAmount * 0.3; // 30% of average

        return transactions.stream()
                .filter(tx -> tx.getAmount() < threshold)
                .map(Transaction::getSenderId)
                .collect(Collectors.toSet());
    }
}