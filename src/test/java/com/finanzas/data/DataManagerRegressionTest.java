package com.finanzas.data;

import com.finanzas.model.GastoHogar;
import com.finanzas.model.FinancialCategory;
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
