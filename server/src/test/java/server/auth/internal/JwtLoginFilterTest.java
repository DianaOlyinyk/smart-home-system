package server.auth.internal;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtLoginFilterTest {
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationEntryPoint authenticationEntryPoint;
    @Mock
    private FilterChain filterChain;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void requestToAnotherPathPassesThroughUnchanged() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/devices");
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtLoginFilter filter =
                new JwtLoginFilter(authenticationManager, jwtService, objectMapper, authenticationEntryPoint);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(authenticationManager, jwtService, authenticationEntryPoint);
    }

    @Test
    void validCredentialsReturnBearerTokenWithoutCookie() throws Exception {
        MockHttpServletRequest request = loginRequest("{\"email\":\"owner@example.com\",\"password\":\"Passw0rd123\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserDetails userDetails = User.withUsername("owner@example.com")
                .password("unused")
                .authorities("ROLE_USER")
                .build();
        when(authenticationManager.authenticate(argThat(token ->
                "owner@example.com".equals(token.getPrincipal())
                        && "Passw0rd123".equals(token.getCredentials())
                        && !token.isAuthenticated())))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(
                        userDetails, null, userDetails.getAuthorities()));
        when(jwtService.generateToken(userDetails)).thenReturn("token");
        when(jwtService.getExpirationMillis()).thenReturn(900_000L);
        JwtLoginFilter filter =
                new JwtLoginFilter(authenticationManager, jwtService, objectMapper, authenticationEntryPoint);

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType().split(";", 2)[0]);
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals("token", body.get("accessToken").asString());
        assertEquals("Bearer", body.get("tokenType").asString());
        assertEquals(900, body.get("expiresIn").asLong());
        assertNull(response.getHeader("Set-Cookie"));
        verify(filterChain, never()).doFilter(request, response);
        verifyNoInteractions(authenticationEntryPoint);
    }

    @Test
    void wrongCredentialsCommenceAuthenticationFailure() throws Exception {
        MockHttpServletRequest request = loginRequest("{\"email\":\"owner@example.com\",\"password\":\"wrong\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
        JwtLoginFilter filter =
                new JwtLoginFilter(authenticationManager, jwtService, objectMapper, authenticationEntryPoint);

        filter.doFilter(request, response, filterChain);

        verify(authenticationEntryPoint).commence(eq(request), eq(response), any(BadCredentialsException.class));
        verify(filterChain, never()).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void malformedBodyCommencesAuthenticationServiceFailure() throws Exception {
        MockHttpServletRequest request = loginRequest("{\"email\":");
        MockHttpServletResponse response = new MockHttpServletResponse();
        JwtLoginFilter filter =
                new JwtLoginFilter(authenticationManager, jwtService, objectMapper, authenticationEntryPoint);

        filter.doFilter(request, response, filterChain);

        verify(authenticationEntryPoint)
                .commence(eq(request), eq(response), any(AuthenticationServiceException.class));
        verifyNoInteractions(authenticationManager, jwtService);
    }

    private MockHttpServletRequest loginRequest(String json) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/login");
        request.setContentType("application/json");
        request.setContent(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return request;
    }
}
