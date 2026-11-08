package com.example.ums;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Exactly three integration tests across security, HTTP layer, and H2 JPA persistence. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserApiIT {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepo repo;
    @Autowired private PasswordEncoder encoder;

    @AfterEach
    void cleanCreatedRecords() {
        repo.findByUsername("integration-student").ifPresent(repo::delete);
        repo.findByUsername("access-test-student").ifPresent(repo::delete);
    }

    @Test
    void IT01_adminBasicLoginReturnsProfile() throws Exception {
        // CommandLineRunner creates the admin in the isolated test database.
        mvc.perform(get("/api/me").with(httpBasic("admin", "admin123"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void IT02_adminCanCreateAndRetrieveStudent() throws Exception {
        mvc.perform(post("/api/students")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"integration-student", "password":"secret123", "name":"Integration Student"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("integration-student"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist());

        AppUser persisted = repo.findByUsername("integration-student").orElseThrow();
        assertTrue(encoder.matches("secret123", persisted.getPassword()),
                "The database must store the hashed password, not the raw password");

        mvc.perform(get("/api/students/{id}", persisted.getId())
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integration-student"));
    }

    @Test
    void IT03_DEMO_FAIL_studentUnauthorizedAccessWrongExpectedStatus() throws Exception {
        AppUser student = new AppUser();
        student.setUsername("access-test-student");
        student.setPassword(encoder.encode("student-pass"));
        student.setName("Access Test Student");
        student.setRole(Role.STUDENT);
        repo.save(student);

        mvc.perform(get("/api/teachers")
                        .with(httpBasic("access-test-student", "student-pass"))
                        .accept(MediaType.APPLICATION_JSON))
                // INTENTIONAL FAILURE for a CI demo: Spring Security correctly
                // denies access with 403, but the test wrongly expects 200.
                // Restore isForbidden() to make the test pass.
                .andExpect(status().isOk());
    }
}
