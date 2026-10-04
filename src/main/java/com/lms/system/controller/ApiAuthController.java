package com.lms.system.controller;

import com.lms.system.dto.api.ApiCurrentUserResponse;
import com.lms.system.dto.api.ApiLoginRequest;
import com.lms.system.dto.api.ApiRegistrationResponse;
import com.lms.system.dto.api.ApiTokenResponse;
import com.lms.system.dto.api.ApiRegisterRequest;
import com.lms.system.model.User;
import com.lms.system.security.AuthenticatedUser;
import com.lms.system.security.JwtTokenService;
import com.lms.system.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public ApiAuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService
    ) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiRegistrationResponse register(@Valid @RequestBody ApiRegisterRequest request) {
        User user = authService.register(request.toRegisterForm());
        return new ApiRegistrationResponse(user.getId(), user.getEmail(), user.getFullName());
    }

    @PostMapping("/login")
    public ApiTokenResponse login(@Valid @RequestBody ApiLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.getEmail().trim(),
                        request.getPassword()
                )
        );
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        JwtTokenService.IssuedToken token = jwtTokenService.issue(user);
        return new ApiTokenResponse("Bearer", token.accessToken(), token.expiresIn());
    }

    @GetMapping("/me")
    public ApiCurrentUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new ApiCurrentUserResponse(
                jwt.getClaim("userId"),
                jwt.getSubject(),
                roles == null ? List.of() : roles
        );
    }
}
