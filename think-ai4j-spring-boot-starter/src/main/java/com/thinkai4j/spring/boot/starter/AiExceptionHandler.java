package com.thinkai4j.spring.boot.starter;

import com.thinkai4j.core.exception.AiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 面向客户端 API 的统一异常响应。
 *
 * <p>把框架抛出的 {@link AiException} 转换为稳定的 JSON 结构，避免客户端直接收到
 * 容器默认的 500 错误页。仅在 Spring Web 环境且用户未自定义同类处理器时自动装配，
 * 可通过 {@code think.ai.error-handler.enabled=false} 关闭。
 */
@RestControllerAdvice
public class AiExceptionHandler {

    public static final String DEFAULT_ERROR_CODE = "AI_BAD_REQUEST";

    @ExceptionHandler(AiException.class)
    public ResponseEntity<Map<String, Object>> handle(AiException exception) {
        int status = resolveStatus(exception);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", exception.getErrorCode() != null ? exception.getErrorCode() : DEFAULT_ERROR_CODE);
        body.put("message", exception.getMessage());
        body.put("provider", exception.getProvider());
        body.put("path", currentPath());
        return ResponseEntity.status(status).body(body);
    }

    private int resolveStatus(AiException exception) {
        String code = exception.getErrorCode();
        if (code == null) {
            // 框架侧参数与配置校验类错误，客户端可据此修正请求
            return 400;
        }
        if (code.startsWith("HTTP_4")) {
            return 400;
        }
        if (code.startsWith("HTTP_5") || "STREAM_ERROR".equals(code)
                || "REQUEST_ERROR".equals(code) || "SETUP_ERROR".equals(code)) {
            return 502;
        }
        return 500;
    }

    private String currentPath() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRequestURI();
        }
        return null;
    }
}
