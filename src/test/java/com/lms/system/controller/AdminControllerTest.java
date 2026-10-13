package com.lms.system.controller;

import com.lms.system.model.PasswordHasher;
import com.lms.system.model.Course;
import com.lms.system.model.CourseMembership;
import com.lms.system.model.CourseMembershipId;
import com.lms.system.model.CourseMembershipStatus;
import com.lms.system.model.CourseStatus;
import com.lms.system.model.Role;
import com.lms.system.model.RoleScope;
import com.lms.system.model.SystemRoleAssignment;
import com.lms.system.model.SystemRoleAssignmentId;
import com.lms.system.model.User;
import com.lms.system.repository.CourseCategoryRepository;
import com.lms.system.repository.CourseMembershipRepository;
import com.lms.system.repository.CourseRepository;
import com.lms.system.repository.RoleRepository;
import com.lms.system.repository.SystemRoleAssignmentRepository;
import com.lms.system.repository.UserRepository;
import com.lms.system.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import jakarta.persistence.EntityManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
    private CourseMembershipRepository membershipRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AuthenticatedUser authenticatedAdmin;
    private Long adminRoleId;

    @BeforeEach
    void createAuthenticatedAdmin() {
        String email = "admin-" + UUID.randomUUID() + "@example.com";
        User admin = userRepository.saveAndFlush(User.register(
                email, "Admin Test User", passwordHasher.hash("StrongPass123")));
        Role role = roleRepository.findByCode("ADMIN").orElseGet(() ->
                roleRepository.saveAndFlush(Role.catalog("ADMIN", "Administrator", RoleScope.SYSTEM)));
        roleRepository.findByCode("TEACHER").orElseGet(() ->
                roleRepository.saveAndFlush(Role.catalog("TEACHER", "Teacher", RoleScope.COURSE)));
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
                .andExpect(content().string(containsString("Course management")));

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
    void sampleCourseCardsOpenTheirDetailsPage() throws Exception {
        mockMvc.perform(get("/courses/demo/psychology-in-cinema").with(user(authenticatedAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Psychology in Cinema")))
                .andExpect(content().string(containsString("Dr. Olivia Bennett")));
    }

    @Test
    void myCoursesAndDashboardShowEnrolledCoursesAndOpenTheirSections() throws Exception {
        Role studentRole = roleRepository.findByCode("STUDENT").orElseGet(() ->
                roleRepository.saveAndFlush(Role.catalog("STUDENT", "Student", RoleScope.COURSE)));
        User student = userRepository.saveAndFlush(User.register(
                "student-" + UUID.randomUUID() + "@example.com",
                "Course Flow Student",
                passwordHasher.hash("StrongPass123")
        ));
        Course course = courseRepository.saveAndFlush(Course.create(
                "FLOW-" + UUID.randomUUID(),
                "Live database course",
                "Course flow test description",
                CourseStatus.PUBLISHED,
                null,
                null,
                null,
                authenticatedAdmin.getId()
        ));
        membershipRepository.saveAndFlush(CourseMembership.bind(
                student, course.getId(), studentRole, CourseMembershipStatus.ACTIVE));
        jdbcTemplate.update("""
                INSERT INTO course_sections (course_id, title, description, position, status)
                VALUES (?, ?, ?, 1, 'VISIBLE')
                """, course.getId(), "Introduction", "Start here");
        Long sectionId = jdbcTemplate.queryForObject(
                "SELECT id FROM course_sections WHERE course_id = ?", Long.class, course.getId());
        AuthenticatedUser authenticatedStudent = new AuthenticatedUser(
                student.getId(),
                student.getEmail(),
                student.getFullName(),
                student.getPasswordHash(),
                true,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        mockMvc.perform(get("/home").with(user(authenticatedStudent)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Live database course")))
                .andExpect(content().string(containsString("href=\"/courses/" + course.getId() + "?theme=light\"")));
        mockMvc.perform(get("/dashboard").with(user(authenticatedStudent)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Live database course")))
                .andExpect(content().string(containsString("href=\"/courses/" + course.getId() + "?theme=light\"")));
        mockMvc.perform(get("/courses/{id}", course.getId()).with(user(authenticatedStudent)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Course flow test description")))
                .andExpect(content().string(containsString("Introduction")))
                .andExpect(content().string(containsString(
                        "href=\"/courses/" + course.getId() + "/sections/" + sectionId + "\"")));
    }

    @Test
    void teacherCanPublishLessonsAndAnnouncementsAndStudentCanEnrollAndViewThem() throws Exception {
        Role teacherRole = roleRepository.findByCode("TEACHER").orElseThrow();
        roleRepository.findByCode("STUDENT").orElseGet(() ->
                roleRepository.saveAndFlush(Role.catalog("STUDENT", "Student", RoleScope.COURSE)));
        User teacher = userRepository.saveAndFlush(User.register(
                "teacher-flow-" + UUID.randomUUID() + "@example.com",
                "Course Flow Teacher",
                passwordHasher.hash("StrongPass123")
        ));
        User student = userRepository.saveAndFlush(User.register(
                "student-flow-" + UUID.randomUUID() + "@example.com",
                "Course Flow Learner",
                passwordHasher.hash("StrongPass123")
        ));
        Course course = courseRepository.saveAndFlush(Course.create(
                "INTERACTION-" + UUID.randomUUID(),
                "Interactive course",
                "A course with real web interactions",
                CourseStatus.PUBLISHED,
                null,
                null,
                null,
                authenticatedAdmin.getId()
        ));
        membershipRepository.saveAllAndFlush(List.of(CourseMembership.bind(
                teacher, course.getId(), teacherRole, CourseMembershipStatus.ACTIVE
        )));
        AuthenticatedUser authenticatedTeacher = authenticatedUser(teacher, "TEACHER");
        AuthenticatedUser authenticatedStudent = authenticatedUser(student, "STUDENT");
        mockMvc.perform(get("/courses/{courseId}", course.getId()).with(user(authenticatedTeacher)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add section")));
        mockMvc.perform(post("/courses/{courseId}/sections", course.getId())
                        .with(user(authenticatedTeacher)).with(csrf())
                        .param("theme", "light")
                        .param("title", "Getting started")
                        .param("description", "First section"))
                .andExpect(status().is3xxRedirection());
        Long sectionId = jdbcTemplate.queryForObject(
                "SELECT id FROM course_sections WHERE course_id = ?", Long.class, course.getId());

        mockMvc.perform(post("/courses/{courseId}/announcements", course.getId())
                        .with(user(authenticatedTeacher)).with(csrf())
                        .param("theme", "light")
                        .param("title", "Welcome")
                        .param("content", "The course is ready."))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/courses/{courseId}/sections/{sectionId}/lessons", course.getId(), sectionId)
                        .with(user(authenticatedTeacher)).with(csrf())
                        .param("theme", "light")
                        .param("title", "First lesson")
                        .param("description", "Introduction")
                        .param("content", "<p>Lesson content</p>"))
                .andExpect(status().is3xxRedirection());
        Long lessonId = jdbcTemplate.queryForObject("""
                SELECT a.id FROM activities a
                JOIN course_sections cs ON cs.id = a.section_id
                WHERE cs.course_id = ? AND cs.id = ? AND a.activity_type = 'LESSON'
                """, Long.class, course.getId(), sectionId);

        mockMvc.perform(get("/courses/catalog").with(user(authenticatedStudent)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Interactive course")));
        mockMvc.perform(post("/courses/{courseId}/enroll", course.getId())
                        .with(user(authenticatedStudent)).with(csrf()).param("theme", "light"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/home").with(user(authenticatedStudent)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Interactive course")));
        mockMvc.perform(get("/dashboard").with(user(authenticatedStudent)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Interactive course")))
                .andExpect(content().string(containsString("The course is ready.")))
                .andExpect(content().string(containsString("Welcome")));
        mockMvc.perform(get("/courses/{courseId}", course.getId()).with(user(authenticatedStudent)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The course is ready.")))
                .andExpect(content().string(containsString("Getting started")));
        mockMvc.perform(get("/courses/{courseId}/sections/{sectionId}", course.getId(), sectionId)
                        .with(user(authenticatedStudent)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("First lesson")))
                .andExpect(content().string(containsString("Lesson content")));
        mockMvc.perform(post("/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}",
                        course.getId(), sectionId, lessonId)
                        .with(user(authenticatedTeacher)).with(csrf())
                        .param("theme", "light")
                        .param("title", "Updated lesson")
                        .param("description", "Updated description")
                        .param("content", "<p>Updated lesson content</p>"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/courses/{courseId}/sections/{sectionId}", course.getId(), sectionId)
                        .with(user(authenticatedStudent)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Updated lesson")))
                .andExpect(content().string(containsString("Updated lesson content")));
        mockMvc.perform(post("/courses/{courseId}/sections/{sectionId}/lessons/{lessonId}/delete",
                        course.getId(), sectionId, lessonId)
                        .with(user(authenticatedTeacher)).with(csrf()).param("theme", "light"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/courses/{courseId}/sections/{sectionId}", course.getId(), sectionId)
                        .with(user(authenticatedStudent)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chương này chưa có bài học.")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(containsString("Updated lesson content"))));
    }

    private AuthenticatedUser authenticatedUser(User user, String role) {
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPasswordHash(),
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
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
    void courseSearchMatchesInstructorName() throws Exception {
        String instructorName = "Instructor " + UUID.randomUUID();
        User instructor = userRepository.saveAndFlush(User.register(
                "teacher-" + UUID.randomUUID() + "@example.com",
                instructorName,
                passwordHasher.hash("StrongPass123")
        ));
        String courseCode = "TEACHER-" + UUID.randomUUID();
        mockMvc.perform(post("/admin/courses")
                        .with(user(authenticatedAdmin))
                        .with(csrf())
                        .param("courseCode", courseCode)
                        .param("title", "Course taught by instructor")
                        .param("status", "PUBLISHED")
                        .param("instructorIds", instructor.getId().toString()))
                .andExpect(status().is3xxRedirection());
        Course course = courseRepository.findAll().stream()
                .filter(found -> found.getCourseCode().equals(courseCode))
                .findFirst()
                .orElseThrow();
        CourseMembershipId membershipId = new CourseMembershipId(course.getId(), instructor.getId());
        assertEquals(CourseMembershipStatus.ACTIVE,
                membershipRepository.findById(membershipId).orElseThrow().getStatus());

        mockMvc.perform(get("/admin/courses")
                        .with(user(authenticatedAdmin))
                        .param("q", instructorName))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(instructorName)))
                .andExpect(content().string(containsString("Course taught by instructor")));

        mockMvc.perform(post("/admin/courses/{id}", course.getId())
                        .with(user(authenticatedAdmin))
                        .with(csrf())
                        .param("courseCode", courseCode)
                        .param("title", "Course taught by instructor")
                        .param("status", "PUBLISHED"))
                .andExpect(status().is3xxRedirection());
        assertEquals(CourseMembershipStatus.WITHDRAWN,
                membershipRepository.findById(membershipId).orElseThrow().getStatus());
        mockMvc.perform(get("/admin/courses")
                        .with(user(authenticatedAdmin))
                        .param("q", instructorName))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No courses found")));
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
