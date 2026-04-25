package com.stockpro.report.export;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ReportExporter {

    boolean supports(ReportFormat format);

    Path export(ReportType reportType, ReportFormat format, List<String> headers, List<List<String>> rows, Path exportDirectory)
            throws IOException;
}
