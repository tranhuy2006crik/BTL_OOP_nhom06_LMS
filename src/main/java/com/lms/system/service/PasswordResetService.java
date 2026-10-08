package com.lms.system.service;

import com.lms.system.exception.InvalidPasswordException;
import com.lms.system.exception.PasswordResetDeliveryException;
import com.lms.system.model.PasswordHasher;
import com.lms.system.model.User;
import com.lms.system.repository.UserRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_LIFETIME_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JavaMailSender mailSender;
    private final String baseUrl;
    private final String senderAddress;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Object tokenLock = new Object();
    private final Map<String, ResetToken> tokensByHash = new HashMap<>();
    private final Map<Long, String> tokenHashesByUserId = new HashMap<>();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            JavaMailSender mailSender,
            @Value("${lms.web.base-url}") String baseUrl,
            @Value("${lms.mail.from}") String senderAddress
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.mailSender = mailSender;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.senderAddress = senderAddress;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        User.normalizeEmail(email);

        userRepository.findByEmailIgnoreCase(User.normalizeEmail(email)).ifPresent(user -> {
            byte[] randomToken = new byte[32];
            secureRandom.nextBytes(randomToken);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
            String tokenHash = sha256(rawToken);
            synchronized (tokenLock) {
                removeExpiredTokens(now);
                String previousHash = tokenHashesByUserId.put(user.getId(), tokenHash);
                if (previousHash != null) {
                    tokensByHash.remove(previousHash);
                }
                tokensByHash.put(tokenHash, new ResetToken(user.getId(), now.plusMinutes(TOKEN_LIFETIME_MINUTES)));
            }
            try {
                sendResetEmail(user.getEmail(), rawToken);
            } catch (PasswordResetDeliveryException ex) {
                synchronized (tokenLock) {
                    tokensByHash.remove(tokenHash);
                    tokenHashesByUserId.remove(user.getId(), tokenHash);
                }
                throw ex;
            }
        });
    }

    public boolean isTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        synchronized (tokenLock) {
            ResetToken token = tokensByHash.get(sha256(rawToken));
            if (token == null || !token.expiresAt().isAfter(now)) {
                if (token != null) {
                    removeToken(sha256(rawToken), token);
                }
                return false;
            }
            return true;
        }
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword, String confirmation) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidPasswordException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
        }
        if (newPassword == null || !newPassword.equals(confirmation)) {
            throw new InvalidPasswordException("Mật khẩu nhập lại không khớp");
        }
        String tokenHash = sha256(rawToken);
        synchronized (tokenLock) {
            ResetToken token = tokensByHash.get(tokenHash);
            if (token == null || !token.expiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
                if (token != null) {
                    removeToken(tokenHash, token);
                }
                throw new InvalidPasswordException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
            }
            User user = userRepository.findById(token.userId())
                    .orElseThrow(() -> new InvalidPasswordException(
                            "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"
                    ));
            user.resetPassword(newPassword, passwordHasher);
            removeToken(tokenHash, token);
        }
    }

    private void removeExpiredTokens(LocalDateTime now) {
        tokensByHash.entrySet().removeIf(entry -> {
            if (entry.getValue().expiresAt().isAfter(now)) {
                return false;
            }
            tokenHashesByUserId.remove(entry.getValue().userId(), entry.getKey());
            return true;
        });
    }

    private void removeToken(String tokenHash, ResetToken token) {
        tokensByHash.remove(tokenHash);
        tokenHashesByUserId.remove(token.userId(), tokenHash);
    }

    private void sendResetEmail(String email, String rawToken) {
        try {
            InternetAddress sender = new InternetAddress(senderAddress);
            sender.validate();
        } catch (AddressException ex) {
            logger.error("Password reset email was not sent because MAIL_FROM is invalid", ex);
            throw new PasswordResetDeliveryException("Password reset email could not be sent", ex);
        }

        String resetUrl = baseUrl + "/reset-password?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(email);
        message.setSubject("Đặt lại mật khẩu LMS");
        message.setText("Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản này.\n\n"
                + "Mở liên kết sau trong vòng " + TOKEN_LIFETIME_MINUTES + " phút để đặt mật khẩu mới:\n"
                + resetUrl + "\n\n"
                + "Nếu bạn không yêu cầu, hãy bỏ qua email này. Mật khẩu hiện tại của bạn sẽ không thay đổi.");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            logger.error("Password reset email could not be delivered", ex);
            throw new PasswordResetDeliveryException("Password reset email could not be delivered", ex);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private record ResetToken(Long userId, LocalDateTime expiresAt) {
    }
}
