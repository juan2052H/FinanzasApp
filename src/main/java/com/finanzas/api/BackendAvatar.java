package com.finanzas.api;

public final class BackendAvatar {
    private final byte[] content;
    private final String etag;
    private final String lastModified;
    private final boolean notModified;

    public BackendAvatar(byte[] content, String etag, String lastModified, boolean notModified) {
        this.content = content == null ? new byte[0] : content.clone();
        this.etag = etag == null ? "" : etag;
        this.lastModified = lastModified == null ? "" : lastModified;
        this.notModified = notModified;
    }

    public byte[] getContent() {
        return content.clone();
    }

    public String getEtag() {
        return etag;
    }

    public String getLastModified() {
        return lastModified;
    }

    public boolean isNotModified() {
        return notModified;
    }
}
