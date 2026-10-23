package server.auth.internal;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.authentication.AuthenticationServiceException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.ApiPaths;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class JwtLoginFilter extends UsernamePasswordAuthenticationFilter {
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtLoginFilter(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          ObjectMapper objectMapper,
                          AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager);

        this.jwtService = jwtService;
        this.objectMapper = objectMapper;

        setRequiresAuthenticationRequestMatcher(
                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, ApiPaths.LOGIN));

        setAuthenticationFailureHandler((request, response, exception) ->
                authenticationEntryPoint.commence(request, response, exception));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        try {
            LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
            var authRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                    loginRequest.email(), loginRequest.password());
            setDetails(request, authRequest);
            return getAuthenticationManager().authenticate(authRequest);
        } catch (IOException | JacksonException ex) {
            throw new AuthenticationServiceException("Помилка читання запиту автентифікації", ex);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult) throws IOException  {
        UserDetails userDetails = (UserDetails) authResult.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        long expiresInSeconds = jwtService.getExpirationMillis() / 1000;
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), AuthResponse.of(token, expiresInSeconds));
    }
}
