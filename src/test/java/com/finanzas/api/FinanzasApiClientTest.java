package com.finanzas.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanzasApiClientTest {
    private HttpServer server;
    private List<String> requests;

    @BeforeEach
    void setUp() throws IOException {
        requests = new ArrayList<String>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void workspaceAndInvitationEndpointsRoundTrip() throws Exception {
        FinanzasApiClient client = new FinanzasApiClient("http://127.0.0.1:" + server.getAddress().getPort());

        List<BackendWorkspace> workspaces = client.listWorkspaces("token");
        BackendWorkspace created = client.createWorkspace("token", "Casa", "HOUSEHOLD");
        BackendInvitation sent = client.inviteMember("token", "ws-1", "ana@example.com", "MEMBER");
        List<BackendInvitation> workspaceInvitations = client.listWorkspaceInvitations("token", "ws-1");
        List<BackendInvitation> mine = client.listMyInvitations("token");
        BackendMember changedMember = client.changeMemberRole("token", "ws-1", "u-2", "VIEWER");
        BackendWorkspace transferred = client.transferWorkspaceOwner("token", "ws-1", "u-2");
        client.leaveWorkspace("token", "ws-1");
        BackendInvitation accepted = client.acceptInvitation("token", "inv-1");
        BackendInvitation rejected = client.rejectInvitation("token", "inv-1");
        client.cancelInvitation("token", "ws-1", "inv-1");
        client.requestEmailVerification("ana@example.com");
        BackendUser verified = client.confirmEmailVerification("verify-token");
        client.requestPasswordReset("ana@example.com");
        client.confirmPasswordReset("reset-token", "nueva-segura");
        client.changePassword("token", "actual", "nueva-segura");
        List<BackendSessionInfo> sessions = client.listSessions("token");
        client.revokeSession("token", "session-1");
        client.revokeAllSessions("token");
        String accountExport = client.exportAccount("token");
        client.deleteAccount("token", "ana@example.com", "actual");
        BackendUser currentUser = client.getCurrentUser("token");
        BackendUser updatedUser = client.updateCurrentUser("token", "Ana", "Lopez", "ana@example.com",
                "Medellin", "Colombia", "USD", "en-US");
        BackendUserSettings settings = client.getUserSettings("token");
        BackendUserSettings updatedSettings = client.updateUserSettings("token", "DARK", "en-US", "UTC",
                "CODE_SUFFIX", false, true, false);
        BackendSyncState syncState = client.getSyncChanges("token", "ws-1", java.time.Instant.parse("2026-09-01T00:00:00Z"));

        assertEquals("ws-1", workspaces.get(0).getId());
        assertEquals("Casa", created.getNombre());
        assertEquals("ana@example.com", sent.getInvitedEmail());
        assertEquals(1, workspaceInvitations.size());
        assertEquals("Casa", mine.get(0).getWorkspaceName());
        assertEquals("VIEWER", changedMember.getRole());
        assertEquals("u-2", transferred.getOwnerId());
        assertEquals("ACCEPTED", accepted.getStatus());
        assertEquals("REJECTED", rejected.getStatus());
        assertTrue(verified.isEmailVerified());
        assertEquals("session-1", sessions.get(0).getId());
        assertTrue(accountExport.contains("\"profile\""));
        assertEquals("Medellin", currentUser.getCiudad());
        assertEquals("Colombia", updatedUser.getPais());
        assertEquals("LIGHT", settings.getTheme());
        assertEquals("DARK", updatedSettings.getTheme());
        assertEquals(3L, updatedSettings.getVersion());
        assertEquals("ws-1", syncState.getWorkspaceId());
        assertEquals(1, syncState.getPendingReceivedInvitations());
        assertEquals(1, syncState.getChangedResources().size());
        assertEquals("transactions", syncState.getChangedResources().get(0));
        assertTrue(requests.contains("DELETE /api/workspaces/ws-1/invitations/inv-1"));
        assertTrue(requests.contains("POST /api/workspaces/ws-1/owner"));
        assertTrue(requests.contains("DELETE /api/workspaces/ws-1/membership"));
        assertTrue(requests.contains("POST /api/auth/password/reset/confirm"));
        assertTrue(requests.contains("PATCH /api/auth/password"));
        assertTrue(requests.contains("DELETE /api/auth/sessions/session-1"));
        assertTrue(requests.contains("DELETE /api/auth/sessions"));
        assertTrue(requests.contains("GET /api/users/me/export"));
        assertTrue(requests.contains("DELETE /api/users/me"));
        assertTrue(requests.contains("PATCH /api/users/me/settings"));
        assertTrue(requests.contains("GET /api/workspaces/ws-1/sync/changes"));
    }

    private void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        requests.add(method + " " + path);
        exchange.getRequestBody().readAllBytes();

        if ("GET".equals(method) && "/api/workspaces".equals(path)) {
            respond(exchange, 200, "[{\"id\":\"ws-1\",\"nombre\":\"Casa\",\"tipo\":\"HOUSEHOLD\",\"ownerId\":\"u-1\",\"role\":\"OWNER\"}]");
            return;
        }
        if ("POST".equals(method) && "/api/workspaces".equals(path)) {
            respond(exchange, 201, "{\"id\":\"ws-2\",\"nombre\":\"Casa\",\"tipo\":\"HOUSEHOLD\",\"ownerId\":\"u-1\",\"role\":\"OWNER\"}");
            return;
        }
        if ("POST".equals(method) && "/api/workspaces/ws-1/invitations".equals(path)) {
            respond(exchange, 201, invitation("PENDING"));
            return;
        }
        if ("GET".equals(method) && "/api/workspaces/ws-1/invitations".equals(path)) {
            respond(exchange, 200, "[" + invitation("PENDING") + "]");
            return;
        }
        if ("DELETE".equals(method) && "/api/workspaces/ws-1/invitations/inv-1".equals(path)) {
            respond(exchange, 204, "");
            return;
        }
        if ("PATCH".equals(method) && "/api/workspaces/ws-1/members/u-2".equals(path)) {
            respond(exchange, 200, "{\"userId\":\"u-2\",\"nombre\":\"Ana\",\"apellido\":\"Lopez\",\"email\":\"ana@example.com\",\"role\":\"VIEWER\"}");
            return;
        }
        if ("POST".equals(method) && "/api/workspaces/ws-1/owner".equals(path)) {
            respond(exchange, 200, "{\"id\":\"ws-1\",\"nombre\":\"Casa\",\"tipo\":\"HOUSEHOLD\",\"ownerId\":\"u-2\",\"role\":\"ADMIN\"}");
            return;
        }
        if ("DELETE".equals(method) && "/api/workspaces/ws-1/membership".equals(path)) {
            respond(exchange, 204, "");
            return;
        }
        if ("GET".equals(method) && "/api/invitations/mine".equals(path)) {
            respond(exchange, 200, "[" + invitation("PENDING") + "]");
            return;
        }
        if ("POST".equals(method) && "/api/invitations/inv-1/accept".equals(path)) {
            respond(exchange, 200, invitation("ACCEPTED"));
            return;
        }
        if ("POST".equals(method) && "/api/invitations/inv-1/reject".equals(path)) {
            respond(exchange, 200, invitation("REJECTED"));
            return;
        }
        if ("POST".equals(method) && "/api/auth/email/verification/request".equals(path)) {
            respond(exchange, 204, "");
            return;
        }
        if ("POST".equals(method) && "/api/auth/email/verification/confirm".equals(path)) {
            respond(exchange, 200, user());
            return;
        }
        if ("POST".equals(method) && "/api/auth/password/reset/request".equals(path)) {
            respond(exchange, 204, "");
            return;
        }
        if ("POST".equals(method) && "/api/auth/password/reset/confirm".equals(path)) {
            respond(exchange, 204, "");
            return;
        }
        if ("PATCH".equals(method) && "/api/auth/password".equals(path)) {
            respond(exchange, 204, "");
            return;
        }
        if ("GET".equals(method) && "/api/auth/sessions".equals(path)) {
            respond(exchange, 200, "[{\"id\":\"session-1\",\"createdAt\":\"2026-09-01T00:00:00Z\",\"lastUsedAt\":\"2026-09-01T01:00:00Z\",\"expiresAt\":\"2026-09-15T00:00:00Z\"}]");
            return;
        }
        if ("DELETE".equals(method) && "/api/auth/sessions/session-1".equals(path)) {
            respond(exchange, 204, "");
            return;
        }
        if ("DELETE".equals(method) && "/api/auth/sessions".equals(path)) {
            respond(exchange, 204, "");
            return;
        }
        if ("GET".equals(method) && "/api/users/me/export".equals(path)) {
            respond(exchange, 200, "{\"profile\":" + user() + ",\"workspaces\":[]}");
            return;
        }
        if ("DELETE".equals(method) && "/api/users/me".equals(path)) {
            respond(exchange, 204, "");
            return;
        }
        if ("GET".equals(method) && "/api/users/me".equals(path)) {
            respond(exchange, 200, user());
            return;
        }
        if ("PATCH".equals(method) && "/api/users/me".equals(path)) {
            respond(exchange, 200, user("Medellin", "Colombia", "USD", "en-US"));
            return;
        }
        if ("GET".equals(method) && "/api/users/me/settings".equals(path)) {
            respond(exchange, 200, settings("LIGHT", "es-CO", "America/Bogota", "SYMBOL_GROUP_DECIMAL", true, true, true, 2));
            return;
        }
        if ("PATCH".equals(method) && "/api/users/me/settings".equals(path)) {
            respond(exchange, 200, settings("DARK", "en-US", "UTC", "CODE_SUFFIX", false, true, false, 3));
            return;
        }
        if ("GET".equals(method) && "/api/workspaces/ws-1/sync/changes".equals(path)) {
            respond(exchange, 200, "{\"workspaceId\":\"ws-1\",\"revision\":\"2026-09-01T01:00:00Z\","
                    + "\"serverTime\":\"2026-09-01T01:00:00Z\",\"changedResources\":[\"transactions\"],"
                    + "\"pendingReceivedInvitations\":1,\"pendingWorkspaceInvitations\":0,\"unreadNotifications\":0}");
            return;
        }
        respond(exchange, 404, "{\"detail\":\"not found\"}");
    }

    private String invitation(String status) {
        return "{\"id\":\"inv-1\",\"workspaceId\":\"ws-1\",\"workspaceName\":\"Casa\","
                + "\"invitedEmail\":\"ana@example.com\",\"invitedByUserId\":\"u-1\","
                + "\"role\":\"MEMBER\",\"status\":\"" + status + "\","
                + "\"expiresAt\":\"2026-09-14T00:00:00Z\",\"createdAt\":\"2026-09-01T00:00:00Z\"}";
    }

    private String user() {
        return user("Medellin", "Colombia", "COP", "es-CO");
    }

    private String user(String ciudad, String pais, String moneda, String locale) {
        return "{\"id\":\"u-1\",\"nombre\":\"Ana\",\"apellido\":\"Lopez\",\"email\":\"ana@example.com\","
                + "\"ciudad\":\"" + ciudad + "\",\"pais\":\"" + pais + "\","
                + "\"moneda\":\"" + moneda + "\",\"locale\":\"" + locale + "\",\"tipoCuenta\":\"PERSONAL\","
                + "\"avatarRef\":\"\",\"emailVerified\":true}";
    }

    private String settings(String theme, String locale, String timeZone, String moneyFormat,
                            boolean notifPresupuesto, boolean notifMetas, boolean notifConsejos, long version) {
        return "{\"theme\":\"" + theme + "\",\"locale\":\"" + locale + "\","
                + "\"timeZone\":\"" + timeZone + "\",\"moneyFormat\":\"" + moneyFormat + "\","
                + "\"notifPresupuesto\":" + notifPresupuesto + ","
                + "\"notifMetas\":" + notifMetas + ","
                + "\"notifConsejos\":" + notifConsejos + ","
                + "\"version\":" + version + "}";
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }
}
