package com.example.ums;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

/** Exactly three isolated unit tests; no Spring context and no database. */
class UserControllerTest {

    private UserRepo repo;
    private PasswordEncoder encoder;
    private UserController controller;

    @BeforeEach
    void setUp() {
        repo = mock(UserRepo.class);
        encoder = mock(PasswordEncoder.class);
        controller = new UserController(repo, encoder);
    }

    @Test
    void UT01_createStudentEncryptsPasswordAndAssignsRole() {
        AppUser request = new AppUser();
        request.setUsername("sam");
        request.setPassword("plain-password");
        request.setName("Sam Lee");
        when(repo.existsByUsername("sam")).thenReturn(false);
        when(encoder.encode("plain-password")).thenReturn("bcrypt-hash");
        when(repo.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<AppUser> response = controller.create("students", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        AppUser saved = response.getBody();
        assertNotNull(saved);
        assertEquals("sam", saved.getUsername());
        assertEquals(Role.STUDENT, saved.getRole());
        assertEquals("bcrypt-hash", saved.getPassword());
        verify(encoder).encode("plain-password");
        verify(repo).save(request);
    }

    @Test
    void UT02_DEMO_FAIL_duplicateUsernameWrongExpectedStatus() {
        AppUser request = new AppUser();
        request.setUsername("already-used");
        request.setPassword("password");
        when(repo.existsByUsername("already-used")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.create("students", request));

        // INTENTIONAL FAILURE for a CI demo: the real endpoint correctly returns
        // 409 CONFLICT, but this assertion incorrectly demands 400 BAD_REQUEST.
        // Restore HttpStatus.CONFLICT to make the test pass.
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode(),
                "INTENTIONAL DEMO FAILURE: duplicate username is correctly HTTP 409, not HTTP 400");
        verify(repo, never()).save(any(AppUser.class));
        verifyNoInteractions(encoder);
    }

    @Test
    void UT03_updateUserChangesNameAndEncryptsNewPassword() {
        AppUser existing = new AppUser();
        existing.setUsername("teacher1");
        existing.setName("Old Name");
        existing.setPassword("old-hash");
        existing.setRole(Role.TEACHER);

        AppUser changes = new AppUser();
        changes.setName("New Name");
        changes.setPassword("new-password");

        when(repo.findById(42L)).thenReturn(Optional.of(existing));
        when(encoder.encode("new-password")).thenReturn("new-hash");
        when(repo.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser updated = controller.update("teachers", 42L, changes);

        assertEquals("New Name", updated.getName());
        assertEquals("new-hash", updated.getPassword());
        assertEquals("teacher1", updated.getUsername());
        assertEquals(Role.TEACHER, updated.getRole());
        verify(encoder).encode("new-password");
        verify(repo).save(existing);
    }
}
