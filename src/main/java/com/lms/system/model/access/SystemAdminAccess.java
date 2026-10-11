package com.lms.system.model.access;

import com.lms.system.model.Permission;
import com.lms.system.model.RoleCode;

import java.util.EnumSet;
import java.util.Set;

public final class SystemAdminAccess extends RoleAccess {

    private static final Set<Permission> ALL = EnumSet.allOf(Permission.class);

    @Override
    public RoleCode roleCode() {
        return RoleCode.ADMIN;
    }

    @Override
    protected boolean appliesTo(AccessContext context) {
        return true;
    }

    @Override
    protected Set<Permission> allowedPermissions() {
        return ALL;
    }
}
