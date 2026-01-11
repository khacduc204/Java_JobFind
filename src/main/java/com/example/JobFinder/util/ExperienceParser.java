package com.example.JobFinder.util;

import com.example.JobFinder.dto.ExperienceEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class ExperienceParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Map<String, Object>>> LIST_TYPE = new TypeReference<>() { };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{.*?\\}", Pattern.DOTALL);

    private ExperienceParser() {
    }

    public static List<ExperienceEntry> parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }

        String candidate = raw.trim();
        List<ExperienceEntry> parsed = tryParse(candidate);
        if (!parsed.isEmpty()) {
            return parsed;
        }

        String decoded = decode(candidate);
        if (!decoded.equals(candidate)) {
            parsed = tryParse(decoded);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }

        List<ExperienceEntry> extracted = extractInnerJson(decoded);
        if (!extracted.isEmpty()) {
            return extracted;
        }

        return List.of(ExperienceEntry.builder()
            .title("Kinh nghiệm làm việc")
            .description(decoded)
            .build());
    }

    private static List<ExperienceEntry> tryParse(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return Collections.emptyList();
        }

        String trimmed = candidate.trim();
        try {
            if (trimmed.startsWith("[")) {
                List<Map<String, Object>> nodes = OBJECT_MAPPER.readValue(trimmed, LIST_TYPE);
                return normalize(nodes);
            }
            if (trimmed.startsWith("{")) {
                Map<String, Object> node = OBJECT_MAPPER.readValue(trimmed, MAP_TYPE);
                return normalize(List.of(node));
            }
        } catch (Exception ignored) {
            // Fallback to other strategies
        }
        return Collections.emptyList();
    }

    private static List<ExperienceEntry> normalize(List<Map<String, Object>> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<ExperienceEntry> entries = new ArrayList<>();
        for (Map<String, Object> node : nodes) {
            if (node == null || node.isEmpty()) {
                continue;
            }

            if (node.size() == 1) {
                Object nested = node.values().iterator().next();
                if (nested instanceof String nestedString && nestedString.contains("{")) {
                    List<ExperienceEntry> inner = parse(nestedString);
                    if (!inner.isEmpty()) {
                        entries.addAll(inner);
                        continue;
                    }
                }
            }

            ExperienceEntry entry = buildEntry(node);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static ExperienceEntry buildEntry(Map<String, Object> node) {
        String title = clean(node.get("title"));
        String company = clean(node.get("company"));
        String start = formatDate(clean(node.get("start")));
        String end = formatDate(clean(node.get("end")));
        String description = clean(node.get("description"));

        boolean hasContent = StringUtils.hasText(title) || StringUtils.hasText(company)
            || StringUtils.hasText(start) || StringUtils.hasText(end)
            || StringUtils.hasText(description);

        if (!hasContent) {
            return null;
        }

        return ExperienceEntry.builder()
            .title(defaultIfBlank(title, "Chưa cập nhật"))
            .company(company)
            .startLabel(start)
            .endLabel(end)
            .description(description)
            .build();
    }

    private static String clean(Object value) {
        if (value == null) {
            return null;
        }
        String text = Objects.toString(value, "").trim();
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        }
        return text;
    }

    private static String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static String decode(String value) {
        String result = value;
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length() - 1);
        }
        result = result
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\/", "/");
        return result.trim();
    }

    private static List<ExperienceEntry> extractInnerJson(String value) {
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(value);
        List<Map<String, Object>> nodes = new ArrayList<>();
        while (matcher.find()) {
            try {
                Map<String, Object> node = OBJECT_MAPPER.readValue(matcher.group(), MAP_TYPE);
                nodes.add(node);
            } catch (Exception ignored) {
                // Skip invalid fragment
            }
        }
        return normalize(nodes);
    }

    private static String formatDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        if ("present".equalsIgnoreCase(trimmed) || "now".equalsIgnoreCase(trimmed)) {
            return "Hiện tại";
        }
        if ("current".equalsIgnoreCase(trimmed)) {
            return "Đang làm";
        }

        try {
            LocalDate date = LocalDate.parse(trimmed);
            return OUTPUT_FORMAT.format(date);
        } catch (DateTimeParseException ignored) {
            // Try next format
        }

        try {
            YearMonth yearMonth = YearMonth.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM"));
            return yearMonth.format(OUTPUT_FORMAT);
        } catch (DateTimeParseException ignored) {
            // Fallback to raw text
        }

        return trimmed;
    }
}
