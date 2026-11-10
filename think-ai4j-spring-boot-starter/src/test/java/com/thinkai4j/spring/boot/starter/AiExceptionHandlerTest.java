package com.thinkai4j.spring.boot.starter;

import com.thinkai4j.core.exception.AiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客户端 API 的结构化错误响应契约。
 * 该测试先于 AiExceptionHandler 实现编写，用于锁定状态码映射与响应字段。
 */
class AiExceptionHandlerTest {

    private final AiExceptionHandler handler = new AiExceptionHandler();

    @Test
    void parameterErrorMapsTo400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handle(new AiException("question cannot be null or blank"));

        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("AI_BAD_REQUEST", body.get("error"));
        assertEquals("question cannot be null or blank", body.get("message"));
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("path"));
    }

    @Test
    void upstreamErrorMapsTo502() {
        ResponseEntity<Map<String, Object>> response =
                handler.handle(new AiException("agnes", "HTTP_503", "upstream unavailable"));

        assertEquals(502, response.getStatusCode().value());
        assertEquals("HTTP_503", response.getBody().get("error"));
        assertEquals("agnes", response.getBody().get("provider"));
    }

    @Test
    void parseErrorMapsTo500() {
        ResponseEntity<Map<String, Object>> response =
                handler.handle(new AiException("agnes", "PARSE_ERROR", "bad json"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals("PARSE_ERROR", response.getBody().get("error"));
    }
}
