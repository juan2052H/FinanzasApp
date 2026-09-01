package com.finanzas.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FinanzasApiClient {
    private static final long MAX_AVATAR_BYTES = 5L * 1024L * 1024L;
    private final HttpClient httpClient;
    private final String baseUrl;

    public FinanzasApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    public BackendSession login(String email, String password) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("email", email);
        body.put("password", password);
        return parseAuthResponse(post("/api/auth/login", SimpleJson.stringify(body), null));
    }

    public BackendSession register(String nombreCompleto, String email, String password, String moneda, String tipoCuenta) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        String[] parts = splitName(nombreCompleto);
        body.put("nombre", parts[0]);
        body.put("apellido", parts[1]);
        body.put("email", email);
        body.put("password", password);
        body.put("moneda", moneda);
        body.put("tipoCuenta", normalizeAccountType(tipoCuenta));
        return parseAuthResponse(post("/api/auth/register", SimpleJson.stringify(body), null));
    }

    public BackendSession loginWithGoogle(String authorizationCode, String codeVerifier, String redirectUri)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("authorizationCode", authorizationCode);
        body.put("codeVerifier", codeVerifier);
        body.put("redirectUri", redirectUri);
        return parseAuthResponse(post("/api/auth/google", SimpleJson.stringify(body), null));
    }

    public BackendSession refresh(String refreshToken) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("refreshToken", refreshToken);
        return parseAuthResponse(post("/api/auth/refresh", SimpleJson.stringify(body), null));
    }

    public List<BackendWorkspace> listWorkspaces(String accessToken) throws IOException, InterruptedException {
        String response = send(HttpRequest.newBuilder(uri("/api/workspaces"))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build());
        List<BackendWorkspace> result = new ArrayList<BackendWorkspace>();
        for (Object item : SimpleJson.asArray(SimpleJson.parse(response))) {
            result.add(toWorkspace(SimpleJson.asObject(item)));
        }
        return result;
    }

    public BackendWorkspace createWorkspace(String accessToken, String nombre, String tipo)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("nombre", nombre);
        body.put("tipo", tipo == null || tipo.trim().isEmpty() ? "PERSONAL" : tipo.trim());
        return toWorkspace(SimpleJson.asObject(SimpleJson.parse(
                post("/api/workspaces", SimpleJson.stringify(body), accessToken))));
    }

    public List<BackendCategory> listCategories(String accessToken, String workspaceId) throws IOException, InterruptedException {
        String response = get("/api/workspaces/" + workspaceId + "/categories", accessToken);
        List<BackendCategory> result = new ArrayList<BackendCategory>();
        for (Object item : SimpleJson.asArray(SimpleJson.parse(response))) {
            result.add(toCategory(SimpleJson.asObject(item)));
        }
        return result;
    }

    public BackendCategory createCategory(String accessToken, String workspaceId, String nombre, String type,
                                          String icono, String color) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("nombre", nombre);
        body.put("type", type);
        body.put("icono", icono);
        body.put("color", color);
        return toCategory(SimpleJson.asObject(SimpleJson.parse(
                post("/api/workspaces/" + workspaceId + "/categories", SimpleJson.stringify(body), accessToken))));
    }

    public BackendCategory updateCategory(String accessToken, String workspaceId, String categoryId, String nombre,
                                          String type, String icono, String color) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("nombre", nombre);
        body.put("type", type);
        body.put("icono", icono);
        body.put("color", color);
        return toCategory(SimpleJson.asObject(SimpleJson.parse(
                put("/api/workspaces/" + workspaceId + "/categories/" + categoryId, SimpleJson.stringify(body), accessToken))));
    }

    public void archiveCategory(String accessToken, String workspaceId, String categoryId) throws IOException, InterruptedException {
        delete("/api/workspaces/" + workspaceId + "/categories/" + categoryId, accessToken);
    }

    public List<BackendTransaction> listTransactions(String accessToken, String workspaceId) throws IOException, InterruptedException {
        String response = get("/api/workspaces/" + workspaceId + "/transactions", accessToken);
        List<BackendTransaction> result = new ArrayList<BackendTransaction>();
        for (Object item : SimpleJson.asArray(SimpleJson.parse(response))) {
            result.add(toTransaction(SimpleJson.asObject(item)));
        }
        return result;
    }

    public BackendTransaction createTransaction(String accessToken, String workspaceId, String categoryId, String type,
                                                String description, BigDecimal amount, LocalDate date)
            throws IOException, InterruptedException {
        Map<String, Object> body = transactionBody(categoryId, type, description, amount, date);
        return toTransaction(SimpleJson.asObject(SimpleJson.parse(
                post("/api/workspaces/" + workspaceId + "/transactions", SimpleJson.stringify(body), accessToken))));
    }

    public BackendTransaction updateTransaction(String accessToken, String workspaceId, String transactionId,
                                                String categoryId, String type, String description,
                                                BigDecimal amount, LocalDate date)
            throws IOException, InterruptedException {
        Map<String, Object> body = transactionBody(categoryId, type, description, amount, date);
        return toTransaction(SimpleJson.asObject(SimpleJson.parse(
                put("/api/workspaces/" + workspaceId + "/transactions/" + transactionId, SimpleJson.stringify(body), accessToken))));
    }

    public void deleteTransaction(String accessToken, String workspaceId, String transactionId) throws IOException, InterruptedException {
        delete("/api/workspaces/" + workspaceId + "/transactions/" + transactionId, accessToken);
    }

    public List<BackendBudget> listBudgets(String accessToken, String workspaceId) throws IOException, InterruptedException {
        String response = get("/api/workspaces/" + workspaceId + "/budgets", accessToken);
        List<BackendBudget> result = new ArrayList<BackendBudget>();
        for (Object item : SimpleJson.asArray(SimpleJson.parse(response))) {
            result.add(toBudget(SimpleJson.asObject(item)));
        }
        return result;
    }

    public BackendBudget createBudget(String accessToken, String workspaceId, String categoryId,
                                      LocalDate periodMonth, BigDecimal amount) throws IOException, InterruptedException {
        Map<String, Object> body = budgetBody(categoryId, periodMonth, amount);
        return toBudget(SimpleJson.asObject(SimpleJson.parse(
                post("/api/workspaces/" + workspaceId + "/budgets", SimpleJson.stringify(body), accessToken))));
    }

    public BackendBudget updateBudget(String accessToken, String workspaceId, String budgetId, String categoryId,
                                      LocalDate periodMonth, BigDecimal amount) throws IOException, InterruptedException {
        Map<String, Object> body = budgetBody(categoryId, periodMonth, amount);
        return toBudget(SimpleJson.asObject(SimpleJson.parse(
                put("/api/workspaces/" + workspaceId + "/budgets/" + budgetId, SimpleJson.stringify(body), accessToken))));
    }

    public void deleteBudget(String accessToken, String workspaceId, String budgetId) throws IOException, InterruptedException {
        delete("/api/workspaces/" + workspaceId + "/budgets/" + budgetId, accessToken);
    }

    public List<BackendSavingsGoal> listSavingsGoals(String accessToken, String workspaceId) throws IOException, InterruptedException {
        String response = get("/api/workspaces/" + workspaceId + "/goals", accessToken);
        List<BackendSavingsGoal> result = new ArrayList<BackendSavingsGoal>();
        for (Object item : SimpleJson.asArray(SimpleJson.parse(response))) {
            result.add(toSavingsGoal(SimpleJson.asObject(item)));
        }
        return result;
    }

    public BackendSavingsGoal createSavingsGoal(String accessToken, String workspaceId, String name,
                                                BigDecimal currentAmount, BigDecimal targetAmount,
                                                String color, String icono, LocalDate dueDate)
            throws IOException, InterruptedException {
        Map<String, Object> body = savingsGoalBody(name, currentAmount, targetAmount, color, icono, dueDate);
        return toSavingsGoal(SimpleJson.asObject(SimpleJson.parse(
                post("/api/workspaces/" + workspaceId + "/goals", SimpleJson.stringify(body), accessToken))));
    }

    public BackendSavingsGoal updateSavingsGoal(String accessToken, String workspaceId, String goalId, String name,
                                                BigDecimal currentAmount, BigDecimal targetAmount,
                                                String color, String icono, LocalDate dueDate)
            throws IOException, InterruptedException {
        Map<String, Object> body = savingsGoalBody(name, currentAmount, targetAmount, color, icono, dueDate);
        return toSavingsGoal(SimpleJson.asObject(SimpleJson.parse(
                put("/api/workspaces/" + workspaceId + "/goals/" + goalId, SimpleJson.stringify(body), accessToken))));
    }

    public BackendSavingsGoal contributeSavingsGoal(String accessToken, String workspaceId, String goalId, BigDecimal amount)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("amount", amount);
        return toSavingsGoal(SimpleJson.asObject(SimpleJson.parse(
                post("/api/workspaces/" + workspaceId + "/goals/" + goalId + "/contributions", SimpleJson.stringify(body), accessToken))));
    }

    public void archiveSavingsGoal(String accessToken, String workspaceId, String goalId) throws IOException, InterruptedException {
        delete("/api/workspaces/" + workspaceId + "/goals/" + goalId, accessToken);
    }

    public BackendSavingsConfig getSavingsConfig(String accessToken, String workspaceId) throws IOException, InterruptedException {
        return toSavingsConfig(SimpleJson.asObject(SimpleJson.parse(
                get("/api/workspaces/" + workspaceId + "/savings/config", accessToken))));
    }

    public BackendSavingsConfig updateSavingsConfig(String accessToken, String workspaceId, boolean enabled,
                                                    BigDecimal percentage, LocalDate effectiveFrom)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("enabled", enabled);
        body.put("percentage", percentage);
        body.put("effectiveFrom", effectiveFrom == null ? null : effectiveFrom.toString());
        return toSavingsConfig(SimpleJson.asObject(SimpleJson.parse(
                put("/api/workspaces/" + workspaceId + "/savings/config", SimpleJson.stringify(body), accessToken))));
    }

    public BackendSavingsSummary getSavingsSummary(String accessToken, String workspaceId) throws IOException, InterruptedException {
        return toSavingsSummary(SimpleJson.asObject(SimpleJson.parse(
                get("/api/workspaces/" + workspaceId + "/savings/summary", accessToken))));
    }

    public BackendSavingsSummary getSavingsSummary(String accessToken, String workspaceId, LocalDate from, LocalDate to)
            throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/api/workspaces/")
                .append(workspaceId)
                .append("/savings/summary");
        String separator = "?";
        if (from != null) {
            path.append(separator).append("from=").append(from);
            separator = "&";
        }
        if (to != null) {
            path.append(separator).append("to=").append(to);
        }
        return toSavingsSummary(SimpleJson.asObject(SimpleJson.parse(get(path.toString(), accessToken))));
    }

    public BackendSavingsMovement depositSavings(String accessToken, String workspaceId, BigDecimal amount,
                                                 LocalDate effectiveDate, String note, String idempotencyKey)
            throws IOException, InterruptedException {
        return postSavingsMovement(accessToken, workspaceId, "/savings/deposits", amount, effectiveDate, note, idempotencyKey);
    }

    public BackendSavingsMovement withdrawSavings(String accessToken, String workspaceId, BigDecimal amount,
                                                  LocalDate effectiveDate, String note, String idempotencyKey)
            throws IOException, InterruptedException {
        return postSavingsMovement(accessToken, workspaceId, "/savings/withdrawals", amount, effectiveDate, note, idempotencyKey);
    }

    public BackendSavingsMovement allocateGoalSavings(String accessToken, String workspaceId, String goalId,
                                                      BigDecimal amount, String note, String idempotencyKey)
            throws IOException, InterruptedException {
        return postGoalSavingsMovement(accessToken, workspaceId, goalId, "allocations", amount, note, idempotencyKey);
    }

    public BackendSavingsMovement releaseGoalSavings(String accessToken, String workspaceId, String goalId,
                                                     BigDecimal amount, String note, String idempotencyKey)
            throws IOException, InterruptedException {
        return postGoalSavingsMovement(accessToken, workspaceId, goalId, "releases", amount, note, idempotencyKey);
    }

    public List<BackendRecurringTransaction> listRecurringTransactions(String accessToken, String workspaceId)
            throws IOException, InterruptedException {
        String response = get("/api/workspaces/" + workspaceId + "/recurring-transactions", accessToken);
        List<BackendRecurringTransaction> result = new ArrayList<BackendRecurringTransaction>();
        for (Object item : SimpleJson.asArray(SimpleJson.parse(response))) {
            result.add(toRecurringTransaction(SimpleJson.asObject(item)));
        }
        return result;
    }

    public BackendRecurringTransaction createRecurringTransaction(String accessToken, String workspaceId, String categoryId,
                                                                  String type, String description, BigDecimal amount,
                                                                  String frequency, int customIntervalDays,
                                                                  LocalDate nextRunDate)
            throws IOException, InterruptedException {
        Map<String, Object> body = recurringTransactionBody(categoryId, type, description, amount, frequency, customIntervalDays, nextRunDate);
        return toRecurringTransaction(SimpleJson.asObject(SimpleJson.parse(
                post("/api/workspaces/" + workspaceId + "/recurring-transactions", SimpleJson.stringify(body), accessToken))));
    }

    public void deactivateRecurringTransaction(String accessToken, String workspaceId, String recurringTransactionId)
            throws IOException, InterruptedException {
        delete("/api/workspaces/" + workspaceId + "/recurring-transactions/" + recurringTransactionId, accessToken);
    }

    public BackendTransaction runRecurringTransaction(String accessToken, String workspaceId, String recurringTransactionId)
            throws IOException, InterruptedException {
        return toTransaction(SimpleJson.asObject(SimpleJson.parse(
                post("/api/workspaces/" + workspaceId + "/recurring-transactions/" + recurringTransactionId + "/run", "{}", accessToken))));
    }

    public List<BackendNotification> refreshNotifications(String accessToken, String workspaceId)
            throws IOException, InterruptedException {
        String response = post("/api/workspaces/" + workspaceId + "/notifications/refresh", "{}", accessToken);
        List<BackendNotification> result = new ArrayList<BackendNotification>();
        for (Object item : SimpleJson.asArray(SimpleJson.parse(response))) {
            result.add(toNotification(SimpleJson.asObject(item)));
        }
        return result;
    }

    public List<BackendNotification> listNotifications(String accessToken, String workspaceId)
            throws IOException, InterruptedException {
        String response = get("/api/workspaces/" + workspaceId + "/notifications", accessToken);
        List<BackendNotification> result = new ArrayList<BackendNotification>();
        for (Object item : SimpleJson.asArray(SimpleJson.parse(response))) {
            result.add(toNotification(SimpleJson.asObject(item)));
        }
        return result;
    }

    public void markNotificationRead(String accessToken, String workspaceId, String notificationId)
            throws IOException, InterruptedException {
        patch("/api/workspaces/" + workspaceId + "/notifications/" + notificationId + "/read", "{}", accessToken);
    }

    public byte[] exportReportCsv(String accessToken, String workspaceId, LocalDate from, LocalDate to)
            throws IOException, InterruptedException {
        StringBuilder path = new StringBuilder("/api/workspaces/")
                .append(workspaceId)
                .append("/reports/export.csv");
        String separator = "?";
        if (from != null) {
            path.append(separator).append("from=").append(from);
            separator = "&";
        }
        if (to != null) {
            path.append(separator).append("to=").append(to);
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path.toString()))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "text/csv")
                .GET();
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return sendBytes(builder.build());
    }

    public BackendUser uploadAvatar(String accessToken, File sourceFile) throws IOException, InterruptedException {
        AvatarUpload upload = avatarUpload(sourceFile);
        String boundary = "FinanzasAvatarBoundary" + System.currentTimeMillis();
        byte[] body = multipartBody(boundary, upload.fileName(), upload.contentType(), upload.content());
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri("/api/users/me/avatar"))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return toUser(SimpleJson.asObject(SimpleJson.parse(send(builder.build()))));
    }

    public BackendAvatar downloadAvatar(String accessToken, String etag) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri("/api/users/me/avatar"))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "image/png")
                .GET();
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        if (etag != null && !etag.trim().isEmpty()) {
            builder.header("If-None-Match", etag);
        }
        HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 304) {
            return new BackendAvatar(new byte[0], firstHeader(response, "ETag", etag), firstHeader(response, "Last-Modified", ""), true);
        }
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return new BackendAvatar(response.body(), firstHeader(response, "ETag", ""), firstHeader(response, "Last-Modified", ""), false);
        }
        String body = response.body() == null ? "" : new String(response.body(), StandardCharsets.UTF_8);
        throw apiException(response.statusCode(), body);
    }

    public void deleteAvatar(String accessToken) throws IOException, InterruptedException {
        delete("/api/users/me/avatar", accessToken);
    }

    public List<BackendMember> listMembers(String accessToken, String workspaceId) throws IOException, InterruptedException {
        String response = get("/api/workspaces/" + workspaceId + "/members", accessToken);
        List<BackendMember> result = new ArrayList<BackendMember>();
        for (Object item : SimpleJson.asArray(SimpleJson.parse(response))) {
            result.add(toMember(SimpleJson.asObject(item)));
        }
        return result;
    }

    public BackendInvitation inviteMember(String accessToken, String workspaceId, String email, String role)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("email", email);
        body.put("role", role);
        return toInvitation(SimpleJson.asObject(SimpleJson.parse(
                post("/api/workspaces/" + workspaceId + "/invitations", SimpleJson.stringify(body), accessToken))));
    }

    public List<BackendInvitation> listWorkspaceInvitations(String accessToken, String workspaceId)
            throws IOException, InterruptedException {
        String response = get("/api/workspaces/" + workspaceId + "/invitations", accessToken);
        List<BackendInvitation> result = new ArrayList<BackendInvitation>();
        for (Object item : SimpleJson.asArray(SimpleJson.parse(response))) {
            result.add(toInvitation(SimpleJson.asObject(item)));
        }
        return result;
    }

    public void cancelInvitation(String accessToken, String workspaceId, String invitationId)
            throws IOException, InterruptedException {
        delete("/api/workspaces/" + workspaceId + "/invitations/" + invitationId, accessToken);
    }

    public List<BackendInvitation> listMyInvitations(String accessToken)
            throws IOException, InterruptedException {
        String response = get("/api/invitations/mine", accessToken);
        List<BackendInvitation> result = new ArrayList<BackendInvitation>();
        for (Object item : SimpleJson.asArray(SimpleJson.parse(response))) {
            result.add(toInvitation(SimpleJson.asObject(item)));
        }
        return result;
    }

    public BackendInvitation acceptInvitation(String accessToken, String invitationId)
            throws IOException, InterruptedException {
        return toInvitation(SimpleJson.asObject(SimpleJson.parse(
                post("/api/invitations/" + invitationId + "/accept", "{}", accessToken))));
    }

    public BackendInvitation rejectInvitation(String accessToken, String invitationId)
            throws IOException, InterruptedException {
        return toInvitation(SimpleJson.asObject(SimpleJson.parse(
                post("/api/invitations/" + invitationId + "/reject", "{}", accessToken))));
    }

    public void removeMember(String accessToken, String workspaceId, String memberId)
            throws IOException, InterruptedException {
        delete("/api/workspaces/" + workspaceId + "/members/" + memberId, accessToken);
    }

    public List<BackendSharedExpense> listSharedExpenses(String accessToken, String workspaceId)
            throws IOException, InterruptedException {
        String response = get("/api/workspaces/" + workspaceId + "/shared-expenses", accessToken);
        List<BackendSharedExpense> result = new ArrayList<BackendSharedExpense>();
        for (Object item : SimpleJson.asArray(SimpleJson.parse(response))) {
            result.add(toSharedExpense(SimpleJson.asObject(item)));
        }
        return result;
    }

    public BackendSharedExpense createSharedExpense(String accessToken, String workspaceId, String description,
                                                    BigDecimal amount, LocalDate date, String paidByUserId,
                                                    String categoryId, String splitMethod,
                                                    List<Map<String, Object>> participants)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("description", description);
        body.put("amount", amount);
        body.put("date", date == null ? null : date.toString());
        body.put("paidByUserId", paidByUserId);
        body.put("categoryId", categoryId);
        body.put("splitMethod", splitMethod);
        body.put("participants", participants);
        return toSharedExpense(SimpleJson.asObject(SimpleJson.parse(
                post("/api/workspaces/" + workspaceId + "/shared-expenses", SimpleJson.stringify(body), accessToken))));
    }

    public void cancelSharedExpense(String accessToken, String workspaceId, String sharedExpenseId)
            throws IOException, InterruptedException {
        delete("/api/workspaces/" + workspaceId + "/shared-expenses/" + sharedExpenseId, accessToken);
    }

    public List<BackendSettlement> listSettlements(String accessToken, String workspaceId)
            throws IOException, InterruptedException {
        String response = get("/api/workspaces/" + workspaceId + "/shared-expenses/settlements", accessToken);
        List<BackendSettlement> result = new ArrayList<BackendSettlement>();
        for (Object item : SimpleJson.asArray(SimpleJson.parse(response))) {
            result.add(toSettlement(SimpleJson.asObject(item)));
        }
        return result;
    }

    public BackendSettlement createSettlement(String accessToken, String workspaceId, String fromUserId,
                                              String toUserId, BigDecimal amount, LocalDate date, String note)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("fromUserId", fromUserId);
        body.put("toUserId", toUserId);
        body.put("amount", amount);
        body.put("date", date == null ? null : date.toString());
        body.put("note", note);
        return toSettlement(SimpleJson.asObject(SimpleJson.parse(
                post("/api/workspaces/" + workspaceId + "/shared-expenses/settlements", SimpleJson.stringify(body), accessToken))));
    }

    public void logout(String refreshToken) throws IOException, InterruptedException {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("refreshToken", refreshToken);
        post("/api/auth/logout", SimpleJson.stringify(body), null);
    }

    private String post(String path, String jsonBody, String accessToken) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return send(builder.build());
    }

    private String put(String path, String jsonBody, String accessToken) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return send(builder.build());
    }

    private String get(String path, String accessToken) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .GET();
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return send(builder.build());
    }

    private String patch(String path, String jsonBody, String accessToken) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody));
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return send(builder.build());
    }

    private void delete(String path, String accessToken) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .DELETE();
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        send(builder.build());
    }

    private String send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body() == null ? "" : response.body();
        }
        throw apiException(response.statusCode(), response.body());
    }

    private byte[] sendBytes(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body() == null ? new byte[0] : response.body();
        }
        String body = response.body() == null ? "" : new String(response.body(), java.nio.charset.StandardCharsets.UTF_8);
        throw apiException(response.statusCode(), body);
    }

    private BackendSession parseAuthResponse(String response) throws IOException, InterruptedException {
        Map<String, Object> root = SimpleJson.asObject(SimpleJson.parse(response));
        BackendUser user = toUser(SimpleJson.asObject(root.get("user")));
        String accessToken = SimpleJson.string(root, "accessToken");
        String refreshToken = SimpleJson.string(root, "refreshToken");
        List<BackendWorkspace> workspaces = accessToken.isEmpty() ? new ArrayList<BackendWorkspace>() : listWorkspaces(accessToken);
        return new BackendSession(accessToken, refreshToken, user, workspaces);
    }

    private BackendUser toUser(Map<String, Object> object) {
        return new BackendUser(
                SimpleJson.string(object, "id"),
                SimpleJson.string(object, "nombre"),
                SimpleJson.string(object, "apellido"),
                SimpleJson.string(object, "email"),
                SimpleJson.string(object, "moneda"),
                SimpleJson.string(object, "locale"),
                SimpleJson.string(object, "tipoCuenta"),
                SimpleJson.string(object, "avatarRef"));
    }

    private BackendWorkspace toWorkspace(Map<String, Object> object) {
        return new BackendWorkspace(
                SimpleJson.string(object, "id"),
                SimpleJson.string(object, "nombre"),
                SimpleJson.string(object, "tipo"),
                SimpleJson.string(object, "ownerId"),
                SimpleJson.string(object, "role"));
    }

    private BackendCategory toCategory(Map<String, Object> object) {
        return new BackendCategory(
                SimpleJson.string(object, "id"),
                SimpleJson.string(object, "workspaceId"),
                SimpleJson.string(object, "nombre"),
                SimpleJson.string(object, "type"),
                SimpleJson.string(object, "icono"),
                SimpleJson.string(object, "color"),
                SimpleJson.bool(object, "archived"));
    }

    private BackendTransaction toTransaction(Map<String, Object> object) throws IOException {
        String date = SimpleJson.string(object, "date");
        return new BackendTransaction(
                SimpleJson.string(object, "id"),
                SimpleJson.string(object, "workspaceId"),
                SimpleJson.string(object, "categoryId"),
                SimpleJson.string(object, "createdByUserId"),
                SimpleJson.string(object, "type"),
                SimpleJson.string(object, "description"),
                SimpleJson.decimal(object, "amount"),
                date.isEmpty() ? LocalDate.now() : LocalDate.parse(date));
    }

    private BackendBudget toBudget(Map<String, Object> object) throws IOException {
        String periodMonth = SimpleJson.string(object, "periodMonth");
        return new BackendBudget(
                SimpleJson.string(object, "id"),
                SimpleJson.string(object, "workspaceId"),
                SimpleJson.string(object, "categoryId"),
                periodMonth.isEmpty() ? LocalDate.now().withDayOfMonth(1) : LocalDate.parse(periodMonth),
                SimpleJson.decimal(object, "amount"));
    }

    private BackendSavingsGoal toSavingsGoal(Map<String, Object> object) throws IOException {
        String dueDate = SimpleJson.string(object, "dueDate");
        return new BackendSavingsGoal(
                SimpleJson.string(object, "id"),
                SimpleJson.string(object, "workspaceId"),
                SimpleJson.string(object, "name"),
                SimpleJson.decimal(object, "currentAmount"),
                SimpleJson.decimal(object, "targetAmount"),
                SimpleJson.string(object, "color"),
                SimpleJson.string(object, "icono"),
                dueDate.isEmpty() ? null : LocalDate.parse(dueDate),
                SimpleJson.string(object, "status"));
    }

    private BackendSavingsConfig toSavingsConfig(Map<String, Object> object) throws IOException {
        String effectiveFrom = SimpleJson.string(object, "effectiveFrom");
        return new BackendSavingsConfig(
                SimpleJson.string(object, "workspaceId"),
                SimpleJson.bool(object, "enabled"),
                SimpleJson.string(object, "allocationMode"),
                SimpleJson.decimal(object, "percentage"),
                effectiveFrom.isEmpty() ? LocalDate.now() : LocalDate.parse(effectiveFrom),
                longValue(object, "version", 0L));
    }

    private BackendSavingsSummary toSavingsSummary(Map<String, Object> object) throws IOException {
        return new BackendSavingsSummary(
                SimpleJson.decimal(object, "ingresos"),
                SimpleJson.decimal(object, "gastos"),
                SimpleJson.decimal(object, "ahorroTotal"),
                SimpleJson.decimal(object, "ahorroAsignadoAMetas"),
                SimpleJson.decimal(object, "ahorroLibre"),
                SimpleJson.decimal(object, "retirosAcumulados"),
                SimpleJson.decimal(object, "saldoDisponibleNoAhorrado"),
                SimpleJson.decimal(object, "tasaAhorro"));
    }

    private BackendSavingsMovement toSavingsMovement(Map<String, Object> object) throws IOException {
        String effectiveDate = SimpleJson.string(object, "effectiveDate");
        return new BackendSavingsMovement(
                SimpleJson.string(object, "id"),
                SimpleJson.string(object, "workspaceId"),
                SimpleJson.string(object, "sourceTransactionId"),
                SimpleJson.string(object, "goalId"),
                SimpleJson.string(object, "type"),
                SimpleJson.string(object, "direction"),
                SimpleJson.decimal(object, "amount"),
                effectiveDate.isEmpty() ? LocalDate.now() : LocalDate.parse(effectiveDate),
                SimpleJson.string(object, "note"));
    }

    private BackendRecurringTransaction toRecurringTransaction(Map<String, Object> object) throws IOException {
        String nextRunDate = SimpleJson.string(object, "nextRunDate");
        return new BackendRecurringTransaction(
                SimpleJson.string(object, "id"),
                SimpleJson.string(object, "workspaceId"),
                SimpleJson.string(object, "categoryId"),
                SimpleJson.string(object, "type"),
                SimpleJson.string(object, "description"),
                SimpleJson.decimal(object, "amount"),
                SimpleJson.string(object, "frequency"),
                intValue(object, "customIntervalDays", 1),
                nextRunDate.isEmpty() ? LocalDate.now() : LocalDate.parse(nextRunDate),
                SimpleJson.bool(object, "active"));
    }

    private BackendNotification toNotification(Map<String, Object> object) {
        return new BackendNotification(
                SimpleJson.string(object, "id"),
                SimpleJson.string(object, "workspaceId"),
                SimpleJson.string(object, "type"),
                SimpleJson.string(object, "title"),
                SimpleJson.string(object, "body"));
    }

    private BackendMember toMember(Map<String, Object> object) {
        return new BackendMember(
                SimpleJson.string(object, "userId"),
                SimpleJson.string(object, "nombre"),
                SimpleJson.string(object, "apellido"),
                SimpleJson.string(object, "email"),
                SimpleJson.string(object, "role"));
    }

    private BackendInvitation toInvitation(Map<String, Object> object) {
        return new BackendInvitation(
                SimpleJson.string(object, "id"),
                SimpleJson.string(object, "workspaceId"),
                SimpleJson.string(object, "workspaceName"),
                SimpleJson.string(object, "invitedEmail"),
                SimpleJson.string(object, "invitedByUserId"),
                SimpleJson.string(object, "role"),
                SimpleJson.string(object, "status"),
                SimpleJson.string(object, "expiresAt"),
                SimpleJson.string(object, "createdAt"));
    }

    private BackendSharedExpense toSharedExpense(Map<String, Object> object) throws IOException {
        String date = SimpleJson.string(object, "date");
        List<BackendExpenseSplit> splits = new ArrayList<BackendExpenseSplit>();
        Object rawSplits = object.get("splits");
        if (rawSplits != null) {
            for (Object item : SimpleJson.asArray(rawSplits)) {
                splits.add(toExpenseSplit(SimpleJson.asObject(item)));
            }
        }
        return new BackendSharedExpense(
                SimpleJson.string(object, "id"),
                SimpleJson.string(object, "workspaceId"),
                SimpleJson.string(object, "paidByUserId"),
                SimpleJson.string(object, "paidByName"),
                SimpleJson.string(object, "categoryId"),
                SimpleJson.string(object, "description"),
                SimpleJson.decimal(object, "amount"),
                date.isEmpty() ? LocalDate.now() : LocalDate.parse(date),
                SimpleJson.string(object, "splitMethod"),
                SimpleJson.string(object, "status"),
                splits);
    }

    private BackendExpenseSplit toExpenseSplit(Map<String, Object> object) throws IOException {
        return new BackendExpenseSplit(
                SimpleJson.string(object, "userId"),
                SimpleJson.string(object, "nombre"),
                SimpleJson.string(object, "email"),
                SimpleJson.decimal(object, "amount"),
                SimpleJson.decimal(object, "percentage"));
    }

    private BackendSettlement toSettlement(Map<String, Object> object) throws IOException {
        String date = SimpleJson.string(object, "date");
        return new BackendSettlement(
                SimpleJson.string(object, "id"),
                SimpleJson.string(object, "workspaceId"),
                SimpleJson.string(object, "fromUserId"),
                SimpleJson.string(object, "fromName"),
                SimpleJson.string(object, "toUserId"),
                SimpleJson.string(object, "toName"),
                SimpleJson.decimal(object, "amount"),
                date.isEmpty() ? LocalDate.now() : LocalDate.parse(date),
                SimpleJson.string(object, "note"));
    }

    private Map<String, Object> transactionBody(String categoryId, String type, String description, BigDecimal amount, LocalDate date) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("categoryId", categoryId);
        body.put("type", type);
        body.put("description", description);
        body.put("amount", amount);
        body.put("date", date == null ? LocalDate.now().toString() : date.toString());
        return body;
    }

    private Map<String, Object> budgetBody(String categoryId, LocalDate periodMonth, BigDecimal amount) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("categoryId", categoryId);
        body.put("periodMonth", (periodMonth == null ? LocalDate.now() : periodMonth).withDayOfMonth(1).toString());
        body.put("amount", amount);
        return body;
    }

    private Map<String, Object> savingsGoalBody(String name, BigDecimal currentAmount, BigDecimal targetAmount,
                                                String color, String icono, LocalDate dueDate) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("name", name);
        body.put("currentAmount", currentAmount);
        body.put("targetAmount", targetAmount);
        body.put("color", color);
        body.put("icono", icono);
        body.put("dueDate", dueDate == null ? null : dueDate.toString());
        return body;
    }

    private Map<String, Object> recurringTransactionBody(String categoryId, String type, String description,
                                                         BigDecimal amount, String frequency, int customIntervalDays,
                                                         LocalDate nextRunDate) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("categoryId", categoryId);
        body.put("type", type);
        body.put("description", description);
        body.put("amount", amount);
        body.put("frequency", frequency);
        body.put("customIntervalDays", Math.max(1, customIntervalDays));
        body.put("nextRunDate", nextRunDate == null ? LocalDate.now().toString() : nextRunDate.toString());
        return body;
    }

    private BackendSavingsMovement postSavingsMovement(String accessToken, String workspaceId, String suffix,
                                                       BigDecimal amount, LocalDate effectiveDate, String note,
                                                       String idempotencyKey)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("amount", amount);
        body.put("effectiveDate", effectiveDate == null ? null : effectiveDate.toString());
        body.put("note", note);
        body.put("idempotencyKey", idempotencyKey);
        return toSavingsMovement(SimpleJson.asObject(SimpleJson.parse(
                post("/api/workspaces/" + workspaceId + suffix, SimpleJson.stringify(body), accessToken))));
    }

    private BackendSavingsMovement postGoalSavingsMovement(String accessToken, String workspaceId, String goalId,
                                                           String action, BigDecimal amount, String note,
                                                           String idempotencyKey)
            throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("amount", amount);
        body.put("note", note);
        body.put("idempotencyKey", idempotencyKey);
        return toSavingsMovement(SimpleJson.asObject(SimpleJson.parse(
                post("/api/workspaces/" + workspaceId + "/savings/goals/" + goalId + "/" + action,
                        SimpleJson.stringify(body), accessToken))));
    }

    private int intValue(Map<String, Object> object, String key, int fallback) {
        Object value = object.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private long longValue(Map<String, Object> object, String key, long fallback) {
        Object value = object.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }

    private String errorMessage(int statusCode, String body) {
        String detail = errorDetail(body);
        if (detail == null || detail.trim().isEmpty()) {
            detail = "HTTP " + statusCode;
        }
        return "Backend FinanzasApp: " + detail;
    }

    private BackendApiException apiException(int statusCode, String body) {
        return new BackendApiException(statusCode, errorMessage(statusCode, body), errorCode(body));
    }

    private String errorDetail(String body) {
        String detail = "";
        try {
            Object parsed = SimpleJson.parse(body);
            if (parsed instanceof Map) {
                Map<String, Object> object = SimpleJson.asObject(parsed);
                detail = firstNonBlank(
                        SimpleJson.string(object, "detail"),
                        SimpleJson.string(object, "message"),
                        SimpleJson.string(object, "error"));
            }
        } catch (Exception ex) {
            detail = body == null ? "" : body;
        }
        if (detail == null || detail.trim().isEmpty()) {
            detail = "";
        }
        return detail;
    }

    private String errorCode(String body) {
        try {
            Object parsed = SimpleJson.parse(body);
            if (parsed instanceof Map) {
                Map<String, Object> object = SimpleJson.asObject(parsed);
                return SimpleJson.string(object, "errorCode");
            }
        } catch (Exception ex) {
            return "";
        }
        return "";
    }

    private String[] splitName(String nombreCompleto) {
        String safe = nombreCompleto == null ? "" : nombreCompleto.trim();
        String[] parts = safe.split("\\s+", 2);
        return new String[]{parts.length > 0 ? parts[0] : "", parts.length > 1 ? parts[1] : ""};
    }

    private String normalizeAccountType(String tipoCuenta) {
        if (tipoCuenta == null) {
            return "PERSONAL";
        }
        if ("Hogar".equalsIgnoreCase(tipoCuenta) || "HOUSEHOLD".equalsIgnoreCase(tipoCuenta)) {
            return "HOUSEHOLD";
        }
        if ("Negocio".equalsIgnoreCase(tipoCuenta) || "BUSINESS".equalsIgnoreCase(tipoCuenta)) {
            return "BUSINESS";
        }
        return "PERSONAL";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private AvatarUpload avatarUpload(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.isFile()) {
            throw new IOException("El archivo seleccionado no existe.");
        }
        long size = Files.size(sourceFile.toPath());
        if (size > MAX_AVATAR_BYTES) {
            throw new IOException("La imagen supera el limite de 5 MB.");
        }
        String safeName = sourceFile.getName() == null ? "avatar.png" : sourceFile.getName().replaceAll("[\\r\\n\"]", "_");
        String lower = safeName.toLowerCase(java.util.Locale.ROOT);
        String contentType;
        if (lower.endsWith(".png")) {
            contentType = "image/png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else {
            throw new IOException("Formato no permitido. Usa PNG, JPG o JPEG.");
        }
        return new AvatarUpload(safeName, contentType, Files.readAllBytes(sourceFile.toPath()));
    }

    private byte[] multipartBody(String boundary, String fileName, String contentType, byte[] content) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        output.write(header.getBytes(StandardCharsets.UTF_8));
        output.write(content);
        output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private String firstHeader(HttpResponse<?> response, String name, String fallback) {
        return response.headers().firstValue(name).orElse(fallback == null ? "" : fallback);
    }

    private static final class AvatarUpload {
        private final String fileName;
        private final String contentType;
        private final byte[] content;

        AvatarUpload(String fileName, String contentType, byte[] content) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.content = content;
        }

        String fileName() { return fileName; }
        String contentType() { return contentType; }
        byte[] content() { return content; }
    }
}
