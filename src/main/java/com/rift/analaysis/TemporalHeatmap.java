package com.rift.analaysis;

import com.rift.model.Transaction;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

@Component
public class TemporalHeatmap {

    public Map<String, Object> generateHeatmap(List<Transaction> transactions) {
        Map<String, Object> heatmap = new LinkedHashMap<>();

        // Hour of day analysis (0-23)
        Map<Integer, Integer> hourlyDistribution = new TreeMap<>();
        // Day of week analysis (1-7, Monday=1)
        Map<Integer, Integer> dailyDistribution = new TreeMap<>();
        // Amount by time
        Map<String, Double> amountByTime = new HashMap<>();
        // Suspicious time slots
        List<String> suspiciousTimes = new ArrayList<>();

        // Initialize all hours with 0
        for (int i = 0; i < 24; i++) {
            hourlyDistribution.put(i, 0);
        }

        // Analyze each transaction
        for (Transaction tx : transactions) {
            LocalDateTime timestamp = tx.getTimestamp();
            int hour = timestamp.getHour();
            int day = timestamp.getDayOfWeek().getValue();

            hourlyDistribution.merge(hour, 1, Integer::sum);
            dailyDistribution.merge(day, 1, Integer::sum);

            String timeKey = String.format("%d-%02d", day, hour);
            amountByTime.merge(timeKey, tx.getAmount(), Double::sum);
        }

        // Find suspicious patterns
        // 1. Late night activity (12 AM - 4 AM)
        int nightTxns = 0;
        for (int h = 0; h <= 4; h++) {
            nightTxns += hourlyDistribution.get(h);
        }
        if (nightTxns > transactions.size() * 0.2) {
            suspiciousTimes.add("UNUSUAL_NIGHT_ACTIVITY");
        }

        // 2. Weekend spikes
        int weekendTxns = dailyDistribution.entrySet().stream()
                .filter(e -> e.getKey() >= 6)
                .mapToInt(Map.Entry::getValue)
                .sum();
        if (weekendTxns > transactions.size() * 0.3) {
            suspiciousTimes.add("WEEKEND_SPIKE");
        }

        // 3. Rapid successive transactions
        int rapidTxns = countRapidTransactions(transactions);
        if (rapidTxns > transactions.size() * 0.5) {
            suspiciousTimes.add("RAPID_TRANSACTIONS");
        }

        heatmap.put("hourly_distribution", hourlyDistribution);
        heatmap.put("daily_distribution", dailyDistribution);
        heatmap.put("amount_by_time", amountByTime);
        heatmap.put("suspicious_times", suspiciousTimes);
        heatmap.put("peak_hour", findPeakHour(hourlyDistribution));
        heatmap.put("risk_score", calculateTemporalRisk(hourlyDistribution, dailyDistribution));

        return heatmap;
    }

    private int countRapidTransactions(List<Transaction> transactions) {
        int count = 0;
        transactions.sort(Comparator.comparing(Transaction::getTimestamp));

        for (int i = 1; i < transactions.size(); i++) {
            long minutes = Duration.between(
                    transactions.get(i-1).getTimestamp(),
                    transactions.get(i).getTimestamp()
            ).toMinutes();

            if (minutes < 5) { // Less than 5 minutes apart
                count++;
            }
        }
        return count;
    }

    private int findPeakHour(Map<Integer, Integer> distribution) {
        return distribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    private double calculateTemporalRisk(Map<Integer, Integer> hourly, Map<Integer, Integer> daily) {
        double risk = 0.0;

        // Night activity risk
        int nightTotal = hourly.entrySet().stream()
                .filter(e -> e.getKey() <= 4 || e.getKey() >= 23)
                .mapToInt(Map.Entry::getValue)
                .sum();
        int dayTotal = hourly.entrySet().stream()
                .filter(e -> e.getKey() >= 9 && e.getKey() <= 17)
                .mapToInt(Map.Entry::getValue)
                .sum();

        if (nightTotal > dayTotal * 0.5) risk += 0.4;

        // Weekend activity risk
        int weekendTotal = daily.entrySet().stream()
                .filter(e -> e.getKey() >= 6)
                .mapToInt(Map.Entry::getValue)
                .sum();
        int weekdayTotal = daily.entrySet().stream()
                .filter(e -> e.getKey() <= 5)
                .mapToInt(Map.Entry::getValue)
                .sum();

        if (weekendTotal > weekdayTotal * 0.3) risk += 0.3;

        // Irregular hours
        if (hourly.values().stream().mapToInt(Integer::intValue).max().orElse(0) > 10) {
            risk += 0.3;
        }

        return Math.min(1.0, risk);
    }
}