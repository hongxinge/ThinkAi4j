package com.thinkai4j.test;

import com.thinkai4j.skill.HttpSkill;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HttpSkillTest {

    @Test
    void testHttpSkillConstruction() {
        HttpSkill httpSkill = new HttpSkill();
        assertNotNull(httpSkill);
    }

    @Test
    void testSetDefaultHeader() {
        HttpSkill httpSkill = new HttpSkill();
        HttpSkill result = httpSkill.setDefaultHeader("Authorization", "Bearer token");
        assertNotNull(result);
        assertSame(httpSkill, result);
    }

    @Test
    void testHttpGetWithInvalidUrl() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpGet("http://nonexistent.invalid.domain/test", null);
        assertNotNull(result);
        assertTrue(result.contains("HTTP请求失败") || result.contains("HTTP ") || result.contains("安全限制"));
    }

    @Test
    void testHttpPostWithInvalidUrl() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpPost("http://nonexistent.invalid.domain/test", null, "{\"key\":\"value\"}");
        assertNotNull(result);
        assertTrue(result.contains("HTTP请求失败") || result.contains("HTTP ") || result.contains("安全限制"));
    }

    @Test
    void testHttpPutWithInvalidUrl() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpPut("http://nonexistent.invalid.domain/test", null, "{\"key\":\"value\"}");
        assertNotNull(result);
        assertTrue(result.contains("HTTP请求失败") || result.contains("HTTP ") || result.contains("安全限制"));
    }

    @Test
    void testHttpDeleteWithInvalidUrl() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpDelete("http://nonexistent.invalid.domain/test", null);
        assertNotNull(result);
        assertTrue(result.contains("HTTP请求失败") || result.contains("HTTP ") || result.contains("安全限制"));
    }

    @Test
    void testHttpGetWithNullHeaders() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpGet("http://nonexistent.invalid.domain/test", null);
        assertNotNull(result);
    }

    @Test
    void testHttpGetWithEmptyHeaders() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpGet("http://nonexistent.invalid.domain/test", "");
        assertNotNull(result);
    }

    @Test
    void testHttpPostWithNullBody() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpPost("http://nonexistent.invalid.domain/test", null, null);
        assertNotNull(result);
        assertTrue(result.contains("HTTP请求失败") || result.contains("HTTP ") || result.contains("安全限制"));
    }

    @Test
    void testMultipleDefaultHeaders() {
        HttpSkill httpSkill = new HttpSkill();
        httpSkill.setDefaultHeader("Header1", "Value1");
        httpSkill.setDefaultHeader("Header2", "Value2");
        httpSkill.setDefaultHeader("Header3", "Value3");

        String result = httpSkill.httpGet("http://nonexistent.invalid.domain/test", null);
        assertNotNull(result);
    }

    @Test
    void testSsrfProtectionBlocksLocalhost() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpGet("http://localhost:8080/admin", null);
        assertNotNull(result);
        assertTrue(result.contains("安全限制"), "Should block localhost, got: " + result);
    }

    @Test
    void testSsrfProtectionBlocksPrivateIp() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpGet("http://192.168.1.1/admin", null);
        assertNotNull(result);
        assertTrue(result.contains("安全限制"), "Should block 192.168.x.x, got: " + result);
    }

    @Test
    void testSsrfProtectionBlocks127() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpGet("http://127.0.0.1:8080/secret", null);
        assertNotNull(result);
        assertTrue(result.contains("安全限制"), "Should block 127.0.0.1, got: " + result);
    }

    @Test
    void testSsrfProtectionBlocks10Network() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpGet("http://10.0.0.1/internal", null);
        assertNotNull(result);
        assertTrue(result.contains("安全限制"), "Should block 10.x.x.x, got: " + result);
    }

    @Test
    void testSsrfProtectionBlocksMetadataEndpoint() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpGet("http://169.254.169.254/latest/meta-data/", null);
        assertNotNull(result);
        assertTrue(result.contains("安全限制"), "Should block cloud metadata endpoint, got: " + result);
    }

    @Test
    void testSsrfProtectionBlocksInvalidProtocol() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpGet("ftp://example.com/file", null);
        assertNotNull(result);
        assertTrue(result.contains("安全限制") || result.contains("HTTP"), "Should block non-HTTP protocol, got: " + result);
    }

    @Test
    void testEmptyUrl() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpGet("", null);
        assertNotNull(result);
        assertTrue(result.contains("不能为空"));
    }

    @Test
    void testNullUrl() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpGet(null, null);
        assertNotNull(result);
        assertTrue(result.contains("不能为空"));
    }

    @Test
    void testSsrfProtectionDisabled() {
        HttpSkill httpSkill = new HttpSkill(null, false);
        String result = httpSkill.httpGet("http://localhost:8080/admin", null);
        assertNotNull(result);
        assertFalse(result.contains("安全限制"), "Should not block when SSRF protection disabled, got: " + result);
    }

    @Test
    void testHostAllowlist() {
        HttpSkill httpSkill = new HttpSkill(Set.of("api.example.com"), true);
        String result = httpSkill.httpGet("http://api.example.com/data", null);
        assertNotNull(result);
        assertFalse(result.contains("不在允许列表中"), "Should allow whitelisted host, got: " + result);
    }

    @Test
    void testHostAllowlistBlocked() {
        HttpSkill httpSkill = new HttpSkill(Set.of("api.example.com"), true);
        String result = httpSkill.httpGet("http://other.example.com/data", null);
        assertNotNull(result);
        assertTrue(result.contains("安全限制") || result.contains("不在允许列表中"), "Should block non-whitelisted host, got: " + result);
    }
}
