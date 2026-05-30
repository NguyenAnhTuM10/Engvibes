package com.englishapp.sm2;

import com.englishapp.common.ApiException;
import com.englishapp.sm2.dto.ImportSummary;
import com.englishapp.sm2.dto.ImportSummary.ImportError;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private static final int MAX_CARDS = 1000;

    private final Sm2DeckRepository   deckRepo;
    private final Sm2CardRepository   cardRepo;
    private final Sm2ReviewRepository reviewRepo;
    private final ObjectMapper         objectMapper;

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Import từ paste text (kiểu Quizlet).
     * Mỗi "card" được phân cách bởi cardSep; front|back|ipa?|example? bởi termSep.
     */
    @Transactional
    public ImportSummary importText(UUID deckId, String content,
                                    String termSep, String cardSep) {
        requireDeckExists(deckId);
        if (content == null || content.isBlank()) {
            return new ImportSummary(0, 0, List.of());
        }

        // Chuẩn hoá line endings trước khi tách
        String normalised = content.replace("\r\n", "\n").replace("\r", "\n");
        // Nếu cardSep là "\n" (escaped string từ JSON) thì unescape
        String sep = unescape(cardSep);
        String tsep = unescape(termSep);

        String[] rawLines = normalised.split(java.util.regex.Pattern.quote(sep), -1);

        List<ParsedCard> parsed = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();

        for (int i = 0; i < rawLines.length; i++) {
            int lineNum = i + 1;
            String line = rawLines[i].trim();
            if (line.isEmpty()) continue;

            String[] cols = line.split(java.util.regex.Pattern.quote(tsep), -1);
            String front = cols.length > 0 ? cols[0].trim() : "";
            String back  = cols.length > 1 ? cols[1].trim() : "";
            String ipa   = cols.length > 2 ? cols[2].trim() : null;
            String ex    = cols.length > 3 ? cols[3].trim() : null;

            ImportError err = validate(lineNum, front, back);
            if (err != null) { errors.add(err); continue; }

            parsed.add(new ParsedCard(lineNum, front, back,
                    emptyToNull(ipa), emptyToNull(ex)));
        }

        return persist(deckId, parsed, errors);
    }

    /**
     * Import từ CSV file.
     * Header tự động bỏ qua nếu dòng đầu là "front,back[,ipa[,example]]".
     * Hỗ trợ double-quoted fields.
     */
    @Transactional
    public ImportSummary importCsv(UUID deckId, InputStream in) throws IOException {
        requireDeckExists(deckId);

        List<List<String>> rows = parseCsv(in);
        List<ParsedCard>   parsed = new ArrayList<>();
        List<ImportError>  errors = new ArrayList<>();

        int startRow = 0;
        if (!rows.isEmpty()) {
            String firstCol = rows.get(0).isEmpty() ? "" : rows.get(0).get(0).toLowerCase().trim();
            if (firstCol.equals("front")) startRow = 1;     // skip header
        }

        for (int i = startRow; i < rows.size(); i++) {
            int lineNum = i + 1;
            List<String> row = rows.get(i);
            String front = row.size() > 0 ? row.get(0).trim() : "";
            String back  = row.size() > 1 ? row.get(1).trim() : "";
            String ipa   = row.size() > 2 ? row.get(2).trim() : null;
            String ex    = row.size() > 3 ? row.get(3).trim() : null;

            ImportError err = validate(lineNum, front, back);
            if (err != null) { errors.add(err); continue; }

            parsed.add(new ParsedCard(lineNum, front, back,
                    emptyToNull(ipa), emptyToNull(ex)));
        }

        return persist(deckId, parsed, errors);
    }

    /**
     * Import từ JSON array: [{front, back, ipa?, exampleSentence?}].
     */
    @Transactional
    public ImportSummary importJson(UUID deckId, InputStream in) throws IOException {
        requireDeckExists(deckId);

        List<Map<String, Object>> items;
        try {
            items = objectMapper.readValue(in,
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw ApiException.badRequest("Invalid JSON: expected a JSON array of objects. " + e.getMessage());
        }

        List<ParsedCard>  parsed = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            int lineNum = i + 1;
            Map<String, Object> obj = items.get(i);
            String front = strVal(obj, "front");
            String back  = strVal(obj, "back");
            String ipa   = strVal(obj, "ipa");
            String ex    = strVal(obj, "exampleSentence");

            ImportError err = validate(lineNum, front, back);
            if (err != null) { errors.add(err); continue; }

            parsed.add(new ParsedCard(lineNum, front, back,
                    emptyToNull(ipa), emptyToNull(ex)));
        }

        return persist(deckId, parsed, errors);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Shared persistence logic
    // ─────────────────────────────────────────────────────────────────────

    private ImportSummary persist(UUID deckId,
                                   List<ParsedCard> parsed,
                                   List<ImportError> errors) {
        int total = parsed.size();
        if (total > MAX_CARDS) {
            throw ApiException.badRequest(
                    "Import limit exceeded: max " + MAX_CARDS + " cards, got " + total);
        }

        // Existing fronts (case-insensitive dedup)
        Set<String> existingFronts = cardRepo.findByDeckId(deckId).stream()
                .map(c -> c.getFront().toLowerCase().trim())
                .collect(Collectors.toSet());

        List<Sm2Card> toSave = new ArrayList<>();
        int skipped = 0;

        for (ParsedCard p : parsed) {
            if (existingFronts.contains(p.front().toLowerCase())) {
                skipped++;
                log.debug("[import] skip dup line={} front='{}'", p.line(), p.front());
                continue;
            }
            existingFronts.add(p.front().toLowerCase());    // prevent dup within batch
            toSave.add(Sm2Card.builder()
                    .deckId(deckId)
                    .front(p.front())
                    .back(p.back())
                    .ipa(p.ipa())
                    .exampleSentence(p.exampleSentence())
                    .build());
        }

        List<Sm2Card> saved = cardRepo.saveAll(toSave);

        // Tạo Review với default SRS (ease=2.5, interval=0, due=now) → vào queue ngay
        List<Sm2Review> reviews = saved.stream()
                .map(c -> Sm2Review.builder()
                        .cardId(c.getId())
                        .dueDate(Instant.now())  // due immediately = in queue
                        .build())
                .toList();
        reviewRepo.saveAll(reviews);

        log.info("[import] deck={} imported={} skipped={} errors={}",
                deckId, saved.size(), skipped, errors.size());

        return new ImportSummary(saved.size(), skipped, errors);
    }

    // ─────────────────────────────────────────────────────────────────────
    // CSV parser (không cần Apache Commons CSV)
    // ─────────────────────────────────────────────────────────────────────

    private List<List<String>> parseCsv(InputStream in) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            boolean first = true;
            String line;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    // Strip UTF-8 BOM (EF BB BF) added by Windows/Excel editors
                    line = line.startsWith("﻿") ? line.substring(1) : line;
                    first = false;
                }
                line = line.trim();
                if (line.isEmpty()) continue;
                rows.add(parseCsvLine(line));
            }
        }
        return rows;
    }

    /** RFC-4180 compliant CSV line parser với double-quote support. */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;                    // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private void requireDeckExists(UUID deckId) {
        if (!deckRepo.existsById(deckId)) {
            throw ApiException.notFound("Deck not found: " + deckId);
        }
    }

    private ImportError validate(int line, String front, String back) {
        if (front == null || front.isBlank()) {
            return new ImportError(line, "missing 'front' field");
        }
        if (back == null || back.isBlank()) {
            return new ImportError(line, "missing 'back' field — front='" + front + "'");
        }
        return null;
    }

    private String strVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : v.toString().trim();
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /** Unescape JSON-style escape sequences: \\t → TAB, \\n → LF. */
    private String unescape(String s) {
        if (s == null) return "\n";
        return s.replace("\\t", "\t").replace("\\n", "\n").replace("\\r", "\r");
    }

    /** Intermediate parsed card before dedup/persist. */
    private record ParsedCard(int line, String front, String back,
                               String ipa, String exampleSentence) {}
}
