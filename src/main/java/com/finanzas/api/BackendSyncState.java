package com.finanzas.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BackendSyncState {
    private final String workspaceId;
    private final Instant revision;
    private final Instant serverTime;
    private final List<String> changedResources;
    private final int pendingReceivedInvitations;
    private final int pendingWorkspaceInvitations;
    private final int unreadNotifications;

    public BackendSyncState(String workspaceId,
                            Instant revision,
                            Instant serverTime,
                            List<String> changedResources,
                            int pendingReceivedInvitations,
                            int pendingWorkspaceInvitations,
                            int unreadNotifications) {
        this.workspaceId = workspaceId == null ? "" : workspaceId;
        this.revision = revision;
        this.serverTime = serverTime;
        this.changedResources = changedResources == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(changedResources));
        this.pendingReceivedInvitations = pendingReceivedInvitations;
        this.pendingWorkspaceInvitations = pendingWorkspaceInvitations;
        this.unreadNotifications = unreadNotifications;
    }

    public String getWorkspaceId() { return workspaceId; }
    public Instant getRevision() { return revision; }
    public Instant getServerTime() { return serverTime; }
    public List<String> getChangedResources() { return changedResources; }
    public int getPendingReceivedInvitations() { return pendingReceivedInvitations; }
    public int getPendingWorkspaceInvitations() { return pendingWorkspaceInvitations; }
    public int getUnreadNotifications() { return unreadNotifications; }
}
