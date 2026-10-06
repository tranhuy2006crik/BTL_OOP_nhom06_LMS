package com.lms.system.controller.api;

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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/calendar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/private-files"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/messages"))
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
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Profile - PTIT Learn</title>")))
                .andExpect(content().string(containsString("User details")))
                .andExpect(content().string(containsString(email)))
                .andExpect(content().string(containsString("Course details")))
                .andExpect(content().string(containsString("Dementia care: Caring for the Carers")))
                .andExpect(content().string(containsString("sample courses")))
                .andExpect(content().string(containsString("href=\"/profile/edit?theme=light\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Change password"))));
        mockMvc.perform(get("/profile/edit").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Edit profile - PTIT Learn</title>")))
                .andExpect(content().string(containsString("General")))
                .andExpect(content().string(containsString("User picture")))
                .andExpect(content().string(containsString("Additional names")))
                .andExpect(content().string(containsString("Interests")))
                .andExpect(content().string(containsString("Optional")))
                .andExpect(content().string(containsString("Other fields")))
                .andExpect(content().string(containsString("Update profile")))
                .andExpect(content().string(containsString("Change password")))
                .andExpect(content().string(containsString("New password")))
                .andExpect(content().string(containsString("name=\"firstName\"")))
                .andExpect(content().string(containsString("name=\"lastName\"")))
                .andExpect(content().string(containsString("name=\"cityTown\"")))
                .andExpect(content().string(containsString("name=\"country\"")))
                .andExpect(content().string(containsString("name=\"interests\"")))
                .andExpect(content().string(containsString("name=\"profilePicture\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("disabled"))));
        mockMvc.perform(get("/profile/edit").session(session).param("theme", "invalid"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/profile").session(session).param("theme", "dark"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-theme=\"dark\"")));
        mockMvc.perform(get("/profile").session(session).param("theme", "invalid"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/home").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(">Home</a>"))))
                .andExpect(content().string(containsString(">Dashboard</a>")))
                .andExpect(content().string(containsString(">My courses</a>")))
                .andExpect(content().string(containsString("Course overview")))
                .andExpect(content().string(containsString("Dementia care: Caring for the Carers")))
                .andExpect(content().string(containsString("Celebrating Cultures")))
                .andExpect(content().string(containsString("id=\"course-status\"")))
                .andExpect(content().string(containsString("id=\"course-view\"")))
                .andExpect(content().string(containsString("Private files")))
                .andExpect(content().string(containsString("href=\"/calendar\"")))
                .andExpect(content().string(containsString("href=\"/messages\"")))
                .andExpect(content().string(containsString("Web Test User")))
                .andExpect(content().string(containsString("sample content")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<script"))));

        mockMvc.perform(get("/dashboard").session(session)
                        .param("month", "2026-10")
                        .param("day", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(">Home</a>"))))
                .andExpect(content().string(containsString(">Dashboard</a>")))
                .andExpect(content().string(containsString(">My courses</a>")));

        mockMvc.perform(get("/home").session(session)
                        .param("q", "chem")
                        .param("status", "all")
                        .param("sort", "name-asc")
                        .param("view", "list")
                        .param("theme", "dark"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chemical Nomenclature")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("Dementia care: Caring for the Carers")
                )))
                .andExpect(content().string(containsString("list-view")))
                .andExpect(content().string(containsString("data-theme=\"dark\"")));

        mockMvc.perform(get("/home").session(session).param("status", "future"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Foundations of Data Science")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("Dementia care: Caring for the Carers")
                )));

        mockMvc.perform(get("/home").session(session).param("status", "invalid"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/dashboard").session(session).param("month", "2026-10").param("day", "5"))
                .andExpect(status().isOk())
                        .andExpect(content().string(containsString("Welcome back,")))
                        .andExpect(content().string(containsString("Web Test User")))
                        .andExpect(content().string(containsString("Course overview")))
                .andExpect(content().string(containsString("Timeline")))
                .andExpect(content().string(containsString("Calendar")))
                .andExpect(content().string(containsString("Latest announcements")))
                .andExpect(content().string(containsString("October 2026")))
                .andExpect(content().string(containsString("Wednesday, 23 December 2026")))
                .andExpect(content().string(containsString("aria-label=\"October 2026 5\"")));

        mockMvc.perform(get("/dashboard").session(session)
                        .param("month", "2026-10")
                        .param("day", "5")
                        .param("status", "all")
                        .param("q", "cultures")
                        .param("view", "list")
                        .param("timelineFilter", "upcoming")
                        .param("timelineSort", "latest")
                        .param("timelineQuery", "window")
                        .param("theme", "dark"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Celebrating Cultures")))
                .andExpect(content().string(containsString("list-view")))
                .andExpect(content().string(containsString("data-theme=\"dark\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Edit mode"))))
                .andExpect(content().string(containsString("View from your window")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("What do you already know?")
                )));

        mockMvc.perform(get("/calendar").session(session)
                        .param("month", "2026-10")
                        .param("day", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1>Calendar</h1>")))
                .andExpect(content().string(containsString("October 2026")))
                .andExpect(content().string(containsString("Events key")))
                .andExpect(content().string(containsString("Course overview and orientation")))
                .andExpect(content().string(containsString("name=\"visible\"")))
                .andExpect(content().string(containsString("Dashboard</a>")))
                .andExpect(content().string(containsString("My courses</a>")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(">Home</a>"))));

        mockMvc.perform(get("/private-files").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Private files</h2>")))
                .andExpect(content().string(containsString("Web Test User")))
                .andExpect(content().string(containsString("103.2 KB")))
                .andExpect(content().string(containsString("100 MB")))
                .andExpect(content().string(containsString("perthlemon.jpg")))
                .andExpect(content().string(containsString("Upload files")))
                .andExpect(content().string(containsString("Dashboard</a>")))
                .andExpect(content().string(containsString("My courses</a>")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(">Home</a>"))));

        mockMvc.perform(get("/private-files").session(session)
                        .param("view", "list")
                        .param("theme", "dark")
                        .param("notice", "upload"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("private-file-list-view")))
                .andExpect(content().string(containsString("data-theme=\"dark\"")))
                .andExpect(content().string(containsString(
                        "File uploads will be available when private storage is connected."
                )));

        mockMvc.perform(get("/private-files").session(session).param("notice", "messages"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Messaging will be available in a future update."
                )));
        mockMvc.perform(get("/private-files").session(session).param("view", "invalid"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/private-files").session(session).param("notice", "invalid"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/messages").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Messages - PTIT Learn</title>")))
                .andExpect(content().string(containsString("Starred")))
                .andExpect(content().string(containsString("Group")))
                .andExpect(content().string(containsString("Private")))
                .andExpect(content().string(containsString(">(<span>0</span>)</span>")))
                .andExpect(content().string(containsString("messages-folder-count")))
                .andExpect(content().string(containsString("Select a conversation to view messages.")))
                .andExpect(content().string(containsString("Dashboard</a>")))
                .andExpect(content().string(containsString("My courses</a>")));

        mockMvc.perform(get("/messages").session(session).param("view", "contacts").param("theme", "dark"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-theme=\"dark\"")))
                .andExpect(content().string(containsString("No contacts yet.")));
        mockMvc.perform(get("/messages").session(session).param("view", "invalid"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/calendar").session(session)
                        .param("month", "2026-10")
                        .param("day", "5")
                        .param("visible", "site"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Welcome to your learning calendar")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("Course overview and orientation")
                )));

        mockMvc.perform(get("/calendar").session(session).param("month", "2026-13"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/calendar").session(session).param("visible", "invalid"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/dashboard").session(session).param("month", "2026-13"))
                .andExpect(status().isBadRequest());
    }

}
