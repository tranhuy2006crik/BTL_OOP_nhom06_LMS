package com.lms.system.service;

import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.exception.PasswordResetDeliveryException;
import com.lms.system.model.PasswordHasher;
import com.lms.system.model.User;
import com.lms.system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final PasswordResetService passwordResetService = new PasswordResetService(
            userRepository, passwordHasher, mailSender, "https://lms.example.com/", "no-reply@example.com"
    );

    @Test
    void requestSendsLinkWithShortLivedInMemoryToken() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(12L);
        when(user.getEmail()).thenReturn("student@example.com");
        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));

        passwordResetService.requestPasswordReset(" Student@Example.com ");

        org.mockito.ArgumentCaptor<SimpleMailMessage> messageCaptor =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        String body = messageCaptor.getValue().getText();
        String rawToken = extractToken(body);

        assertTrue(body.contains("https://lms.example.com/reset-password?token="));
        assertTrue(body.contains("30 phút"));
        assertTrue(passwordResetService.isTokenValid(rawToken));
        assertTrue(messageCaptor.getValue().getFrom().equals("no-reply@example.com"));
    }

    @Test
    void requestForUnknownEmailDoesNotSendEmail() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset("missing@example.com");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void mailDeliveryFailureIsReportedAndTokenIsDiscarded() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(12L);
        when(user.getEmail()).thenReturn("student@example.com");
        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(PasswordResetDeliveryException.class,
                () -> passwordResetService.requestPasswordReset("student@example.com"));

        org.mockito.ArgumentCaptor<SimpleMailMessage> messageCaptor =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertFalse(passwordResetService.isTokenValid(extractToken(messageCaptor.getValue().getText())));
    }

    @Test
    void validResetChangesPasswordAndConsumesTokenExactlyOnce() {
        User user = mock(User.class);
        String rawToken = issueTokenFor(user);
        when(userRepository.findById(12L)).thenReturn(Optional.of(user));

        passwordResetService.resetPassword(rawToken, "NewStrongPass1", "NewStrongPass1");

        verify(user).resetPassword("NewStrongPass1", passwordHasher);
        assertFalse(passwordResetService.isTokenValid(rawToken));
        assertThrows(InvalidPasswordException.class,
                () -> passwordResetService.resetPassword(rawToken, "OtherStrongPass1", "OtherStrongPass1"));
    }

    @Test
    void mismatchedPasswordDoesNotConsumeValidToken() {
        User user = mock(User.class);
        String rawToken = issueTokenFor(user);

        assertThrows(InvalidPasswordException.class,
                () -> passwordResetService.resetPassword(rawToken, "NewStrongPass1", "OtherStrongPass1"));

        assertTrue(passwordResetService.isTokenValid(rawToken));
        verify(userRepository, never()).findById(12L);
    }

    @Test
    void requestingAnotherLinkInvalidatesThePreviousOne() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(12L);
        when(user.getEmail()).thenReturn("student@example.com");
        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));

        passwordResetService.requestPasswordReset("student@example.com");
        org.mockito.ArgumentCaptor<SimpleMailMessage> firstMessage =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(firstMessage.capture());
        String oldToken = extractToken(firstMessage.getValue().getText());

        passwordResetService.requestPasswordReset("student@example.com");
        org.mockito.ArgumentCaptor<SimpleMailMessage> messages =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.times(2)).send(messages.capture());
        String newToken = extractToken(messages.getAllValues().get(1).getText());

        assertFalse(passwordResetService.isTokenValid(oldToken));
        assertTrue(passwordResetService.isTokenValid(newToken));
    }

    private String issueTokenFor(User user) {
        when(user.getId()).thenReturn(12L);
        when(user.getEmail()).thenReturn("student@example.com");
        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        passwordResetService.requestPasswordReset("student@example.com");

        org.mockito.ArgumentCaptor<SimpleMailMessage> messageCaptor =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        return extractToken(messageCaptor.getValue().getText());
    }

    private String extractToken(String body) {
        Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("Reset email did not contain a token link");
        }
        return matcher.group(1);
    }
}
