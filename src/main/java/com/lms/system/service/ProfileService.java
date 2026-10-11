package com.lms.system.service;

import com.lms.system.dto.ChangePasswordForm;
import com.lms.system.dto.ProfileForm;
import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.exception.ResourceNotFoundException;
import com.lms.system.model.PasswordHasher;
import com.lms.system.model.User;
import com.lms.system.model.UserProfileDetails;
import com.lms.system.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public ProfileService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional(readOnly = true)
    public User getProfile(Long userId) {
        return userRepository.findWithAccessById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ"));
    }

    @Transactional
    public User updateProfile(
            Long userId,
            ProfileForm form,
            byte[] profilePicture,
            String pictureContentType,
            boolean deleteProfilePicture
    ) {
        User user = getProfile(userId);
        UserProfileDetails details = form.toUserProfileDetails();
        if (profilePicture != null && profilePicture.length > 0) {
            User.validateProfilePicture(profilePicture, pictureContentType);
        }
        user.updateProfile(form.getFullName(), details);
        if (profilePicture != null && profilePicture.length > 0) {
            user.updateProfilePicture(profilePicture, pictureContentType);
        } else if (deleteProfilePicture) {
            user.updateProfilePicture(null, null);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(Long userId, ProfileForm form) {
        return updateProfile(userId, form, null, null, false);
    }

    @Transactional(readOnly = true)
    public User getProfilePicture(Long userId) {
        return getProfile(userId);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordForm form) {
        if (!form.getNewPassword().equals(form.getConfirmNewPassword())) {
            throw new InvalidPasswordException("Mật khẩu mới nhập lại không khớp");
        }
        User user = getProfile(userId);
        user.changePassword(form.getCurrentPassword(), form.getNewPassword(), passwordHasher);
        userRepository.save(user);
    }
}
