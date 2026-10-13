package com.pintu.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthRateLimitFilterTest {

    @Test
    void rejectsRequestsBeyondConfiguredWindowLimit() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-06-14T06:00:00Z"), ZoneOffset.UTC);
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new ObjectMapper(), 2, 60, clock);

        assertEquals(200, execute(filter).getStatus());
        assertEquals(200, execute(filter).getStatus());
        MockHttpServletResponse rejected = execute(filter);
        assertEquals(429, rejected.getStatus());
        assertEquals("60", rejected.getHeader("Retry-After"));
    }

    private MockHttpServletResponse execute(AuthRateLimitFilter filter) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
