/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.service;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Servicio de exportación CSV/PDF para los listados de administración.
 */
@Service
public class AdminExportService {
    private static final int PDF_LINES_PER_PAGE = 35;
    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /**
     * Construye una respuesta CSV descargable.
     *
     * @param filename nombre base del archivo.
     * @param headers cabeceras del listado.
     * @param rows filas del listado.
     * @return respuesta HTTP con CSV.
     */
    public ResponseEntity<byte[]> csv(String filename, List<String> headers, List<List<String>> rows) {
        StringBuilder builder = new StringBuilder("\uFEFF");
        builder.append(toCsvLine(headers)).append('\n');
        rows.forEach(row -> builder.append(toCsvLine(row)).append('\n'));
        return download(filename + ".csv", "text/csv; charset=UTF-8", builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Construye una respuesta PDF descargable con una tabla textual simple.
     *
     * @param filename nombre base del archivo.
     * @param title título del documento.
     * @param headers cabeceras del listado.
     * @param rows filas del listado.
     * @return respuesta HTTP con PDF.
     */
    public ResponseEntity<byte[]> pdf(String filename, String title, List<String> headers, List<List<String>> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(title);
        lines.add("Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        lines.add(String.join(" | ", headers));
        lines.add("-".repeat(120));
        rows.stream()
            .map(row -> String.join(" | ", row))
            .map(this::truncateForPdf)
            .forEach(lines::add);
        byte[] pdf = buildPdf(title, lines);
        return download(filename + ".pdf", MediaType.APPLICATION_PDF_VALUE, pdf);
    }

    /**
     * Genera un nombre identificable con fecha para evitar sobrescrituras accidentales.
     *
     * @param base nombre base funcional.
     * @return nombre con timestamp.
     */
    public String timestampedName(String base) {
        return base + "-" + LocalDateTime.now().format(EXPORT_DATE_FORMAT);
    }

    /** Convierte una fila en CSV escapando comillas, saltos de línea y separador. */
    private String toCsvLine(List<String> values) {
        return values.stream().map(this::escapeCsv).reduce((a, b) -> a + ";" + b).orElse("");
    }

    /** Escapa un valor CSV con separador punto y coma. */
    private String escapeCsv(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(";") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    /** Crea una respuesta HTTP de descarga. */
    private ResponseEntity<byte[]> download(String filename, String contentType, byte[] content) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .body(content);
    }

    /** Limita líneas de PDF para que no desborden horizontalmente. */
    private String truncateForPdf(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 155 ? value.substring(0, 152) + "..." : value;
    }

    /** Construye un PDF mínimo sin dependencias externas. */
    private byte[] buildPdf(String title, List<String> lines) {
        List<List<String>> pages = splitPages(lines);
        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        objects.add("PAGES_PLACEHOLDER");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            int pageObjectId = 4 + i * 2;
            int contentObjectId = pageObjectId + 1;
            kids.append(pageObjectId).append(" 0 R ");
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents " + contentObjectId + " 0 R >>");
            String stream = contentStream(title, pages.get(i), i + 1, pages.size());
            objects.add("<< /Length " + stream.getBytes(StandardCharsets.ISO_8859_1).length + " >>\nstream\n" + stream + "endstream");
        }
        objects.set(1, "<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>");
        return assemblePdf(objects);
    }

    /** Divide el contenido en páginas con número de líneas estable. */
    private List<List<String>> splitPages(List<String> lines) {
        List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += PDF_LINES_PER_PAGE) {
            pages.add(lines.subList(i, Math.min(lines.size(), i + PDF_LINES_PER_PAGE)));
        }
        if (pages.isEmpty()) {
            pages.add(List.of("Sin datos"));
        }
        return pages;
    }

    /** Genera el flujo de texto de una página PDF. */
    private String contentStream(String title, List<String> lines, int page, int totalPages) {
        StringBuilder builder = new StringBuilder();
        builder.append("BT\n/F1 16 Tf\n45 800 Td\n(").append(pdfText(title)).append(") Tj\n");
        builder.append("/F1 9 Tf\n0 -24 Td\n");
        for (String line : lines) {
            builder.append("(").append(pdfText(line)).append(") Tj\n0 -18 Td\n");
        }
        builder.append("/F1 8 Tf\n0 -8 Td\n(Pagina ").append(page).append(" de ").append(totalPages).append(") Tj\nET\n");
        return builder.toString();
    }

    /** Ensambla objetos PDF y tabla xref. */
    private byte[] assemblePdf(List<String> objects) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n");
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            write(out, (i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n");
        }
        int xrefStart = out.size();
        write(out, "xref\n0 " + (objects.size() + 1) + "\n");
        write(out, "0000000000 65535 f \n");
        for (Integer offset : offsets) {
            write(out, String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        write(out, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefStart + "\n%%EOF");
        return out.toByteArray();
    }

    /** Escribe texto ISO-8859-1 sobre el PDF. */
    private void write(ByteArrayOutputStream out, String value) {
        out.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1));
    }

    /** Limpia texto para PDF Helvetica básico y escapa paréntesis. */
    private String pdfText(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replace("€", "EUR");
        return normalized.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
