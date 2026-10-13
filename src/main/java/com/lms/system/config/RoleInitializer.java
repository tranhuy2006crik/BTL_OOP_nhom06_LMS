package com.lms.system.config;

import com.lms.system.model.Role;
import com.lms.system.model.RoleCode;
import com.lms.system.model.RoleScope;
import com.lms.system.repository.RoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RoleInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    public RoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureRole(RoleCode.ADMIN, "Administrator", RoleScope.SYSTEM);
        ensureRole(RoleCode.TEACHER, "Teacher", RoleScope.COURSE);
        ensureRole(RoleCode.STUDENT, "Student", RoleScope.COURSE);
    }

    private void ensureRole(RoleCode code, String name, RoleScope scope) {
        if (roleRepository.findByCode(code.name()).isEmpty()) {
            roleRepository.save(Role.catalog(code.name(), name, scope));
        }
    }
}
