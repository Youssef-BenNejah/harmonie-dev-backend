package com.harmoniedev.api.common.paging;

import com.harmoniedev.api.common.dto.ApiResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

/**
 * Shared pagination for list endpoints: {@code ?page=0&size=50}. The body stays the usual
 * {@code data: [...]} array; totals travel in {@code X-Total-Count} / {@code X-Total-Pages} headers.
 * Size is clamped so one request can never pull an entire collection.
 */
public final class Paging {
	public static final int DEFAULT_SIZE = 50;
	public static final int MAX_SIZE = 200;

	private Paging() {
	}

	public static PageRequest of(int page, int size) {
		int safePage = Math.max(0, page);
		int safeSize = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
		return PageRequest.of(safePage, safeSize, Sort.by("id"));
	}

	public static <T> ResponseEntity<ApiResponse<List<T>>> respond(String message, Page<T> page) {
		return ResponseEntity.ok()
				.header("X-Total-Count", String.valueOf(page.getTotalElements()))
				.header("X-Total-Pages", String.valueOf(page.getTotalPages()))
				.header("X-Page", String.valueOf(page.getNumber()))
				.header("X-Page-Size", String.valueOf(page.getSize()))
				.body(ApiResponse.success(message, page.getContent()));
	}
}
