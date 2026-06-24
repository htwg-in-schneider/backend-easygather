package de.htwg.in.schneider.easygather.backend.controller;

import java.util.List;
import java.util.Optional;

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
import org.springframework.web.bind.annotation.RestController;

import de.htwg.in.schneider.easygather.backend.dto.DeliveryStatusUpdateRequest;
import de.htwg.in.schneider.easygather.backend.model.DeliveryOrder;
import de.htwg.in.schneider.easygather.backend.model.Role;
import de.htwg.in.schneider.easygather.backend.model.User;
import de.htwg.in.schneider.easygather.backend.repository.DeliveryOrderRepository;
import de.htwg.in.schneider.easygather.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    private static final Logger LOG = LoggerFactory.getLogger(DeliveryController.class);

    @Autowired
    private DeliveryOrderRepository deliveryOrderRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/assigned")
    public ResponseEntity<List<DeliveryOrder>> getAssignedDeliveries(@AuthenticationPrincipal Jwt jwt) {
        Optional<User> driver = getFahrerFromJwt(jwt);
        if (driver.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<DeliveryOrder> orders = deliveryOrderRepository.findByDriverIdOrderByOrderNumberAsc(driver.get().getId());
        LOG.info("Returning {} assigned deliveries for driver {}", orders.size(), driver.get().getEmail());
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DeliveryOrder> updateDeliveryStatus(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id, @RequestBody DeliveryStatusUpdateRequest request) {
        Optional<User> driver = getFahrerFromJwt(jwt);
        if (driver.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (request == null || request.getStatus() == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<DeliveryOrder> orderOpt = deliveryOrderRepository.findByIdAndDriverId(id, driver.get().getId());
        if (orderOpt.isEmpty()) {
            LOG.warn("Driver {} tried to update delivery {} not assigned to them", driver.get().getEmail(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        DeliveryOrder order = orderOpt.get();
        order.setStatus(request.getStatus());
        DeliveryOrder updated = deliveryOrderRepository.save(order);
        LOG.info("Updated delivery {} status to {}", updated.getOrderNumber(), updated.getStatus());
        return ResponseEntity.ok(updated);
    }

    private Optional<User> getFahrerFromJwt(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            return Optional.empty();
        }
        Optional<User> user = userRepository.findByOauthId(jwt.getSubject());
        if (user.isEmpty() || user.get().getRole() != Role.FAHRER) {
            return Optional.empty();
        }
        return user;
    }
}
