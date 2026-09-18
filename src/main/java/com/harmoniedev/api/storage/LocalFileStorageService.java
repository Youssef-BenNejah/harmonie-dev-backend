package com.harmoniedev.api.storage;

import com.harmoniedev.api.config.AppProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Standalone-desktop replacement for {@link CloudinaryService} — writes uploads to a local
 * directory instead of a cloud bucket, and serves them back via {@link StaticResourceConfig}'s
 * {@code /uploads/**} mapping. Only active when {@code app.mode=desktop}.
 */
@Service
@ConditionalOnProperty(prefix = "app", name = "mode", havingValue = "desktop")
public class LocalFileStorageService implements FileStorageService {
	private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
	private static final long MAX_DOCUMENT_SIZE_BYTES = 8L * 1024 * 1024;

	private final Path rootDir;
	private final AppProperties appProperties;

	public LocalFileStorageService(
			@Value("${storage.local-dir}") String localDir,
			AppProperties appProperties) {
		this.rootDir = Path.of(localDir);
		this.appProperties = appProperties;
	}

	@Override
	public CloudinaryUploadResult uploadImage(MultipartFile file, String folder, String publicId) {
		validate(file, MAX_SIZE_BYTES, true);
		return store(file, folder, publicId);
	}

	@Override
	public CloudinaryUploadResult uploadDocument(MultipartFile file, String folder, String publicId) {
		validate(file, MAX_DOCUMENT_SIZE_BYTES, false);
		return store(file, folder, publicId);
	}

	private void validate(MultipartFile file, long maxSize, boolean imageOnly) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
		}
		String contentType = file.getContentType();
		boolean isImage = contentType != null && contentType.startsWith("image/");
		boolean isPdf = "application/pdf".equals(contentType);
		if (imageOnly ? !isImage : (!isImage && !isPdf)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					imageOnly ? "Only image files are allowed" : "Only image or PDF files are allowed");
		}
		if (file.getSize() > maxSize) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds the allowed size limit");
		}
	}

	private CloudinaryUploadResult store(MultipartFile file, String folder, String publicId) {
		try {
			String extension = extensionFor(file);
			Path targetDir = rootDir.resolve(folder).normalize();
			if (!targetDir.startsWith(rootDir)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid folder");
			}
			Files.createDirectories(targetDir);
			Path targetFile = targetDir.resolve(publicId + extension);
			file.transferTo(targetFile);
			String relativePath = folder + "/" + publicId + extension;
			String url = "http://localhost:" + appProperties.getPort() + "/uploads/" + relativePath;
			return new CloudinaryUploadResult(url, relativePath);
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file locally", ex);
		}
	}

	private String extensionFor(MultipartFile file) {
		String contentType = file.getContentType();
		if ("application/pdf".equals(contentType)) {
			return ".pdf";
		}
		if ("image/png".equals(contentType)) {
			return ".png";
		}
		if ("image/webp".equals(contentType)) {
			return ".webp";
		}
		return ".jpg";
	}
}
