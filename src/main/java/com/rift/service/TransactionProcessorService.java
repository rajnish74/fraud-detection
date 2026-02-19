package com.rift.service;

import com.rift.model.*;
import com.rift.utils.CsvParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransactionProcessorService {

    @Autowired
    private FraudDetectionService fraudDetectionService;

    public DetectionResult processTransactions(MultipartFile file) throws IOException {
        long startTime = System.currentTimeMillis();

        // Parse CSV
        List<Transaction> transactions = CsvParser.parseTransactions(file);

        // Build account graph
        Map<String, Account> accounts = buildAccountGraph(transactions);

        // Create result object
        DetectionResult result = new DetectionResult();
        result.setAccounts(accounts);

        // Run fraud detection
        fraudDetectionService.detectFraud(result);

        // Build final output
        result.buildOutput();

        result.setProcessingTime(System.currentTimeMillis() - startTime);
        return result;
    }

    private Map<String, Account> buildAccountGraph(List<Transaction> transactions) {
        Map<String, Account> accounts = new ConcurrentHashMap<>();

        for (Transaction tx : transactions) {
            // Get or create sender account
            Account sender = accounts.computeIfAbsent(tx.getSenderId(),
                    Account::new);

            // Get or create receiver account
            Account receiver = accounts.computeIfAbsent(tx.getReceiverId(),
                    Account::new);

            // Update sender metrics
            sender.getOutgoingTo().add(tx.getReceiverId());
            sender.setOutgoingCount(sender.getOutgoingCount() + 1);
            sender.setTotalSent(sender.getTotalSent() + tx.getAmount());
            sender.getTransactions().add(tx);

            // Update receiver metrics
            receiver.getIncomingFrom().add(tx.getSenderId());
            receiver.setIncomingCount(receiver.getIncomingCount() + 1);
            receiver.setTotalReceived(receiver.getTotalReceived() + tx.getAmount());
            receiver.getTransactions().add(tx);

            // Update transaction counts
            sender.setTransactionCount(sender.getTransactionCount() + 1);
            receiver.setTransactionCount(receiver.getTransactionCount() + 1);
        }

        return accounts;
    }
}