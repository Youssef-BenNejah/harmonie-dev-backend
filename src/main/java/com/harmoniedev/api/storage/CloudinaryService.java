package com.harmoniedev.api.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Uploads images to Cloudinary under a single well-known root folder ("harmonie-dev") so every
 * asset the app stores — company logos today, other per-tenant files later — lives in one place
 * in the Cloudinary media library instead of scattered at the account root.
 *
 * <p>Only active for the cloud deployment ({@code app.mode=cloud}, the default) — the standalone
 * desktop build has no Cloudinary account and uses {@link LocalFileStorageService} instead.
 */
@Service
@ConditionalOnProperty(prefix = "app", name = "mode", havingValue = "cloud", matchIfMissing = true)
public class CloudinaryService implements FileStorageService {
	private static final String ROOT_FOLDER = "harmonie-dev";
	private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
	private static final long MAX_DOCUMENT_SIZE_BYTES = 8L * 1024 * 1024;

	private final Cloudinary cloudinary;

	public CloudinaryService(Cloudinary cloudinary) {
		this.cloudinary = cloudinary;
	}

	/**
	 * @param folder relative folder path under the "harmonie-dev" root, e.g. "companies/{ownerId}"
	 * @param publicId stable id within that folder — re-uploading with the same id replaces the asset
	 */
	@Override
	public CloudinaryUploadResult uploadImage(MultipartFile file, String folder, String publicId) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
		}
		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
		}
		if (file.getSize() > MAX_SIZE_BYTES) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds the 5 MB limit");
		}
		try {
			Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
					"folder", ROOT_FOLDER + "/" + folder,
					"public_id", publicId,
					"overwrite", true,
					"invalidate", true,
					"resource_type", "image"));
			return new CloudinaryUploadResult((String) result.get("secure_url"), (String) result.get("public_id"));
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to upload file to Cloudinary", ex);
		}
	}

	/**
	 * Like {@link #uploadImage}, but also accepts a PDF (for a scanned invoice/receipt) — Cloudinary
	 * serves either back from the same "image" resource type, so the frontend can render an
	 * <img>/<iframe> straight off the returned URL without knowing which one it got.
	 */
	@Override
	public CloudinaryUploadResult uploadDocument(MultipartFile file, String folder, String publicId) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
		}
		String contentType = file.getContentType();
		boolean isImage = contentType != null && contentType.startsWith("image/");
		boolean isPdf = "application/pdf".equals(contentType);
		if (!isImage && !isPdf) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image or PDF files are allowed");
		}
		if (file.getSize() > MAX_DOCUMENT_SIZE_BYTES) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds the 8 MB limit");
		}
		try {
			Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
					"folder", ROOT_FOLDER + "/" + folder,
					"public_id", publicId,
					"overwrite", true,
					"invalidate", true,
					"resource_type", "image"));
			return new CloudinaryUploadResult((String) result.get("secure_url"), (String) result.get("public_id"));
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to upload file to Cloudinary", ex);
		}
	}
}
