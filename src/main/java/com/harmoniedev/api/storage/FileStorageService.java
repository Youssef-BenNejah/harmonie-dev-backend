package com.harmoniedev.api.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over where uploaded files (logos, profile photos, invoice attachments) end up.
 * {@link CloudinaryService} backs this in the cloud deployment; {@link LocalFileStorageService}
 * backs it in the standalone desktop build, which has no Cloudinary account.
 */
public interface FileStorageService {
	CloudinaryUploadResult uploadImage(MultipartFile file, String folder, String publicId);

	CloudinaryUploadResult uploadDocument(MultipartFile file, String folder, String publicId);
}
