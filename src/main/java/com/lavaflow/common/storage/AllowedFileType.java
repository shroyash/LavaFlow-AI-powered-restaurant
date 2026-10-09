package com.lavaflow.common.storage;

import lombok.Getter;

@Getter
public enum AllowedFileType {

    PDF("application/pdf", "pdf", new int[]{0x25, 0x50, 0x44, 0x46}),
    JPEG("image/jpeg", "jpg", new int[]{0xFF, 0xD8, 0xFF}),
    PNG("image/png", "png", new int[]{0x89, 0x50, 0x4E, 0x47});

    private final String contentType;
    private final String extension;
    private final int[] signature;

    AllowedFileType(String contentType, String extension, int[] signature) {
        this.contentType = contentType;
        this.extension = extension;
        this.signature = signature;
    }

    public boolean matches(byte[] header) {
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((header[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}