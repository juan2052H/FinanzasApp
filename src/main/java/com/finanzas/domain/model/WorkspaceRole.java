package com.finanzas.domain.model;

public enum WorkspaceRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER;

    public boolean canWriteFinancialData() {
        return this == OWNER || this == ADMIN || this == MEMBER;
    }

    public boolean canManageMembers() {
        return this == OWNER || this == ADMIN;
    }

    public boolean canTransferOwnership() {
        return this == OWNER;
    }
}
