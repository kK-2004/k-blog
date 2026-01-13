package com.kk.kblog.api;

import java.lang.reflect.Method;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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
        return new ResponseEntity<>(new ApiError("Validation failed"), HttpStatus.BAD_REQUEST);
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        boolean isDev = Arrays.asList(env.getActiveProfiles()).contains("dev");
        log.error("Unhandled exception in controller", ex);
        String msg = isDev
                ? ("服务器错误 [" + ex.getClass().getSimpleName() + ": " + String.valueOf(ex.getMessage()) + "]")
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
}
