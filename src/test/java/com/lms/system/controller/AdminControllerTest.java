package com.lms.system.controller;

import com.lms.system.model.PasswordHasher;
import com.lms.system.model.Course;
import com.lms.system.model.CourseStatus;
import com.lms.system.model.Role;
import com.lms.system.model.RoleScope;
import com.lms.system.model.SystemRoleAssignment;
import com.lms.system.model.SystemRoleAssignmentId;
import com.lms.system.model.User;
import com.lms.system.repository.RoleRepository;
import com.lms.system.repository.CourseRepository;
import com.lms.system.repository.CourseCategoryRepository;
import com.lms.system.repository.SystemRoleAssignmentRepository;
import com.lms.system.repository.UserRepository;
import com.lms.system.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SystemRoleAssignmentRepository assignmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseCategoryRepository categoryRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private EntityManager entityManager;

    private AuthenticatedUser authenticatedAdmin;
    private Long adminRoleId;

    @BeforeEach
    void createAuthenticatedAdmin() {
        String email = "admin-" + UUID.randomUUID() + "@example.com";
        User admin = userRepository.saveAndFlush(User.register(
                email, "Admin Test User", passwordHasher.hash("StrongPass123")));
        Role role = roleRepository.findByCode("ADMIN")
                .filter(found -> found.getScope() == RoleScope.SYSTEM)
                .orElseThrow();
        SystemRoleAssignment assignment = SystemRoleAssignment.bind(admin, role);
        admin.attachSystemRole(assignment);
        assignmentRepository.saveAndFlush(assignment);
        adminRoleId = role.getId();
        authenticatedAdmin = new AuthenticatedUser(
                admin.getId(),
                admin.getEmail(),
                admin.getFullName(),
                admin.getPasswordHash(),
                true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        entityManager.clear();
    }

    @Test
    void adminDashboardRendersWithLiveSummary() throws Exception {
        assertEquals(1, userRepository.countActiveUsersWithSystemRole("ADMIN"));
        mockMvc.perform(get("/admin").with(user(authenticatedAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Management tools")))
                .andExpect(content().string(containsString("User accounts")));
    }

    @Test
    void userCourseAndCategoryPagesRender() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(authenticatedAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User accounts")));

        mockMvc.perform(get("/admin/courses").with(user(authenticatedAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Course catalog")));

        mockMvc.perform(get("/admin/categories").with(user(authenticatedAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Categories")));
    }

    @Test
    void creationFormsRender() throws Exception {
        mockMvc.perform(get("/admin/users/new").with(user(authenticatedAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create user account")));

        mockMvc.perform(get("/admin/courses/new").with(user(authenticatedAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create course")));

        mockMvc.perform(get("/admin/categories/new").with(user(authenticatedAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create category")));
    }

    @Test
    void accountAndCourseDetailsRenderFromPersistentEntities() throws Exception {
        mockMvc.perform(get("/admin/users/{id}", authenticatedAdmin.getId()).with(user(authenticatedAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin Test User")));

        Course course = courseRepository.saveAndFlush(Course.create(
                "TEST-" + UUID.randomUUID(),
                "Admin managed OOP course",
                "Course detail smoke test",
                CourseStatus.DRAFT,
                null,
                null,
                null,
                authenticatedAdmin.getId()
        ));
        mockMvc.perform(get("/admin/courses/{id}", course.getId()).with(user(authenticatedAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin managed OOP course")))
                .andExpect(content().string(containsString("Course detail smoke test")));
    }

    @Test
    void adminCanCreateUserCategoryAndCourse() throws Exception {
        String email = "created-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/admin/users")
                        .with(user(authenticatedAdmin))
                        .with(csrf())
                        .param("email", email)
                        .param("fullName", "Created Admin User")
                        .param("password", "StrongPass123")
                        .param("admin", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
        User createdUser = userRepository.findWithAccessByEmail(email).orElseThrow();
        assertEquals(true, createdUser.isSystemAdmin());

        String updatedEmail = "updated-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/admin/users/{id}", createdUser.getId())
                        .with(user(authenticatedAdmin))
                        .with(csrf())
                        .param("email", updatedEmail)
                        .param("fullName", "Updated Admin User")
                        .param("admin", "true"))
                .andExpect(status().is3xxRedirection());
        assertEquals("Updated Admin User",
                userRepository.findWithAccessByEmail(updatedEmail).orElseThrow().getFullName());

        mockMvc.perform(post("/admin/users/{id}/suspend", createdUser.getId())
                        .with(user(authenticatedAdmin))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(com.lms.system.model.UserStatus.SUSPENDED,
                userRepository.findWithAccessByEmail(updatedEmail).orElseThrow().getStatus());
        mockMvc.perform(post("/admin/users/{id}/activate", createdUser.getId())
                        .with(user(authenticatedAdmin))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        String categoryName = "Admin category " + UUID.randomUUID();
        mockMvc.perform(post("/admin/categories")
                        .with(user(authenticatedAdmin))
                        .with(csrf())
                        .param("name", categoryName)
                        .param("description", "Managed by the admin page"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));
        Long categoryId = categoryRepository.findAll().stream()
                .filter(category -> category.getName().equals(categoryName))
                .map(category -> category.getId())
                .findFirst()
                .orElseThrow();

        String courseCode = "ADMIN-" + UUID.randomUUID();
        mockMvc.perform(post("/admin/courses")
                        .with(user(authenticatedAdmin))
                        .with(csrf())
                        .param("courseCode", courseCode)
                        .param("title", "Created OOP course")
                        .param("categoryId", categoryId.toString())
                        .param("status", "DRAFT"))
                .andExpect(status().is3xxRedirection());
        Course course = courseRepository.findAll().stream()
                .filter(found -> found.getCourseCode().equals(courseCode))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/admin/courses/{id}", course.getId())
                        .with(user(authenticatedAdmin))
                        .with(csrf())
                        .param("courseCode", courseCode)
                        .param("title", "Updated OOP course")
                        .param("categoryId", categoryId.toString())
                        .param("status", "PUBLISHED")
                        .param("startAt", "2026-10-01T09:00")
                        .param("endAt", "2026-12-01T17:00"))
                .andExpect(status().is3xxRedirection());
        assertEquals(CourseStatus.PUBLISHED,
                courseRepository.findWithCategoryById(course.getId()).orElseThrow().getStatus());

        mockMvc.perform(post("/admin/courses/{id}/archive", course.getId())
                        .with(user(authenticatedAdmin))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(CourseStatus.ARCHIVED,
                courseRepository.findWithCategoryById(course.getId()).orElseThrow().getStatus());

        String updatedCategoryName = categoryName + " updated";
        mockMvc.perform(post("/admin/categories/{id}", categoryId)
                        .with(user(authenticatedAdmin))
                        .with(csrf())
                        .param("name", updatedCategoryName)
                        .param("description", "Updated description"))
                .andExpect(status().is3xxRedirection());
        assertEquals(updatedCategoryName, categoryRepository.findById(categoryId).orElseThrow().getName());

        mockMvc.perform(post("/admin/courses/{id}", course.getId())
                        .with(user(authenticatedAdmin))
                        .with(csrf())
                        .param("courseCode", courseCode)
                        .param("title", "Updated OOP course")
                        .param("categoryId", "")
                        .param("status", "ARCHIVED"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/categories/{id}/delete", categoryId)
                        .with(user(authenticatedAdmin))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(false, categoryRepository.existsById(categoryId));

        mockMvc.perform(post("/admin/users/{id}/delete", createdUser.getId())
                        .with(user(authenticatedAdmin))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(com.lms.system.model.UserStatus.DELETED,
                userRepository.findWithAccessByEmail(updatedEmail).orElseThrow().getStatus());
    }

    @Test
    void nonAdministratorCannotOpenAdminPages() throws Exception {
        AuthenticatedUser standardUser = new AuthenticatedUser(
                authenticatedAdmin.getId(),
                authenticatedAdmin.getUsername(),
                authenticatedAdmin.getFullName(),
                authenticatedAdmin.getPassword(),
                true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(get("/admin").with(user(standardUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokedAdministratorCannotUseAnExistingAdminSession() throws Exception {
        assignmentRepository.deleteById(new SystemRoleAssignmentId(authenticatedAdmin.getId(), adminRoleId));
        assignmentRepository.flush();
        entityManager.clear();

        mockMvc.perform(get("/admin").with(user(authenticatedAdmin)))
                .andExpect(status().isForbidden());
    }
}
