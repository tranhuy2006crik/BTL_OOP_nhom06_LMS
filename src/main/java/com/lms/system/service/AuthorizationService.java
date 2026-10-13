package com.lms.system.service;

import com.lms.system.exception.ResourceNotFoundException;
import com.lms.system.model.Permission;
import com.lms.system.model.User;
import com.lms.system.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationService {

    private final UserRepository userRepository;

    public AuthorizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public boolean can(Long userId, Permission permission, Long courseId) {
        User user = userRepository.findWithAccessById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        return user.hasPermission(permission, courseId);
    }

    public boolean can(User user, Permission permission, Long courseId) {
        return user != null && user.hasPermission(permission, courseId);
    }
}
