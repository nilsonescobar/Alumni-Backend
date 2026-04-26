package org.fia.alumni.alumnifiauesbackend.service.storage;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.PublicAccessType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AzureBlobService {

    private final BlobContainerClient containerClient;
    private final String blobBaseUrl;

    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    public AzureBlobService(
            @Value("${azure.storage.connection-string}") String connectionString,
            @Value("${azure.storage.container-name}") String containerName,
            @Value("${azure.storage.blob-url}") String blobBaseUrl
    ) {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        this.containerClient = serviceClient.getBlobContainerClient(containerName);
        this.blobBaseUrl = blobBaseUrl.endsWith("/") ? blobBaseUrl : blobBaseUrl + "/";
    }

    @PostConstruct
    public void init() {
        try {
            if (!containerClient.exists()) {
                containerClient.createWithResponse(null, PublicAccessType.BLOB, null, null);
                log.info("Azure Blob container created: {}", containerClient.getBlobContainerName());
            } else {
                log.info("Azure Blob container ready: {}", containerClient.getBlobContainerName());
            }
        } catch (Exception e) {
            log.warn("Could not verify Azure container on startup: {}", e.getMessage());
        }
    }

    public String uploadImage(MultipartFile file, String folder) {
        validateImage(file);

        String blobName = buildBlobName(folder, file.getOriginalFilename());

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            BlobHttpHeaders headers = new BlobHttpHeaders()
                    .setContentType(file.getContentType());

            blobClient.uploadWithResponse(
                    file.getInputStream(),
                    file.getSize(),
                    null, headers, null, null, null, null, null
            );

            String url = blobClient.getBlobUrl();
            log.info("Uploaded: {}", url);
            return url;

        } catch (IOException e) {
            log.error("Error uploading blob: {}", e.getMessage());
            throw new BadRequestException("Error al subir la imagen: " + e.getMessage());
        }
    }

    public String replaceImage(MultipartFile file, String oldUrl, String folder) {
        String newUrl = uploadImage(file, folder);
        if (oldUrl != null && !oldUrl.isBlank()) {
            deleteByUrl(oldUrl);
        }
        return newUrl;
    }

    public void deleteByUrl(String blobUrl) {
        if (blobUrl == null || blobUrl.isBlank()) return;
        try {
            String blobName = extractBlobName(blobUrl);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            if (blobClient.exists()) {
                blobClient.delete();
                log.info("Deleted blob: {}", blobName);
            }
        } catch (Exception e) {
            log.warn("Could not delete blob {}: {}", blobUrl, e.getMessage());
        }
    }

    public void deleteAll(List<String> urls) {
        if (urls == null) return;
        urls.forEach(this::deleteByUrl);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new BadRequestException("El archivo no puede estar vacío");
        if (!ALLOWED_TYPES.contains(file.getContentType()))
            throw new BadRequestException("Solo se aceptan: JPEG, PNG, WebP, GIF");
        if (file.getSize() > MAX_SIZE_BYTES)
            throw new BadRequestException("El archivo no puede superar 10MB");
    }

    private String buildBlobName(String folder, String originalName) {
        String ext = "";
        if (originalName != null && originalName.contains("."))
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        return folder + "/" + UUID.randomUUID() + ext;
    }

    private String extractBlobName(String blobUrl) {
        if (blobUrl.startsWith(blobBaseUrl))
            return blobUrl.substring(blobBaseUrl.length());
        String[] parts = blobUrl.split("/");
        StringBuilder name = new StringBuilder();
        for (int i = 4; i < parts.length; i++) {
            if (name.length() > 0) name.append("/");
            name.append(parts[i]);
        }
        return name.toString();
    }
}