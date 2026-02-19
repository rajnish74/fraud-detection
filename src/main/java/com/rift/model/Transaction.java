package com.rift.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Transaction {
    private String transactionId;
    private String senderId;
    private String receiverId;
    private double amount;
    private LocalDateTime timestamp;

    // Custom constructor for CSV parsing
    public Transaction(String[] csvRow) {
        this.transactionId = csvRow[0];
        this.senderId = csvRow[1];
        this.receiverId = csvRow[2];
        this.amount = Double.parseDouble(csvRow[3]);
        this.timestamp = LocalDateTime.parse(csvRow[4].replace(" ", "T"));
    }
}