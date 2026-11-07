package com.careermate.ai;

import com.careermate.ai.GeminiKeyDtos.GeminiKeyStatus;
import com.careermate.common.exception.BadRequestException;
import com.careermate.common.exception.ResourceNotFoundException;
import com.careermate.security.ApiKeyEncryptionService;
import com.careermate.user.User;
import com.careermate.user.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiKeyService {
    private static final int MIN_KEY_LENGTH = 20;
    private static final int MAX_KEY_LENGTH = 512;

    private final UserRepository userRepository;
    private final GeminiClient gemini;
    private final ApiKeyEncryptionService encryption;

    /** The user's own key when available, otherwise the shared server key (which may be blank). */
    public String resolveKey(User user) {
        if (user != null && user.getGeminiApiKeyEncrypted() != null && !user.getGeminiApiKeyEncrypted().isBlank()) {
            try {
                String userKey = encryption.decrypt(user.getGeminiApiKeyEncrypted());
                if (userKey != null && !userKey.isBlank()) {
                    return userKey;
                }
            } catch (IllegalStateException e) {
                log.warn("Could not decrypt Gemini key for user {}: {}", user.getId(), e.getMessage());
            }
        }
        return serverKey();
    }

    public boolean isConfigured(User user) {
        String key = resolveKey(user);
        return key != null && !key.isBlank();
    }

    @Transactional(readOnly = true)
    public GeminiKeyStatus status(User user) {
        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toStatus(fresh);
    }

    @Transactional
    public GeminiKeyStatus save(User principal, String rawKey) {
        String apiKey = rawKey == null ? "" : rawKey.trim();
        if (apiKey.length() < MIN_KEY_LENGTH || apiKey.length() > MAX_KEY_LENGTH) {
            throw new BadRequestException("Enter a valid Gemini API key");
        }
        if (apiKey.chars().anyMatch(Character::isWhitespace)) {
            throw new BadRequestException("The Gemini API key must not contain spaces or line breaks");
        }
        gemini.validateApiKey(apiKey);

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setGeminiApiKeyEncrypted(encryption.encrypt(apiKey));
        user.setGeminiApiKeyHint(mask(apiKey));
        user.setGeminiApiKeyUpdatedAt(Instant.now());
        userRepository.save(user);
        return toStatus(user);
    }

    @Transactional
    public GeminiKeyStatus delete(User principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setGeminiApiKeyEncrypted(null);
        user.setGeminiApiKeyHint(null);
        user.setGeminiApiKeyUpdatedAt(null);
        userRepository.save(user);
        return toStatus(user);
    }

    private GeminiKeyStatus toStatus(User user) {
        String encrypted = user.getGeminiApiKeyEncrypted();
        boolean configured = false;
        if (encrypted != null && !encrypted.isBlank()) {
            try {
                String decrypted = encryption.decrypt(encrypted);
                configured = decrypted != null && !decrypted.isBlank();
            } catch (IllegalStateException e) {
                log.warn("Stored Gemini key for user {} cannot be decrypted: {}", user.getId(), e.getMessage());
            }
        }
        boolean server = gemini.isConfigured();
        return new GeminiKeyStatus(configured, configured ? user.getGeminiApiKeyHint() : null,
                server, configured || server, configured ? user.getGeminiApiKeyUpdatedAt() : null);
    }

    private String serverKey() {
        String key = gemini.serverApiKey();
        return key == null || key.isBlank() ? null : key;
    }

    private static String mask(String apiKey) {
        return "****" + apiKey.substring(apiKey.length() - 4);
    }
}
