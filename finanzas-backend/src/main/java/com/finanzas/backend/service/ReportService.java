package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.ReportDtos;
import com.finanzas.backend.api.dto.SavingsDtos;
import com.finanzas.backend.domain.BudgetEntity;
import com.finanzas.backend.domain.CategoryEntity;
import com.finanzas.backend.domain.RecurringTransactionEntity;
import com.finanzas.backend.domain.SavingsGoalEntity;
import com.finanzas.backend.domain.TransactionEntity;
import com.finanzas.backend.domain.TransactionType;
import com.finanzas.backend.repo.BudgetRepository;
import com.finanzas.backend.repo.CategoryRepository;
import com.finanzas.backend.repo.RecurringTransactionRepository;
import com.finanzas.backend.repo.SavingsGoalRepository;
import com.finanzas.backend.repo.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ReportService {
    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final BudgetRepository budgets;
    private final SavingsGoalRepository goals;
    private final RecurringTransactionRepository recurringTransactions;
    private final WorkspaceAccessService access;
    private final AuditLogService auditLogs;
    private final SavingsService savings;

    public ReportService(TransactionRepository transactions,
                         CategoryRepository categories,
                         BudgetRepository budgets,
                         SavingsGoalRepository goals,
                         RecurringTransactionRepository recurringTransactions,
                         WorkspaceAccessService access,
                         AuditLogService auditLogs,
                         SavingsService savings) {
        this.transactions = transactions;
        this.categories = categories;
        this.budgets = budgets;
        this.goals = goals;
        this.recurringTransactions = recurringTransactions;
        this.access = access;
        this.auditLogs = auditLogs;
        this.savings = savings;
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportResponse summary(UUID userId, UUID workspaceId, LocalDate from, LocalDate to) {
        access.requireMember(userId, workspaceId);
        DateRange range = normalizeRange(from, to);
        Map<UUID, CategoryEntity> categoriesById = categoriesById(workspaceId);
        List<TransactionEntity> periodTransactions = transactions.findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                workspaceId, range.from(), range.to());

        BigDecimal income = sum(periodTransactions, TransactionType.INCOME);
        BigDecimal expenses = sum(periodTransactions, TransactionType.EXPENSE);
        BigDecimal balance = money(income.subtract(expenses));
        SavingsDtos.SavingsSummaryResponse savingsSummary = savings.summary(userId, workspaceId, range.from(), range.to());
        Map<String, BigDecimal> expensesByCategory = expensesByCategory(periodTransactions, categoriesById);

        return new ReportDtos.ReportResponse(
                workspaceId,
                range.from(),
                range.to(),
                income,
                expenses,
                balance,
                savingsSummary.tasaAhorro(),
                savingsSummary.ahorroTotal(),
                savingsSummary.ahorroAsignadoAMetas(),
                savingsSummary.ahorroLibre(),
                savingsSummary.saldoDisponibleNoAhorrado(),
                expensesByCategory,
                toReportTransactions(periodTransactions, categoriesById),
                budgetUsage(workspaceId, range, periodTransactions, categoriesById),
                goalProgress(workspaceId),
                recurringUpcoming(workspaceId, range.to().plusDays(30)));
    }

    @Transactional
    public byte[] exportCsv(UUID userId, UUID workspaceId, LocalDate from, LocalDate to) {
        ReportDtos.ReportResponse report = summary(userId, workspaceId, from, to);
        StringBuilder csv = new StringBuilder();
        csv.append("tipo,fecha,categoria,descripcion,monto\n");
        csv.append(row("RESUMEN", report.from().toString(), "Ingresos", "Total de ingresos", report.ingresos().toPlainString()));
        csv.append(row("RESUMEN", report.from().toString(), "Gastos", "Total de gastos", report.gastos().toPlainString()));
        csv.append(row("RESUMEN", report.from().toString(), "Balance", "Balance del periodo", report.balance().toPlainString()));
        for (ReportDtos.ReportTransaction transaction : report.transacciones()) {
            csv.append(row(
                    transaction.type().name(),
                    transaction.date().toString(),
                    transaction.category(),
                    transaction.description(),
                    transaction.amount().toPlainString()));
        }
        auditLogs.record(workspaceId, userId, "REPORT_EXPORTED", "Report", workspaceId, Map.of(
                "from", report.from().toString(),
                "to", report.to().toString(),
                "format", "CSV"));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public byte[] exportPdf(UUID userId, UUID workspaceId, LocalDate from, LocalDate to) {
        ReportDtos.ReportResponse report = summary(userId, workspaceId, from, to);
        List<String> lines = reportLines(report);
        StringBuilder content = new StringBuilder();
        int y = 790;
        for (String line : lines) {
            if (y < 50) {
                break;
            }
            content.append("BT /F1 11 Tf 50 ").append(y).append(" Td (")
                    .append(escapePdf(line))
                    .append(") Tj ET\n");
            y -= 16;
        }
        byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);
        StringBuilder pdf = new StringBuilder();
        List<Integer> offsets = new ArrayList<>();
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
        StringBuilder tail = new StringBuilder();
        tail.append("endstream\nendobj\n");
        int xrefOffset = pdf.length() + contentBytes.length + tail.length();
        tail.append("xref\n0 6\n0000000000 65535 f \n");
        for (Integer offset : offsets) {
            tail.append(String.format(java.util.Locale.US, "%010d 00000 n \n", offset));
        }
        tail.append("trailer<< /Size 6 /Root 1 0 R >>\nstartxref\n").append(xrefOffset).append("\n%%EOF");
        auditExport(workspaceId, userId, report, "PDF");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            output.write(pdf.toString().getBytes(StandardCharsets.ISO_8859_1));
            output.write(contentBytes);
            output.write(tail.toString().getBytes(StandardCharsets.ISO_8859_1));
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible crear PDF.", ex);
        }
        return output.toByteArray();
    }

    @Transactional
    public byte[] exportXlsx(UUID userId, UUID workspaceId, LocalDate from, LocalDate to) {
        ReportDtos.ReportResponse report = summary(userId, workspaceId, from, to);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            zip(zip, "[Content_Types].xml", contentTypes());
            zip(zip, "_rels/.rels", rootRels());
            zip(zip, "xl/workbook.xml", workbookXml());
            zip(zip, "xl/_rels/workbook.xml.rels", workbookRels());
            zip(zip, "xl/worksheets/sheet1.xml", sheetXml(summaryRows(report)));
            zip(zip, "xl/worksheets/sheet2.xml", sheetXml(transactionRows(report)));
            zip(zip, "xl/worksheets/sheet3.xml", sheetXml(budgetRows(report)));
            zip(zip, "xl/worksheets/sheet4.xml", sheetXml(goalRows(report)));
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible crear XLSX.", ex);
        }
        auditExport(workspaceId, userId, report, "XLSX");
        return output.toByteArray();
    }

    private DateRange normalizeRange(LocalDate from, LocalDate to) {
        LocalDate resolvedFrom = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate resolvedTo = to == null ? LocalDate.now() : to;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango de fechas es invalido.");
        }
        return new DateRange(resolvedFrom, resolvedTo);
    }

    private BigDecimal sum(List<TransactionEntity> periodTransactions, TransactionType type) {
        return money(periodTransactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(TransactionEntity::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private Map<String, BigDecimal> expensesByCategory(List<TransactionEntity> periodTransactions, Map<UUID, CategoryEntity> categoriesById) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        periodTransactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .forEach(transaction -> {
                    String category = categoryName(transaction.getCategoryId(), categoriesById);
                    result.merge(category, transaction.getMonto(), BigDecimal::add);
                });
        return result.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> money(entry.getValue()), (left, right) -> left, LinkedHashMap::new));
    }

    private List<ReportDtos.ReportTransaction> toReportTransactions(List<TransactionEntity> periodTransactions,
                                                                    Map<UUID, CategoryEntity> categoriesById) {
        return periodTransactions.stream()
                .map(transaction -> new ReportDtos.ReportTransaction(
                        transaction.getId(),
                        transaction.getTransactionDate(),
                        transaction.getType(),
                        categoryName(transaction.getCategoryId(), categoriesById),
                        transaction.getDescripcion(),
                        transaction.getMonto()))
                .toList();
    }

    private List<ReportDtos.BudgetUsage> budgetUsage(UUID workspaceId, DateRange range, List<TransactionEntity> periodTransactions,
                                                     Map<UUID, CategoryEntity> categoriesById) {
        LocalDate firstMonth = range.from().withDayOfMonth(1);
        LocalDate lastMonth = range.to().withDayOfMonth(1);
        Map<String, BigDecimal> spentByMonthAndCategory = periodTransactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getTransactionDate().withDayOfMonth(1) + "|" + transaction.getCategoryId(),
                        Collectors.mapping(TransactionEntity::getMonto, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        return budgets.findByWorkspaceIdOrderByPeriodMonthDesc(workspaceId).stream()
                .filter(budget -> !budget.getPeriodMonth().isBefore(firstMonth) && !budget.getPeriodMonth().isAfter(lastMonth))
                .map(budget -> toBudgetUsage(budget, spentByMonthAndCategory, categoriesById))
                .sorted(Comparator.comparing(ReportDtos.BudgetUsage::periodMonth).reversed())
                .toList();
    }

    private ReportDtos.BudgetUsage toBudgetUsage(BudgetEntity budget, Map<String, BigDecimal> spentByMonthAndCategory,
                                                 Map<UUID, CategoryEntity> categoriesById) {
        BigDecimal spent = money(spentByMonthAndCategory.getOrDefault(budget.getPeriodMonth() + "|" + budget.getCategoryId(), BigDecimal.ZERO));
        BigDecimal available = money(budget.getAmount().subtract(spent));
        BigDecimal usage = budget.getAmount().signum() <= 0
                ? money(BigDecimal.ZERO)
                : spent.multiply(new BigDecimal("100")).divide(budget.getAmount(), 2, RoundingMode.HALF_UP);
        return new ReportDtos.BudgetUsage(
                budget.getId(),
                categoryName(budget.getCategoryId(), categoriesById),
                budget.getPeriodMonth(),
                budget.getAmount(),
                spent,
                available,
                usage);
    }

    private List<ReportDtos.GoalProgress> goalProgress(UUID workspaceId) {
        return goals.findByWorkspaceIdAndStatusNotOrderByDueDateAsc(workspaceId, "ARCHIVED").stream()
                .map(this::toGoalProgress)
                .toList();
    }

    private ReportDtos.GoalProgress toGoalProgress(SavingsGoalEntity goal) {
        BigDecimal progress = goal.getTargetAmount().signum() <= 0
                ? money(BigDecimal.ZERO)
                : goal.getCurrentAmount().multiply(new BigDecimal("100")).divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
        return new ReportDtos.GoalProgress(
                goal.getId(),
                goal.getNombre(),
                goal.getCurrentAmount(),
                goal.getTargetAmount(),
                progress,
                goal.getDueDate(),
                goal.getStatus());
    }

    private List<ReportDtos.RecurringUpcoming> recurringUpcoming(UUID workspaceId, LocalDate until) {
        return recurringTransactions.findByWorkspaceIdAndActiveTrueOrderByNextRunDateAsc(workspaceId).stream()
                .filter(recurring -> !recurring.getNextRunDate().isAfter(until))
                .map(this::toRecurringUpcoming)
                .toList();
    }

    private ReportDtos.RecurringUpcoming toRecurringUpcoming(RecurringTransactionEntity recurring) {
        return new ReportDtos.RecurringUpcoming(
                recurring.getId(),
                recurring.getNextRunDate(),
                recurring.getType(),
                recurring.getDescription(),
                recurring.getAmount());
    }

    private Map<UUID, CategoryEntity> categoriesById(UUID workspaceId) {
        return categories.findByWorkspaceIdAndArchivedFalseOrderByNombre(workspaceId).stream()
                .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));
    }

    private String categoryName(UUID categoryId, Map<UUID, CategoryEntity> categoriesById) {
        CategoryEntity category = categoriesById.get(categoryId);
        return category == null ? "Sin categoria" : category.getNombre();
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String row(String type, String date, String category, String description, String amount) {
        return csv(type) + "," + csv(date) + "," + csv(category) + "," + csv(description) + "," + csv(amount) + "\n";
    }

    private String csv(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private void auditExport(UUID workspaceId, UUID userId, ReportDtos.ReportResponse report, String format) {
        auditLogs.record(workspaceId, userId, "REPORT_EXPORTED", "Report", workspaceId, Map.of(
                "from", report.from().toString(),
                "to", report.to().toString(),
                "format", format));
    }

    private List<String> reportLines(ReportDtos.ReportResponse report) {
        List<String> lines = new ArrayList<>();
        lines.add("Reporte financiero");
        lines.add("Periodo: " + report.from() + " a " + report.to());
        lines.add("Ingresos: " + report.ingresos().toPlainString());
        lines.add("Gastos: " + report.gastos().toPlainString());
        lines.add("Balance: " + report.balance().toPlainString());
        lines.add("Ahorro total: " + report.ahorroTotal().toPlainString());
        lines.add("Ahorro libre: " + report.ahorroLibre().toPlainString());
        lines.add("Disponible fuera del ahorro: " + report.saldoDisponibleNoAhorrado().toPlainString());
        lines.add("Tasa de ahorro: " + report.tasaAhorro().toPlainString() + "%");
        lines.add("");
        lines.add("Gastos por categoria");
        report.gastosPorCategoria().forEach((category, amount) -> lines.add(category + ": " + amount.toPlainString()));
        lines.add("");
        lines.add("Transacciones");
        report.transacciones().stream().limit(20).forEach(transaction ->
                lines.add(transaction.date() + " " + transaction.type() + " " + transaction.category() + " " + transaction.amount()));
        return lines;
    }

    private List<List<String>> summaryRows(ReportDtos.ReportResponse report) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Campo", "Valor"));
        rows.add(List.of("Desde", report.from().toString()));
        rows.add(List.of("Hasta", report.to().toString()));
        rows.add(List.of("Ingresos", report.ingresos().toPlainString()));
        rows.add(List.of("Gastos", report.gastos().toPlainString()));
        rows.add(List.of("Balance", report.balance().toPlainString()));
        rows.add(List.of("Ahorro total", report.ahorroTotal().toPlainString()));
        rows.add(List.of("Ahorro asignado a metas", report.ahorroAsignadoAMetas().toPlainString()));
        rows.add(List.of("Ahorro libre", report.ahorroLibre().toPlainString()));
        rows.add(List.of("Disponible fuera del ahorro", report.saldoDisponibleNoAhorrado().toPlainString()));
        rows.add(List.of("Tasa ahorro", report.tasaAhorro().toPlainString()));
        return rows;
    }

    private List<List<String>> transactionRows(ReportDtos.ReportResponse report) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Fecha", "Tipo", "Categoria", "Descripcion", "Monto"));
        for (ReportDtos.ReportTransaction transaction : report.transacciones()) {
            rows.add(List.of(
                    transaction.date().toString(),
                    transaction.type().name(),
                    safe(transaction.category()),
                    safe(transaction.description()),
                    transaction.amount().toPlainString()));
        }
        return rows;
    }

    private List<List<String>> budgetRows(ReportDtos.ReportResponse report) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Mes", "Categoria", "Presupuestado", "Gastado", "Disponible", "Uso"));
        for (ReportDtos.BudgetUsage budget : report.presupuestos()) {
            rows.add(List.of(
                    budget.periodMonth().toString(),
                    safe(budget.category()),
                    budget.budgeted().toPlainString(),
                    budget.spent().toPlainString(),
                    budget.available().toPlainString(),
                    budget.usagePercent().toPlainString()));
        }
        return rows;
    }

    private List<List<String>> goalRows(ReportDtos.ReportResponse report) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Meta", "Actual", "Objetivo", "Progreso", "Fecha", "Estado"));
        for (ReportDtos.GoalProgress goal : report.metas()) {
            rows.add(List.of(
                    safe(goal.name()),
                    goal.currentAmount().toPlainString(),
                    goal.targetAmount().toPlainString(),
                    goal.progressPercent().toPlainString(),
                    goal.dueDate() == null ? "" : goal.dueDate().toString(),
                    safe(goal.status())));
        }
        return rows;
    }

    private String sheetXml(List<List<String>> rows) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            xml.append("<row r=\"").append(rowIndex + 1).append("\">");
            List<String> row = rows.get(rowIndex);
            for (int col = 0; col < row.size(); col++) {
                xml.append("<c r=\"").append(columnName(col)).append(rowIndex + 1).append("\" t=\"inlineStr\"><is><t>");
                xml.append(escapeXml(row.get(col)));
                xml.append("</t></is></c>");
            }
            xml.append("</row>");
        }
        xml.append("</sheetData></worksheet>");
        return xml.toString();
    }

    private String workbookXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets>"
                + "<sheet name=\"Resumen\" sheetId=\"1\" r:id=\"rId1\"/>"
                + "<sheet name=\"Transacciones\" sheetId=\"2\" r:id=\"rId2\"/>"
                + "<sheet name=\"Presupuestos\" sheetId=\"3\" r:id=\"rId3\"/>"
                + "<sheet name=\"Metas\" sheetId=\"4\" r:id=\"rId4\"/>"
                + "</sheets></workbook>";
    }

    private String workbookRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
                + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet3.xml\"/>"
                + "<Relationship Id=\"rId4\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet4.xml\"/>"
                + "</Relationships>";
    }

    private String rootRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private String contentTypes() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet3.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet4.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "</Types>";
    }

    private void zip(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String columnName(int index) {
        StringBuilder name = new StringBuilder();
        int value = index + 1;
        while (value > 0) {
            int remainder = (value - 1) % 26;
            name.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return name.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String escapeXml(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String escapePdf(String value) {
        return safe(value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}
