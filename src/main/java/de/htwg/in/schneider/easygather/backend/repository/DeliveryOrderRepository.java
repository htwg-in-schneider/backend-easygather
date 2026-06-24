package de.htwg.in.schneider.easygather.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.htwg.in.schneider.easygather.backend.model.DeliveryOrder;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {

    List<DeliveryOrder> findByDriverIdOrderByOrderNumberAsc(Long driverId);

    long countByDriverId(Long driverId);

    Optional<DeliveryOrder> findByIdAndDriverId(Long id, Long driverId);
}
