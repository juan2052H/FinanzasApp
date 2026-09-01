package com.finanzas.data;

import com.finanzas.api.BackendConfig;
import com.finanzas.api.BackendAvatar;
import com.finanzas.api.BackendApiException;
import com.finanzas.api.BackendBudget;
import com.finanzas.api.BackendCategory;
import com.finanzas.api.BackendExpenseSplit;
import com.finanzas.api.BackendInvitation;
import com.finanzas.api.BackendMember;
import com.finanzas.api.BackendNotification;
import com.finanzas.api.BackendRecurringTransaction;
import com.finanzas.api.BackendSettlement;
import com.finanzas.api.BackendSavingsGoal;
import com.finanzas.api.BackendSavingsSummary;
import com.finanzas.api.BackendSharedExpense;
import com.finanzas.api.BackendSession;
import com.finanzas.api.BackendTransaction;
import com.finanzas.api.BackendUser;
import com.finanzas.api.BackendWorkspace;
import com.finanzas.api.FinanzasApiClient;
import com.finanzas.api.GoogleAuthorizationResult;
import com.finanzas.api.GoogleOAuthDesktopFlow;
import com.finanzas.model.GastoHogar;
import com.finanzas.model.FinancialCategory;
import com.finanzas.model.MetaAhorro;
import com.finanzas.model.Money;
import com.finanzas.model.Presupuesto;
import com.finanzas.model.RecurringTransaction;
import com.finanzas.model.Settlement;
import com.finanzas.model.Transaccion;
import com.finanzas.model.Transaccion.Tipo;
import com.finanzas.model.Usuario;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataManager {
    private static final Logger LOGGER = Logger.getLogger(DataManager.class.getName());
    private static final String DEMO_EMAIL = "samuel@finanzasapp.com";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_LOCK_SECONDS = 300L;

    private static DataManager instance;

    public static class UserProfile implements Serializable {
        private static final long serialVersionUID = 1L;

        public Usuario usuario = new Usuario();
        public String password;
        public List<Transaccion> transacciones = new ArrayList<Transaccion>();
        public List<MetaAhorro> metas = new ArrayList<MetaAhorro>();
        public List<Presupuesto> presupuestos = new ArrayList<Presupuesto>();
        public List<GastoHogar> gastosHogar = new ArrayList<GastoHogar>();
        public List<String> miembrosHogar = new ArrayList<String>();
        public List<RecurringTransaction> recurringTransactions = new ArrayList<RecurringTransaction>();
        public List<Settlement> settlements = new ArrayList<Settlement>();
        public List<FinancialCategory> categories = new ArrayList<FinancialCategory>();
        public transient List<NotificationItem> backendNotifications = new ArrayList<NotificationItem>();
        public transient Map<String, String> backendMemberIdsByName = new LinkedHashMap<String, String>();
        public transient List<BackendInvitation> backendReceivedInvitations = new ArrayList<BackendInvitation>();
        public transient List<BackendInvitation> backendWorkspaceInvitations = new ArrayList<BackendInvitation>();
        public transient BackendSavingsSummary backendSavingsSummary;
        public transient BackendSavingsSummary backendMonthlySavingsSummary;
        public String selectedBackendWorkspaceId = "";
        public boolean onboardingCompleted;
        public BigDecimal estimatedMonthlyIncome = Money.ZERO;
        public String primaryGoal = "";
        public Boolean savingsRecommendationEnabled = Boolean.TRUE;
        public String savingsRecommendationMode = "Equilibrado";
        public BigDecimal savingsRecommendationPercent = new BigDecimal("20.00");
        public BigDecimal savingsRecommendationFixedAmount = Money.ZERO;

        public UserProfile(String nombreCompleto, String email, String password, String moneda, String tipoCuenta) {
            String safeName = nombreCompleto == null ? "" : nombreCompleto.trim();
            String[] partes = safeName.split("\\s+", 2);
            usuario.setNombre(partes.length > 0 ? partes[0] : safeName);
            usuario.setApellido(partes.length > 1 ? partes[1] : "");
            usuario.setEmail(email);
            usuario.setMoneda(moneda == null || moneda.trim().isEmpty() ? "COP" : moneda);
            usuario.setTipoCuenta(tipoCuenta == null || tipoCuenta.trim().isEmpty() ? "Personal" : tipoCuenta);
            this.password = password;
        }
    }

    private final Map<String, UserProfile> profiles = new HashMap<String, UserProfile>();
    private final List<GastoHogar> legacyGastosHogar = new ArrayList<GastoHogar>();
    private final List<String> legacyMiembrosHogar = new ArrayList<String>();
    private final List<Runnable> listeners = new ArrayList<Runnable>();
    private final Map<String, LoginAttempt> loginAttempts = new HashMap<String, LoginAttempt>();
    private final InsightProvider insightProvider = new RuleBasedInsightProvider();
    private final FinanzasApiClient apiClient = new FinanzasApiClient(BackendConfig.baseUrl());

    private String currentUser;
    private String lastErrorMessage = "";
    private BackendSession backendSession;
    private final Object backendRefreshLock = new Object();

    private static final class LoginAttempt {
        private int failures;
        private Instant lockedUntil;
    }

    private interface BackendCall<T> {
        T execute(String accessToken) throws IOException, InterruptedException;
    }

    private interface BackendVoidCall {
        void execute(String accessToken) throws IOException, InterruptedException;
    }

    private DataManager() {
        if (!loadState() && isDemoMode()) {
            initDemoData();
            saveState();
        }
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    static void resetForTests() {
        instance = null;
    }

    public void addListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void notifyListeners() {
        saveState();
        for (Runnable listener : new ArrayList<Runnable>(listeners)) {
            listener.run();
        }
    }

    public boolean login(String email, String password) {
        if (BackendConfig.isEnabled()) {
            return loginWithBackend(email, password);
        }
        String normalizedEmail = AuthService.normalizeEmail(email);
        if (isLoginLocked(normalizedEmail)) {
            lastErrorMessage = "Demasiados intentos fallidos. Intenta nuevamente en unos minutos.";
            return false;
        }
        if (AuthService.login(profiles, normalizedEmail, password)) {
            currentUser = normalizedEmail;
            loginAttempts.remove(normalizedEmail);
            lastErrorMessage = "";
            notifyListeners();
            return true;
        }
        recordFailedLogin(normalizedEmail);
        return false;
    }

    public boolean register(String nombreCompleto, String email, String password, String moneda, String tipoCuenta) {
        if (BackendConfig.isEnabled()) {
            return registerWithBackend(nombreCompleto, email, password, moneda, tipoCuenta);
        }
        boolean registered = AuthService.register(profiles, nombreCompleto, email, password, moneda, tipoCuenta);
        if (registered) {
            String normalizedEmail = AuthService.normalizeEmail(email);
            ensureProfileInitialized(normalizedEmail, profiles.get(normalizedEmail));
            notifyListeners();
        }
        return registered;
    }

    public boolean changePassword(String currentPassword, String newPassword) {
        if (p() == null) {
            return false;
        }
        boolean changed = AuthService.changePassword(p(), currentPassword, newPassword);
        if (changed) {
            notifyListeners();
        }
        return changed;
    }

    public void updateProfile(String nombre, String apellido, String email, String ciudad, String pais) {
        UserProfile profile = p();
        if (profile == null) {
            throw new IllegalStateException("No hay una sesion activa.");
        }

        String normalizedEmail = AuthService.normalizeEmail(email);
        if (!AuthService.isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Ingresa un correo electronico valido.");
        }
        if (!currentUser.equals(normalizedEmail) && profiles.containsKey(normalizedEmail)) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo.");
        }

        String previousKey = currentUser;
        String previousName = profile.usuario.getNombreCompleto();
        profile.usuario.setNombre(safeTrim(nombre));
        profile.usuario.setApellido(safeTrim(apellido));
        profile.usuario.setEmail(normalizedEmail);
        profile.usuario.setCiudad(safeTrim(ciudad));
        profile.usuario.setPais(safeTrim(pais));

        if (!previousKey.equals(normalizedEmail)) {
            profiles.remove(previousKey);
            profiles.put(normalizedEmail, profile);
            currentUser = normalizedEmail;
        }

        replaceHouseholdMemberName(profile, previousName, profile.usuario.getNombreCompleto());
        notifyListeners();
    }

    public List<String> getAllUsers() {
        return AuthService.allUsers(profiles);
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public boolean isBackendSessionActive() {
        return backendSession != null;
    }

    public String getBackendAccessToken() {
        return backendSession == null ? "" : backendSession.getAccessToken();
    }

    public List<BackendWorkspace> getBackendWorkspaces() {
        return backendSession == null ? new ArrayList<BackendWorkspace>() : backendSession.getWorkspaces();
    }

    public BackendWorkspace getActiveBackendWorkspace() {
        String selectedWorkspaceId = activeBackendWorkspaceId();
        if (selectedWorkspaceId.isEmpty() || backendSession == null) {
            return null;
        }
        for (BackendWorkspace workspace : backendSession.getWorkspaces()) {
            if (selectedWorkspaceId.equals(workspace.getId())) {
                return workspace;
            }
        }
        return null;
    }

    public boolean canManageActiveBackendWorkspace() {
        BackendWorkspace workspace = getActiveBackendWorkspace();
        if (workspace == null) {
            return false;
        }
        return "OWNER".equalsIgnoreCase(workspace.getRole()) || "ADMIN".equalsIgnoreCase(workspace.getRole());
    }

    public boolean selectBackendWorkspace(String workspaceId) {
        UserProfile profile = p();
        if (profile == null || backendSession == null) {
            lastErrorMessage = "No hay una sesion backend activa.";
            return false;
        }
        BackendWorkspace selected = findBackendWorkspace(workspaceId);
        if (selected == null) {
            lastErrorMessage = "El workspace seleccionado no esta disponible para esta cuenta.";
            return false;
        }
        String previousWorkspaceId = profile.selectedBackendWorkspaceId;
        profile.selectedBackendWorkspaceId = selected.getId();
        try {
            syncBackendSnapshot();
            notifyListeners();
            lastErrorMessage = "";
            return true;
        } catch (Exception ex) {
            profile.selectedBackendWorkspaceId = previousWorkspaceId;
            handleBackendMutationError("No fue posible cambiar el workspace activo.", ex);
            return false;
        }
    }

    public boolean createBackendWorkspace(String nombre, String tipo) {
        if (backendSession == null) {
            lastErrorMessage = "No hay una sesion backend activa.";
            return false;
        }
        try {
            BackendWorkspace created = callBackend(token -> apiClient.createWorkspace(
                    token,
                    nombre == null ? "" : nombre.trim(),
                    tipo == null || tipo.trim().isEmpty() ? "PERSONAL" : tipo.trim()));
            reloadBackendWorkspaces();
            UserProfile profile = p();
            if (profile != null && created != null && !created.getId().isEmpty()) {
                profile.selectedBackendWorkspaceId = created.getId();
            }
            syncBackendSnapshot();
            notifyListeners();
            lastErrorMessage = "";
            return true;
        } catch (Exception ex) {
            handleBackendMutationError("No fue posible crear el workspace.", ex);
            return false;
        }
    }

    public boolean refreshBackendData() {
        if (backendSession == null) {
            return true;
        }
        try {
            reloadBackendWorkspaces();
            syncBackendSnapshot();
            notifyListeners();
            return true;
        } catch (Exception ex) {
            lastErrorMessage = "No fue posible sincronizar datos financieros del backend.";
            LOGGER.log(Level.WARNING, "Sincronizacion backend fallida.", ex);
            return false;
        }
    }

    public File exportBackendReportCsv(File destination, ReportSnapshot snapshot) throws IOException {
        if (!hasBackendFinancialSession()) {
            throw new IOException("No hay una sesion backend activa.");
        }
        File target = ensureExtension(destination, ".csv");
        try {
            LocalDate from = snapshot == null || snapshot.getPeriod() == null ? null : snapshot.getPeriod().getStartDate();
            LocalDate to = snapshot == null || snapshot.getPeriod() == null ? null : snapshot.getPeriod().getEndDate();
            byte[] csv = callBackend(token -> apiClient.exportReportCsv(token, activeBackendWorkspaceId(), from, to));
            Files.write(target.toPath(), csv);
            lastErrorMessage = "";
            return target;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Exportacion interrumpida.", ex);
        }
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void logout() {
        revokeBackendSession();
        backendSession = null;
        currentUser = null;
        notifyListeners();
    }

    private boolean loginWithBackend(String email, String password) {
        try {
            BackendSession session = apiClient.login(email, password);
            applyBackendSession(session);
            lastErrorMessage = "";
            try {
                syncBackendSnapshot();
            } catch (Exception syncError) {
                lastErrorMessage = "Sesion iniciada, pero no fue posible sincronizar datos financieros del backend.";
                LOGGER.log(Level.WARNING, "Sincronizacion backend fallida.", syncError);
            }
            notifyListeners();
            return true;
        } catch (Exception ex) {
            lastErrorMessage = ex.getMessage() == null ? "No fue posible iniciar sesion con el backend." : ex.getMessage();
            LOGGER.log(Level.WARNING, "Login backend fallido.", ex);
            return false;
        }
    }

    private boolean registerWithBackend(String nombreCompleto, String email, String password, String moneda, String tipoCuenta) {
        try {
            BackendSession session = apiClient.register(nombreCompleto, email, password, moneda, tipoCuenta);
            apiClient.logout(session.getRefreshToken());
            lastErrorMessage = "";
            return true;
        } catch (Exception ex) {
            lastErrorMessage = ex.getMessage() == null ? "No fue posible registrar la cuenta en el backend." : ex.getMessage();
            LOGGER.log(Level.WARNING, "Registro backend fallido.", ex);
            return false;
        }
    }

    public boolean loginWithGoogle(String googleClientId) {
        if (!BackendConfig.isEnabled()) {
            lastErrorMessage = "Activa FINANZAS_API_ENABLED=true para usar Google Sign-In.";
            return false;
        }
        try {
            GoogleAuthorizationResult authorization = new GoogleOAuthDesktopFlow().authorize(googleClientId);
            BackendSession session = apiClient.loginWithGoogle(
                    authorization.getAuthorizationCode(),
                    authorization.getCodeVerifier(),
                    authorization.getRedirectUri());
            applyBackendSession(session);
            lastErrorMessage = "";
            try {
                syncBackendSnapshot();
            } catch (Exception syncError) {
                lastErrorMessage = "Sesion iniciada, pero no fue posible sincronizar datos financieros del backend.";
                LOGGER.log(Level.WARNING, "Sincronizacion backend fallida.", syncError);
            }
            notifyListeners();
            return true;
        } catch (Exception ex) {
            lastErrorMessage = ex.getMessage() == null ? "No fue posible iniciar sesion con Google." : ex.getMessage();
            LOGGER.log(Level.WARNING, "Google Sign-In fallido.", ex);
            return false;
        }
    }

    private void applyBackendSession(BackendSession session) {
        if (session == null || session.getUser() == null) {
            throw new IllegalArgumentException("El backend no devolvio una sesion valida.");
        }
        BackendUser user = session.getUser();
        String normalizedEmail = AuthService.normalizeEmail(user.getEmail());
        UserProfile profile = profiles.get(normalizedEmail);
        if (profile == null) {
            profile = new UserProfile(user.getNombre() + " " + user.getApellido(), normalizedEmail, "", user.getMoneda(), mapBackendAccountType(user.getTipoCuenta()));
            profiles.put(normalizedEmail, profile);
        }
        profile.usuario.setNombre(user.getNombre());
        profile.usuario.setApellido(user.getApellido());
        profile.usuario.setEmail(normalizedEmail);
        profile.usuario.setMoneda(user.getMoneda());
        profile.usuario.setTipoCuenta(mapBackendAccountType(user.getTipoCuenta()));
        profile.selectedBackendWorkspaceId = selectedBackendWorkspaceId(profile.selectedBackendWorkspaceId, session.getWorkspaces());
        if (user.getAvatarRef().isEmpty()) {
            profile.usuario.setProfileImagePath("");
        } else {
            String cachedAvatar = cacheBackendAvatar(session.getAccessToken(), user.getId());
            if (!cachedAvatar.isEmpty()) {
                profile.usuario.setProfileImagePath(cachedAvatar);
            }
        }
        ensureProfileInitialized(normalizedEmail, profile);
        currentUser = normalizedEmail;
        backendSession = session;
    }

    private void revokeBackendSession() {
        if (backendSession == null || backendSession.getRefreshToken() == null || backendSession.getRefreshToken().trim().isEmpty()) {
            return;
        }
        try {
            apiClient.logout(backendSession.getRefreshToken());
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "No fue posible revocar la sesion backend.", ex);
        }
    }

    private <T> T callBackend(BackendCall<T> call) throws IOException, InterruptedException {
        if (backendSession == null) {
            throw new IOException("No hay una sesion backend activa.");
        }
        String accessToken = backendSession.getAccessToken();
        try {
            return call.execute(accessToken);
        } catch (BackendApiException ex) {
            if (ex.getStatusCode() != 401 || !refreshBackendSession(accessToken)) {
                throw ex;
            }
            return call.execute(backendSession.getAccessToken());
        }
    }

    private void runBackend(BackendVoidCall call) throws IOException, InterruptedException {
        callBackend(token -> {
            call.execute(token);
            return null;
        });
    }

    private boolean refreshBackendSession(String failedAccessToken) throws IOException, InterruptedException {
        synchronized (backendRefreshLock) {
            if (backendSession == null || backendSession.getRefreshToken() == null || backendSession.getRefreshToken().trim().isEmpty()) {
                return false;
            }
            if (failedAccessToken != null && !failedAccessToken.equals(backendSession.getAccessToken())) {
                return true;
            }
            try {
                BackendSession refreshed = apiClient.refresh(backendSession.getRefreshToken());
                applyBackendSession(refreshed);
                return true;
            } catch (BackendApiException ex) {
                backendSession = null;
                throw ex;
            }
        }
    }

    private void reloadBackendWorkspaces() throws IOException, InterruptedException {
        if (backendSession == null) {
            return;
        }
        List<BackendWorkspace> workspaces = callBackend(token -> apiClient.listWorkspaces(token));
        BackendSession currentSession = backendSession;
        backendSession = new BackendSession(
                currentSession.getAccessToken(),
                currentSession.getRefreshToken(),
                currentSession.getUser(),
                workspaces);
        UserProfile profile = p();
        if (profile != null) {
            profile.selectedBackendWorkspaceId = selectedBackendWorkspaceId(profile.selectedBackendWorkspaceId, workspaces);
        }
    }

    private BackendWorkspace findBackendWorkspace(String workspaceId) {
        if (backendSession == null || workspaceId == null) {
            return null;
        }
        for (BackendWorkspace workspace : backendSession.getWorkspaces()) {
            if (workspaceId.equals(workspace.getId())) {
                return workspace;
            }
        }
        return null;
    }

    private String selectedBackendWorkspaceId(String requestedWorkspaceId, List<BackendWorkspace> workspaces) {
        if (workspaces == null || workspaces.isEmpty()) {
            return "";
        }
        String requested = requestedWorkspaceId == null ? "" : requestedWorkspaceId.trim();
        if (!requested.isEmpty()) {
            for (BackendWorkspace workspace : workspaces) {
                if (requested.equals(workspace.getId())) {
                    return requested;
                }
            }
        }
        for (BackendWorkspace workspace : workspaces) {
            if (workspace.getId() != null && !workspace.getId().trim().isEmpty()) {
                return workspace.getId();
            }
        }
        return "";
    }

    private String mapBackendAccountType(String tipoCuenta) {
        if ("HOUSEHOLD".equalsIgnoreCase(tipoCuenta)) {
            return "Hogar";
        }
        if ("BUSINESS".equalsIgnoreCase(tipoCuenta)) {
            return "Negocio";
        }
        return "Personal";
    }

    private void syncBackendSnapshot() throws IOException, InterruptedException {
        UserProfile profile = p();
        String workspaceId = activeBackendWorkspaceId();
        if (profile == null || backendSession == null || workspaceId.isEmpty()) {
            return;
        }

        List<BackendCategory> remoteCategories = callBackend(token -> apiClient.listCategories(token, workspaceId));
        Map<String, String> categoryNameById = new HashMap<String, String>();
        Map<String, String> categoryColorById = new HashMap<String, String>();
        profile.categories.clear();
        for (BackendCategory remote : remoteCategories) {
            FinancialCategory category = new FinancialCategory(
                    kindFromBackendCategoryType(remote.getType()),
                    remote.getNombre(),
                    remote.getIcono(),
                    remote.getColor());
            category.setBackendId(remote.getId());
            category.setBackendWorkspaceId(remote.getWorkspaceId());
            if (remote.isArchived()) {
                category.archive();
            }
            profile.categories.add(category);
            categoryNameById.put(remote.getId(), remote.getNombre());
            categoryColorById.put(remote.getId(), remote.getColor());
        }
        ensureDefaultCategories(profile);

        List<BackendTransaction> remoteTransactions = callBackend(token -> apiClient.listTransactions(token, workspaceId));
        profile.transacciones.clear();
        for (BackendTransaction remote : remoteTransactions) {
            Tipo tipo = kindFromBackendTransactionType(remote.getType());
            String categoryName = categoryNameById.get(remote.getCategoryId());
            if (categoryName == null || categoryName.trim().isEmpty()) {
                categoryName = defaultCategoryName(tipo);
            }
            Transaccion transaction = new Transaccion(
                    tipo,
                    categoryName,
                    remote.getDescription(),
                    remote.getAmount(),
                    remote.getDate());
            transaction.setBackendId(remote.getId());
            transaction.setBackendCategoryId(remote.getCategoryId());
            profile.transacciones.add(transaction);
        }

        YearMonth currentMonth = YearMonth.now();
        profile.presupuestos.clear();
        for (BackendBudget remote : callBackend(token -> apiClient.listBudgets(token, workspaceId))) {
            if (!YearMonth.from(remote.getPeriodMonth()).equals(currentMonth)) {
                continue;
            }
            String categoryName = categoryNameById.get(remote.getCategoryId());
            if (categoryName == null || categoryName.trim().isEmpty()) {
                categoryName = defaultCategoryName(Tipo.GASTO);
            }
            Presupuesto budget = new Presupuesto(
                    categoryName,
                    remote.getAmount(),
                    BigDecimal.ZERO,
                    categoryColorById.getOrDefault(remote.getCategoryId(), "#1a73e8"));
            budget.setBackendId(remote.getId());
            budget.setBackendCategoryId(remote.getCategoryId());
            budget.setBackendPeriodMonth(remote.getPeriodMonth().toString());
            profile.presupuestos.add(budget);
        }

        profile.metas.clear();
        for (BackendSavingsGoal remote : callBackend(token -> apiClient.listSavingsGoals(token, workspaceId))) {
            MetaAhorro goal = new MetaAhorro(
                    remote.getName(),
                    remote.getIcono(),
                    remote.getCurrentAmount(),
                    remote.getTargetAmount(),
                    remote.getColor(),
                    remote.getDueDate());
            goal.setBackendId(remote.getId());
            goal.setBackendWorkspaceId(remote.getWorkspaceId());
            profile.metas.add(goal);
        }
        profile.backendSavingsSummary = callBackend(token -> apiClient.getSavingsSummary(token, workspaceId));
        profile.backendMonthlySavingsSummary = callBackend(token -> apiClient.getSavingsSummary(
                token,
                workspaceId,
                currentMonth.atDay(1),
                currentMonth.atEndOfMonth()));

        profile.recurringTransactions.clear();
        for (BackendRecurringTransaction remote : callBackend(token -> apiClient.listRecurringTransactions(token, workspaceId))) {
            Tipo tipo = kindFromBackendTransactionType(remote.getType());
            String categoryName = categoryNameById.get(remote.getCategoryId());
            if (categoryName == null || categoryName.trim().isEmpty()) {
                categoryName = defaultCategoryName(tipo);
            }
            RecurringTransaction recurring = new RecurringTransaction(
                    tipo,
                    categoryName,
                    remote.getDescription(),
                    remote.getAmount(),
                    frequencyFromBackend(remote.getFrequency()),
                    remote.getCustomIntervalDays(),
                    remote.getNextRunDate());
            recurring.setActive(remote.isActive());
            recurring.setBackendId(remote.getId());
            recurring.setBackendCategoryId(remote.getCategoryId());
            profile.recurringTransactions.add(recurring);
        }

        syncBackendHousehold(profile, workspaceId, categoryNameById);

        profile.backendNotifications.clear();
        for (BackendNotification remote : callBackend(token -> apiClient.refreshNotifications(token, workspaceId))) {
            profile.backendNotifications.add(toNotificationItem(remote));
        }
        refreshBackendInvitationCaches(profile, workspaceId);
        refreshBudgetUsage(profile);
    }

    private void refreshBackendInvitationCaches(UserProfile profile, String workspaceId) {
        if (profile == null || backendSession == null) {
            return;
        }
        profile.backendReceivedInvitations.clear();
        profile.backendWorkspaceInvitations.clear();
        try {
            profile.backendReceivedInvitations.addAll(callBackend(token -> apiClient.listMyInvitations(token)));
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "No fue posible cargar invitaciones recibidas.", ex);
        }
        if (!canManageActiveBackendWorkspace() || workspaceId == null || workspaceId.trim().isEmpty()) {
            return;
        }
        try {
            profile.backendWorkspaceInvitations.addAll(callBackend(token -> apiClient.listWorkspaceInvitations(token, workspaceId)));
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "No fue posible cargar invitaciones del workspace.", ex);
        }
    }

    private void syncBackendHousehold(UserProfile profile, String workspaceId, Map<String, String> categoryNameById)
            throws IOException, InterruptedException {
        profile.backendMemberIdsByName.clear();
        profile.miembrosHogar.clear();
        for (BackendMember member : callBackend(token -> apiClient.listMembers(token, workspaceId))) {
            String name = member.getDisplayName();
            if (name != null && !name.trim().isEmpty()) {
                profile.miembrosHogar.add(name);
                profile.backendMemberIdsByName.put(name, member.getUserId());
            }
        }

        profile.gastosHogar.clear();
        for (BackendSharedExpense remote : callBackend(token -> apiClient.listSharedExpenses(token, workspaceId))) {
            String categoryName = categoryNameById.get(remote.getCategoryId());
            if (categoryName == null || categoryName.trim().isEmpty()) {
                categoryName = "Otros";
            }
            String paidBy = remote.getPaidByName() == null || remote.getPaidByName().trim().isEmpty()
                    ? memberNameById(profile, remote.getPaidByUserId())
                    : remote.getPaidByName();
            GastoHogar expense = new GastoHogar(
                    remote.getDescription(),
                    categoryName,
                    remote.getAmount(),
                    paidBy,
                    remote.getDate(),
                    true);
            expense.setBackendId(remote.getId());
            expense.setBackendCategoryId(remote.getCategoryId());
            expense.setBackendPaidByUserId(remote.getPaidByUserId());
            applyBackendSplit(expense, remote.getSplitMethod(), remote.getSplits());
            profile.gastosHogar.add(expense);
        }

        profile.settlements.clear();
        for (BackendSettlement remote : callBackend(token -> apiClient.listSettlements(token, workspaceId))) {
            Settlement settlement = new Settlement(
                    memberNameOrFallback(profile, remote.getFromUserId(), remote.getFromName()),
                    memberNameOrFallback(profile, remote.getToUserId(), remote.getToName()),
                    remote.getAmount(),
                    remote.getDate(),
                    remote.getNote());
            settlement.setBackendId(remote.getId());
            settlement.setBackendFromUserId(remote.getFromUserId());
            settlement.setBackendToUserId(remote.getToUserId());
            profile.settlements.add(settlement);
        }
    }

    private void applyBackendSplit(GastoHogar expense, String method, List<BackendExpenseSplit> splits) {
        Map<String, BigDecimal> values = new LinkedHashMap<String, BigDecimal>();
        for (BackendExpenseSplit split : splits) {
            String name = split.getDisplayName();
            if (name != null && !name.trim().isEmpty()) {
                values.put(name, "PERCENTAGE".equalsIgnoreCase(method) ? split.getPercentage() : split.getAmount());
            }
        }
        if (values.isEmpty()) {
            expense.setDividido(false);
            return;
        }
        if ("PERCENTAGE".equalsIgnoreCase(method)) {
            expense.definePercentageSplit(values);
        } else if ("EQUAL".equalsIgnoreCase(method)) {
            expense.defineEqualSplit(new ArrayList<String>(values.keySet()));
        } else {
            expense.defineCustomAmountSplit(values);
        }
    }

    private String memberNameById(UserProfile profile, String userId) {
        for (Map.Entry<String, String> entry : profile.backendMemberIdsByName.entrySet()) {
            if (entry.getValue().equals(userId)) {
                return entry.getKey();
            }
        }
        return "";
    }

    private String memberNameOrFallback(UserProfile profile, String userId, String fallback) {
        String name = memberNameById(profile, userId);
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return fallback == null || fallback.trim().isEmpty() ? "Miembro" : fallback.trim();
    }

    private String backendMemberIdForName(String name) throws IOException {
        UserProfile profile = p();
        if (profile == null || profile.backendMemberIdsByName == null) {
            throw new IOException("No hay miembros backend cargados.");
        }
        String userId = profile.backendMemberIdsByName.get(name);
        if (userId == null || userId.trim().isEmpty()) {
            throw new IOException("El miembro no pertenece al workspace backend: " + name);
        }
        return userId;
    }

    private String backendSplitMethod(GastoHogar gastoHogar) {
        if (gastoHogar == null || !gastoHogar.isDividido()) {
            return "CUSTOM_AMOUNT";
        }
        return gastoHogar.getSplitMethod().name();
    }

    private List<Map<String, Object>> backendSplitParticipants(GastoHogar gastoHogar, String paidByUserId) throws IOException {
        List<Map<String, Object>> participants = new ArrayList<Map<String, Object>>();
        if (gastoHogar == null) {
            return participants;
        }
        if (!gastoHogar.isDividido()) {
            Map<String, Object> participant = new LinkedHashMap<String, Object>();
            participant.put("userId", paidByUserId);
            participant.put("amount", gastoHogar.getMontoDecimal());
            participants.add(participant);
            return participants;
        }
        if (gastoHogar.getSplitMethod() == GastoHogar.SplitMethod.EQUAL) {
            return participants;
        }
        for (Map.Entry<String, BigDecimal> entry : gastoHogar.getSplitAmounts().entrySet()) {
            Map<String, Object> participant = new LinkedHashMap<String, Object>();
            participant.put("userId", backendMemberIdForName(entry.getKey()));
            if (gastoHogar.getSplitMethod() == GastoHogar.SplitMethod.PERCENTAGE) {
                participant.put("percentage", entry.getValue());
            } else {
                participant.put("amount", entry.getValue());
            }
            participants.add(participant);
        }
        return participants;
    }

    private String activeBackendWorkspaceId() {
        if (backendSession == null || backendSession.getWorkspaces().isEmpty()) {
            return "";
        }
        UserProfile profile = p();
        String selected = profile == null ? "" : profile.selectedBackendWorkspaceId;
        String resolved = selectedBackendWorkspaceId(selected, backendSession.getWorkspaces());
        if (profile != null && !resolved.equals(profile.selectedBackendWorkspaceId)) {
            profile.selectedBackendWorkspaceId = resolved;
        }
        return resolved;
    }

    private boolean hasBackendFinancialSession() {
        return backendSession != null && !activeBackendWorkspaceId().isEmpty();
    }

    private FinancialCategory.Kind kindFromBackendCategoryType(String type) {
        return "INCOME".equalsIgnoreCase(type) ? FinancialCategory.Kind.INCOME : FinancialCategory.Kind.EXPENSE;
    }

    private Tipo kindFromBackendTransactionType(String type) {
        return "INCOME".equalsIgnoreCase(type) ? Tipo.INGRESO : Tipo.GASTO;
    }

    private String backendCategoryType(FinancialCategory.Kind kind) {
        return kind == FinancialCategory.Kind.INCOME ? "INCOME" : "EXPENSE";
    }

    private String backendTransactionType(Tipo tipo) {
        return tipo == Tipo.INGRESO ? "INCOME" : "EXPENSE";
    }

    private RecurringTransaction.Frequency frequencyFromBackend(String frequency) {
        try {
            return RecurringTransaction.Frequency.valueOf(frequency == null ? "MONTHLY" : frequency);
        } catch (IllegalArgumentException ex) {
            return RecurringTransaction.Frequency.MONTHLY;
        }
    }

    private String backendFrequency(RecurringTransaction.Frequency frequency) {
        return frequency == null ? "MONTHLY" : frequency.name();
    }

    private String defaultCategoryName(Tipo tipo) {
        return tipo == Tipo.INGRESO ? "Otros" : "Otros";
    }

    private String ensureBackendCategoryId(Tipo tipo, String categoryName) throws IOException, InterruptedException {
        UserProfile profile = p();
        if (profile == null) {
            return "";
        }
        FinancialCategory.Kind kind = tipo == Tipo.INGRESO ? FinancialCategory.Kind.INCOME : FinancialCategory.Kind.EXPENSE;
        String safeName = categoryName == null || categoryName.trim().isEmpty() ? defaultCategoryName(tipo) : categoryName.trim();
        ensureDefaultCategories(profile);
        FinancialCategory category = findCategory(profile, kind, safeName);
        if (category != null && !category.getBackendId().isEmpty()) {
            return category.getBackendId();
        }

        String icon = category == null ? "OTR" : category.getIcon();
        String color = category == null ? "#64748b" : category.getColor();
        BackendCategory created = callBackend(token -> apiClient.createCategory(
                token,
                activeBackendWorkspaceId(),
                safeName,
                backendCategoryType(kind),
                icon,
                color));
        if (category == null) {
            category = new FinancialCategory(kind, created.getNombre(), created.getIcono(), created.getColor());
            profile.categories.add(category);
        }
        category.setBackendId(created.getId());
        category.setBackendWorkspaceId(created.getWorkspaceId());
        return created.getId();
    }

    private FinancialCategory findCategoryByBackendId(UserProfile profile, String backendId) {
        if (profile == null || backendId == null || backendId.trim().isEmpty()) {
            return null;
        }
        for (FinancialCategory category : profile.categories) {
            if (backendId.equals(category.getBackendId())) {
                return category;
            }
        }
        return null;
    }

    private void handleBackendMutationError(String message, Exception ex) {
        lastErrorMessage = message;
        LOGGER.log(Level.WARNING, message, ex);
    }

    private NotificationItem toNotificationItem(BackendNotification notification) {
        return new NotificationItem(
                notification.getTitle(),
                notification.getBody(),
                notificationSeverity(notification.getType()),
                notificationSection(notification.getType()));
    }

    private NotificationItem.Severity notificationSeverity(String type) {
        if ("BUDGET_THRESHOLD".equalsIgnoreCase(type) || "RECURRING_DUE_SOON".equalsIgnoreCase(type)) {
            return NotificationItem.Severity.WARNING;
        }
        if ("GOAL_COMPLETED".equalsIgnoreCase(type)) {
            return NotificationItem.Severity.SUCCESS;
        }
        return NotificationItem.Severity.INFO;
    }

    private String notificationSection(String type) {
        if (type != null && type.toUpperCase(java.util.Locale.ROOT).contains("BUDGET")) {
            return "presupuesto";
        }
        if (type != null && type.toUpperCase(java.util.Locale.ROOT).contains("GOAL")) {
            return "metas";
        }
        if (type != null && type.toUpperCase(java.util.Locale.ROOT).contains("RECURRING")) {
            return "inicio";
        }
        return "inicio";
    }

    private void initDemoData() {
        if (profiles.containsKey(DEMO_EMAIL)) {
            return;
        }
        AuthService.register(profiles, "Samuel Calle", DEMO_EMAIL, "123456", "COP", "Hogar");
        UserProfile profile = profiles.get(DEMO_EMAIL);
        ensureProfileInitialized(DEMO_EMAIL, profile);
        profile.usuario.setCiudad("Bello");
        profile.usuario.setPais("Colombia");

        profile.metas.add(new MetaAhorro("Fondo de emergencia", "SEG", 500000, 2000000, "#1a73e8", LocalDate.now().plusMonths(5)));
        profile.metas.add(new MetaAhorro("Viaje familiar", "VIA", 1500000, 3000000, "#34a853", LocalDate.now().plusMonths(8)));

        profile.transacciones.add(new Transaccion(Tipo.INGRESO, "Salario", "Quincena abril", 2500000, LocalDate.now().minusDays(2)));
        profile.transacciones.add(new Transaccion(Tipo.GASTO, "Alimentacion", "Mercado mensual", 450000, LocalDate.now().minusDays(1)));
        profile.transacciones.add(new Transaccion(Tipo.GASTO, "Transporte", "Gasolina", 120000, LocalDate.now()));
        profile.transacciones.add(new Transaccion(Tipo.GASTO, "Servicios", "Agua y luz", 180000, LocalDate.now()));
        refreshBudgetUsage(profile);
    }

    private UserProfile p() {
        return currentUser == null ? null : profiles.get(currentUser);
    }

    public List<Transaccion> getTransacciones() {
        return p() != null ? p().transacciones : new ArrayList<Transaccion>();
    }

    public List<MetaAhorro> getMetas() {
        return p() != null ? p().metas : new ArrayList<MetaAhorro>();
    }

    public List<Presupuesto> getPresupuestos() {
        UserProfile profile = p();
        if (profile == null) {
            return new ArrayList<Presupuesto>();
        }
        refreshBudgetUsage(profile);
        return profile.presupuestos;
    }

    public List<GastoHogar> getGastosHogar() {
        return p() != null ? p().gastosHogar : new ArrayList<GastoHogar>();
    }

    public List<RecurringTransaction> getRecurringTransactions() {
        return p() != null ? p().recurringTransactions : new ArrayList<RecurringTransaction>();
    }

    public List<Settlement> getSettlements() {
        return p() != null ? p().settlements : new ArrayList<Settlement>();
    }

    public List<RecurringTransaction> getUpcomingRecurringTransactions(int daysAhead) {
        List<RecurringTransaction> upcoming = new ArrayList<RecurringTransaction>();
        if (p() == null) {
            return upcoming;
        }
        LocalDate limit = LocalDate.now().plusDays(Math.max(1, daysAhead));
        for (RecurringTransaction recurring : p().recurringTransactions) {
            if (recurring.isActive() && !recurring.getNextDate().isAfter(limit)) {
                upcoming.add(recurring);
            }
        }
        upcoming.sort((left, right) -> left.getNextDate().compareTo(right.getNextDate()));
        return upcoming;
    }

    public List<String> getMiembrosHogar() {
        return p() != null ? p().miembrosHogar : new ArrayList<String>();
    }

    public List<FinancialCategory> getCategories(FinancialCategory.Kind kind, boolean includeArchived) {
        List<FinancialCategory> result = new ArrayList<FinancialCategory>();
        UserProfile profile = p();
        if (profile == null) {
            return result;
        }
        ensureDefaultCategories(profile);
        for (FinancialCategory category : profile.categories) {
            if (category.getKind() == kind && (includeArchived || !category.isArchived())) {
                result.add(category);
            }
        }
        return result;
    }

    public String[] getCategoryNames(FinancialCategory.Kind kind) {
        List<FinancialCategory> categories = getCategories(kind, false);
        if (categories.isEmpty()) {
            return new String[]{"Otros"};
        }
        String[] names = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            names[i] = categories.get(i).getName();
        }
        return names;
    }

    public void addCategory(FinancialCategory.Kind kind, String name, String icon, String color) {
        UserProfile profile = p();
        if (profile == null) {
            return;
        }
        if (hasBackendFinancialSession() && kind != FinancialCategory.Kind.HOUSEHOLD) {
            try {
                callBackend(token -> apiClient.createCategory(
                        token,
                        activeBackendWorkspaceId(),
                        name,
                        backendCategoryType(kind),
                        icon,
                        color));
                syncBackendSnapshot();
                notifyListeners();
            } catch (Exception ex) {
                handleBackendMutationError("No fue posible crear la categoria en el backend.", ex);
                throw new IllegalStateException(lastErrorMessage, ex);
            }
            return;
        }
        ensureDefaultCategories(profile);
        FinancialCategory existing = findCategory(profile, kind, name);
        if (existing != null) {
            existing.update(name, icon, color);
            existing.restore();
        } else {
            profile.categories.add(new FinancialCategory(kind, name, icon, color));
        }
        notifyListeners();
    }

    public void updateCategory(FinancialCategory category, String name, String icon, String color) {
        if (category == null || p() == null) {
            return;
        }
        if (hasBackendFinancialSession() && category.getKind() != FinancialCategory.Kind.HOUSEHOLD) {
            if (category.getBackendId().isEmpty()) {
                addCategory(category.getKind(), name, icon, color);
                return;
            }
            try {
                callBackend(token -> apiClient.updateCategory(
                        token,
                        activeBackendWorkspaceId(),
                        category.getBackendId(),
                        name,
                        backendCategoryType(category.getKind()),
                        icon,
                        color));
                syncBackendSnapshot();
                notifyListeners();
            } catch (Exception ex) {
                handleBackendMutationError("No fue posible editar la categoria en el backend.", ex);
                throw new IllegalStateException(lastErrorMessage, ex);
            }
            return;
        }
        FinancialCategory duplicate = findCategory(p(), category.getKind(), name);
        if (duplicate != null && duplicate != category) {
            throw new IllegalArgumentException("Ya existe una categoria con ese nombre.");
        }
        category.update(name, icon, color);
        notifyListeners();
    }

    public void archiveCategory(FinancialCategory category) {
        if (category == null || p() == null) {
            return;
        }
        if (hasBackendFinancialSession() && category.getKind() != FinancialCategory.Kind.HOUSEHOLD && !category.getBackendId().isEmpty()) {
            try {
                runBackend(token -> apiClient.archiveCategory(token, activeBackendWorkspaceId(), category.getBackendId()));
                syncBackendSnapshot();
                notifyListeners();
            } catch (Exception ex) {
                handleBackendMutationError("No fue posible archivar la categoria en el backend.", ex);
                throw new IllegalStateException(lastErrorMessage, ex);
            }
            return;
        }
        category.archive();
        notifyListeners();
    }

    public Usuario getUsuario() {
        return p() != null ? p().usuario : new Usuario();
    }

    public boolean needsOnboarding() {
        return p() != null && !p().onboardingCompleted;
    }

    public void completeOnboarding(String accountType, String currency, BigDecimal estimatedMonthlyIncome,
                                   String primaryGoal, String firstBudgetCategory, BigDecimal firstBudgetAmount) {
        UserProfile profile = p();
        if (profile == null) {
            return;
        }
        if (accountType != null && !accountType.trim().isEmpty()) {
            profile.usuario.setTipoCuenta(accountType.trim());
        }
        if (currency != null && !currency.trim().isEmpty()) {
            profile.usuario.setMoneda(currency.trim());
        }
        profile.estimatedMonthlyIncome = Money.normalize(estimatedMonthlyIncome);
        profile.primaryGoal = primaryGoal == null ? "" : primaryGoal.trim();
        if (firstBudgetCategory != null
                && !firstBudgetCategory.trim().isEmpty()
                && firstBudgetAmount != null
                && Money.normalize(firstBudgetAmount).compareTo(BigDecimal.ZERO) > 0) {
            profile.presupuestos.add(new Presupuesto(firstBudgetCategory.trim(), Money.normalize(firstBudgetAmount), BigDecimal.ZERO, "#1a73e8"));
        }
        profile.onboardingCompleted = true;
        ensureProfileInitialized(currentUser, profile);
        notifyListeners();
    }

    public boolean updateProfileImage(File sourceFile) {
        if (p() == null || sourceFile == null) {
            lastErrorMessage = "No hay una sesion activa o no se selecciono archivo.";
            return false;
        }
        try {
            if (backendSession != null) {
                BackendUser updatedUser = callBackend(token -> apiClient.uploadAvatar(token, sourceFile));
                if (backendSession != null) {
                    backendSession = new BackendSession(
                            backendSession.getAccessToken(),
                            backendSession.getRefreshToken(),
                            updatedUser,
                            backendSession.getWorkspaces());
                }
                String cachedAvatar = cacheBackendAvatar(backendSession == null ? "" : backendSession.getAccessToken(), updatedUser.getId());
                p().usuario.setProfileImagePath(cachedAvatar);
                lastErrorMessage = "";
                notifyListeners();
                return true;
            }
            String storedPath = PersistenceService.storeProfileImage(currentUser, sourceFile);
            p().usuario.setProfileImagePath(storedPath);
            lastErrorMessage = "";
            notifyListeners();
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            lastErrorMessage = "Carga de avatar interrumpida.";
            LOGGER.log(Level.WARNING, "Carga de avatar interrumpida.", ex);
            return false;
        } catch (IOException ex) {
            lastErrorMessage = ex.getMessage();
            LOGGER.log(Level.WARNING, "No fue posible almacenar la imagen de perfil.", ex);
            return false;
        }
    }

    public void clearProfileImage() {
        if (p() == null) {
            return;
        }
        if (backendSession != null) {
            try {
                runBackend(token -> apiClient.deleteAvatar(token));
            } catch (Exception ex) {
                handleBackendMutationError("No fue posible eliminar el avatar en el backend.", ex);
                return;
            }
        }
        try {
            PersistenceService.deleteProfileImage(p().usuario.getProfileImagePath());
            lastErrorMessage = "";
        } catch (IOException ex) {
            lastErrorMessage = ex.getMessage();
            LOGGER.log(Level.WARNING, "No fue posible eliminar la imagen de perfil.", ex);
        }
        p().usuario.setProfileImagePath("");
        notifyListeners();
    }

    private String cacheBackendAvatar(String accessToken, String userId) {
        if (accessToken == null || accessToken.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            return "";
        }
        try {
            BackendAvatar avatar = apiClient.downloadAvatar(accessToken, "");
            if (avatar.isNotModified()) {
                return "";
            }
            return PersistenceService.storeRemoteProfileImage(userId, avatar.getContent(), avatar.getEtag());
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "No fue posible descargar/cachear el avatar remoto.", ex);
            return "";
        }
    }

    public boolean createBackup(File destination) {
        try {
            PersistenceService.createBackup(destination, new PersistenceService.AppState(profiles, currentUser, legacyGastosHogar, legacyMiembrosHogar));
            lastErrorMessage = "";
            return true;
        } catch (IOException ex) {
            lastErrorMessage = ex.getMessage();
            LOGGER.log(Level.WARNING, "No fue posible crear el respaldo.", ex);
            return false;
        }
    }

    public boolean restoreBackup(File source) {
        try {
            PersistenceService.AppState state = PersistenceService.restoreBackup(source);
            if (state == null) {
                lastErrorMessage = "El respaldo no contiene un estado valido.";
                return false;
            }
            profiles.clear();
            profiles.putAll(state.profiles);
            legacyGastosHogar.clear();
            if (state.gastosHogar != null) {
                legacyGastosHogar.addAll(state.gastosHogar);
            }
            legacyMiembrosHogar.clear();
            if (state.miembrosHogar != null) {
                legacyMiembrosHogar.addAll(state.miembrosHogar);
            }
            initializeLoadedProfiles(state.currentUser);
            currentUser = null;
            lastErrorMessage = "";
            notifyListeners();
            return true;
        } catch (Exception ex) {
            lastErrorMessage = ex.getMessage();
            LOGGER.log(Level.WARNING, "No fue posible restaurar el respaldo.", ex);
            return false;
        }
    }

    public void addTransaccion(Transaccion transaccion) {
        if (p() != null && transaccion != null) {
            if (hasBackendFinancialSession()) {
                try {
                    String categoryId = ensureBackendCategoryId(transaccion.getTipo(), transaccion.getCategoria());
                    callBackend(token -> apiClient.createTransaction(
                            token,
                            activeBackendWorkspaceId(),
                            categoryId,
                            backendTransactionType(transaccion.getTipo()),
                            transaccion.getDescripcion(),
                            transaccion.getMontoDecimal(),
                            transaccion.getFecha()));
                    syncBackendSnapshot();
                    notifyListeners();
                } catch (Exception ex) {
                    handleBackendMutationError("No fue posible crear la transaccion en el backend.", ex);
                }
                return;
            }
            p().transacciones.add(0, transaccion);
            refreshBudgetUsage(p());
            notifyListeners();
        }
    }

    public void removeTransaccion(Transaccion transaccion) {
        if (p() != null) {
            if (hasBackendFinancialSession() && transaccion != null && !transaccion.getBackendId().isEmpty()) {
                try {
                    runBackend(token -> apiClient.deleteTransaction(token, activeBackendWorkspaceId(), transaccion.getBackendId()));
                    syncBackendSnapshot();
                    notifyListeners();
                } catch (Exception ex) {
                    handleBackendMutationError("No fue posible eliminar la transaccion en el backend.", ex);
                }
                return;
            }
            p().transacciones.remove(transaccion);
            refreshBudgetUsage(p());
            notifyListeners();
        }
    }

    public void updateTransaccion(Transaccion anterior, Transaccion nueva) {
        if (p() == null) {
            return;
        }
        if (hasBackendFinancialSession() && nueva != null) {
            try {
                String categoryId = ensureBackendCategoryId(nueva.getTipo(), nueva.getCategoria());
                if (anterior != null && !anterior.getBackendId().isEmpty()) {
                    callBackend(token -> apiClient.updateTransaction(
                            token,
                            activeBackendWorkspaceId(),
                            anterior.getBackendId(),
                            categoryId,
                            backendTransactionType(nueva.getTipo()),
                            nueva.getDescripcion(),
                            nueva.getMontoDecimal(),
                            nueva.getFecha()));
                } else {
                    callBackend(token -> apiClient.createTransaction(
                            token,
                            activeBackendWorkspaceId(),
                            categoryId,
                            backendTransactionType(nueva.getTipo()),
                            nueva.getDescripcion(),
                            nueva.getMontoDecimal(),
                            nueva.getFecha()));
                }
                syncBackendSnapshot();
                notifyListeners();
            } catch (Exception ex) {
                handleBackendMutationError("No fue posible actualizar la transaccion en el backend.", ex);
            }
            return;
        }
        int index = p().transacciones.indexOf(anterior);
        if (index >= 0) {
            p().transacciones.set(index, nueva);
            refreshBudgetUsage(p());
            notifyListeners();
        }
    }

    public void addMeta(MetaAhorro meta) {
        if (p() != null && meta != null) {
            if (hasBackendFinancialSession()) {
                try {
                    callBackend(token -> apiClient.createSavingsGoal(
                            token,
                            activeBackendWorkspaceId(),
                            meta.getNombre(),
                            meta.getMontoActualDecimal(),
                            meta.getMontoMetaDecimal(),
                            meta.getColor(),
                            meta.getIcono(),
                            meta.getFechaLimite()));
                    syncBackendSnapshot();
                    notifyListeners();
                } catch (Exception ex) {
                    handleBackendMutationError("No fue posible crear la meta en el backend.", ex);
                }
                return;
            }
            p().metas.add(meta);
            notifyListeners();
        }
    }

    public void removeMeta(MetaAhorro meta) {
        if (p() != null) {
            if (hasBackendFinancialSession() && meta != null && !meta.getBackendId().isEmpty()) {
                try {
                    runBackend(token -> apiClient.archiveSavingsGoal(token, activeBackendWorkspaceId(), meta.getBackendId()));
                    syncBackendSnapshot();
                    notifyListeners();
                } catch (Exception ex) {
                    handleBackendMutationError("No fue posible archivar la meta en el backend.", ex);
                }
                return;
            }
            p().metas.remove(meta);
            notifyListeners();
        }
    }

    public void updateMeta(MetaAhorro meta) {
        if (p() == null || meta == null) {
            return;
        }
        if (hasBackendFinancialSession()) {
            if (meta.getBackendId().isEmpty()) {
                addMeta(meta);
                return;
            }
            try {
                callBackend(token -> apiClient.updateSavingsGoal(
                        token,
                        activeBackendWorkspaceId(),
                        meta.getBackendId(),
                        meta.getNombre(),
                        meta.getMontoActualDecimal(),
                        meta.getMontoMetaDecimal(),
                        meta.getColor(),
                        meta.getIcono(),
                        meta.getFechaLimite()));
                syncBackendSnapshot();
                notifyListeners();
            } catch (Exception ex) {
                handleBackendMutationError("No fue posible actualizar la meta en el backend.", ex);
            }
            return;
        }
        notifyListeners();
    }

    public boolean depositarMeta(MetaAhorro meta, double monto) {
        if (meta == null) {
            return false;
        }
        if (hasBackendFinancialSession() && !meta.getBackendId().isEmpty()) {
            try {
                callBackend(token -> apiClient.contributeSavingsGoal(
                        token,
                        activeBackendWorkspaceId(),
                        meta.getBackendId(),
                        Money.of(monto)));
                syncBackendSnapshot();
                notifyListeners();
                return true;
            } catch (Exception ex) {
                handleBackendMutationError("No fue posible registrar el aporte en el backend.", ex);
                return false;
            }
        }
        BigDecimal nuevoMonto = meta.getMontoActualDecimal().add(Money.of(monto));
        if (nuevoMonto.compareTo(meta.getMontoMetaDecimal()) > 0) {
            nuevoMonto = meta.getMontoMetaDecimal();
        }
        meta.setMontoActualDecimal(nuevoMonto);
        notifyListeners();
        return true;
    }

    public void addPresupuesto(Presupuesto presupuesto) {
        if (p() != null && presupuesto != null) {
            if (hasBackendFinancialSession()) {
                try {
                    String categoryId = ensureBackendCategoryId(Tipo.GASTO, presupuesto.getCategoria());
                    callBackend(token -> apiClient.createBudget(
                            token,
                            activeBackendWorkspaceId(),
                            categoryId,
                            LocalDate.now().withDayOfMonth(1),
                            presupuesto.getMontoPresupuestadoDecimal()));
                    syncBackendSnapshot();
                    notifyListeners();
                } catch (Exception ex) {
                    handleBackendMutationError("No fue posible crear el presupuesto en el backend.", ex);
                }
                return;
            }
            presupuesto.setMontoGastado(calcularGastadoPresupuesto(presupuesto));
            p().presupuestos.add(presupuesto);
            notifyListeners();
        }
    }

    public void removePresupuesto(Presupuesto presupuesto) {
        if (p() != null) {
            if (hasBackendFinancialSession() && presupuesto != null && !presupuesto.getBackendId().isEmpty()) {
                try {
                    runBackend(token -> apiClient.deleteBudget(token, activeBackendWorkspaceId(), presupuesto.getBackendId()));
                    syncBackendSnapshot();
                    notifyListeners();
                } catch (Exception ex) {
                    handleBackendMutationError("No fue posible eliminar el presupuesto en el backend.", ex);
                }
                return;
            }
            p().presupuestos.remove(presupuesto);
            notifyListeners();
        }
    }

    public void updatePresupuesto(Presupuesto presupuesto, double nuevo, double gastadoIgnorado) {
        updatePresupuesto(presupuesto, nuevo, presupuesto == null ? "#1a73e8" : presupuesto.getColor());
    }

    public void updatePresupuesto(Presupuesto presupuesto, double nuevo, String color) {
        updatePresupuesto(presupuesto, Money.of(nuevo), color);
    }

    public void updatePresupuesto(Presupuesto presupuesto, BigDecimal nuevo, String color) {
        if (presupuesto == null) {
            return;
        }
        if (hasBackendFinancialSession()) {
            try {
                String categoryId = ensureBackendCategoryId(Tipo.GASTO, presupuesto.getCategoria());
                if (presupuesto.getBackendId().isEmpty()) {
                    callBackend(token -> apiClient.createBudget(
                            token,
                            activeBackendWorkspaceId(),
                            categoryId,
                            LocalDate.now().withDayOfMonth(1),
                            nuevo));
                } else {
                    callBackend(token -> apiClient.updateBudget(
                            token,
                            activeBackendWorkspaceId(),
                            presupuesto.getBackendId(),
                            categoryId,
                            LocalDate.now().withDayOfMonth(1),
                            nuevo));
                }
                syncBackendSnapshot();
                notifyListeners();
            } catch (Exception ex) {
                handleBackendMutationError("No fue posible actualizar el presupuesto en el backend.", ex);
            }
            return;
        }
        presupuesto.setMontoPresupuestadoDecimal(nuevo);
        presupuesto.setColor(color);
        presupuesto.setMontoGastado(calcularGastadoPresupuesto(presupuesto));
        notifyListeners();
    }

    public void addGastoHogar(GastoHogar gastoHogar) {
        if (p() != null && gastoHogar != null) {
            if (hasBackendFinancialSession()) {
                try {
                    String paidByUserId = backendMemberIdForName(gastoHogar.getPagadoPor());
                    String categoryId = ensureBackendCategoryId(Tipo.GASTO, gastoHogar.getCategoria());
                    callBackend(token -> apiClient.createSharedExpense(
                            token,
                            activeBackendWorkspaceId(),
                            gastoHogar.getDescripcion(),
                            gastoHogar.getMontoDecimal(),
                            gastoHogar.getFecha(),
                            paidByUserId,
                            categoryId,
                            backendSplitMethod(gastoHogar),
                            backendSplitParticipants(gastoHogar, paidByUserId)));
                    syncBackendSnapshot();
                    notifyListeners();
                } catch (Exception ex) {
                    handleBackendMutationError("No fue posible crear el gasto compartido en el backend.", ex);
                    throw new IllegalStateException(lastErrorMessage, ex);
                }
                return;
            }
            if (gastoHogar.isDividido() && gastoHogar.getSplitAmounts().isEmpty()) {
                gastoHogar.defineEqualSplit(p().miembrosHogar);
            }
            p().gastosHogar.add(0, gastoHogar);
            notifyListeners();
        }
    }

    public void addSettlement(Settlement settlement) {
        if (p() != null && settlement != null) {
            if (hasBackendFinancialSession()) {
                try {
                    callBackend(token -> apiClient.createSettlement(
                            token,
                            activeBackendWorkspaceId(),
                            backendMemberIdForName(settlement.getFromMember()),
                            backendMemberIdForName(settlement.getToMember()),
                            settlement.getAmountDecimal(),
                            settlement.getDate(),
                            settlement.getNote()));
                    syncBackendSnapshot();
                    notifyListeners();
                } catch (Exception ex) {
                    handleBackendMutationError("No fue posible registrar la liquidacion en el backend.", ex);
                    throw new IllegalStateException(lastErrorMessage, ex);
                }
                return;
            }
            p().settlements.add(0, settlement);
            notifyListeners();
        }
    }

    public void addRecurringTransaction(RecurringTransaction recurringTransaction) {
        if (p() != null && recurringTransaction != null) {
            if (hasBackendFinancialSession()) {
                try {
                    String categoryId = ensureBackendCategoryId(recurringTransaction.getTipo(), recurringTransaction.getCategoria());
                    callBackend(token -> apiClient.createRecurringTransaction(
                            token,
                            activeBackendWorkspaceId(),
                            categoryId,
                            backendTransactionType(recurringTransaction.getTipo()),
                            recurringTransaction.getDescripcion(),
                            recurringTransaction.getMontoDecimal(),
                            backendFrequency(recurringTransaction.getFrequency()),
                            recurringTransaction.getCustomIntervalDays(),
                            recurringTransaction.getNextDate()));
                    syncBackendSnapshot();
                    notifyListeners();
                } catch (Exception ex) {
                    handleBackendMutationError("No fue posible crear la recurrencia en el backend.", ex);
                }
                return;
            }
            p().recurringTransactions.add(recurringTransaction);
            notifyListeners();
        }
    }

    public void removeRecurringTransaction(RecurringTransaction recurringTransaction) {
        if (p() != null) {
            if (hasBackendFinancialSession() && recurringTransaction != null && !recurringTransaction.getBackendId().isEmpty()) {
                try {
                    runBackend(token -> apiClient.deactivateRecurringTransaction(
                            token,
                            activeBackendWorkspaceId(),
                            recurringTransaction.getBackendId()));
                    syncBackendSnapshot();
                    notifyListeners();
                } catch (Exception ex) {
                    handleBackendMutationError("No fue posible desactivar la recurrencia en el backend.", ex);
                }
                return;
            }
            p().recurringTransactions.remove(recurringTransaction);
            notifyListeners();
        }
    }

    public void materializeRecurringTransaction(RecurringTransaction recurringTransaction) {
        if (p() == null || recurringTransaction == null || !recurringTransaction.isActive()) {
            return;
        }
        if (hasBackendFinancialSession() && !recurringTransaction.getBackendId().isEmpty()) {
            try {
                callBackend(token -> apiClient.runRecurringTransaction(
                        token,
                        activeBackendWorkspaceId(),
                        recurringTransaction.getBackendId()));
                syncBackendSnapshot();
                notifyListeners();
            } catch (Exception ex) {
                handleBackendMutationError("No fue posible ejecutar la recurrencia en el backend.", ex);
            }
            return;
        }
        p().transacciones.add(0, recurringTransaction.createTransactionForNextDate());
        recurringTransaction.advanceNextDate();
        refreshBudgetUsage(p());
        notifyListeners();
    }

    public void removeGastoHogar(GastoHogar gastoHogar) {
        if (p() != null) {
            if (hasBackendFinancialSession() && gastoHogar != null && !gastoHogar.getBackendId().isEmpty()) {
                try {
                    runBackend(token -> apiClient.cancelSharedExpense(token, activeBackendWorkspaceId(), gastoHogar.getBackendId()));
                    syncBackendSnapshot();
                    notifyListeners();
                } catch (Exception ex) {
                    handleBackendMutationError("No fue posible cancelar el gasto compartido en el backend.", ex);
                }
                return;
            }
            p().gastosHogar.remove(gastoHogar);
            notifyListeners();
        }
    }

    public void addMiembro(String nombre) {
        if (p() != null) {
            if (hasBackendFinancialSession()) {
                lastErrorMessage = "En modo backend los miembros se gestionan con invitaciones del workspace.";
                return;
            }
            String safeName = safeTrim(nombre);
            if (!safeName.isEmpty() && !p().miembrosHogar.contains(safeName)) {
                p().miembrosHogar.add(safeName);
                notifyListeners();
            }
        }
    }

    public List<BackendInvitation> getBackendReceivedInvitations() {
        UserProfile profile = p();
        if (profile == null || profile.backendReceivedInvitations == null) {
            return new ArrayList<BackendInvitation>();
        }
        return new ArrayList<BackendInvitation>(profile.backendReceivedInvitations);
    }

    public List<BackendInvitation> getBackendWorkspaceInvitations() {
        UserProfile profile = p();
        if (profile == null || profile.backendWorkspaceInvitations == null) {
            return new ArrayList<BackendInvitation>();
        }
        return new ArrayList<BackendInvitation>(profile.backendWorkspaceInvitations);
    }

    public boolean refreshBackendInvitations() {
        if (!isBackendSessionActive()) {
            lastErrorMessage = "No hay una sesion backend activa.";
            return false;
        }
        try {
            reloadBackendWorkspaces();
            refreshBackendInvitationCaches(p(), activeBackendWorkspaceId());
            notifyListeners();
            lastErrorMessage = "";
            return true;
        } catch (Exception ex) {
            handleBackendMutationError("No fue posible actualizar las invitaciones.", ex);
            return false;
        }
    }

    public boolean inviteBackendMember(String email, String role) {
        if (!hasBackendFinancialSession()) {
            lastErrorMessage = "No hay una sesion backend activa.";
            return false;
        }
        try {
            runBackend(token -> apiClient.inviteMember(
                    token,
                    activeBackendWorkspaceId(),
                    email,
                    role == null || role.trim().isEmpty() ? "MEMBER" : role.trim()));
            syncBackendSnapshot();
            notifyListeners();
            lastErrorMessage = "";
            return true;
        } catch (Exception ex) {
            handleBackendMutationError("No fue posible enviar la invitacion.", ex);
            return false;
        }
    }

    public boolean acceptBackendInvitation(String invitationId) {
        if (!isBackendSessionActive()) {
            lastErrorMessage = "No hay una sesion backend activa.";
            return false;
        }
        try {
            BackendInvitation accepted = callBackend(token -> apiClient.acceptInvitation(token, invitationId));
            reloadBackendWorkspaces();
            UserProfile profile = p();
            if (profile != null && accepted != null && !accepted.getWorkspaceId().isEmpty()) {
                profile.selectedBackendWorkspaceId = accepted.getWorkspaceId();
            }
            syncBackendSnapshot();
            notifyListeners();
            lastErrorMessage = "";
            return true;
        } catch (Exception ex) {
            handleBackendMutationError("No fue posible aceptar la invitacion.", ex);
            return false;
        }
    }

    public boolean rejectBackendInvitation(String invitationId) {
        if (!isBackendSessionActive()) {
            lastErrorMessage = "No hay una sesion backend activa.";
            return false;
        }
        try {
            callBackend(token -> apiClient.rejectInvitation(token, invitationId));
            refreshBackendInvitationCaches(p(), activeBackendWorkspaceId());
            notifyListeners();
            lastErrorMessage = "";
            return true;
        } catch (Exception ex) {
            handleBackendMutationError("No fue posible rechazar la invitacion.", ex);
            return false;
        }
    }

    public boolean cancelBackendInvitation(String invitationId) {
        if (!hasBackendFinancialSession()) {
            lastErrorMessage = "No hay una sesion backend activa.";
            return false;
        }
        try {
            runBackend(token -> apiClient.cancelInvitation(token, activeBackendWorkspaceId(), invitationId));
            refreshBackendInvitationCaches(p(), activeBackendWorkspaceId());
            notifyListeners();
            lastErrorMessage = "";
            return true;
        } catch (Exception ex) {
            handleBackendMutationError("No fue posible cancelar la invitacion.", ex);
            return false;
        }
    }

    public void removeMiembro(String nombre) {
        if (p() != null) {
            if (hasBackendFinancialSession()) {
                try {
                    runBackend(token -> apiClient.removeMember(
                            token,
                            activeBackendWorkspaceId(),
                            backendMemberIdForName(nombre)));
                    syncBackendSnapshot();
                    notifyListeners();
                    lastErrorMessage = "";
                } catch (Exception ex) {
                    handleBackendMutationError("No fue posible eliminar el miembro del workspace.", ex);
                    throw new IllegalStateException(lastErrorMessage, ex);
                }
                return;
            }
            p().miembrosHogar.remove(nombre);
            notifyListeners();
        }
    }

    public double getTotalIngresos() {
        if (p() == null) {
            return 0;
        }
        return FinancialInsights.totalIngresos(p().transacciones);
    }

    public double getTotalGastos() {
        if (p() == null) {
            return 0;
        }
        return FinancialInsights.totalGastos(p().transacciones);
    }

    public double getIngresosMesActual() {
        if (p() == null) {
            return 0;
        }
        return FinancialInsights.totalByTypeInMonth(p().transacciones, Tipo.INGRESO, YearMonth.now());
    }

    public double getGastosMesActual() {
        if (p() == null) {
            return 0;
        }
        return FinancialInsights.totalByTypeInMonth(p().transacciones, Tipo.GASTO, YearMonth.now());
    }

    public double getSaldoActual() {
        return getTotalIngresos() - getTotalGastos();
    }

    public double getSaldoMesActual() {
        return getIngresosMesActual() - getGastosMesActual();
    }

    public double getAhorros() {
        if (p() == null) {
            return 0;
        }
        if (hasBackendFinancialSession() && p().backendSavingsSummary != null) {
            return p().backendSavingsSummary.getAhorroTotal().doubleValue();
        }
        return FinancialInsights.totalAhorros(p().metas);
    }

    public double getAhorroMesActual() {
        if (p() != null && hasBackendFinancialSession() && p().backendMonthlySavingsSummary != null) {
            return p().backendMonthlySavingsSummary.getAhorroTotal().doubleValue();
        }
        return Math.max(0, getSaldoMesActual());
    }

    public double getTasaAhorroMesActual() {
        if (p() != null && hasBackendFinancialSession() && p().backendMonthlySavingsSummary != null) {
            return p().backendMonthlySavingsSummary.getTasaAhorro().doubleValue();
        }
        double ingresos = getIngresosMesActual();
        if (ingresos <= 0) {
            return 0;
        }
        return (getAhorroMesActual() / ingresos) * 100.0d;
    }

    public FinancialHealthSnapshot getFinancialHealthSnapshot() {
        int score = 0;
        List<String> strengths = new ArrayList<String>();
        List<String> opportunities = new ArrayList<String>();

        double ingresos = getIngresosMesActual();
        double gastos = getGastosMesActual();
        double tasaAhorro = getTasaAhorroMesActual();

        if (ingresos <= 0) {
            opportunities.add("Registra ingresos para calcular tu tasa de ahorro.");
        } else if (tasaAhorro >= 20) {
            score += 35;
            strengths.add("Ahorras " + String.format(java.util.Locale.US, "%.1f", tasaAhorro) + "% de tus ingresos.");
        } else if (tasaAhorro >= 10) {
            score += 25;
            strengths.add("Mantienes una tasa de ahorro positiva de " + String.format(java.util.Locale.US, "%.1f", tasaAhorro) + "%.");
        } else if (tasaAhorro > 0) {
            score += 15;
            opportunities.add("Tu tasa de ahorro es " + String.format(java.util.Locale.US, "%.1f", tasaAhorro) + "%; intenta acercarla a 20%.");
        } else {
            opportunities.add("Tus gastos igualan o superan tus ingresos del mes.");
        }

        List<Presupuesto> presupuestos = getPresupuestos();
        if (presupuestos.isEmpty()) {
            score += 10;
            opportunities.add("Crea presupuestos para medir cumplimiento.");
        } else {
            int ok = 0;
            int exceeded = 0;
            for (Presupuesto presupuesto : presupuestos) {
                if (presupuesto.getPorcentajeUsado() <= 100) {
                    ok++;
                } else {
                    exceeded++;
                }
            }
            if (ok > 0) {
                strengths.add("Estas dentro de " + ok + " de " + presupuestos.size() + " presupuestos.");
            }
            if (exceeded > 0) {
                opportunities.add("Tienes " + exceeded + " presupuestos excedidos.");
            }
            score += exceeded == 0 ? 30 : Math.max(0, 30 - (exceeded * 8));
        }

        if (ingresos > 0 && gastos <= ingresos) {
            score += 20;
            strengths.add("Tu flujo de caja del mes es positivo.");
        } else if (ingresos > 0) {
            opportunities.add("Reduce gastos para recuperar flujo de caja positivo.");
        }

        if (!getMetas().isEmpty()) {
            score += 15;
            strengths.add("Tienes metas de ahorro activas.");
        } else {
            opportunities.add("Crea una meta para darle destino a tu ahorro.");
        }

        return new FinancialHealthSnapshot(score, healthLabel(score), strengths, opportunities);
    }

    private String healthLabel(int score) {
        if (score >= 80) return "Excelente";
        if (score >= 65) return "Buena";
        if (score >= 45) return "En progreso";
        return "Necesita atencion";
    }

    public String getSavingsRecommendationText() {
        UserProfile profile = p();
        if (profile == null) {
            return "Objetivo de ahorro: pendiente de configurar";
        }
        ensureSavingsRecommendationDefaults(profile);
        if (!profile.savingsRecommendationEnabled.booleanValue()) {
            return "Recomendacion de ahorro desactivada";
        }
        BigDecimal monthlyIncome = Money.of(getIngresosMesActual());
        BigDecimal suggested = getSavingsRecommendationAmount();
        if (monthlyIncome.compareTo(BigDecimal.ZERO) <= 0 && suggested.compareTo(BigDecimal.ZERO) <= 0) {
            return "Objetivo de ahorro: pendiente de configurar";
        }
        if ("Monto fijo".equalsIgnoreCase(profile.savingsRecommendationMode)) {
            return "Ahorro sugerido: $" + suggested.setScale(0, RoundingMode.HALF_UP) + " (monto fijo)";
        }
        return "Ahorro sugerido: $" + suggested.setScale(0, RoundingMode.HALF_UP)
                + " (" + profile.savingsRecommendationPercent.setScale(0, RoundingMode.HALF_UP) + "% de ingresos)";
    }

    public BigDecimal getSavingsRecommendationAmount() {
        UserProfile profile = p();
        if (profile == null) {
            return Money.ZERO;
        }
        ensureSavingsRecommendationDefaults(profile);
        if (!profile.savingsRecommendationEnabled.booleanValue()) {
            return Money.ZERO;
        }
        if ("Monto fijo".equalsIgnoreCase(profile.savingsRecommendationMode)) {
            return Money.normalize(profile.savingsRecommendationFixedAmount);
        }
        BigDecimal monthlyIncome = Money.of(getIngresosMesActual());
        if (monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return Money.ZERO;
        }
        return monthlyIncome.multiply(profile.savingsRecommendationPercent)
                .divide(BigDecimal.valueOf(100), Money.SCALE, Money.ROUNDING);
    }

    public String getSavingsRecommendationMode() {
        UserProfile profile = p();
        if (profile == null) {
            return "Equilibrado";
        }
        ensureSavingsRecommendationDefaults(profile);
        return profile.savingsRecommendationMode;
    }

    public BigDecimal getSavingsRecommendationPercent() {
        UserProfile profile = p();
        if (profile == null) {
            return new BigDecimal("20.00");
        }
        ensureSavingsRecommendationDefaults(profile);
        return profile.savingsRecommendationPercent;
    }

    public BigDecimal getSavingsRecommendationFixedAmount() {
        UserProfile profile = p();
        if (profile == null) {
            return Money.ZERO;
        }
        ensureSavingsRecommendationDefaults(profile);
        return profile.savingsRecommendationFixedAmount;
    }

    public boolean isSavingsRecommendationEnabled() {
        UserProfile profile = p();
        if (profile == null) {
            return true;
        }
        ensureSavingsRecommendationDefaults(profile);
        return profile.savingsRecommendationEnabled.booleanValue();
    }

    public void configureSavingsRecommendation(boolean enabled, String mode, BigDecimal percent, BigDecimal fixedAmount) {
        UserProfile profile = p();
        if (profile == null) {
            return;
        }
        String safeMode = mode == null || mode.trim().isEmpty() ? "Equilibrado" : mode.trim();
        BigDecimal resolvedPercent = resolveSavingsPercent(safeMode, percent);
        if (resolvedPercent.compareTo(BigDecimal.ZERO) < 0 || resolvedPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100.");
        }
        BigDecimal resolvedFixed = Money.normalize(fixedAmount);
        if (resolvedFixed.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto fijo no puede ser negativo.");
        }
        profile.savingsRecommendationEnabled = Boolean.valueOf(enabled);
        profile.savingsRecommendationMode = safeMode;
        profile.savingsRecommendationPercent = resolvedPercent;
        profile.savingsRecommendationFixedAmount = resolvedFixed;
        notifyListeners();
    }

    public String getIncomeVariationText() {
        return p() == null ? "Sin periodo anterior para comparar" : FinancialInsights.variationVsPreviousMonth(p().transacciones, Tipo.INGRESO);
    }

    public String getExpenseVariationText() {
        return p() == null ? "Sin periodo anterior para comparar" : FinancialInsights.variationVsPreviousMonth(p().transacciones, Tipo.GASTO);
    }

    public String getBalanceVariationText() {
        if (p() == null) {
            return "Sin periodo anterior para comparar";
        }
        double previousIncome = FinancialInsights.totalByTypeInMonth(p().transacciones, Tipo.INGRESO, YearMonth.now().minusMonths(1));
        double previousExpense = FinancialInsights.totalByTypeInMonth(p().transacciones, Tipo.GASTO, YearMonth.now().minusMonths(1));
        return (previousIncome == 0 && previousExpense == 0) ? "Sin periodo anterior para comparar" : "Calculado con transacciones reales";
    }

    public double[] getIngresosUltimos6Meses() {
        return p() == null ? new double[0] : FinancialInsights.totalsByMonth(p().transacciones, Tipo.INGRESO, 6);
    }

    public double[] getGastosUltimos6Meses() {
        return p() == null ? new double[0] : FinancialInsights.totalsByMonth(p().transacciones, Tipo.GASTO, 6);
    }

    public String[] getMesesLabels() {
        return FinancialInsights.monthLabels(6);
    }

    public String[] getCategorias() {
        return p() == null ? new String[0] : FinancialInsights.categoryLabels(p().transacciones);
    }

    public double[] getPorcentajes() {
        return p() == null ? new double[0] : FinancialInsights.categoryPercentages(p().transacciones);
    }

    public List<Transaccion> buscarTransacciones(String texto, Tipo tipo) {
        if (p() == null) {
            return new ArrayList<Transaccion>();
        }
        return FinancialInsights.buscarTransacciones(p().transacciones, texto, tipo);
    }

    public List<RecurringExpenseInsight> detectPossibleRecurringExpenses() {
        if (p() == null) {
            return new ArrayList<RecurringExpenseInsight>();
        }
        return FinancialInsights.detectPossibleRecurringExpenses(p().transacciones);
    }

    public ReportSnapshot getReportSnapshot(String periodLabel) {
        return getReportSnapshot(periodLabel, null, null);
    }

    public ReportSnapshot getReportSnapshot(String periodLabel, LocalDate customStart, LocalDate customEnd) {
        ReportPeriod period = ReportPeriod.fromSelection(periodLabel, customStart, customEnd);
        UserProfile profile = p();
        if (profile == null) {
            return new ReportSnapshot(period, new ArrayList<Transaccion>(), Money.ZERO, Money.ZERO, new LinkedHashMap<String, BigDecimal>());
        }

        List<Transaccion> filtered = new ArrayList<Transaccion>();
        BigDecimal income = Money.ZERO;
        BigDecimal expenses = Money.ZERO;
        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<String, BigDecimal>();

        for (Transaccion transaccion : profile.transacciones) {
            if (!period.contains(transaccion.getFecha())) {
                continue;
            }
            filtered.add(transaccion);
            if (transaccion.getTipo() == Tipo.INGRESO) {
                income = income.add(transaccion.getMontoDecimal());
            } else {
                expenses = expenses.add(transaccion.getMontoDecimal());
                expenseByCategory.merge(transaccion.getCategoria(), transaccion.getMontoDecimal(), BigDecimal::add);
            }
        }

        Map<String, BigDecimal> sortedCategories = expenseByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), Money.normalize(entry.getValue())),
                        LinkedHashMap::putAll);

        return new ReportSnapshot(period, filtered, income, expenses, sortedCategories);
    }

    public List<SearchResult> searchGlobal(String text) {
        List<SearchResult> results = new ArrayList<SearchResult>();
        UserProfile profile = p();
        String query = normalizeSearch(text);
        if (profile == null || query.isEmpty()) {
            return results;
        }

        for (Transaccion transaction : profile.transacciones) {
            if (matches(query, transaction.getCategoria())
                    || matches(query, transaction.getDescripcion())
                    || matches(query, transaction.getTipo().name())
                    || matches(query, transaction.getFecha().toString())) {
                String section = transaction.getTipo() == Tipo.INGRESO ? "ingresos" : "gastos";
                String amount = (transaction.getTipo() == Tipo.INGRESO ? "+" : "-") + "$" + Money.normalize(transaction.getMontoDecimal());
                results.add(new SearchResult("Transaccion", transaction.getDescripcion(), transaction.getCategoria() + " | " + amount + " | " + transaction.getFecha(), section));
            }
        }

        for (Presupuesto presupuesto : profile.presupuestos) {
            if (matches(query, presupuesto.getCategoria())) {
                results.add(new SearchResult(
                        "Presupuesto",
                        presupuesto.getCategoria(),
                        "Uso " + String.format(java.util.Locale.US, "%.1f", presupuesto.getPorcentajeUsado()) + "% | $" + Money.normalize(presupuesto.getMontoPresupuestadoDecimal()),
                        "presupuesto"));
            }
        }

        for (MetaAhorro meta : profile.metas) {
            if (matches(query, meta.getNombre()) || matches(query, meta.getIcono())) {
                results.add(new SearchResult(
                        "Meta",
                        meta.getNombre(),
                        meta.getProgreso() + "% | faltan $" + Money.normalize(meta.getFaltanteDecimal()),
                        "metas"));
            }
        }

        for (String member : profile.miembrosHogar) {
            if (matches(query, member)) {
                results.add(new SearchResult("Miembro", member, "Integrante del hogar", "hogar"));
            }
        }

        for (FinancialCategory category : profile.categories) {
            if (!category.isArchived() && matches(query, category.getName())) {
                results.add(new SearchResult("Categoria", category.getName(), category.getKind().name() + " | " + category.getColor(), "config"));
            }
        }

        return results;
    }

    public List<NotificationItem> getNotifications() {
        List<NotificationItem> notifications = new ArrayList<NotificationItem>();
        UserProfile profile = p();
        if (profile == null || profile.usuario == null) {
            return notifications;
        }
        if (hasBackendFinancialSession()) {
            if (profile.backendNotifications == null) {
                profile.backendNotifications = new ArrayList<NotificationItem>();
            }
            return new ArrayList<NotificationItem>(profile.backendNotifications);
        }

        if (getSaldoActual() < 0) {
            notifications.add(new NotificationItem(
                    "Saldo negativo",
                    "Tus gastos superan a tus ingresos por $" + Money.normalize(Money.of(Math.abs(getSaldoActual()))) + ".",
                    NotificationItem.Severity.CRITICAL,
                    "inicio"));
        }

        refreshBudgetUsage(profile);
        if (profile.usuario.isNotifPresupuesto()) {
            for (Presupuesto presupuesto : profile.presupuestos) {
                double percentage = presupuesto.getPorcentajeUsado();
                if (percentage >= 100) {
                    notifications.add(new NotificationItem(
                            "Presupuesto excedido: " + presupuesto.getCategoria(),
                            "Ya usaste " + String.format(java.util.Locale.US, "%.0f", percentage) + "% del limite mensual.",
                            NotificationItem.Severity.CRITICAL,
                            "presupuesto"));
                } else if (percentage >= 85) {
                    notifications.add(new NotificationItem(
                            "Presupuesto en riesgo: " + presupuesto.getCategoria(),
                            "Has utilizado " + String.format(java.util.Locale.US, "%.0f", percentage) + "% del presupuesto.",
                            NotificationItem.Severity.WARNING,
                            "presupuesto"));
                }
            }
        }

        if (profile.usuario.isNotifMetas()) {
            for (MetaAhorro meta : profile.metas) {
                if (meta.getProgreso() >= 100) {
                    notifications.add(new NotificationItem(
                            "Meta completada: " + meta.getNombre(),
                            "Objetivo alcanzado. Puedes definir un nuevo destino para ese ahorro.",
                            NotificationItem.Severity.SUCCESS,
                            "metas"));
                } else if (meta.getDiasRestantes() < 0) {
                    notifications.add(new NotificationItem(
                            "Meta vencida: " + meta.getNombre(),
                            "La fecha limite fue el " + meta.getFechaLimite() + ".",
                            NotificationItem.Severity.CRITICAL,
                            "metas"));
                } else if (meta.getDiasRestantes() <= 15) {
                    notifications.add(new NotificationItem(
                            "Meta cercana: " + meta.getNombre(),
                            "Quedan " + meta.getDiasRestantes() + " dias y faltan $" + Money.normalize(meta.getFaltanteDecimal()) + ".",
                            NotificationItem.Severity.INFO,
                            "metas"));
                }
            }
        }

        if (profile.usuario.isNotifConsejos()) {
            NotificationItem unusual = detectUnusualExpenseNotification(profile);
            if (unusual != null) {
                notifications.add(unusual);
            }

            for (RecurringTransaction recurring : getUpcomingRecurringTransactions(2)) {
                String timing = describeDueDate(recurring.getNextDate());
                String title = recurring.getTipo() == Tipo.GASTO ? "Proximo pago" : "Proximo ingreso";
                notifications.add(new NotificationItem(
                        title + ": " + recurring.getDescripcion(),
                        recurring.getCategoria() + " " + timing + " por $" + Money.normalize(recurring.getMontoDecimal()) + ".",
                        recurring.getTipo() == Tipo.GASTO ? NotificationItem.Severity.WARNING : NotificationItem.Severity.INFO,
                        "inicio"));
            }

            List<RecurringExpenseInsight> recurringInsights = detectPossibleRecurringExpenses();
            if (!recurringInsights.isEmpty()) {
                RecurringExpenseInsight insight = recurringInsights.get(0);
                notifications.add(new NotificationItem(
                        "Posible gasto recurrente",
                        insight.getDescription() + " aparece " + insight.getEvidenceCount()
                                + " veces. Estimado mensual: $" + Money.normalize(insight.getEstimatedMonthlyAmount())
                                + " | anual: $" + Money.normalize(insight.getEstimatedAnnualAmount()) + ".",
                        NotificationItem.Severity.INFO,
                        "gastos"));
            }
        }

        return notifications;
    }

    public List<String> getRuleBasedInsights() {
        return insightProvider.generateInsights(this);
    }

    public Map<String, Double> calcularDeudas() {
        if (p() == null) {
            return Collections.emptyMap();
        }
        return HouseholdInsights.calcularDeudas(p().miembrosHogar, p().gastosHogar, p().settlements);
    }

    private boolean loadState() {
        try {
            PersistenceService.AppState state = PersistenceService.load();
            if (state == null) {
                return false;
            }
            profiles.clear();
            profiles.putAll(state.profiles);
            legacyGastosHogar.clear();
            if (state.gastosHogar != null) {
                legacyGastosHogar.addAll(state.gastosHogar);
            }
            legacyMiembrosHogar.clear();
            if (state.miembrosHogar != null) {
                legacyMiembrosHogar.addAll(state.miembrosHogar);
            }
            initializeLoadedProfiles(state.currentUser);
            currentUser = null;
            saveState();
            return true;
        } catch (Exception ex) {
            lastErrorMessage = ex.getMessage();
            LOGGER.log(Level.WARNING, "No fue posible cargar el estado persistido.", ex);
            return false;
        }
    }

    private void saveState() {
        try {
            PersistenceService.save(new PersistenceService.AppState(profiles, currentUser, legacyGastosHogar, legacyMiembrosHogar));
            lastErrorMessage = "";
        } catch (IOException ex) {
            lastErrorMessage = ex.getMessage();
            LOGGER.log(Level.WARNING, "No fue posible guardar el estado persistido.", ex);
        }
    }

    private void initializeLoadedProfiles(String legacyCurrentUser) {
        for (Map.Entry<String, UserProfile> entry : new ArrayList<Map.Entry<String, UserProfile>>(profiles.entrySet())) {
            ensureProfileInitialized(entry.getKey(), entry.getValue());
        }
        migrateLegacyHousehold(legacyCurrentUser);
        AuthService.migratePasswords(profiles);
        for (UserProfile profile : profiles.values()) {
            refreshBudgetUsage(profile);
        }
    }

    private void ensureProfileInitialized(String profileKey, UserProfile profile) {
        if (profile == null) {
            return;
        }
        if (profile.usuario == null) {
            profile.usuario = new Usuario();
        }
        String normalizedKey = AuthService.normalizeEmail(profileKey);
        if (profile.usuario.getEmail() == null || profile.usuario.getEmail().trim().isEmpty()) {
            profile.usuario.setEmail(normalizedKey);
        }
        profile.usuario.setProfileImagePath(PersistenceService.normalizeStoredProfileImagePath(profile.usuario.getProfileImagePath()));
        if (profile.transacciones == null) {
            profile.transacciones = new ArrayList<Transaccion>();
        }
        if (profile.metas == null) {
            profile.metas = new ArrayList<MetaAhorro>();
        }
        if (profile.presupuestos == null) {
            profile.presupuestos = new ArrayList<Presupuesto>();
        }
        if (profile.gastosHogar == null) {
            profile.gastosHogar = new ArrayList<GastoHogar>();
        }
        if (profile.miembrosHogar == null) {
            profile.miembrosHogar = new ArrayList<String>();
        }
        if (profile.recurringTransactions == null) {
            profile.recurringTransactions = new ArrayList<RecurringTransaction>();
        }
        if (profile.settlements == null) {
            profile.settlements = new ArrayList<Settlement>();
        }
        if (profile.categories == null) {
            profile.categories = new ArrayList<FinancialCategory>();
        }
        if (profile.backendNotifications == null) {
            profile.backendNotifications = new ArrayList<NotificationItem>();
        }
        if (profile.backendMemberIdsByName == null) {
            profile.backendMemberIdsByName = new LinkedHashMap<String, String>();
        }
        if (profile.backendReceivedInvitations == null) {
            profile.backendReceivedInvitations = new ArrayList<BackendInvitation>();
        }
        if (profile.backendWorkspaceInvitations == null) {
            profile.backendWorkspaceInvitations = new ArrayList<BackendInvitation>();
        }
        if (!BackendConfig.isEnabled()) {
            profile.backendSavingsSummary = null;
            profile.backendMonthlySavingsSummary = null;
            profile.backendReceivedInvitations.clear();
            profile.backendWorkspaceInvitations.clear();
        }
        if (profile.selectedBackendWorkspaceId == null) {
            profile.selectedBackendWorkspaceId = "";
        }
        ensureDefaultCategories(profile);
        if (profile.estimatedMonthlyIncome == null) {
            profile.estimatedMonthlyIncome = Money.ZERO;
        }
        if (profile.primaryGoal == null) {
            profile.primaryGoal = "";
        }
        ensureSavingsRecommendationDefaults(profile);
        if (!profile.onboardingCompleted
                && (!profile.transacciones.isEmpty()
                || !profile.metas.isEmpty()
                || !profile.presupuestos.isEmpty()
                || !profile.gastosHogar.isEmpty())) {
            profile.onboardingCompleted = true;
        }
        if ("Hogar".equalsIgnoreCase(profile.usuario.getTipoCuenta())) {
            String ownName = profile.usuario.getNombreCompleto();
            if (!ownName.isEmpty() && !profile.miembrosHogar.contains(ownName)) {
                profile.miembrosHogar.add(ownName);
            }
        }
    }

    private void migrateLegacyHousehold(String legacyCurrentUser) {
        if (legacyGastosHogar.isEmpty() && legacyMiembrosHogar.isEmpty()) {
            return;
        }

        String ownerKey = AuthService.normalizeEmail(legacyCurrentUser);
        if ((ownerKey == null || ownerKey.isEmpty() || !profiles.containsKey(ownerKey)) && profiles.size() == 1) {
            ownerKey = profiles.keySet().iterator().next();
        }
        if (ownerKey == null || ownerKey.isEmpty() || !profiles.containsKey(ownerKey)) {
            LOGGER.warning("Existen datos legacy de hogar sin propietario confiable. No se expondran a ningun usuario.");
            return;
        }

        UserProfile owner = profiles.get(ownerKey);
        if (owner.gastosHogar.isEmpty()) {
            owner.gastosHogar.addAll(legacyGastosHogar);
        }
        for (String member : legacyMiembrosHogar) {
            if (!owner.miembrosHogar.contains(member)) {
                owner.miembrosHogar.add(member);
            }
        }
        legacyGastosHogar.clear();
        legacyMiembrosHogar.clear();
    }

    private void refreshBudgetUsage(UserProfile profile) {
        if (profile == null || profile.presupuestos == null) {
            return;
        }
        for (Presupuesto presupuesto : profile.presupuestos) {
            presupuesto.setMontoGastado(calcularGastadoPresupuesto(presupuesto));
        }
    }

    private double calcularGastadoPresupuesto(Presupuesto presupuesto) {
        if (p() == null || presupuesto == null) {
            return 0;
        }
        return FinancialInsights.totalGastosByCategoryInMonth(p().transacciones, presupuesto.getCategoria(), YearMonth.now());
    }

    private void replaceHouseholdMemberName(UserProfile profile, String previousName, String newName) {
        if (profile == null || previousName == null || previousName.trim().isEmpty() || newName == null || newName.trim().isEmpty()) {
            return;
        }
        int index = profile.miembrosHogar.indexOf(previousName);
        if (index >= 0 && !profile.miembrosHogar.contains(newName)) {
            profile.miembrosHogar.set(index, newName);
        }
    }

    private boolean isDemoMode() {
        return "true".equalsIgnoreCase(System.getenv("FINANZAS_DEMO_MODE"))
                || "true".equalsIgnoreCase(System.getenv("DEMO_MODE"));
    }

    private void ensureDefaultCategories(UserProfile profile) {
        if (profile == null) {
            return;
        }
        addDefaultCategory(profile, FinancialCategory.Kind.INCOME, "Salario", "ING", "#34a853");
        addDefaultCategory(profile, FinancialCategory.Kind.INCOME, "Freelance", "FRE", "#1a73e8");
        addDefaultCategory(profile, FinancialCategory.Kind.INCOME, "Negocio", "NEG", "#1a73e8");
        addDefaultCategory(profile, FinancialCategory.Kind.INCOME, "Inversiones", "INV", "#34a853");
        addDefaultCategory(profile, FinancialCategory.Kind.INCOME, "Arriendo", "ARR", "#34a853");
        addDefaultCategory(profile, FinancialCategory.Kind.INCOME, "Otros", "OTR", "#64748b");

        addDefaultCategory(profile, FinancialCategory.Kind.EXPENSE, "Alimentacion", "ALI", "#f59e0b");
        addDefaultCategory(profile, FinancialCategory.Kind.EXPENSE, "Vivienda", "VIV", "#1a73e8");
        addDefaultCategory(profile, FinancialCategory.Kind.EXPENSE, "Transporte", "TRA", "#6366f1");
        addDefaultCategory(profile, FinancialCategory.Kind.EXPENSE, "Salud", "SAL", "#ef4444");
        addDefaultCategory(profile, FinancialCategory.Kind.EXPENSE, "Educacion", "EDU", "#14b8a6");
        addDefaultCategory(profile, FinancialCategory.Kind.EXPENSE, "Entretenimiento", "ENT", "#8b5cf6");
        addDefaultCategory(profile, FinancialCategory.Kind.EXPENSE, "Servicios", "SER", "#0ea5e9");
        addDefaultCategory(profile, FinancialCategory.Kind.EXPENSE, "Supermercado", "SUP", "#f97316");
        addDefaultCategory(profile, FinancialCategory.Kind.EXPENSE, "Restaurante", "RES", "#ec4899");
        addDefaultCategory(profile, FinancialCategory.Kind.EXPENSE, "Deudas", "DEU", "#b91c1c");
        addDefaultCategory(profile, FinancialCategory.Kind.EXPENSE, "Suscripciones", "SUB", "#475569");
        addDefaultCategory(profile, FinancialCategory.Kind.EXPENSE, "Otros", "OTR", "#64748b");

        addDefaultCategory(profile, FinancialCategory.Kind.HOUSEHOLD, "Alimentacion", "ALI", "#f59e0b");
        addDefaultCategory(profile, FinancialCategory.Kind.HOUSEHOLD, "Servicios", "SER", "#0ea5e9");
        addDefaultCategory(profile, FinancialCategory.Kind.HOUSEHOLD, "Limpieza", "LIM", "#14b8a6");
        addDefaultCategory(profile, FinancialCategory.Kind.HOUSEHOLD, "Mantenimiento", "MAN", "#6366f1");
        addDefaultCategory(profile, FinancialCategory.Kind.HOUSEHOLD, "Arriendo", "ARR", "#1a73e8");
        addDefaultCategory(profile, FinancialCategory.Kind.HOUSEHOLD, "Entretenimiento", "ENT", "#8b5cf6");
        addDefaultCategory(profile, FinancialCategory.Kind.HOUSEHOLD, "Otros", "OTR", "#64748b");
    }

    private void addDefaultCategory(UserProfile profile, FinancialCategory.Kind kind, String name, String icon, String color) {
        if (findCategory(profile, kind, name) == null) {
            profile.categories.add(new FinancialCategory(kind, name, icon, color));
        }
    }

    private FinancialCategory findCategory(UserProfile profile, FinancialCategory.Kind kind, String name) {
        if (profile == null || profile.categories == null) {
            return null;
        }
        String normalizedName = normalizeSearch(name);
        for (FinancialCategory category : profile.categories) {
            if (category.getKind() == kind && normalizeSearch(category.getName()).equals(normalizedName)) {
                return category;
            }
        }
        return null;
    }

    private void ensureSavingsRecommendationDefaults(UserProfile profile) {
        if (profile.savingsRecommendationEnabled == null) {
            profile.savingsRecommendationEnabled = Boolean.TRUE;
        }
        if (profile.savingsRecommendationMode == null || profile.savingsRecommendationMode.trim().isEmpty()) {
            profile.savingsRecommendationMode = "Equilibrado";
        }
        if (profile.savingsRecommendationPercent == null) {
            profile.savingsRecommendationPercent = resolveSavingsPercent(profile.savingsRecommendationMode, new BigDecimal("20.00"));
        }
        if (profile.savingsRecommendationFixedAmount == null) {
            profile.savingsRecommendationFixedAmount = Money.ZERO;
        }
    }

    private BigDecimal resolveSavingsPercent(String mode, BigDecimal customPercent) {
        if ("Conservador".equalsIgnoreCase(mode)) {
            return new BigDecimal("10.00");
        }
        if ("Equilibrado".equalsIgnoreCase(mode)) {
            return new BigDecimal("20.00");
        }
        if ("Agresivo".equalsIgnoreCase(mode)) {
            return new BigDecimal("30.00");
        }
        return Money.normalize(customPercent == null ? new BigDecimal("20.00") : customPercent);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private File ensureExtension(File file, String extension) {
        String path = file.getAbsolutePath();
        if (path.toLowerCase(java.util.Locale.ROOT).endsWith(extension)) {
            return file;
        }
        return new File(path + extension);
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean matches(String query, String value) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(query);
    }

    private NotificationItem detectUnusualExpenseNotification(UserProfile profile) {
        YearMonth currentMonth = YearMonth.now();
        for (Transaccion transaction : profile.transacciones) {
            if (transaction.getTipo() != Tipo.GASTO || !YearMonth.from(transaction.getFecha()).equals(currentMonth)) {
                continue;
            }
            BigDecimal previousAverage = averagePreviousExpense(profile, transaction);
            if (previousAverage.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal threshold = previousAverage.multiply(BigDecimal.valueOf(1.5d));
            if (transaction.getMontoDecimal().compareTo(threshold) > 0) {
                return new NotificationItem(
                        "Gasto inusual: " + transaction.getCategoria(),
                        transaction.getDescripcion() + " fue $" + Money.normalize(transaction.getMontoDecimal())
                                + ", sobre tu promedio de $" + Money.normalize(previousAverage) + ".",
                        NotificationItem.Severity.WARNING,
                        "gastos");
            }
        }
        return null;
    }

    private BigDecimal averagePreviousExpense(UserProfile profile, Transaccion reference) {
        BigDecimal total = Money.ZERO;
        int count = 0;
        String category = normalizeSearch(reference.getCategoria());
        for (Transaccion transaction : profile.transacciones) {
            if (transaction == reference
                    || transaction.getTipo() != Tipo.GASTO
                    || !normalizeSearch(transaction.getCategoria()).equals(category)
                    || !transaction.getFecha().isBefore(reference.getFecha())) {
                continue;
            }
            total = total.add(transaction.getMontoDecimal());
            count++;
        }
        if (count < 3) {
            return Money.ZERO;
        }
        return total.divide(BigDecimal.valueOf(count), Money.SCALE, Money.ROUNDING);
    }

    private String describeDueDate(LocalDate dueDate) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        if (days <= 0) {
            return "vence hoy";
        }
        if (days == 1) {
            return "vence manana";
        }
        return "vence en " + days + " dias";
    }

    private boolean isLoginLocked(String normalizedEmail) {
        LoginAttempt attempt = loginAttempts.get(normalizedEmail);
        if (attempt == null || attempt.lockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(attempt.lockedUntil)) {
            loginAttempts.remove(normalizedEmail);
            return false;
        }
        return true;
    }

    private void recordFailedLogin(String normalizedEmail) {
        LoginAttempt attempt = loginAttempts.computeIfAbsent(normalizedEmail, ignored -> new LoginAttempt());
        attempt.failures++;
        if (attempt.failures >= MAX_LOGIN_ATTEMPTS) {
            attempt.lockedUntil = Instant.now().plusSeconds(LOGIN_LOCK_SECONDS);
            lastErrorMessage = "Demasiados intentos fallidos. Intenta nuevamente en unos minutos.";
        } else {
            lastErrorMessage = "Correo o contrasena invalidos.";
        }
    }
}
