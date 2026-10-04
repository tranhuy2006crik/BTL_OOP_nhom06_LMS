package com.lms.system.service;

import com.lms.system.dto.ChangePasswordForm;
import com.lms.system.dto.ProfileForm;
import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.exception.ResourceNotFoundException;
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

class ProfileServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final ProfileService profileService = new ProfileService(userRepository, passwordHasher);
    private User user;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.reset(userRepository, passwordHasher);
        user = User.register("student@example.com", "Old Name", "bcrypt-old");
        when(userRepository.findWithAccessById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void updateProfileChangesOnlyTheAuthenticatedUsersName() {
        ProfileForm form = new ProfileForm("  New Name  ");

        User updated = profileService.updateProfile(7L, form);

        assertEquals("New Name", updated.getFullName());
        assertEquals("student@example.com", updated.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordRequiresConfirmationAndCurrentPassword() {
        ChangePasswordForm mismatched = changePasswordForm("OldPass1", "NewPass123", "DifferentPass123");
        assertThrows(InvalidPasswordException.class, () -> profileService.changePassword(7L, mismatched));

        ChangePasswordForm wrongCurrent = changePasswordForm("WrongPass1", "NewPass123", "NewPass123");
        when(passwordHasher.matches("WrongPass1", "bcrypt-old")).thenReturn(false);
        assertThrows(InvalidPasswordException.class, () -> profileService.changePassword(7L, wrongCurrent));

        verify(userRepository, never()).save(any());
        verify(passwordHasher, never()).hash(any());
    }

    @Test
    void changePasswordStoresANewHashAfterValidatingTheCurrentPassword() {
        ChangePasswordForm form = changePasswordForm("OldPass1", "NewPass123", "NewPass123");
        when(passwordHasher.matches("OldPass1", "bcrypt-old")).thenReturn(true);
        when(passwordHasher.matches("NewPass123", "bcrypt-old")).thenReturn(false);
        when(passwordHasher.hash("NewPass123")).thenReturn("bcrypt-new");

        profileService.changePassword(7L, form);

        assertEquals("bcrypt-new", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    void profileLookupReportsMissingAccounts() {
        when(userRepository.findWithAccessById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> profileService.getProfile(99L));
    }

    private ChangePasswordForm changePasswordForm(String current, String next, String confirmation) {
        ChangePasswordForm form = new ChangePasswordForm();
        form.setCurrentPassword(current);
        form.setNewPassword(next);
        form.setConfirmNewPassword(confirmation);
        return form;
    }
}
