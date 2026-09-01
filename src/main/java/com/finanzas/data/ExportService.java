package com.finanzas.data;

import com.finanzas.model.GastoHogar;
import com.finanzas.model.MetaAhorro;
import com.finanzas.model.Money;
import com.finanzas.model.Presupuesto;
import com.finanzas.model.Transaccion;
import com.finanzas.model.Transaccion.Tipo;
import com.finanzas.model.Usuario;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.nio.file.Files;

public final class ExportService {
    private static final ExportService INSTANCE = new ExportService();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getNumberInstance(new Locale("es", "CO"));

    private ExportService() {
    }

    public static final class ImportResult {
        private final int imported;
        private final int skipped;

        public ImportResult(int imported, int skipped) {
            this.imported = imported;
            this.skipped = skipped;
        }

        public int getImported() {
            return imported;
        }

        public int getSkipped() {
            return skipped;
        }
    }

    public static final class TransactionPreviewItem {
        private final Transaccion transaccion;
        private final String status;
        private final String detail;

        public TransactionPreviewItem(Transaccion transaccion, String status, String detail) {
            this.transaccion = transaccion;
            this.status = status;
            this.detail = detail;
        }

        public Transaccion getTransaccion() {
            return transaccion;
        }

        public String getStatus() {
            return status;
        }

        public String getDetail() {
            return detail;
        }
    }

    public static final class TransactionPreviewResult {
        private final List<TransactionPreviewItem> items;

        public TransactionPreviewResult(List<TransactionPreviewItem> items) {
            this.items = items;
        }

        public List<TransactionPreviewItem> getItems() {
            return items;
        }

        public long countByStatus(String status) {
            return items.stream().filter(item -> status.equals(item.getStatus())).count();
        }
    }

    public static final class GoalPreviewItem {
        private final MetaAhorro meta;
        private final String status;
        private final String detail;

        public GoalPreviewItem(MetaAhorro meta, String status, String detail) {
            this.meta = meta;
            this.status = status;
            this.detail = detail;
        }

        public MetaAhorro getMeta() {
            return meta;
        }

        public String getStatus() {
            return status;
        }

        public String getDetail() {
            return detail;
        }
    }

    public static final class GoalPreviewResult {
        private final List<GoalPreviewItem> items;

        public GoalPreviewResult(List<GoalPreviewItem> items) {
            this.items = items;
        }

        public List<GoalPreviewItem> getItems() {
            return items;
        }

        public long countByStatus(String status) {
            return items.stream().filter(item -> status.equals(item.getStatus())).count();
        }
    }

    public static final class BudgetPreviewItem {
        private final Presupuesto presupuesto;
        private final String status;
        private final String detail;

        public BudgetPreviewItem(Presupuesto presupuesto, String status, String detail) {
            this.presupuesto = presupuesto;
            this.status = status;
            this.detail = detail;
        }

        public Presupuesto getPresupuesto() {
            return presupuesto;
        }

        public String getStatus() {
            return status;
        }

        public String getDetail() {
            return detail;
        }
    }

    public static final class BudgetPreviewResult {
        private final List<BudgetPreviewItem> items;

        public BudgetPreviewResult(List<BudgetPreviewItem> items) {
            this.items = items;
        }

        public List<BudgetPreviewItem> getItems() {
            return items;
        }

        public long countByStatus(String status) {
            return items.stream().filter(item -> status.equals(item.getStatus())).count();
        }
    }

    public static ExportService getInstance() {
        return INSTANCE;
    }

    public void exportCsv(File file, DataManager data) throws IOException {
        File target = ensureExtension(file, ".csv");
        try (PrintWriter writer = new PrintWriter(target, StandardCharsets.UTF_8.name())) {
            writer.println("Fecha,Tipo,Categoria,Descripcion,Monto");
            for (Transaccion transaccion : data.getTransacciones()) {
                writer.printf(
                        "%s,%s,%s,%s,%.2f%n",
                        transaccion.getFecha(),
                        escapeCsv(transaccion.getTipo().name()),
                        escapeCsv(transaccion.getCategoria()),
                        escapeCsv(transaccion.getDescripcion()),
                        transaccion.getMonto());
            }
        }
    }

    public File exportExcel(File file, DataManager data, String periodo) throws IOException {
        return exportExcel(file, data, data.getReportSnapshot(periodo));
    }

    public File exportExcel(File file, DataManager data, ReportSnapshot snapshot) throws IOException {
        File target = ensureExtension(file, ".xls");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8))) {
            writer.write(buildSpreadsheet(data, snapshot));
        }
        return target;
    }

    public File exportPdf(File file, DataManager data, String periodo) throws IOException {
        return exportPdf(file, data, data.getReportSnapshot(periodo));
    }

    public File exportPdf(File file, DataManager data, ReportSnapshot snapshot) throws IOException {
        File target = ensureExtension(file, ".pdf");
        List<String> lines = buildPdfLines(data, snapshot);
        writeSimplePdf(target, lines);
        return target;
    }

    public File exportTransactionsTemplate(File file) throws IOException {
        File target = ensureExtension(file, ".csv");
        try (PrintWriter writer = new PrintWriter(target, StandardCharsets.UTF_8.name())) {
            writer.println("Fecha,Tipo,Categoria,Descripcion,Monto");
            writer.println("\"2025-01-15\",\"INGRESO\",\"Salario\",\"Pago mensual\",\"2500000\"");
            writer.println("\"2025-01-18\",\"GASTO\",\"Alimentacion\",\"Mercado semanal\",\"185000\"");
        }
        return target;
    }

    public File exportGoalsTemplate(File file) throws IOException {
        File target = ensureExtension(file, ".csv");
        try (PrintWriter writer = new PrintWriter(target, StandardCharsets.UTF_8.name())) {
            writer.println("Meta,Icono,Monto actual,Monto objetivo,Progreso,Fecha limite");
            writer.println("\"Fondo de emergencia\",\"SEG\",\"500000\",\"2000000\",\"25\",\"2025-12-31\"");
            writer.println("\"Viaje familiar\",\"VIA\",\"1200000\",\"3000000\",\"40\",\"2026-03-15\"");
        }
        return target;
    }

    public File exportBudgetsTemplate(File file) throws IOException {
        File target = ensureExtension(file, ".csv");
        try (PrintWriter writer = new PrintWriter(target, StandardCharsets.UTF_8.name())) {
            writer.println("Categoria,Presupuestado");
            writer.println("\"Alimentacion\",\"800000\"");
            writer.println("\"Transporte\",\"300000\"");
        }
        return target;
    }

    public ImportResult importTransactionsCsv(File file, DataManager data) throws IOException {
        int imported = 0;
        int skipped = 0;
        TransactionPreviewResult preview = previewTransactionsCsv(file, data);
        for (TransactionPreviewItem item : preview.getItems()) {
            if ("VALIDA".equals(item.getStatus())) {
                data.addTransaccion(item.getTransaccion());
                imported++;
            } else {
                skipped++;
            }
        }
        return new ImportResult(imported, skipped);
    }

    public TransactionPreviewResult previewTransactionsCsv(File file, DataManager data) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        List<TransactionPreviewItem> items = new ArrayList<TransactionPreviewItem>();
        boolean firstRow = true;
        List<Transaccion> existing = data.getTransacciones();
        for (String rawLine : lines) {
            String line = stripBom(rawLine);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            if (firstRow) {
                firstRow = false;
                if (isTransactionsHeader(line)) {
                    continue;
                }
            }
            try {
                List<String> values = parseCsvLine(line);
                if (values.size() < 5) {
                    items.add(new TransactionPreviewItem(null, "INVALIDA", "Columnas insuficientes"));
                    continue;
                }
                LocalDate fecha = LocalDate.parse(values.get(0).trim());
                Tipo tipo = Tipo.valueOf(values.get(1).trim().toUpperCase(Locale.ROOT));
                String categoria = values.get(2).trim();
                String descripcion = values.get(3).trim();
                BigDecimal monto = parseMoneyValueDecimal(values.get(4));
                if (categoria.isEmpty() || descripcion.isEmpty() || monto.compareTo(BigDecimal.ZERO) <= 0) {
                    items.add(new TransactionPreviewItem(null, "INVALIDA", "Datos obligatorios incompletos"));
                    continue;
                }
                Transaccion transaccion = new Transaccion(tipo, categoria, descripcion, monto, fecha);
                if (isDuplicateTransaction(existing, transaccion) || isDuplicateInPreview(items, transaccion)) {
                    items.add(new TransactionPreviewItem(transaccion, "DUPLICADA", "Ya existe una transaccion igual"));
                } else {
                    items.add(new TransactionPreviewItem(transaccion, "VALIDA", "Lista para importar"));
                }
            } catch (Exception ex) {
                items.add(new TransactionPreviewItem(null, "INVALIDA", "Formato no valido"));
            }
        }
        return new TransactionPreviewResult(items);
    }

    public ImportResult importGoalsCsv(File file, DataManager data) throws IOException {
        int imported = 0;
        int skipped = 0;
        GoalPreviewResult preview = previewGoalsCsv(file, data);
        for (GoalPreviewItem item : preview.getItems()) {
            if ("VALIDA".equals(item.getStatus())) {
                data.addMeta(item.getMeta());
                imported++;
            } else {
                skipped++;
            }
        }
        return new ImportResult(imported, skipped);
    }

    public ImportResult importBudgetsCsv(File file, DataManager data) throws IOException {
        int imported = 0;
        int skipped = 0;
        BudgetPreviewResult preview = previewBudgetsCsv(file, data);
        for (BudgetPreviewItem item : preview.getItems()) {
            if ("VALIDA".equals(item.getStatus())) {
                data.addPresupuesto(item.getPresupuesto());
                imported++;
            } else {
                skipped++;
            }
        }
        return new ImportResult(imported, skipped);
    }

    public GoalPreviewResult previewGoalsCsv(File file, DataManager data) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        List<GoalPreviewItem> items = new ArrayList<GoalPreviewItem>();
        boolean firstRow = true;
        for (String rawLine : lines) {
            String line = stripBom(rawLine);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            if (firstRow) {
                firstRow = false;
                if (isGoalsHeader(line)) {
                    continue;
                }
            }
            try {
                List<String> values = parseCsvLine(line);
                if (values.size() < 6) {
                    items.add(new GoalPreviewItem(null, "INVALIDA", "Columnas insuficientes"));
                    continue;
                }
                String nombre = values.get(0).trim();
                String icono = values.get(1).trim();
                BigDecimal montoActual = parseMoneyValueDecimal(values.get(2));
                BigDecimal montoMeta = parseMoneyValueDecimal(values.get(3));
                LocalDate fechaLimite = LocalDate.parse(values.get(5).trim());
                if (nombre.isEmpty() || montoMeta.compareTo(BigDecimal.ZERO) <= 0) {
                    items.add(new GoalPreviewItem(null, "INVALIDA", "Datos obligatorios incompletos"));
                    continue;
                }
                MetaAhorro meta = new MetaAhorro(nombre, icono.isEmpty() ? "META" : icono, montoActual, montoMeta, "#1a73e8", fechaLimite);
                if (isDuplicateGoal(data.getMetas(), meta) || isDuplicateGoalInPreview(items, meta)) {
                    items.add(new GoalPreviewItem(meta, "DUPLICADA", "Ya existe una meta con ese nombre y fecha"));
                } else {
                    items.add(new GoalPreviewItem(meta, "VALIDA", "Lista para importar"));
                }
            } catch (Exception ex) {
                items.add(new GoalPreviewItem(null, "INVALIDA", "Formato no valido"));
            }
        }
        return new GoalPreviewResult(items);
    }

    public BudgetPreviewResult previewBudgetsCsv(File file, DataManager data) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        List<BudgetPreviewItem> items = new ArrayList<BudgetPreviewItem>();
        boolean firstRow = true;
        for (String rawLine : lines) {
            String line = stripBom(rawLine);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            if (firstRow) {
                firstRow = false;
                if (isBudgetsHeader(line)) {
                    continue;
                }
            }
            try {
                List<String> values = parseCsvLine(line);
                if (values.size() < 2) {
                    items.add(new BudgetPreviewItem(null, "INVALIDA", "Columnas insuficientes"));
                    continue;
                }
                String categoria = values.get(0).trim();
                BigDecimal presupuestado = parseMoneyValueDecimal(values.get(1));
                BigDecimal gastado = values.size() >= 3 ? parseMoneyValueDecimal(values.get(2)) : BigDecimal.ZERO;
                if (categoria.isEmpty() || presupuestado.compareTo(BigDecimal.ZERO) <= 0 || gastado.compareTo(BigDecimal.ZERO) < 0) {
                    items.add(new BudgetPreviewItem(null, "INVALIDA", "Datos obligatorios incompletos"));
                    continue;
                }
                Presupuesto presupuesto = new Presupuesto(categoria, presupuestado, gastado, "#34a853");
                if (isDuplicateBudget(data.getPresupuestos(), presupuesto) || isDuplicateBudgetInPreview(items, presupuesto)) {
                    items.add(new BudgetPreviewItem(presupuesto, "DUPLICADA", "Ya existe un presupuesto para esa categoria"));
                } else {
                    items.add(new BudgetPreviewItem(presupuesto, "VALIDA", "Lista para importar"));
                }
            } catch (Exception ex) {
                items.add(new BudgetPreviewItem(null, "INVALIDA", "Formato no valido"));
            }
        }
        return new BudgetPreviewResult(items);
    }

    public int importCsv(File file, DataManager data) throws IOException {
        return importTransactionsCsv(file, data).getImported();
    }

    private String buildSpreadsheet(DataManager data, ReportSnapshot snapshot) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>");
        xml.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"");
        xml.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">");
        xml.append("<Styles>");
        xml.append("<Style ss:ID=\"header\"><Font ss:Bold=\"1\"/><Interior ss:Color=\"#DCEBFA\" ss:Pattern=\"Solid\"/></Style>");
        xml.append("<Style ss:ID=\"title\"><Font ss:Bold=\"1\" ss:Size=\"14\"/></Style>");
        xml.append("</Styles>");

        addWorksheet(xml, "Resumen", buildSummaryRows(data, snapshot));
        addWorksheet(xml, "Transacciones", buildTransactionRows(snapshot));
        addWorksheet(xml, "Categorias", buildCategoryRows(snapshot));
        addWorksheet(xml, "Metas", buildGoalRows(data));
        addWorksheet(xml, "Presupuestos", buildBudgetRows(data));
        addWorksheet(xml, "Hogar", buildHouseholdRows(data));

        xml.append("</Workbook>");
        return xml.toString();
    }

    private void addWorksheet(StringBuilder xml, String name, List<List<String>> rows) {
        xml.append("<Worksheet ss:Name=\"").append(escapeXml(name)).append("\">");
        xml.append("<Table>");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            xml.append("<Row>");
            List<String> row = rows.get(rowIndex);
            boolean isHeader = rowIndex == 0;
            boolean isTitle = rowIndex == 0 && row.size() == 1;
            for (String value : row) {
                xml.append("<Cell");
                if (isTitle) {
                    xml.append(" ss:StyleID=\"title\"");
                } else if (isHeader) {
                    xml.append(" ss:StyleID=\"header\"");
                }
                xml.append("><Data ss:Type=\"String\">");
                xml.append(escapeXml(value));
                xml.append("</Data></Cell>");
            }
            xml.append("</Row>");
        }
        xml.append("</Table>");
        xml.append("</Worksheet>");
    }

    private List<List<String>> buildSummaryRows(DataManager data, ReportSnapshot snapshot) {
        Usuario usuario = data.getUsuario();
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(singleCellRow("Reporte financiero"));
        rows.add(row("Periodo", snapshot.getPeriod().getDisplayLabel()));
        rows.add(row("Fecha de exportacion", DATE_FORMAT.format(LocalDate.now())));
        rows.add(row("Usuario", usuario == null ? "" : safe(usuario.getNombre() + " " + usuario.getApellido()).trim()));
        rows.add(row("Moneda", usuario == null ? "" : safe(usuario.getMoneda())));
        rows.add(row("Ingresos del periodo", formatMoney(snapshot.getIncome())));
        rows.add(row("Gastos del periodo", formatMoney(snapshot.getExpenses())));
        rows.add(row("Balance del periodo", formatMoney(snapshot.getBalance())));
        rows.add(row("Ahorro del periodo", formatMoney(snapshot.getSavings())));
        rows.add(row("Tasa de ahorro", snapshot.getSavingsRatePercent() + "%"));
        rows.add(row("Transacciones del periodo", String.valueOf(snapshot.getTransactions().size())));
        rows.add(row("Metas activas", String.valueOf(data.getMetas().size())));
        rows.add(row("Presupuestos activos", String.valueOf(data.getPresupuestos().size())));
        rows.add(row("Gastos del hogar", String.valueOf(data.getGastosHogar().size())));
        return rows;
    }

    private List<List<String>> buildTransactionRows(ReportSnapshot snapshot) {
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(row("Fecha", "Tipo", "Categoria", "Descripcion", "Monto"));
        for (Transaccion transaccion : snapshot.getTransactions()) {
            rows.add(row(
                    DATE_FORMAT.format(transaccion.getFecha()),
                    transaccion.getTipo().name(),
                    safe(transaccion.getCategoria()),
                    safe(transaccion.getDescripcion()),
                    formatMoney(transaccion.getMontoDecimal())));
        }
        return rows;
    }

    private List<List<String>> buildCategoryRows(ReportSnapshot snapshot) {
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(row("Categoria", "Gasto", "Participacion"));
        for (java.util.Map.Entry<String, BigDecimal> entry : snapshot.getExpenseByCategory().entrySet()) {
            BigDecimal percentage = snapshot.getExpenses().compareTo(BigDecimal.ZERO) <= 0
                    ? Money.ZERO
                    : entry.getValue().multiply(BigDecimal.valueOf(100)).divide(snapshot.getExpenses(), 2, Money.ROUNDING);
            rows.add(row(safe(entry.getKey()), formatMoney(entry.getValue()), percentage + "%"));
        }
        return rows;
    }

    private List<List<String>> buildGoalRows(DataManager data) {
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(row("Meta", "Icono", "Monto actual", "Monto objetivo", "Progreso", "Fecha limite"));
        for (MetaAhorro meta : data.getMetas()) {
            rows.add(row(
                    safe(meta.getNombre()),
                    safe(meta.getIcono()),
                    formatMoney(meta.getMontoActual()),
                    formatMoney(meta.getMontoMeta()),
                    meta.getProgreso() + "%",
                    DATE_FORMAT.format(meta.getFechaLimite())));
        }
        return rows;
    }

    private List<List<String>> buildBudgetRows(DataManager data) {
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(row("Categoria", "Presupuestado", "Gastado", "Disponible", "Uso"));
        for (Presupuesto presupuesto : data.getPresupuestos()) {
            rows.add(row(
                    safe(presupuesto.getCategoria()),
                    formatMoney(presupuesto.getMontoPresupuestado()),
                    formatMoney(presupuesto.getMontoGastado()),
                    formatMoney(presupuesto.getSaldo()),
                    String.format(Locale.US, "%.1f%%", presupuesto.getPorcentajeUsado())));
        }
        return rows;
    }

    private List<List<String>> buildHouseholdRows(DataManager data) {
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(row("Fecha", "Descripcion", "Categoria", "Pagado por", "Monto", "Dividido"));
        for (GastoHogar gasto : data.getGastosHogar()) {
            rows.add(row(
                    DATE_FORMAT.format(gasto.getFecha()),
                    safe(gasto.getDescripcion()),
                    safe(gasto.getCategoria()),
                    safe(gasto.getPagadoPor()),
                    formatMoney(gasto.getMonto()),
                    gasto.isDividido() ? "Si" : "No"));
        }
        return rows;
    }

    private List<String> buildPdfLines(DataManager data, ReportSnapshot snapshot) {
        Usuario usuario = data.getUsuario();
        List<String> lines = new ArrayList<String>();
        lines.add("Reporte financiero");
        lines.add("Periodo: " + snapshot.getPeriod().getDisplayLabel());
        lines.add("Fecha de exportacion: " + DATE_FORMAT.format(LocalDate.now()));
        if (usuario != null) {
            lines.add("Usuario: " + safe((usuario.getNombre() + " " + usuario.getApellido()).trim()));
            lines.add("Moneda: " + safe(usuario.getMoneda()));
        }
        lines.add("");
        lines.add("Resumen");
        lines.add("Ingresos del periodo: " + formatMoney(snapshot.getIncome()));
        lines.add("Gastos del periodo: " + formatMoney(snapshot.getExpenses()));
        lines.add("Balance del periodo: " + formatMoney(snapshot.getBalance()));
        lines.add("Ahorro del periodo: " + formatMoney(snapshot.getSavings()));
        lines.add("Tasa de ahorro: " + snapshot.getSavingsRatePercent() + "%");
        lines.add("");
        lines.add("Gastos por categoria");
        for (java.util.Map.Entry<String, BigDecimal> entry : snapshot.getExpenseByCategory().entrySet()) {
            lines.add(safe(entry.getKey()) + ": " + formatMoney(entry.getValue()));
        }
        lines.add("");
        lines.add("Metas de ahorro");
        for (MetaAhorro meta : data.getMetas()) {
            lines.add(String.format(
                    Locale.US,
                    "%s %s - %s de %s (%d%%)",
                    safe(meta.getIcono()),
                    safe(meta.getNombre()),
                    formatMoney(meta.getMontoActual()),
                    formatMoney(meta.getMontoMeta()),
                    meta.getProgreso()));
        }
        lines.add("");
        lines.add("Presupuestos");
        for (Presupuesto presupuesto : data.getPresupuestos()) {
            lines.add(String.format(
                    Locale.US,
                    "%s - gastado %s de %s",
                    safe(presupuesto.getCategoria()),
                    formatMoney(presupuesto.getMontoGastado()),
                    formatMoney(presupuesto.getMontoPresupuestado())));
        }
        lines.add("");
        lines.add("Transacciones recientes");
        int transactionCount = Math.min(12, snapshot.getTransactions().size());
        for (int i = 0; i < transactionCount; i++) {
            Transaccion transaccion = snapshot.getTransactions().get(i);
            lines.add(String.format(
                    Locale.US,
                    "%s - %s - %s - %s",
                    DATE_FORMAT.format(transaccion.getFecha()),
                    transaccion.getTipo().name(),
                    safe(transaccion.getCategoria()),
                    formatMoney(transaccion.getMontoDecimal())));
        }
        return lines;
    }

    private void writeSimplePdf(File file, List<String> lines) throws IOException {
        StringBuilder content = new StringBuilder();
        int y = 790;
        for (String line : lines) {
            if (y < 50) {
                break;
            }
            content.append("BT /F1 11 Tf 50 ").append(y).append(" Td (");
            content.append(escapePdf(line));
            content.append(") Tj ET\n");
            y -= 16;
        }

        byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);
        StringBuilder pdf = new StringBuilder();
        List<Integer> offsets = new ArrayList<Integer>();
        pdf.append("%PDF-1.4\n");

        offsets.add(pdf.length());
        pdf.append("1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n");
        offsets.add(pdf.length());
        pdf.append("2 0 obj<< /Type /Pages /Count 1 /Kids [3 0 R] >>endobj\n");
        offsets.add(pdf.length());
        pdf.append("3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>endobj\n");
        offsets.add(pdf.length());
        pdf.append("4 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj\n");
        offsets.add(pdf.length());
        pdf.append("5 0 obj<< /Length ").append(contentBytes.length).append(" >>stream\n");

        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(pdf.toString().getBytes(StandardCharsets.ISO_8859_1));
            output.write(contentBytes);

            StringBuilder tail = new StringBuilder();
            tail.append("endstream\nendobj\n");
            int xrefOffset = pdf.length() + contentBytes.length + tail.length();
            tail.append("xref\n0 6\n");
            tail.append("0000000000 65535 f \n");
            for (Integer offset : offsets) {
                tail.append(String.format(Locale.US, "%010d 00000 n \n", offset));
            }
            tail.append("trailer<< /Size 6 /Root 1 0 R >>\n");
            tail.append("startxref\n").append(xrefOffset).append("\n%%EOF");
            output.write(tail.toString().getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    private String escapeCsv(String value) {
        String safeValue = safe(value).replace("\"", "\"\"");
        return "\"" + safeValue + "\"";
    }

    private String escapeXml(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String escapePdf(String value) {
        return sanitizePdfText(value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String sanitizePdfText(String value) {
        String normalized = safe(value)
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("ñ", "n")
                .replace("Ñ", "N");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch >= 32 && ch <= 126) {
                builder.append(ch);
            } else {
                builder.append('?');
            }
        }
        return builder.toString();
    }

    private File ensureExtension(File file, String extension) {
        String path = file.getAbsolutePath();
        if (path.toLowerCase(Locale.ROOT).endsWith(extension)) {
            return file;
        }
        return new File(path + extension);
    }

    private String formatMoney(double value) {
        return "$" + MONEY_FORMAT.format(Math.round(value));
    }

    private String formatMoney(BigDecimal value) {
        return "$" + MONEY_FORMAT.format(Money.normalize(value));
    }

    private List<String> row(String... values) {
        List<String> row = new ArrayList<String>();
        for (String value : values) {
            row.add(value);
        }
        return row;
    }

    private List<String> singleCellRow(String value) {
        List<String> row = new ArrayList<String>();
        row.add(value);
        return row;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isTransactionsHeader(String line) {
        String normalized = line.toLowerCase(Locale.ROOT);
        return normalized.contains("fecha") && normalized.contains("tipo") && normalized.contains("categoria");
    }

    private boolean isGoalsHeader(String line) {
        String normalized = line.toLowerCase(Locale.ROOT);
        return normalized.contains("meta") && normalized.contains("monto") && normalized.contains("fecha");
    }

    private boolean isBudgetsHeader(String line) {
        String normalized = line.toLowerCase(Locale.ROOT);
        return normalized.contains("categoria") && normalized.contains("presup");
    }

    private String stripBom(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        return line.charAt(0) == '\uFEFF' ? line.substring(1) : line;
    }

    private BigDecimal parseMoneyValueDecimal(String value) {
        return Money.parseFlexible(safe(value).replace("%", ""));
    }

    private boolean isDuplicateTransaction(List<Transaccion> existing, Transaccion candidate) {
        for (Transaccion transaccion : existing) {
            if (sameTransaction(transaccion, candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDuplicateInPreview(List<TransactionPreviewItem> items, Transaccion candidate) {
        for (TransactionPreviewItem item : items) {
            if (item.getTransaccion() != null && "VALIDA".equals(item.getStatus()) && sameTransaction(item.getTransaccion(), candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean sameTransaction(Transaccion left, Transaccion right) {
        return left.getFecha().equals(right.getFecha())
                && left.getTipo() == right.getTipo()
                && safe(left.getCategoria()).equalsIgnoreCase(safe(right.getCategoria()))
                && safe(left.getDescripcion()).equalsIgnoreCase(safe(right.getDescripcion()))
                && Math.abs(left.getMonto() - right.getMonto()) < 0.001d;
    }

    private boolean isDuplicateGoal(List<MetaAhorro> existing, MetaAhorro candidate) {
        for (MetaAhorro meta : existing) {
            if (safe(meta.getNombre()).equalsIgnoreCase(safe(candidate.getNombre()))
                    && meta.getFechaLimite().equals(candidate.getFechaLimite())) {
                return true;
            }
        }
        return false;
    }

    private boolean isDuplicateGoalInPreview(List<GoalPreviewItem> items, MetaAhorro candidate) {
        for (GoalPreviewItem item : items) {
            if (item.getMeta() != null
                    && "VALIDA".equals(item.getStatus())
                    && safe(item.getMeta().getNombre()).equalsIgnoreCase(safe(candidate.getNombre()))
                    && item.getMeta().getFechaLimite().equals(candidate.getFechaLimite())) {
                return true;
            }
        }
        return false;
    }

    private boolean isDuplicateBudget(List<Presupuesto> existing, Presupuesto candidate) {
        for (Presupuesto presupuesto : existing) {
            if (safe(presupuesto.getCategoria()).equalsIgnoreCase(safe(candidate.getCategoria()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isDuplicateBudgetInPreview(List<BudgetPreviewItem> items, Presupuesto candidate) {
        for (BudgetPreviewItem item : items) {
            if (item.getPresupuesto() != null
                    && "VALIDA".equals(item.getStatus())
                    && safe(item.getPresupuesto().getCategoria()).equalsIgnoreCase(safe(candidate.getCategoria()))) {
                return true;
            }
        }
        return false;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }
}
