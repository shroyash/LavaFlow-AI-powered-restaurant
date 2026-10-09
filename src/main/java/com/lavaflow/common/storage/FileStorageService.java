package com.lavaflow.common.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String store(MultipartFile file, String directory, String extension);

    Resource load(String path);

    void delete(String path);
}