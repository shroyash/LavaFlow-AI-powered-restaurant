package com.lavaflow.common.storage;

import com.lavaflow.common.exception.InvalidUploadException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Component
public class UploadedFileValidator {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int HEADER_LENGTH = 8;

    public AllowedFileType validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("Uploaded file is empty.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidUploadException("Each file must be 5 MB or smaller.");
        }

        byte[] header = readHeader(file);

        return Arrays.stream(AllowedFileType.values())
                .filter(type -> type.matches(header))
                .findFirst()
                .orElseThrow(() -> new InvalidUploadException("Only PDF, JPG and PNG files are allowed."));
    }

    private byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(HEADER_LENGTH);
        } catch (IOException e) {
            throw new InvalidUploadException("Could not read uploaded file.");
        }
    }
}