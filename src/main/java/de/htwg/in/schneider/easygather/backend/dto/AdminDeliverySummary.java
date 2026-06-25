package de.htwg.in.schneider.easygather.backend.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import de.htwg.in.schneider.easygather.backend.model.DeliveryOrder;
import de.htwg.in.schneider.easygather.backend.model.DeliveryStatus;
import de.htwg.in.schneider.easygather.backend.model.User;

public class AdminDeliverySummary {

    private Long id;
    private Long orderId;
    private String orderNumber;
    private String deliveryAddress;
    private String contentSummary;
    private DeliveryStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime orderCreatedAt;

    private Long driverId;
    private String driverFirstName;
    private String driverLastName;
    private String driverEmail;

    public static AdminDeliverySummary from(DeliveryOrder delivery) {
        AdminDeliverySummary summary = new AdminDeliverySummary();
        summary.id = delivery.getId();
        summary.orderNumber = delivery.getOrderNumber();
        summary.deliveryAddress = delivery.getDeliveryAddress();
        summary.contentSummary = delivery.getContentSummary();
        summary.status = delivery.getStatus();
        summary.orderCreatedAt = delivery.getOrderCreatedAt();
        if (delivery.getCustomerOrder() != null) {
            summary.orderId = delivery.getCustomerOrder().getId();
        }
        User driver = delivery.getDriver();
        if (driver != null) {
            summary.driverId = driver.getId();
            summary.driverFirstName = driver.getFirstName();
            summary.driverLastName = driver.getLastName();
            summary.driverEmail = driver.getEmail();
        }
        return summary;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getContentSummary() {
        return contentSummary;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public LocalDateTime getOrderCreatedAt() {
        return orderCreatedAt;
    }

    public Long getDriverId() {
        return driverId;
    }

    public String getDriverFirstName() {
        return driverFirstName;
    }

    public String getDriverLastName() {
        return driverLastName;
    }

    public String getDriverEmail() {
        return driverEmail;
    }
}
