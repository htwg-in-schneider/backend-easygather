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

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.PutMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;



import de.htwg.in.schneider.easygather.backend.dto.AdminDeliverySummary;

import de.htwg.in.schneider.easygather.backend.dto.AssignDriverRequest;

import de.htwg.in.schneider.easygather.backend.dto.DeliveryStatusUpdateRequest;

import de.htwg.in.schneider.easygather.backend.dto.DriverDashboardResponse;

import de.htwg.in.schneider.easygather.backend.model.DeliveryOrder;

import de.htwg.in.schneider.easygather.backend.model.Role;

import de.htwg.in.schneider.easygather.backend.model.User;

import de.htwg.in.schneider.easygather.backend.repository.UserRepository;

import de.htwg.in.schneider.easygather.backend.service.DeliveryService;

import de.htwg.in.schneider.easygather.backend.util.AdminAuth;



@RestController

@RequestMapping("/api/delivery")

public class DeliveryController {



    private static final Logger LOG = LoggerFactory.getLogger(DeliveryController.class);



    @Autowired

    private UserRepository userRepository;



    @Autowired

    private DeliveryService deliveryService;



    @GetMapping("/assigned")

    public ResponseEntity<DriverDashboardResponse> getDriverDashboard(@AuthenticationPrincipal Jwt jwt) {

        Optional<User> driver = getFahrerFromJwt(jwt);

        if (driver.isEmpty()) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        }

        DriverDashboardResponse dashboard = deliveryService.getDriverDashboard(driver.get());

        LOG.info("Returning {} available and {} assigned deliveries for driver {}",

                dashboard.getAvailable().size(),

                dashboard.getMyDeliveries().size(),

                driver.get().getEmail());

        return ResponseEntity.ok(dashboard);

    }



    @GetMapping("/admin/all")

    public ResponseEntity<List<AdminDeliverySummary>> getAllDeliveriesForAdmin(@AuthenticationPrincipal Jwt jwt,

            @RequestParam(required = false) String q) {

        if (!AdminAuth.isAdmin(jwt, userRepository)) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        }

        List<AdminDeliverySummary> deliveries = deliveryService.getAllForAdmin(q);

        LOG.info("Returning {} deliveries for admin search q={}", deliveries.size(), q);

        return ResponseEntity.ok(deliveries);

    }



    @PutMapping("/admin/{id}/assign")

    public ResponseEntity<?> assignDriverByAdmin(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,

            @RequestBody AssignDriverRequest request) {

        if (!AdminAuth.isAdmin(jwt, userRepository)) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        }

        if (request == null || request.getDriverId() == null) {

            return ResponseEntity.badRequest().body("Driver id is required");

        }

        try {

            AdminDeliverySummary updated = deliveryService.assignDriverByAdmin(id, request.getDriverId());

            LOG.info("Admin assigned driver {} to delivery {}", request.getDriverId(), updated.getOrderNumber());

            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException ex) {

            return ResponseEntity.notFound().build();

        } catch (IllegalStateException ex) {

            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());

        }

    }



    @PutMapping("/admin/{id}/unassign")

    public ResponseEntity<?> unassignDriverByAdmin(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {

        if (!AdminAuth.isAdmin(jwt, userRepository)) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        }

        try {

            AdminDeliverySummary updated = deliveryService.unassignDriverByAdmin(id);

            LOG.info("Admin unassigned driver from delivery {}", updated.getOrderNumber());

            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException ex) {

            return ResponseEntity.notFound().build();

        } catch (IllegalStateException ex) {

            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());

        }

    }



    @PostMapping("/{id}/accept")

    public ResponseEntity<DeliveryOrder> acceptDelivery(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {

        Optional<User> driver = getFahrerFromJwt(jwt);

        if (driver.isEmpty()) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        }

        try {

            DeliveryOrder accepted = deliveryService.acceptDelivery(id, driver.get());

            LOG.info("Driver {} accepted delivery {}", driver.get().getEmail(), accepted.getOrderNumber());

            return ResponseEntity.ok(accepted);

        } catch (IllegalArgumentException ex) {

            return ResponseEntity.notFound().build();

        } catch (IllegalStateException ex) {

            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        }

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

        try {

            DeliveryOrder updated = deliveryService.updateDeliveryStatus(id, request.getStatus(), driver.get());

            LOG.info("Updated delivery {} status to {}", updated.getOrderNumber(), updated.getStatus());

            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException ex) {

            return ResponseEntity.notFound().build();

        }

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


