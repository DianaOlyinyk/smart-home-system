package server.auth.internal;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock
    private JwtValidatorService jwtValidatorService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private AuthenticationEntryPoint authenticationEntryPoint;
    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void corsConfigurationAllowsOnlyTheConfiguredOriginAndHeaders() {
        CorsConfigurationSource source = new CorsConfig().corsConfigurationSource();
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/devices"));

        assertNotNull(configuration);
        assertEquals(java.util.List.of("http://localhost:3000"), configuration.getAllowedOrigins());
        assertEquals(java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"),
                configuration.getAllowedMethods());
        assertEquals(java.util.List.of("Authorization", "Content-Type"), configuration.getAllowedHeaders());
        assertFalse(configuration.getAllowCredentials());
        assertEquals(3600L, configuration.getMaxAge());
    }

    @Test
    void requestWithoutBearerTokenPassesThroughUnchanged() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/devices");
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtValidatorService, userDetailsService, authenticationEntryPoint);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtValidatorService, userDetailsService, authenticationEntryPoint);
    }

    @Test
    void requestWithAnotherAuthorizationSchemePassesThroughUnchanged() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/devices");
        request.addHeader("Authorization", "Basic credentials");
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtValidatorService, userDetailsService, authenticationEntryPoint);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtValidatorService, userDetailsService, authenticationEntryPoint);
    }

    @Test
    void validBearerTokenAuthenticatesTheLoadedUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/devices");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserDetails userDetails = User.withUsername("alice@example.com")
                .password("unused")
                .authorities("ROLE_USER")
                .build();
        when(jwtValidatorService.extractUsername("token")).thenReturn("alice@example.com");
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(userDetails);
        when(jwtValidatorService.isTokenValid("token", userDetails)).thenReturn(true);
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtValidatorService, userDetailsService, authenticationEntryPoint);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(authenticationEntryPoint);
    }

    @Test
    void invalidBearerTokenClearsContextAndCommencesAuthenticationFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/devices");
        request.addHeader("Authorization", "Bearer invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtValidatorService.extractUsername("invalid")).thenThrow(new JwtException("Невалідний токен"));
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtValidatorService, userDetailsService, authenticationEntryPoint);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationEntryPoint).commence(eq(request), eq(response), any(BadCredentialsException.class));
        verify(filterChain, never()).doFilter(request, response);
    }
}
