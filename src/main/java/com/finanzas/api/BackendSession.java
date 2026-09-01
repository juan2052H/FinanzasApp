package com.finanzas.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BackendSession {
    private final String accessToken;
    private final String refreshToken;
    private final BackendUser user;
    private final List<BackendWorkspace> workspaces;

    public BackendSession(String accessToken, String refreshToken, BackendUser user, List<BackendWorkspace> workspaces) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
        this.workspaces = workspaces == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<BackendWorkspace>(workspaces));
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public BackendUser getUser() { return user; }
    public List<BackendWorkspace> getWorkspaces() { return workspaces; }
}
