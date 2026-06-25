package de.htwg.in.schneider.easygather.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import de.htwg.in.schneider.easygather.backend.model.Order;
import de.htwg.in.schneider.easygather.backend.model.OrderItem;
import de.htwg.in.schneider.easygather.backend.model.OrderStatus;
import de.htwg.in.schneider.easygather.backend.model.PaymentMethod;
import de.htwg.in.schneider.easygather.backend.model.DeliveryStatus;
import de.htwg.in.schneider.easygather.backend.model.User;

public class AdminOrderDetail {

    private Long id;
    private String orderNumber;
    private String street;
    private String postalCode;
    private String city;
    private PaymentMethod paymentMethod;
    private OrderStatus status;
    private double subtotal;
    private double shippingCost;
    private double discountAmount;
    private String couponCode;
    private double total;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private List<OrderItem> items;
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;

    private Long deliveryId;
    private DeliveryStatus deliveryStatus;
    private Long driverId;
    private String driverFirstName;
    private String driverLastName;
    private String driverEmail;

    public static AdminOrderDetail from(Order order) {
        return from(order, null);
    }

    public static AdminOrderDetail from(Order order, AdminDeliverySummary delivery) {
        AdminOrderDetail detail = new AdminOrderDetail();
        detail.id = order.getId();
        detail.orderNumber = order.getOrderNumber();
        detail.street = order.getStreet();
        detail.postalCode = order.getPostalCode();
        detail.city = order.getCity();
        detail.paymentMethod = order.getPaymentMethod();
        detail.status = order.getStatus();
        detail.subtotal = order.getSubtotal();
        detail.shippingCost = order.getShippingCost();
        detail.discountAmount = order.getDiscountAmount();
        detail.couponCode = order.getCouponCode();
        detail.total = order.getTotal();
        detail.createdAt = order.getCreatedAt();
        detail.items = order.getItems();
        User customer = order.getUser();
        if (customer != null) {
            detail.customerFirstName = customer.getFirstName();
            detail.customerLastName = customer.getLastName();
            detail.customerEmail = customer.getEmail();
        }
        if (delivery != null) {
            detail.deliveryId = delivery.getId();
            detail.deliveryStatus = delivery.getStatus();
            detail.driverId = delivery.getDriverId();
            detail.driverFirstName = delivery.getDriverFirstName();
            detail.driverLastName = delivery.getDriverLastName();
            detail.driverEmail = delivery.getDriverEmail();
        }
        return detail;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getStreet() {
        return street;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCity() {
        return city;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getShippingCost() {
        return shippingCost;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public double getTotal() {
        return total;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public String getCustomerFirstName() {
        return customerFirstName;
    }

    public String getCustomerLastName() {
        return customerLastName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public Long getDeliveryId() {
        return deliveryId;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
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
