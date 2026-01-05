package com.katlehouniversity.ecd.controller;

import com.katlehouniversity.ecd.entity.UploadedStatement;
import com.katlehouniversity.ecd.entity.User;
import com.katlehouniversity.ecd.repository.UserRepository;
import com.katlehouniversity.ecd.service.StatementUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class StatementUploadController {

    private final StatementUploadService statementUploadService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadStatement(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Received statement upload request: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".csv") &&
                !filename.toLowerCase().endsWith(".md") &&
                !filename.toLowerCase().endsWith(".pdf"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only CSV, MD, and PDF files are supported"));
        }

        try {
            // Fetch the actual User from the database
            String username = userDetails != null ? userDetails.getUsername() : "admin";
            User uploadedBy = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            UploadedStatement statement = statementUploadService.uploadAndProcessStatement(file, uploadedBy);

            Map<String, Object> response = new HashMap<>();
            response.put("id", statement.getId());
            response.put("fileName", statement.getFileName());
            response.put("fileType", statement.getFileType());
            response.put("status", statement.getStatus());
            response.put("totalTransactions", statement.getTotalTransactions());
            response.put("matchedCount", statement.getMatchedCount());
            response.put("unmatchedCount", statement.getUnmatchedCount());
            response.put("uploadDate", statement.getUploadDate());
            response.put("processedDate", statement.getProcessedDate());

            if (statement.getStatus() == UploadedStatement.ProcessingStatus.FAILED) {
                response.put("errorMessage", statement.getErrorMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading statement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<UploadedStatement>> getAllStatements() {
        try {
            List<UploadedStatement> statements = statementUploadService.getAllStatements();
            return ResponseEntity.ok(statements);
        } catch (Exception e) {
            log.error("Error fetching statements", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UploadedStatement> getStatementById(@PathVariable Long id) {
        try {
            UploadedStatement statement = statementUploadService.getStatementById(id);
            return ResponseEntity.ok(statement);
        } catch (Exception e) {
            log.error("Error fetching statement", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
