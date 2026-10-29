package com.prashant.api.ecom.ducart.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {
    @Test
    void browserTestOriginIsAllowedWithoutCookieCredentials() throws Exception {
        mockMvc.perform(options("/user/login").header("Origin", "http://127.0.0.1:3001")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:3001"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void unknownBrowserOriginIsRejected() throws Exception {
        mockMvc.perform(options("/user/login").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Autowired MockMvc mockMvc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AuthenticationProvider authenticationProvider;

    @Test
    void securityBeans_shouldUseBcryptAndCreateAuthenticationProvider() {
        assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder);
        assertNotNull(authenticationProvider);
        assertTrue(passwordEncoder.matches("secret", passwordEncoder.encode("secret")));
    }

    @Test
    void publicCatalogRead_shouldBeAccessibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/product"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedBuyerRoute_shouldReturnJson401WithoutLogin() throws Exception {
        mockMvc.perform(get("/cart/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required or token is invalid"));
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void adminEndpoint_shouldReturnJson403ForBuyer() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminEndpoint_shouldBeAccessibleForAdmin() throws Exception {
        mockMvc.perform(get("/user"))
                .andExpect(status().isOk());
    }

    @Test
    void legacyPhoneReset_shouldNotBePublic() throws Exception {
        mockMvc.perform(put("/user/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"missing\",\"phone\":\"0000000\",\"newPassword\":\"secret\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordResetRequestPost_shouldBePublicButValidateBody() throws Exception {
        mockMvc.perform(post("/user/forgot-password").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordResetConfirmationPost_shouldBePublicButRequireToken() throws Exception {
        mockMvc.perform(post("/user/reset-password").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
