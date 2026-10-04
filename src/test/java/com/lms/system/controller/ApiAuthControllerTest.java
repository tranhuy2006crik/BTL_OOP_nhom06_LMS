package com.lms.system.controller;

import com.lms.system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "lms.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
@Transactional
class ApiAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void protectedApiRejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationLoginAndBearerAuthenticationWorkEndToEnd() throws Exception {
        String email = "api-" + UUID.randomUUID() + "@example.com";
        String registration = """
                {
                  "email": "%s",
                  "fullName": "API Test User",
                  "password": "StrongPass123",
                  "confirmPassword": "StrongPass123"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("API Test User"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertTrue(userRepository.findByEmailIgnoreCase(email)
                .orElseThrow()
                .getPasswordHash()
                .startsWith("$2"));

        String login = """
                {"email": "%s", "password": "StrongPass123"}
                """.formatted(email);
        String tokenResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Matcher tokenMatcher = Pattern.compile("\"accessToken\":\"([^\"]+)\"").matcher(tokenResponse);
        assertTrue(tokenMatcher.find());
        String accessToken = tokenMatcher.group(1);
        assertTrue(accessToken.split("\\.").length == 3);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void invalidLoginReturnsUnauthorizedJson() throws Exception {
        String login = """
                {"email": "missing@example.com", "password": "WrongPass123"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(
                        "Email hoặc mật khẩu không đúng, hoặc tài khoản đã bị khóa"
                ));
    }

    @Test
    void browserRoutesRequireLoginAndFormLoginCreatesAnAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        String email = "web-" + UUID.randomUUID() + "@example.com";
        String registration = """
                {
                  "email": "%s",
                  "fullName": "Web Test User",
                  "password": "StrongPass123",
                  "confirmPassword": "StrongPass123"
                }
                """.formatted(email);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated());

        var loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", email)
                        .param("password", "StrongPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/profile").session(session))
                .andExpect(status().isOk());
    }

}
