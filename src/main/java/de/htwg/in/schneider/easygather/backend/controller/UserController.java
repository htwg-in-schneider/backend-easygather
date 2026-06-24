package de.htwg.in.schneider.easygather.backend.controller;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.htwg.in.schneider.easygather.backend.dto.AdminUserUpdateRequest;
import de.htwg.in.schneider.easygather.backend.model.Role;
import de.htwg.in.schneider.easygather.backend.model.User;
import de.htwg.in.schneider.easygather.backend.repository.UserRepository;
import de.htwg.in.schneider.easygather.backend.util.AdminAuth;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<User>> getUsers(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role) {
        if (!AdminAuth.isAdmin(jwt, userRepository)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String search = normalizeSearch(q);
        Role roleFilter = parseRoleFilter(role);
        List<User> users = userRepository.findAll().stream()
                .filter(user -> matchesRole(user, roleFilter))
                .filter(user -> matchesSearch(user, search))
                .collect(Collectors.toList());
        LOG.info("Returning {} users for admin search q={} role={}", users.size(), search, roleFilter);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        if (!AdminAuth.isAdmin(jwt, userRepository)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @RequestBody AdminUserUpdateRequest request) {
        if (!AdminAuth.isAdmin(jwt, userRepository)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!isValidUpdate(request)) {
            return ResponseEntity.badRequest().body("Invalid user data");
        }
        Role role;
        try {
            role = Role.valueOf(request.getRole().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Invalid role");
        }
        User user = opt.get();
        Optional<String> roleChangeError = validateRoleChange(jwt, user, role);
        if (roleChangeError.isPresent()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(roleChangeError.get());
        }
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setStreet(trimOrEmpty(request.getStreet()));
        user.setPostalCode(trimOrEmpty(request.getPostalCode()));
        user.setCity(trimOrEmpty(request.getCity()));
        user.setRole(role);
        User saved = userRepository.save(user);
        LOG.info("Admin updated user id={} role={}", saved.getId(), saved.getRole());
        return ResponseEntity.ok(saved);
    }

    private Optional<String> validateRoleChange(Jwt jwt, User user, Role newRole) {
        Role currentRole = user.getRole();
        if (newRole == currentRole) {
            return Optional.empty();
        }
        Optional<User> actingAdmin = AdminAuth.findAdmin(jwt, userRepository);
        if (actingAdmin.isPresent() && actingAdmin.get().getId().equals(user.getId()) && newRole != Role.ADMIN) {
            return Optional.of("Du kannst deine eigene Administrator-Rolle nicht entfernen.");
        }
        if (currentRole == Role.ADMIN && newRole != Role.ADMIN
                && userRepository.countByRole(Role.ADMIN) <= 1) {
            return Optional.of("Der letzte Administrator kann nicht entfernt werden.");
        }
        return Optional.empty();
    }

    private boolean matchesSearch(User user, String search) {
        if (search == null) {
            return true;
        }
        String lower = search.toLowerCase(Locale.ROOT);
        String fullName = buildFullName(user).toLowerCase(Locale.ROOT);
        if (fullName.contains(lower)) {
            return true;
        }
        if (matchesRoleLabel(user, lower)) {
            return true;
        }
        String[] tokens = lower.split("\\s+");
        if (tokens.length > 1) {
            return java.util.Arrays.stream(tokens)
                    .filter(token -> !token.isBlank())
                    .allMatch(token -> matchesUserToken(user, token, fullName));
        }
        return containsIgnoreCase(user.getFirstName(), lower)
                || containsIgnoreCase(user.getLastName(), lower)
                || containsIgnoreCase(user.getEmail(), lower)
                || matchesRoleLabel(user, lower);
    }

    private String buildFullName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        return (firstName + " " + lastName).trim();
    }

    private boolean matchesUserToken(User user, String token, String fullName) {
        return fullName.contains(token)
                || containsIgnoreCase(user.getEmail(), token)
                || matchesRoleLabel(user, token);
    }

    private boolean matchesRoleLabel(User user, String token) {
        if (user.getRole() == null || token == null || token.isBlank()) {
            return false;
        }
        return switch (user.getRole()) {
            case KUNDE -> token.equals("kunde") || token.equals("kunden") || token.equals("customer");
            case FAHRER -> token.equals("fahrer") || token.equals("fahrerin");
            case ADMIN -> token.equals("admin") || token.equals("administrator") || token.equals("administratoren");
        };
    }

    private Role parseRoleFilter(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean matchesRole(User user, Role roleFilter) {
        return roleFilter == null || user.getRole() == roleFilter;
    }

    private boolean containsIgnoreCase(String value, String lowerSearch) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerSearch);
    }

    private String normalizeSearch(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.trim();
    }

    private boolean isValidUpdate(AdminUserUpdateRequest request) {
        if (request == null) {
            return false;
        }
        return isNotBlank(request.getFirstName())
                && isNotBlank(request.getLastName())
                && isNotBlank(request.getRole());
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
