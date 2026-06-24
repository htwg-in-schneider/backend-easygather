package de.htwg.in.schneider.easygather.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.htwg.in.schneider.easygather.backend.model.DeliveryOrder;
import de.htwg.in.schneider.easygather.backend.model.DeliveryStatus;
import de.htwg.in.schneider.easygather.backend.model.User;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {

    List<DeliveryOrder> findByCustomerOrderIsNotNullOrderByCustomerOrderCreatedAtDesc();

    List<DeliveryOrder> findByDriverAndCustomerOrderIsNotNullOrderByOrderCreatedAtDesc(User driver);

    List<DeliveryOrder> findByDriverIsNullAndStatusAndCustomerOrderIsNotNullOrderByOrderCreatedAtDesc(
            DeliveryStatus status);

    Optional<DeliveryOrder> findByIdAndDriver(Long id, User driver);

    Optional<DeliveryOrder> findByCustomerOrderId(Long customerOrderId);

    boolean existsByCustomerOrderId(Long customerOrderId);

    Optional<DeliveryOrder> findByIdAndCustomerOrderIsNotNull(Long id);
}
