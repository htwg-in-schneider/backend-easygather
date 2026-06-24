package de.htwg.in.schneider.easygather.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.htwg.in.schneider.easygather.backend.model.Order;
import de.htwg.in.schneider.easygather.backend.model.User;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserOrderByCreatedAtDesc(User user);

    Optional<Order> findByIdAndUser(Long id, User user);

    List<Order> findAllByOrderByCreatedAtDesc();
}
