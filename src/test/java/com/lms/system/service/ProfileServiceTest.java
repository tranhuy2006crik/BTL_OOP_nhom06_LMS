package com.lms.system.service;

import com.lms.system.dto.ChangePasswordForm;
import com.lms.system.dto.ProfileForm;
import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.exception.ResourceNotFoundException;
import com.lms.system.model.PasswordHasher;
import com.lms.system.model.User;
import com.lms.system.repository.UserRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void updateProfilePersistsEditableDetailsForTheAuthenticatedUser() {
        ProfileForm form = new ProfileForm("  New Name  ");
        form.setCityTown(" Orange City ");
        form.setCountry("Australia");
        form.setTimezone("Australia/Sydney");
        form.setDescription("Learner profile");
        form.setInterests("music, films");
        form.setPhone("+61 555 0100");
        form.setEmailVisibility("HIDDEN");

        User updated = profileService.updateProfile(7L, form);

        assertEquals("New Name", updated.getFullName());
        assertEquals("student@example.com", updated.getEmail());
        assertEquals("Orange City", updated.getCityTown());
        assertEquals("Australia", updated.getCountry());
        assertEquals("Australia/Sydney", updated.getTimezone());
        assertEquals("Learner profile", updated.getDescription());
        assertEquals("music, films", updated.getInterests());
        assertEquals("+61 555 0100", updated.getPhone());
        assertEquals("HIDDEN", updated.getEmailVisibility());
        verify(userRepository).save(user);
    }

    @Test
    void profileFormSplitsAndCombinesTheExistingFullName() {
        ProfileForm form = new ProfileForm(user);

        assertEquals("Old", form.getFirstName());
        assertEquals("Name", form.getLastName());

        form.setFirstName("New");
        form.setLastName("Learner");
        assertEquals("New Learner", form.getFullName());

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(validatorFactory.getValidator().validate(form).isEmpty());
        }
    }

    @Test
    void updateProfileStoresSupportedImageBytesAndRejectsUnsupportedImageData() {
        ProfileForm form = new ProfileForm("Learner");
        byte[] pngSignature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

        profileService.updateProfile(7L, form, pngSignature, "image/png", false);

        org.junit.jupiter.api.Assertions.assertArrayEquals(pngSignature, user.getProfilePicture());
        assertEquals("image/png", user.getProfilePictureContentType());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> profileService.updateProfile(7L, form, new byte[]{1, 2, 3}, "image/png", false));
        assertEquals("Learner", user.getFullName());
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
