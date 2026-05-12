package com.thinkai4j.test;

import com.thinkai4j.skill.HttpSkill;
import org.junit.jupiter.api.Test;

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
        assertTrue(result.contains("HTTP请求失败") || result.contains("HTTP "));
    }

    @Test
    void testHttpPostWithInvalidUrl() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpPost("http://nonexistent.invalid.domain/test", null, "{\"key\":\"value\"}");
        assertNotNull(result);
        assertTrue(result.contains("HTTP请求失败") || result.contains("HTTP "));
    }

    @Test
    void testHttpPutWithInvalidUrl() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpPut("http://nonexistent.invalid.domain/test", null, "{\"key\":\"value\"}");
        assertNotNull(result);
        assertTrue(result.contains("HTTP请求失败") || result.contains("HTTP "));
    }

    @Test
    void testHttpDeleteWithInvalidUrl() {
        HttpSkill httpSkill = new HttpSkill();
        String result = httpSkill.httpDelete("http://nonexistent.invalid.domain/test", null);
        assertNotNull(result);
        assertTrue(result.contains("HTTP请求失败") || result.contains("HTTP "));
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
        assertTrue(result.contains("HTTP请求失败") || result.contains("HTTP "));
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
}
