package de.htwg.in.schneider.easygather.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.htwg.in.schneider.easygather.backend.dto.ProfileUpdateRequest;
import de.htwg.in.schneider.easygather.backend.model.Role;
import de.htwg.in.schneider.easygather.backend.model.User;
import de.htwg.in.schneider.easygather.backend.repository.UserRepository;

import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<User> getProfile(@AuthenticationPrincipal Jwt jwt) {
        String oauthId = jwt.getSubject();
        LOG.info("getProfile called for principal: {}", oauthId);

        if (oauthId == null) {
            LOG.warn("JWT does not contain 'sub' claim");
            return ResponseEntity.badRequest().build();
        }
        return findOrCreateUser(jwt)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<User> updateProfile(@AuthenticationPrincipal Jwt jwt,
            @RequestBody ProfileUpdateRequest request) {
        String oauthId = jwt.getSubject();
        LOG.info("updateProfile called for principal: {}", oauthId);

        if (oauthId == null) {
            LOG.warn("JWT does not contain 'sub' claim");
            return ResponseEntity.badRequest().build();
        }
        if (!isValidProfileUpdate(request)) {
            LOG.warn("Invalid profile update for principal: {}", oauthId);
            return ResponseEntity.badRequest().build();
        }

        return findOrCreateUser(jwt)
                .map(user -> {
                    user.setFirstName(request.getFirstName().trim());
                    user.setLastName(request.getLastName().trim());
                    user.setStreet(trimOrEmpty(request.getStreet()));
                    user.setPostalCode(trimOrEmpty(request.getPostalCode()));
                    user.setCity(trimOrEmpty(request.getCity()));
                    User saved = userRepository.save(user);
                    LOG.info("Profile updated for user id={}", saved.getId());
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> {
                    LOG.warn("Profile not found for principal: {}", oauthId);
                    return ResponseEntity.notFound().build();
                });
    }

    private Optional<User> findOrCreateUser(Jwt jwt) {
        String oauthId = jwt.getSubject();
        if (oauthId == null) {
            return Optional.empty();
        }

        Optional<User> byOauthId = userRepository.findByOauthId(oauthId);
        if (byOauthId.isPresent()) {
            return byOauthId;
        }

        String email = extractEmail(jwt);
        if (email != null) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                user.setOauthId(oauthId);
                User saved = userRepository.save(user);
                LOG.info("Linked oauthId to existing user id={} email={}", saved.getId(), email);
                return Optional.of(saved);
            }
        }

        if (email == null || email.isBlank()) {
            LOG.warn("Cannot create user without email for oauthId={}", oauthId);
            return Optional.empty();
        }

        User user = new User();
        user.setOauthId(oauthId);
        user.setEmail(email);
        user.setRole(Role.KUNDE);
        user.setFirstName("");
        user.setLastName("");
        user.setStreet("");
        user.setPostalCode("");
        user.setCity("");
        User saved = userRepository.save(user);
        LOG.info("Created new KUNDE user id={} email={} oauthId={}", saved.getId(), email, oauthId);
        return Optional.of(saved);
    }

    private String extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        Object raw = jwt.getClaim("email");
        return raw != null ? raw.toString().trim() : null;
    }

    private boolean isValidProfileUpdate(ProfileUpdateRequest request) {
        if (request == null) {
            return false;
        }
        return isNotBlank(request.getFirstName())
                && isNotBlank(request.getLastName())
                && isValidOptionalPostalCode(request.getPostalCode());
    }

    private boolean isValidOptionalPostalCode(String postalCode) {
        if (postalCode == null || postalCode.isBlank()) {
            return true;
        }
        return postalCode.trim().matches("\\d{5}");
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
