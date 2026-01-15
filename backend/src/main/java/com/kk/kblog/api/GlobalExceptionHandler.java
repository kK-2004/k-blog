package com.kk.kblog.api;

import java.lang.reflect.Method;
import java.util.Arrays;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.validation.FieldError;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final Environment env;

    public GlobalExceptionHandler(Environment env) {
        this.env = env;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        return new ResponseEntity<>(new ApiError(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleConflict(IllegalStateException ex) {
        String msg = ex.getMessage();
        boolean isDev = Arrays.asList(env.getActiveProfiles()).contains("dev");
        if (isDev && ex.getCause() != null) {
            Throwable cause = ex.getCause();
            try {
                String ossExtra = buildOssExtraIfPresent(cause);
                if (ossExtra != null) {
                    msg = msg + ossExtra;
                } else {
                    msg = msg + " [cause=" + cause.getClass().getSimpleName() + ": " + cause.getMessage() + "]";
                }
            } catch (Throwable ignored) {
            }
        }
        return new ResponseEntity<>(new ApiError(msg), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        try {
            var binding = ex.getBindingResult();
            var errors = binding.getFieldErrors().stream()
                    .map(this::formatFieldError)
                    .toList();
            var msg = errors.isEmpty() ? "Validation failed" : "Validation failed: " + String.join("; ", errors);
            log.warn("Validation failed: {}", msg);
            return new ResponseEntity<>(new ApiError(msg), HttpStatus.BAD_REQUEST);
        } catch (Exception ignored) {
            return new ResponseEntity<>(new ApiError("Validation failed"), HttpStatus.BAD_REQUEST);
        }
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex) {
        return new ResponseEntity<>(new ApiError("无权限"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex) {
        return new ResponseEntity<>(new ApiError("用户名或密码错误"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return new ResponseEntity<>(new ApiError("上传文件过大，请压缩或分批上传"), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<String> handleAsyncTimeout(AsyncRequestTimeoutException ex) {
        // Streaming endpoints may have already committed headers as text/plain; return text to avoid converter errors.
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .contentType(MediaType.TEXT_PLAIN)
                .body("Request timeout");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        boolean isDev = Arrays.asList(env.getActiveProfiles()).contains("dev");
        log.error("Unhandled exception in controller", ex);
        String msg = isDev
                ? ("服务器错误 [" + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "]")
                : "服务器错误";
        try {
            if (isDev && ex.getCause() != null) {
                String ossExtra = buildOssExtraIfPresent(ex.getCause());
                if (ossExtra != null) {
                    msg += ossExtra;
                }
            }
        } catch (Throwable ignored) {
        }
        return new ResponseEntity<>(new ApiError(msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(com.kk.kblog.util.ratelimit.RateLimitedException.class)
    public ResponseEntity<ApiError> handleRateLimited(com.kk.kblog.util.ratelimit.RateLimitedException ex) {
        return new ResponseEntity<>(new ApiError(ex.getMessage()), HttpStatus.TOO_MANY_REQUESTS);
    }
    
    @ExceptionHandler
    public ResponseEntity<ApiError> handleEntityNotFoundException(EntityNotFoundException ex){
        return new ResponseEntity<>(new ApiError("博客不存在"), HttpStatus.NOT_FOUND);
    }

    private String buildOssExtraIfPresent(Throwable cause) throws Exception {
        if (cause == null) return null;
        if (!"com.aliyun.oss.OSSException".equals(cause.getClass().getName())) return null;

        Method getErrorCode = cause.getClass().getMethod("getErrorCode");
        Method getRequestId = cause.getClass().getMethod("getRequestId");
        Method getHostId = cause.getClass().getMethod("getHostId");
        Method getErrorMessage = cause.getClass().getMethod("getErrorMessage");

        Object errorCode = getErrorCode.invoke(cause);
        Object requestId = getRequestId.invoke(cause);
        Object hostId = getHostId.invoke(cause);
        Object errorMessage = getErrorMessage.invoke(cause);

        return String.format(" [OSS] code=%s, reqId=%s, host=%s, err=%s",
                String.valueOf(errorCode),
                String.valueOf(requestId),
                String.valueOf(hostId),
                String.valueOf(errorMessage));
    }

    private String formatFieldError(FieldError err) {
        var field = err.getField();
        var message = err.getDefaultMessage();
        if (message == null || message.isBlank()) {
            message = "invalid";
        }
        return field + " " + message;
    }
}
