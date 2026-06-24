package de.htwg.in.schneider.easygather.backend.util;

import java.util.Optional;

import org.springframework.security.oauth2.jwt.Jwt;

import de.htwg.in.schneider.easygather.backend.model.Role;
import de.htwg.in.schneider.easygather.backend.model.User;
import de.htwg.in.schneider.easygather.backend.repository.UserRepository;

public final class AdminAuth {

    private AdminAuth() {
    }

    public static Optional<User> findAdmin(Jwt jwt, UserRepository userRepository) {
        if (jwt == null || jwt.getSubject() == null) {
            return Optional.empty();
        }
        return userRepository.findByOauthId(jwt.getSubject())
                .filter(user -> user.getRole() == Role.ADMIN);
    }

    public static boolean isAdmin(Jwt jwt, UserRepository userRepository) {
        return findAdmin(jwt, userRepository).isPresent();
    }
}
