package com.rift.controller;

import com.rift.model.DetectionResult;
import com.rift.service.TransactionProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class FileUploadController {

    @Autowired
    private TransactionProcessorService processorService;

    // Store latest result
    public static DetectionResult latestResult = null;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();

        try {
            System.out.println("📁 Processing file: " + file.getOriginalFilename());

            // Process file
            DetectionResult result = processorService.processTransactions(file);

            // Store in static variable
            latestResult = result;

            // Update GraphController
            GraphController.setLatestResult(result);

            System.out.println("✅ Processed: " + result.getAccounts().size() + " accounts");
            System.out.println("🔍 Rings detected: " + result.getFraudRings().size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "file", latestResult != null ? "loaded" : "none"
        ));
    }
}