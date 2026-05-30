package com.island.common;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
		HttpStatus status = ex.getCode() == 401 ? HttpStatus.UNAUTHORIZED
				: ex.getCode() == 403 ? HttpStatus.FORBIDDEN
				: ex.getCode() == 404 ? HttpStatus.NOT_FOUND
				: HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(status)
				.body(ApiResponse.fail(ex.getCode(), ex.getMessage()));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ApiResponse.fail(403, "无权限访问"));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.fail(401, "邮箱或密码错误"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
		String msg = ex.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.joining("; "));
		return ResponseEntity.badRequest().body(ApiResponse.fail(400, msg));
	}

	@ExceptionHandler(RedisConnectionFailureException.class)
	public ResponseEntity<ApiResponse<Void>> handleRedis(RedisConnectionFailureException ex) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(ApiResponse.fail(503, "Redis 未启动，请先运行 docker compose up -d redis，或在 application-dev.yml 设置 island.verification.store=memory"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
		String message = ex.getMessage() != null ? ex.getMessage() : "服务器错误";
		if (message.contains("Unable to connect to Redis")) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body(ApiResponse.fail(503, "Redis 未启动，请先运行 docker compose up -d redis，或在 application-dev.yml 设置 island.verification.store=memory"));
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.fail(500, message));
	}
}
