package com.lms.system.dto.api;

import java.util.List;

public record ApiCurrentUserResponse(Long id, String email, List<String> roles) {
}
