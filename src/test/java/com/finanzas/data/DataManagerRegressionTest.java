package com.finanzas.data;

import com.finanzas.api.BackendInvoice;
import com.finanzas.api.BackendTaxConfig;
import com.finanzas.api.BackendTaxSummary;
import com.finanzas.model.GastoHogar;
import com.finanzas.model.FinancialCategory;
import com.finanzas.model.MetaAhorro;
import com.finanzas.model.Presupuesto;
import com.finanzas.model.Settlement;
import com.finanzas.model.Transaccion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataManagerRegressionTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        DataManager.resetForTests();
        System.clearProperty("finanzas.data.dir");
    }

    @Test
    void newAccountStartsWithNoFinancialDemoData() {
        DataManager data = freshDataManager();

        assertTrue(data.register("Cuenta Nueva", "nueva@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("nueva@example.com", "secreto1"));

        assertEquals(0, data.getTotalIngresos(), 0.001);
        assertEquals(0, data.getTotalGastos(), 0.001);
        assertEquals(0, data.getSaldoActual(), 0.001);
        assertEquals(0, data.getAhorros(), 0.001);
        assertEquals(0, data.getCategorias().length);
        assertEquals("Objetivo de ahorro: pendiente de configurar", data.getSavingsRecommendationText());
    }

    @Test
    void householdDataIsolatedBetweenUsers() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Usuario A", "a@example.com", "secreto1", "COP", "Hogar"));
        assertTrue(data.register("Usuario B", "b@example.com", "secreto1", "COP", "Hogar"));

        assertTrue(data.login("a@example.com", "secreto1"));
        data.addMiembro("Integrante A");
        data.addGastoHogar(new GastoHogar("Mercado", "Alimentacion", 120000, "Usuario A", LocalDate.now(), true));
        assertEquals(1, data.getGastosHogar().size());
        data.logout();

        assertTrue(data.login("b@example.com", "secreto1"));
        assertEquals(0, data.getGastosHogar().size());
        assertFalse(data.getMiembrosHogar().contains("Integrante A"));
    }

    @Test
    void householdSplitAndSettlementAdjustBalances() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Juan Perez", "juan@example.com", "secreto1", "COP", "Hogar"));
        assertTrue(data.login("juan@example.com", "secreto1"));
        data.addMiembro("Ana");
        data.addMiembro("Luis");
        data.addMiembro("Maria");

        data.addGastoHogar(new GastoHogar("Mercado", "Alimentacion", new BigDecimal("120000.00"), "Juan Perez", LocalDate.now(), true));

        assertEquals(90000.00, data.calcularDeudas().get("Juan Perez"), 0.01);
        assertEquals(-30000.00, data.calcularDeudas().get("Ana"), 0.01);

        data.addSettlement(new Settlement("Ana", "Juan Perez", new BigDecimal("30000.00"), LocalDate.now(), "Pago mercado"));

        assertEquals(60000.00, data.calcularDeudas().get("Juan Perez"), 0.01);
        assertEquals(0.00, data.calcularDeudas().get("Ana"), 0.01);
    }

    @Test
    void changingEmailKeepsDataWithSameProfileAndMovesLoginKey() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Correo Viejo", "old@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("old@example.com", "secreto1"));
        data.addTransaccion(new Transaccion(Transaccion.Tipo.INGRESO, "Salario", "Pago", 1000, LocalDate.now()));

        data.updateProfile("Correo", "Nuevo", "new@example.com", "", "");
        data.logout();

        assertFalse(data.login("old@example.com", "secreto1"));
        assertTrue(data.login("new@example.com", "secreto1"));
        assertEquals(1, data.getTransacciones().size());
        assertEquals("new@example.com", data.getUsuario().getEmail());
    }

    @Test
    void budgetSpentIsCalculatedFromCurrentMonthTransactions() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Presupuesto Real", "budget@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("budget@example.com", "secreto1"));

        Presupuesto presupuesto = new Presupuesto("Alimentacion", 800000, 0, "#1a73e8");
        data.addPresupuesto(presupuesto);
        data.addTransaccion(new Transaccion(Transaccion.Tipo.GASTO, "Alimentacion", "Mercado", 250000, LocalDate.now()));
        data.addTransaccion(new Transaccion(Transaccion.Tipo.GASTO, "Alimentacion", "Gasto viejo", 300000, LocalDate.now().minusMonths(1)));
        data.addTransaccion(new Transaccion(Transaccion.Tipo.GASTO, "Transporte", "Bus", 100000, LocalDate.now()));

        Presupuesto recalculated = data.getPresupuestos().get(0);
        assertEquals(250000, recalculated.getMontoGastado(), 0.001);
        assertEquals(31.25, recalculated.getPorcentajeUsado(), 0.001);
    }

    @Test
    void profileAvatarIsValidatedStoredAndReloaded() throws Exception {
        DataManager data = freshDataManager();
        assertTrue(data.register("Avatar User", "avatar@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("avatar@example.com", "secreto1"));
        File source = createTestPng();

        assertTrue(data.updateProfileImage(source), data.getLastErrorMessage());
        String storedPath = data.getUsuario().getProfileImagePath();
        assertTrue(new File(storedPath).isAbsolute());
        assertTrue(Files.exists(Path.of(storedPath)));

        DataManager.resetForTests();
        DataManager reloaded = DataManager.getInstance();
        assertTrue(reloaded.login("avatar@example.com", "secreto1"));
        assertEquals(storedPath, reloaded.getUsuario().getProfileImagePath());
        assertTrue(Files.exists(Path.of(reloaded.getUsuario().getProfileImagePath())));
        assertNotEquals(source.getAbsolutePath(), storedPath);
    }

    @Test
    void monetaryValuesKeepDecimalPrecision() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Precision Money", "precision@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("precision@example.com", "secreto1"));

        data.addTransaccion(new Transaccion(Transaccion.Tipo.INGRESO, "Prueba", "Decimal A", new BigDecimal("0.10"), LocalDate.now()));
        data.addTransaccion(new Transaccion(Transaccion.Tipo.INGRESO, "Prueba", "Decimal B", new BigDecimal("0.20"), LocalDate.now()));

        assertEquals(new BigDecimal("0.10"), data.getTransacciones().get(1).getMontoDecimal());
        assertEquals(new BigDecimal("0.20"), data.getTransacciones().get(0).getMontoDecimal());
        assertEquals(0.30, data.getTotalIngresos(), 0.0001);
    }

    @Test
    void reportSnapshotFiltersPeriodAndGlobalSearchFindsSections() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Report User", "report@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("report@example.com", "secreto1"));

        data.addTransaccion(new Transaccion(Transaccion.Tipo.INGRESO, "Salario", "Actual", new BigDecimal("1000.00"), LocalDate.now()));
        data.addTransaccion(new Transaccion(Transaccion.Tipo.GASTO, "Comida", "Actual gasto", new BigDecimal("250.00"), LocalDate.now()));
        data.addTransaccion(new Transaccion(Transaccion.Tipo.GASTO, "Viaje", "Gasto viejo", new BigDecimal("500.00"), LocalDate.now().minusDays(80)));

        ReportSnapshot last30 = data.getReportSnapshot(ReportPeriod.LAST_30_DAYS);

        assertEquals(2, last30.getTransactions().size());
        assertEquals(new BigDecimal("250.00"), last30.getExpenses());
        assertEquals("gastos", data.searchGlobal("Actual gasto").get(0).getSection());
    }

    @Test
    void notificationsRespectUserPreferences() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Notify User", "notify@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("notify@example.com", "secreto1"));

        data.addPresupuesto(new Presupuesto("Comida", new BigDecimal("100.00"), BigDecimal.ZERO, "#1a73e8"));
        data.addTransaccion(new Transaccion(Transaccion.Tipo.GASTO, "Comida", "Mercado", new BigDecimal("90.00"), LocalDate.now()));

        assertTrue(data.getNotifications().stream().anyMatch(item -> item.getTitle().contains("Presupuesto en riesgo")));

        data.getUsuario().setNotifPresupuesto(false);
        data.notifyListeners();

        assertFalse(data.getNotifications().stream().anyMatch(item -> item.getTitle().contains("Presupuesto")));
    }

    @Test
    void settingsPersistAcrossLocalRestart() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Settings User", "settings@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("settings@example.com", "secreto1"));

        assertTrue(data.updateSettings("USD", "en-US", "UTC", "CODE_SUFFIX", "DARK", false, true, false));

        DataManager.resetForTests();
        DataManager reloaded = DataManager.getInstance();
        assertTrue(reloaded.login("settings@example.com", "secreto1"));

        assertEquals("USD", reloaded.getUsuario().getMoneda());
        assertEquals("en-US", reloaded.getUsuario().getLocale());
        assertEquals("UTC", reloaded.getUsuario().getTimeZone());
        assertEquals("CODE_SUFFIX", reloaded.getUsuario().getMoneyFormat());
        assertEquals("DARK", reloaded.getUsuario().getTheme());
        assertFalse(reloaded.getUsuario().isNotifPresupuesto());
        assertTrue(reloaded.getUsuario().isNotifMetas());
        assertFalse(reloaded.getUsuario().isNotifConsejos());
    }

    @Test
    void customCategoriesCanBeArchivedAndRestored() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Cat User", "cat@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("cat@example.com", "secreto1"));

        data.addCategory(FinancialCategory.Kind.EXPENSE, "Mascotas", "PET", "#00aa00");
        assertTrue(java.util.Arrays.asList(data.getCategoryNames(FinancialCategory.Kind.EXPENSE)).contains("Mascotas"));

        FinancialCategory category = data.getCategories(FinancialCategory.Kind.EXPENSE, true).stream()
                .filter(item -> item.getName().equals("Mascotas"))
                .findFirst()
                .orElseThrow(AssertionError::new);
        data.archiveCategory(category);
        assertFalse(java.util.Arrays.asList(data.getCategoryNames(FinancialCategory.Kind.EXPENSE)).contains("Mascotas"));

        data.addCategory(FinancialCategory.Kind.EXPENSE, "Mascotas", "PET", "#00aa00");
        assertTrue(java.util.Arrays.asList(data.getCategoryNames(FinancialCategory.Kind.EXPENSE)).contains("Mascotas"));
    }

    @Test
    void savingsRecommendationUsesRealIncomeAndCanBeDisabled() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Save User", "save@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("save@example.com", "secreto1"));

        assertEquals("Objetivo de ahorro: pendiente de configurar", data.getSavingsRecommendationText());

        data.addTransaccion(new Transaccion(Transaccion.Tipo.INGRESO, "Salario", "Pago", new BigDecimal("1000.00"), LocalDate.now()));
        data.configureSavingsRecommendation(true, "Agresivo", BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(new BigDecimal("300.00"), data.getSavingsRecommendationAmount());

        data.configureSavingsRecommendation(true, "Monto fijo", BigDecimal.ZERO, new BigDecimal("123.45"));
        assertEquals(new BigDecimal("123.45"), data.getSavingsRecommendationAmount());

        data.configureSavingsRecommendation(false, "Equilibrado", BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO.setScale(2), data.getSavingsRecommendationAmount());
    }

    @Test
    void loginIsTemporarilyBlockedAfterRepeatedFailures() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Rate User", "rate@example.com", "secreto1", "COP", "Personal"));

        for (int i = 0; i < 5; i++) {
            assertFalse(data.login("rate@example.com", "incorrecta"));
        }

        assertFalse(data.login("rate@example.com", "secreto1"));
        assertTrue(data.getLastErrorMessage().contains("Demasiados intentos"));
    }

    @Test
    void retirarMetaValidatesBoundsAndDepositRejectsNonPositiveAmounts() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Meta User", "meta@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("meta@example.com", "secreto1"));

        MetaAhorro meta = new MetaAhorro("Vacaciones", "VAC", 100.00, 500.00, "#1a73e8", LocalDate.now().plusMonths(3));
        data.addMeta(meta);

        assertFalse(data.depositarMeta(meta, 0));
        assertFalse(data.depositarMeta(meta, -50));
        assertEquals(100.00, meta.getMontoActual(), 0.001);

        assertFalse(data.retirarMeta(meta, 0));
        assertFalse(data.retirarMeta(meta, -10));
        assertFalse(data.retirarMeta(meta, 100.01));
        assertEquals(100.00, meta.getMontoActual(), 0.001);

        assertTrue(data.retirarMeta(meta, 100.00));
        assertEquals(0.00, meta.getMontoActual(), 0.001);
    }

    @Test
    void ahorroMesActualClampsAtZeroWhenWithdrawalsExceedThisMonthsDeposits() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Ahorro User", "ahorro@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("ahorro@example.com", "secreto1"));

        // Pre-existing balance from creation (not this month's contribution),
        // then a withdrawal this month with no matching deposit this month:
        // the net ledger movement is negative and must clamp at 0, not go negative.
        MetaAhorro meta = new MetaAhorro("Emergencia", "SEG", 1000.00, 2000.00, "#1a73e8", LocalDate.now().plusMonths(6));
        data.addMeta(meta);
        assertTrue(data.retirarMeta(meta, 600.00));

        assertEquals(0.00, data.getAhorroMesActual(), 0.001);
    }

    @Test
    void ahorroMesActualCountsNetDepositsAndWithdrawalsThisMonth() {
        DataManager data = freshDataManager();
        assertTrue(data.register("Ahorro Neto", "ahorroneto@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("ahorroneto@example.com", "secreto1"));

        MetaAhorro meta = new MetaAhorro("Viaje", "VIA", 0.00, 2000.00, "#1a73e8", LocalDate.now().plusMonths(6));
        data.addMeta(meta);
        assertTrue(data.depositarMeta(meta, 500.00));
        assertTrue(data.retirarMeta(meta, 200.00));

        assertEquals(300.00, data.getAhorroMesActual(), 0.001);
    }

    @Test
    void localInvoicesAreStoredMostRecentFirstAndReplacingAttachmentDeletesThePrevious() throws Exception {
        DataManager data = freshDataManager();
        assertTrue(data.register("Factura User", "factura@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("factura@example.com", "secreto1"));

        BackendInvoice first = data.createInvoice(null, "F-001", "Proveedor A", "900123456",
                LocalDate.now(), new BigDecimal("100.00"), new BigDecimal("19.00"), new BigDecimal("119.00"), "");
        BackendInvoice second = data.createInvoice(null, "F-002", "Proveedor B", "900654321",
                LocalDate.now(), new BigDecimal("50.00"), new BigDecimal("9.50"), new BigDecimal("59.50"), "");

        List<BackendInvoice> invoices = data.listInvoices(null, null);
        assertEquals(2, invoices.size());
        assertEquals(second.getId(), invoices.get(0).getId());
        assertEquals(first.getId(), invoices.get(1).getId());

        File png = tempDir.resolve("receipt.png").toFile();
        Files.writeString(png.toPath(), "fake-png-bytes");
        BackendInvoice withPng = data.uploadInvoiceAttachment(first.getId(), png);
        assertTrue(withPng.hasAttachment());
        String pngAttachmentPath = withPng.getAttachmentRef();
        assertTrue(Files.exists(Path.of(pngAttachmentPath)));

        File jpg = tempDir.resolve("receipt.jpg").toFile();
        Files.writeString(jpg.toPath(), "fake-jpg-bytes");
        BackendInvoice withJpg = data.uploadInvoiceAttachment(first.getId(), jpg);
        assertNotEquals(pngAttachmentPath, withJpg.getAttachmentRef());
        assertFalse(Files.exists(Path.of(pngAttachmentPath)), "replacing the attachment must delete the old file");
        assertTrue(Files.exists(Path.of(withJpg.getAttachmentRef())));
    }

    @Test
    void taxSummaryUsesDefaultsAndFlagsExactThresholdAsExceeded() throws Exception {
        DataManager data = freshDataManager();
        assertTrue(data.register("Dian User", "dian@example.com", "secreto1", "COP", "Personal"));
        assertTrue(data.login("dian@example.com", "secreto1"));

        int year = LocalDate.now().getYear();

        // No config stored yet: falls back to defaultUvtForYear(...) and 1400 UVT for income.
        BackendTaxSummary baseline = data.getTaxSummary(year);
        assertFalse(baseline.isExceedsIncomeThreshold());
        assertFalse(baseline.isObligationToDeclare());

        BigDecimal uvt = DataManager.defaultUvtForYear(year);
        BigDecimal incomeThreshold = uvt.multiply(BigDecimal.valueOf(1400));

        data.addTransaccion(new Transaccion(Transaccion.Tipo.INGRESO, "Salario", "Ingreso limite",
                incomeThreshold, LocalDate.of(year, 6, 1)));
        // A transaction dated the first day of next year must not count toward this year's total.
        data.addTransaccion(new Transaccion(Transaccion.Tipo.INGRESO, "Salario", "Fuera de rango",
                new BigDecimal("999999999.00"), LocalDate.of(year + 1, 1, 1)));

        BackendTaxSummary summary = data.getTaxSummary(year);
        assertEquals(0, incomeThreshold.compareTo(summary.getTotalIncome()));
        assertTrue(summary.isExceedsIncomeThreshold(), "income exactly at the threshold must count as exceeding it");
        assertTrue(summary.isObligationToDeclare());

        BackendTaxConfig updated = data.updateTaxConfig(year, uvt, 1400, 1400, 1400, 4500, BigDecimal.ZERO);
        assertEquals(0, uvt.compareTo(updated.getUvtValue()));
    }

    private DataManager freshDataManager() {
        System.setProperty("finanzas.data.dir", tempDir.toString());
        DataManager.resetForTests();
        return DataManager.getInstance();
    }

    private File createTestPng() throws Exception {
        BufferedImage image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 320, 240);
        graphics.setColor(Color.WHITE);
        graphics.fillOval(90, 50, 120, 120);
        graphics.dispose();
        File file = tempDir.resolve("avatar-source.png").toFile();
        ImageIO.write(image, "png", file);
        return file;
    }
}
