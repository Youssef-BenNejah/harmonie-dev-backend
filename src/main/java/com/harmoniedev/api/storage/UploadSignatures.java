package com.harmoniedev.api.storage;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Checks the file's leading bytes against the format it claims to be, so a renamed script or HTML file is rejected. */
final class UploadSignatures {
	private UploadSignatures() {
	}

	static void assertMatchesDeclaredType(MultipartFile file) {
		byte[] head = new byte[12];
		int read;
		try (var in = file.getInputStream()) {
			read = in.readNBytes(head, 0, head.length);
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unreadable file", ex);
		}
		String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
		boolean ok = read >= 4 && switch (type) {
			case "image/png" -> startsWith(head, 0x89, 'P', 'N', 'G');
			case "image/jpeg", "image/jpg" -> startsWith(head, 0xFF, 0xD8, 0xFF);
			case "image/gif" -> startsWith(head, 'G', 'I', 'F', '8');
			case "image/webp" -> read >= 12 && startsWith(head, 'R', 'I', 'F', 'F') && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
			case "application/pdf" -> startsWith(head, '%', 'P', 'D', 'F');
			default -> false;
		};
		if (!ok) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File content does not match its declared type (allowed: PNG, JPEG, GIF, WebP, PDF)");
		}
	}

	private static boolean startsWith(byte[] head, int... expected) {
		for (int i = 0; i < expected.length; i++) {
			if ((head[i] & 0xFF) != expected[i]) {
				return false;
			}
		}
		return true;
	}
}
