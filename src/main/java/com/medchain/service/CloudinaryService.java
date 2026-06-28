package com.medchain.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.medchain.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/webp"};

    public String uploadImage(MultipartFile file, String folder) {
        // Validate file type
        String contentType = file.getContentType();
        boolean isValidType = false;
        for (String type : ALLOWED_TYPES) {
            if (type.equals(contentType)) {
                isValidType = true;
                break;
            }
        }
        if (!isValidType) {
            throw new ValidationException("Invalid file type. Only JPG, PNG, and WEBP are allowed");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("File size exceeds 5MB limit");
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "medchain/" + folder,
                            "resource_type", "image"
                    ));
            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Image uploaded to Cloudinary: {}", secureUrl);
            return secureUrl;
        } catch (Exception e) {
            log.warn("Cloudinary upload failed ({}), report will be saved without image", e.getMessage());
            return null;
        }
    }

    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary", e);
        }
    }
}
