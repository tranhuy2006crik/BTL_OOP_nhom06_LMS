package com.lms.system.service;

import com.lms.system.dto.RegisterForm;
import com.lms.system.exception.DuplicateEmailException;
import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.model.PasswordHasher;
import com.lms.system.model.User;
import com.lms.system.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public User register(RegisterForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new InvalidPasswordException("Mật khẩu nhập lại không khớp");
        }
        User.validateRawPassword(form.getPassword());
        String email = User.normalizeEmail(form.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }
        User user = User.register(email, form.getFullName(), passwordHasher.hash(form.getPassword()));
        return userRepository.save(user);
    }
}
