package com.rift.controller;

import com.rift.model.DetectionResult;
import com.rift.model.Account;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/graph")
public class GraphController {

    private static DetectionResult latestResult = null;

    public static void setLatestResult(DetectionResult result) {
        latestResult = result;
        System.out.println("📊 Graph updated: " +
                (result != null ? result.getAccounts().size() : 0) + " accounts");
    }

    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getGraphData() {
        if (latestResult == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        for (Account account : latestResult.getAccounts().values()) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", account.getAccountId());
            node.put("label", account.getAccountId());

            double score = account.getSuspicionScore();
            if (score > 70) {
                node.put("color", "#ff4444");
                node.put("size", 30);
            } else if (score > 60) {
                node.put("color", "#ff8800");
                node.put("size", 25);
            } else if (score > 50) {
                node.put("color", "#ffaa00");
                node.put("size", 22);
            } else {
                node.put("color", "#44aa44");
                node.put("size", 20);
            }

            node.put("title", String.format(
                    "Account: %s<br>Score: %.1f<br>Patterns: %s<br>Ring: %s",
                    account.getAccountId(),
                    account.getSuspicionScore(),
                    account.getPatterns(),
                    account.getRingId()
            ));

            nodes.add(node);
        }

        for (Account account : latestResult.getAccounts().values()) {
            for (String receiver : account.getOutgoingTo()) {
                Map<String, Object> edge = new HashMap<>();
                edge.put("from", account.getAccountId());
                edge.put("to", receiver);
                edge.put("arrows", "to");
                edges.add(edge);
            }
        }

        response.put("nodes", nodes);
        response.put("edges", edges);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}