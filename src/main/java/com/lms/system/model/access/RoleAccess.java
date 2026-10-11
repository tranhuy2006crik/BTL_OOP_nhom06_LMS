package com.lms.system.model.access;

import com.lms.system.model.Permission;
import com.lms.system.model.RoleCode;

import java.util.Set;

public abstract class RoleAccess {

    public final boolean grants(Permission permission, AccessContext context) {
        if (permission == null || context == null) {
            return false;
        }
        if (permission.isSelfService()) {
            return true;
        }
        if (!appliesTo(context)) {
            return false;
        }
        return allowedPermissions().contains(permission);
    }

    public abstract RoleCode roleCode();

    protected abstract boolean appliesTo(AccessContext context);

    protected abstract Set<Permission> allowedPermissions();
}
