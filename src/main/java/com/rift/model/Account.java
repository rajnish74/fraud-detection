package com.rift.model;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Account {
    private String accountId;
    private double totalSent;
    private double totalReceived;
    private int transactionCount;
    private Set<String> incomingFrom = ConcurrentHashMap.newKeySet();
    private Set<String> outgoingTo = ConcurrentHashMap.newKeySet();
    private List<Transaction> transactions = new ArrayList<>();
    private double suspicionScore;
    private Set<String> patterns = ConcurrentHashMap.newKeySet();
    private String ringId;

    // Metrics for detection
    private int incomingCount;
    private int outgoingCount;
    private LocalDateTime firstTransaction;
    private LocalDateTime lastTransaction;
    private double averageTransactionAmount;

    public Account(String accountId) {
        this.accountId = accountId;
    }
}