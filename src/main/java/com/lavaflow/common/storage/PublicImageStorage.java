package com.lavaflow.common.storage;

import com.lavaflow.common.exception.InvalidUploadException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class PublicImageStorage {

    private static final String STORAGE_PREFIX = "public/";
    private static final String URL_PREFIX = "/media/";

    private final FileStorageService fileStorageService;
    private final UploadedFileValidator uploadedFileValidator;

    public String store(String subdirectory, MultipartFile file) {
        AllowedFileType type = uploadedFileValidator.validate(file);
        if (type == AllowedFileType.PDF) {
            throw new InvalidUploadException("Only JPG and PNG images are allowed.");
        }

        String storageKey = fileStorageService.store(file, STORAGE_PREFIX + subdirectory, type.getExtension());
        String publicPath = URL_PREFIX + storageKey.substring(STORAGE_PREFIX.length());

        deleteWhen(publicPath, false);
        return publicPath;
    }

    public void deleteOnCommit(String publicPath) {
        if (publicPath == null) {
            return;
        }
        deleteWhen(publicPath, true);
    }

    private void deleteWhen(String publicPath, boolean onCommit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (onCommit) {
                delete(publicPath);
            }
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                boolean committed = status == STATUS_COMMITTED;
                if (committed == onCommit) {
                    delete(publicPath);
                }
            }
        });
    }

    private void delete(String publicPath) {
        if (publicPath.startsWith(URL_PREFIX)) {
            fileStorageService.delete(STORAGE_PREFIX + publicPath.substring(URL_PREFIX.length()));
        }
    }
}