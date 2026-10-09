package com.lavaflow.restaurant.service;

import com.lavaflow.common.enums.RestaurantDocumentType;
import com.lavaflow.common.exception.InvalidUploadException;
import com.lavaflow.common.storage.AllowedFileType;
import com.lavaflow.common.storage.FileStorageService;
import com.lavaflow.common.storage.UploadedFileValidator;
import com.lavaflow.restaurant.dto.RestaurantDocumentDownload;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.entity.RestaurantDocument;
import com.lavaflow.restaurant.repository.RestaurantDocumentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantDocumentService {

    private static final int MAX_DOCUMENTS = 5;
    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final RestaurantDocumentRepository restaurantDocumentRepository;
    private final FileStorageService fileStorageService;
    private final UploadedFileValidator uploadedFileValidator;

    @Transactional
    public void storeDocuments(
            Restaurant restaurant,
            List<MultipartFile> files,
            List<RestaurantDocumentType> types
    ) {
        validateSubmission(files, types);

        List<AllowedFileType> detectedTypes = files.stream()
                .map(uploadedFileValidator::validate)
                .toList();

        List<String> storedPaths = new ArrayList<>();
        deleteStoredFilesOnRollback(storedPaths);

        String directory = "restaurants/" + restaurant.getId() + "/documents";
        List<RestaurantDocument> documents = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            AllowedFileType detected = detectedTypes.get(i);

            String storedPath = fileStorageService.store(file, directory, detected.getExtension());
            storedPaths.add(storedPath);

            RestaurantDocument document = new RestaurantDocument();
            document.setRestaurant(restaurant);
            document.setDocumentType(types.get(i));
            document.setOriginalFileName(safeFileName(file.getOriginalFilename()));
            document.setStoredPath(storedPath);
            document.setContentType(detected.getContentType());
            document.setFileSize(file.getSize());
            documents.add(document);
        }

        restaurantDocumentRepository.saveAll(documents);
    }

    @Transactional(readOnly = true)
    public List<RestaurantDocument> getDocuments(UUID restaurantId) {
        return restaurantDocumentRepository.findByRestaurantIdOrderByCreatedAtAsc(restaurantId);
    }

    @Transactional(readOnly = true)
    public RestaurantDocumentDownload getDownload(UUID restaurantId, UUID documentId) {
        RestaurantDocument document = restaurantDocumentRepository
                .findByIdAndRestaurantId(documentId, restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found."));

        return new RestaurantDocumentDownload(
                fileStorageService.load(document.getStoredPath()),
                document.getContentType(),
                document.getOriginalFileName()
        );
    }

    private void validateSubmission(List<MultipartFile> files, List<RestaurantDocumentType> types) {
        if (files == null || files.isEmpty()) {
            throw new InvalidUploadException("At least one verification document is required.");
        }
        if (files.size() > MAX_DOCUMENTS) {
            throw new InvalidUploadException("You can upload at most " + MAX_DOCUMENTS + " documents.");
        }
        if (types == null || types.size() != files.size() || types.contains(null)) {
            throw new InvalidUploadException("Each document must have a document type.");
        }
        if (!types.contains(RestaurantDocumentType.BUSINESS_REGISTRATION)) {
            throw new InvalidUploadException("A business registration document is required.");
        }
    }

    private void deleteStoredFilesOnRollback(List<String> storedPaths) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    storedPaths.forEach(fileStorageService::delete);
                }
            }
        });
    }

    private String safeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "document";
        }
        String name = Paths.get(originalFileName.replace("\\", "/")).getFileName().toString().trim();
        if (name.isEmpty()) {
            return "document";
        }
        return name.length() > MAX_FILE_NAME_LENGTH ? name.substring(0, MAX_FILE_NAME_LENGTH) : name;
    }
}