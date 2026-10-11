package com.pradeep.user.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApiKeyFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsInternalApiCallsWhenNoKeyIsConfigured() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(503);
    }

    @Test
    void rejectsInternalApiCallsWithMissingOrInvalidKey() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("expected-key");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");
        request.addHeader(InternalApiKeyFilter.HEADER_NAME, "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void authenticatesTrustedInternalApiCalls() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("expected-key");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");
        request.addHeader(InternalApiKeyFilter.HEADER_NAME, "expected-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_INTERNAL_SERVICE");
    }
}
