package com.lavaflow.common.storage;

import com.lavaflow.common.exception.InvalidUploadException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final Path basePath;

    public LocalFileStorageService(@Value("${lavaflow.storage.base-path:uploads}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, String directory, String extension) {
        String relativePath = directory + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(relativePath);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file.", e);
        }

        return relativePath;
    }

    @Override
    public Resource load(String path) {
        Path target = resolve(path);
        if (!Files.exists(target)) {
            throw new EntityNotFoundException("File not found.");
        }
        return new FileSystemResource(target);
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(resolve(path));
        } catch (IOException e) {
            log.warn("Could not delete file {}", path, e);
        }
    }

    private Path resolve(String relativePath) {
        Path target = basePath.resolve(relativePath).normalize();
        if (!target.startsWith(basePath)) {
            throw new InvalidUploadException("Invalid file path.");
        }
        return target;
    }
}