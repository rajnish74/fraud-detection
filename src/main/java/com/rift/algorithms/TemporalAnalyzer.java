package com.rift.algorithms;

import com.rift.model.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class TemporalAnalyzer {

    public void analyzeTemporalPatterns(DetectionResult result) {
        for (Account account : result.getAccounts().values()) {
            List<Transaction> txs = account.getTransactions();
            if (txs.size() < 2) continue;

            txs.sort(Comparator.comparing(Transaction::getTimestamp));

            // Detect high velocity (many transactions in short time)
            if (hasHighVelocity(txs)) {
                account.getPatterns().add("high_velocity");
            }

            // Detect unusual timing (night transactions, weekends)
            if (hasUnusualTiming(txs)) {
                account.getPatterns().add("unusual_timing");
            }

            // Detect rapid round-tripping
            if (hasRoundTripping(txs, result)) {
                account.getPatterns().add("round_tripping");
            }
        }
    }

    private boolean hasHighVelocity(List<Transaction> txs) {
        int rapidTxCount = 0;
        LocalDateTime windowStart = txs.get(0).getTimestamp();

        for (Transaction tx : txs) {
            if (ChronoUnit.MINUTES.between(windowStart, tx.getTimestamp()) <= 60) {
                rapidTxCount++;
                if (rapidTxCount >= 5) return true;
            } else {
                windowStart = tx.getTimestamp();
                rapidTxCount = 1;
            }
        }

        return false;
    }

    private boolean hasUnusualTiming(List<Transaction> txs) {
        int nightTxCount = 0;
        int weekendTxCount = 0;

        for (Transaction tx : txs) {
            LocalDateTime timestamp = tx.getTimestamp();
            int hour = timestamp.getHour();

            // Night transactions (11 PM - 5 AM)
            if (hour >= 23 || hour <= 5) {
                nightTxCount++;
            }

            // Weekend transactions
            if (timestamp.getDayOfWeek().getValue() >= 6) {
                weekendTxCount++;
            }
        }

        double nightRatio = (double) nightTxCount / txs.size();
        double weekendRatio = (double) weekendTxCount / txs.size();

        return nightRatio > 0.3 || weekendRatio > 0.5;
    }

    private boolean hasRoundTripping(List<Transaction> txs, DetectionResult result) {
        // Check if money returns to source quickly
        for (int i = 0; i < txs.size() - 1; i++) {
            Transaction tx1 = txs.get(i);
            for (int j = i + 1; j < txs.size(); j++) {
                Transaction tx2 = txs.get(j);

                if (tx1.getSenderId().equals(tx2.getReceiverId()) &&
                        tx1.getReceiverId().equals(tx2.getSenderId())) {

                    long hoursBetween = ChronoUnit.HOURS.between(
                            tx1.getTimestamp(), tx2.getTimestamp());

                    if (hoursBetween <= 24) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}