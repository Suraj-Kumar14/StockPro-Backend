package com.stockpro.report.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CsvReportExporter implements ReportExporter {

    private static final DateTimeFormatter FILE_NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public boolean supports(ReportFormat format) {
        return ReportFormat.CSV == format;
    }

    @Override
    public Path export(ReportType reportType,
            ReportFormat format,
            List<String> headers,
            List<List<String>> rows,
            Path exportDirectory) throws IOException {
        Files.createDirectories(exportDirectory);
        String fileName = reportType.name().toLowerCase() + "-" + FILE_NAME_TIMESTAMP.format(LocalDateTime.now()) + ".csv";
        Path filePath = exportDirectory.resolve(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(toCsvLine(headers));
            writer.newLine();
            for (List<String> row : rows) {
                writer.write(toCsvLine(row));
                writer.newLine();
            }
        }

        return filePath;
    }

    private String toCsvLine(List<String> values) {
        return values.stream()
                .map(this::escape)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private String escape(String value) {
        String safeValue = value == null ? "" : value;
        boolean requiresQuotes = safeValue.contains(",") || safeValue.contains("\"") || safeValue.contains("\n");
        String escapedValue = safeValue.replace("\"", "\"\"");
        return requiresQuotes ? "\"" + escapedValue + "\"" : escapedValue;
    }
}
