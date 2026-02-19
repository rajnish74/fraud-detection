package com.rift.utils;

import com.rift.model.Transaction;
import org.apache.commons.csv.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.*;

public class CsvParser {

    public static List<Transaction> parseTransactions(MultipartFile file)
            throws IOException {
        List<Transaction> transactions = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVParser csvParser = new CSVParser(reader,
                     CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                try {
                    String[] row = new String[]{
                            record.get("transaction_id"),
                            record.get("sender_id"),
                            record.get("receiver_id"),
                            record.get("amount"),
                            record.get("timestamp")
                    };
                    transactions.add(new Transaction(row));
                } catch (Exception e) {
                    // Log and skip malformed rows
                    System.err.println("Skipping malformed row: " + e.getMessage());
                }
            }
        }

        return transactions;
    }
}