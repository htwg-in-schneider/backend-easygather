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
import de.htwg.in.schneider.easygather.backend.model.User;
import de.htwg.in.schneider.easygather.backend.repository.UserRepository;

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
        return userRepository.findByOauthId(oauthId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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

        return userRepository.findByOauthId(oauthId)
                .map(user -> {
                    user.setFirstName(request.getFirstName().trim());
                    user.setLastName(request.getLastName().trim());
                    user.setStreet(request.getStreet().trim());
                    user.setPostalCode(request.getPostalCode().trim());
                    user.setCity(request.getCity().trim());
                    User saved = userRepository.save(user);
                    LOG.info("Profile updated for user id={}", saved.getId());
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> {
                    LOG.warn("Profile not found for principal: {}", oauthId);
                    return ResponseEntity.notFound().build();
                });
    }

    private boolean isValidProfileUpdate(ProfileUpdateRequest request) {
        if (request == null) {
            return false;
        }
        return isNotBlank(request.getFirstName())
                && isNotBlank(request.getLastName())
                && isNotBlank(request.getStreet())
                && isNotBlank(request.getPostalCode())
                && isNotBlank(request.getCity());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
