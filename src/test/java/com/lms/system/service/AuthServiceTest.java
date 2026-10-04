package com.lms.system.service;

import com.lms.system.dto.RegisterForm;
import com.lms.system.exception.DuplicateEmailException;
import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.model.PasswordHasher;
import com.lms.system.model.User;
import com.lms.system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final AuthService authService = new AuthService(userRepository, passwordHasher);

    @BeforeEach
    void resetMocks() {
        org.mockito.Mockito.reset(userRepository, passwordHasher);
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndPersistsUser() {
        RegisterForm form = registerForm("  Student@Example.com ", "StrongPass1", "StrongPass1");
        when(userRepository.existsByEmailIgnoreCase("student@example.com")).thenReturn(false);
        when(passwordHasher.hash("StrongPass1")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = authService.register(form);

        assertEquals("student@example.com", registered.getEmail());
        assertEquals("bcrypt-hash", registered.getPasswordHash());
        assertEquals("Nguyen Van An", registered.getFullName());
        verify(userRepository).save(registered);
    }

    @Test
    void registerRejectsExistingEmailBeforeHashingOrSaving() {
        RegisterForm form = registerForm("student@example.com", "StrongPass1", "StrongPass1");
        when(userRepository.existsByEmailIgnoreCase("student@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(form));

        verify(passwordHasher, never()).hash(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsMismatchedAndWeakPasswordsBeforeSaving() {
        RegisterForm mismatched = registerForm("student@example.com", "StrongPass1", "DifferentPass1");
        assertThrows(InvalidPasswordException.class, () -> authService.register(mismatched));

        RegisterForm weak = registerForm("student@example.com", "password", "password");
        assertThrows(InvalidPasswordException.class, () -> authService.register(weak));

        verify(userRepository, never()).save(any());
        verify(passwordHasher, never()).hash(any());
    }

    private RegisterForm registerForm(String email, String password, String confirmation) {
        RegisterForm form = new RegisterForm();
        form.setEmail(email);
        form.setFullName("Nguyen Van An");
        form.setPassword(password);
        form.setConfirmPassword(confirmation);
        return form;
    }
}
