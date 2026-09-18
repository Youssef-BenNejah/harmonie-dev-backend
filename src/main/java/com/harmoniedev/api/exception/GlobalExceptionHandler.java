package com.harmoniedev.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
		String detail = ex.getReason() == null ? ex.getMessage() : ex.getReason();
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), detail);
		problem.setTitle(ex.getStatusCode().toString());
		problem.setType(URI.create("https://yourapp.com/errors/" + ex.getStatusCode().value()));
		problem.setInstance(URI.create(request.getRequestURI()));
		return ResponseEntity.status(ex.getStatusCode()).body(problem);
	}

	@ExceptionHandler(PlanLimitExceededException.class)
	public ResponseEntity<ProblemDetail> handlePlanLimit(PlanLimitExceededException ex, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
		problem.setTitle("Limite du plan atteinte");
		problem.setType(URI.create("https://yourapp.com/errors/plan-limit"));
		problem.setInstance(URI.create(request.getRequestURI()));
		return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(problem);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(this::formatFieldError)
				.collect(Collectors.joining(", "));
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle("Validation failed");
		problem.setType(URI.create("https://yourapp.com/errors/validation"));
		problem.setInstance(URI.create(request.getRequestURI()));
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler({
			HttpMessageNotReadableException.class,
			MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class,
			ConstraintViolationException.class})
	public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "Bad request", "Malformed or invalid request", request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ProblemDetail> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "Not found", "Resource not found", request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ProblemDetail> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
		return problem(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", "Method not allowed", request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ProblemDetail> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
		return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", "Unsupported media type", request);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ProblemDetail> handleTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
		return problem(HttpStatus.PAYLOAD_TOO_LARGE, "File too large", "The uploaded file exceeds the size limit", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
		problem.setTitle("Internal Server Error");
		problem.setType(URI.create("https://yourapp.com/errors/internal"));
		problem.setInstance(URI.create(request.getRequestURI()));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setType(URI.create("https://yourapp.com/errors/" + status.value()));
		problem.setInstance(URI.create(request.getRequestURI()));
		return ResponseEntity.status(status).body(problem);
	}

	private String formatFieldError(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}
}
