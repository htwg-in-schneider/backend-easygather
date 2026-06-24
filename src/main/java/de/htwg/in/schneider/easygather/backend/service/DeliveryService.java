package de.htwg.in.schneider.easygather.backend.service;



import java.util.List;

import java.util.Optional;



import java.util.stream.Collectors;



import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import de.htwg.in.schneider.easygather.backend.dto.DriverDashboardResponse;

import de.htwg.in.schneider.easygather.backend.model.DeliveryOrder;

import de.htwg.in.schneider.easygather.backend.model.DeliveryStatus;

import de.htwg.in.schneider.easygather.backend.model.Order;

import de.htwg.in.schneider.easygather.backend.model.OrderItem;

import de.htwg.in.schneider.easygather.backend.model.OrderStatus;

import de.htwg.in.schneider.easygather.backend.model.User;

import de.htwg.in.schneider.easygather.backend.repository.DeliveryOrderRepository;

import de.htwg.in.schneider.easygather.backend.repository.OrderRepository;



@Service

public class DeliveryService {



    @Autowired

    private DeliveryOrderRepository deliveryOrderRepository;



    @Autowired

    private OrderRepository orderRepository;



    @Transactional

    public DeliveryOrder createFromOrder(Order order) {

        if (order == null || order.getId() == null) {

            throw new IllegalArgumentException("Order must be persisted before creating a delivery");

        }

        Long orderId = order.getId();

        Optional<DeliveryOrder> existing = deliveryOrderRepository.findByCustomerOrderId(orderId);

        if (existing.isPresent()) {

            DeliveryOrder delivery = existing.get();

            Order linkedOrder = orderRepository.findById(orderId).orElse(order);

            syncDeliveryOrderNumber(delivery, linkedOrder);

            return deliveryOrderRepository.save(delivery);

        }

        Order orderWithItems = orderRepository.findByIdWithItems(orderId)

                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        DeliveryOrder delivery = buildDeliveryOrder(orderWithItems);

        return deliveryOrderRepository.save(delivery);

    }



    @Transactional

    public void removeOrphanDeliveries() {

        for (DeliveryOrder delivery : deliveryOrderRepository.findAll()) {

            if (delivery.getCustomerOrder() == null) {

                deliveryOrderRepository.delete(delivery);

            }

        }

    }



    public DriverDashboardResponse getDriverDashboard(User driver) {

        List<DeliveryOrder> available = deliveryOrderRepository

                .findByDriverIsNullAndStatusAndCustomerOrderIsNotNullOrderByOrderCreatedAtDesc(

                        DeliveryStatus.EINGEGANGEN);

        List<DeliveryOrder> myDeliveries = deliveryOrderRepository

                .findByDriverAndCustomerOrderIsNotNullOrderByOrderCreatedAtDesc(driver);

        return new DriverDashboardResponse(available, myDeliveries);

    }



    @Transactional

    public DeliveryOrder acceptDelivery(Long deliveryId, User driver) {

        DeliveryOrder delivery = deliveryOrderRepository.findByIdAndCustomerOrderIsNotNull(deliveryId)

                .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));

        if (delivery.getDriver() != null || delivery.getStatus() != DeliveryStatus.EINGEGANGEN) {

            throw new IllegalStateException("Delivery is no longer available");

        }

        delivery.setDriver(driver);

        delivery.setStatus(DeliveryStatus.ANGENOMMEN);

        return deliveryOrderRepository.save(delivery);

    }



    @Transactional

    public DeliveryOrder updateDeliveryStatus(Long deliveryId, DeliveryStatus deliveryStatus, User driver) {

        DeliveryOrder delivery = deliveryOrderRepository.findByIdAndDriver(deliveryId, driver)

                .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));

        if (!isAllowedStatusTransition(delivery.getStatus(), deliveryStatus)) {

            throw new IllegalArgumentException("Invalid status transition");

        }

        delivery.setStatus(deliveryStatus);

        DeliveryOrder updated = deliveryOrderRepository.save(delivery);

        syncCustomerOrderStatus(updated, deliveryStatus);

        return updated;

    }



    @Transactional

    public void syncCustomerOrderStatus(Long deliveryId, DeliveryStatus deliveryStatus) {

        deliveryOrderRepository.findById(deliveryId)

                .ifPresent(delivery -> syncCustomerOrderStatus(delivery, deliveryStatus));

    }



    private void syncCustomerOrderStatus(DeliveryOrder delivery, DeliveryStatus deliveryStatus) {

        Order customerOrder = delivery.getCustomerOrder();

        if (customerOrder == null) {

            return;

        }

        OrderStatus orderStatus = mapDeliveryStatus(deliveryStatus);

        if (orderStatus == null) {

            return;

        }

        customerOrder.setStatus(orderStatus);

        orderRepository.save(customerOrder);

    }



    private OrderStatus mapDeliveryStatus(DeliveryStatus deliveryStatus) {

        return switch (deliveryStatus) {

            case EINGEGANGEN, ANGENOMMEN -> OrderStatus.BESTAETIGT;

            case UNTERWEGS -> OrderStatus.UNTERWEGS;

            case GELIEFERT -> OrderStatus.ABGESCHLOSSEN;

        };

    }



    private boolean isAllowedStatusTransition(DeliveryStatus current, DeliveryStatus next) {

        if (current == next) {

            return true;

        }

        return switch (current) {

            case ANGENOMMEN -> next == DeliveryStatus.UNTERWEGS;

            case UNTERWEGS -> next == DeliveryStatus.GELIEFERT;

            case EINGEGANGEN, GELIEFERT -> false;

        };

    }



    @Transactional

    public void syncDeliveryOrderNumbersFromCustomerOrders() {

        for (DeliveryOrder delivery : deliveryOrderRepository.findAll()) {

            Order customerOrder = delivery.getCustomerOrder();

            if (customerOrder == null) {

                continue;

            }

            syncDeliveryOrderNumber(delivery, customerOrder);

            deliveryOrderRepository.save(delivery);

        }

    }



    private void syncDeliveryOrderNumber(DeliveryOrder delivery, Order order) {

        if (order.getOrderNumber() != null && !order.getOrderNumber().isBlank()) {

            delivery.setOrderNumber(order.getOrderNumber());

        }

        if (order.getCreatedAt() != null) {

            delivery.setOrderCreatedAt(order.getCreatedAt());

        }

    }



    private DeliveryOrder buildDeliveryOrder(Order order) {

        DeliveryOrder delivery = new DeliveryOrder();

        delivery.setCustomerOrder(order);

        delivery.setOrderNumber(resolveOrderNumber(order));

        delivery.setDeliveryAddress(formatDeliveryAddress(order));

        delivery.setContentSummary(buildContentSummary(order));

        delivery.setStatus(DeliveryStatus.EINGEGANGEN);

        delivery.setOrderCreatedAt(order.getCreatedAt());

        return delivery;

    }



    private String resolveOrderNumber(Order order) {

        if (order.getOrderNumber() != null && !order.getOrderNumber().isBlank()) {

            return order.getOrderNumber();

        }

        return String.format("EG-%04d", order.getId());

    }



    private String formatDeliveryAddress(Order order) {

        return order.getStreet() + ", " + order.getPostalCode() + " " + order.getCity();

    }



    private String buildContentSummary(Order order) {

        return order.getItems().stream()

                .map(this::formatOrderItem)

                .collect(Collectors.joining(", "));

    }



    private String formatOrderItem(OrderItem item) {

        if (item.getQuantity() > 1) {

            return item.getProductTitle() + " " + item.getQuantity() + "x";

        }

        return item.getProductTitle();

    }

}


