package com.stockpro.reportservice.export;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class ReportExportService {

    public byte[] exportCsv(List<String> headers, List<? extends List<?>> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.join(",", headers)).append('\n');
        for (List<?> row : rows) {
            builder.append(row.stream()
                    .map(value -> "\"" + String.valueOf(value == null ? "" : value).replace("\"", "\"\"") + "\"")
                    .reduce((left, right) -> left + "," + right)
                    .orElse(""))
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportExcel(String sheetName, List<String> headers, List<? extends List<?>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<?> values = rows.get(rowIndex);
                for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                    row.createCell(columnIndex).setCellValue(String.valueOf(values.get(columnIndex)));
                }
            }
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate Excel export", ex);
        }
    }

    public byte[] exportPdf(String title, List<String> headers, List<? extends List<?>> rows) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();
            document.add(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(headers.size());
            for (String header : headers) {
                PdfPCell cell = new PdfPCell();
                cell.setPhrase(new Paragraph(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                table.addCell(cell);
            }
            for (List<?> row : rows) {
                for (Object value : row) {
                    table.addCell(String.valueOf(value == null ? "" : value));
                }
            }
            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Unable to generate PDF export", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate PDF export", ex);
        }
    }
}
