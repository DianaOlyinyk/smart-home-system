package server.auth.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import server.auth.AuthService;
import server.auth.InvalidCredentialsException;
import server.auth.IssuedToken;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TokenController.class)
@AutoConfigureMockMvc(addFilters = false)
class TokenControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthService authService;

    @Test
    void correctCredentialsGiveBackAToken() throws Exception {
        UUID ownerId = UUID.randomUUID();
        when(authService.login("nazar@example.com", "pass1234"))
                .thenReturn(new IssuedToken("abc.def.ghi", ownerId));

        mvc.perform(post("/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"nazar@example.com\", \"password\": \"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("abc.def.ghi"))
                .andExpect(jsonPath("$.userId").value(ownerId.toString()));
    }

    @Test
    void wrongPasswordGivesUnauthorized() throws Exception {
        when(authService.login(anyString(), anyString())).thenThrow(new InvalidCredentialsException());

        mvc.perform(post("/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"nazar@example.com\", \"password\": \"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Невірний email або пароль"));
    }

    @Test
    void blankCredentialsAreRejectedBeforeReachingTheService() throws Exception {
        mvc.perform(post("/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"\", \"password\": \"\"}"))
                .andExpect(status().isBadRequest());
    }
}
