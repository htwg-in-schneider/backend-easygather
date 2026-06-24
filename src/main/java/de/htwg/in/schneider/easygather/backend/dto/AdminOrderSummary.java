package de.htwg.in.schneider.easygather.backend.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import de.htwg.in.schneider.easygather.backend.model.OrderStatus;
import de.htwg.in.schneider.easygather.backend.model.PaymentMethod;

public class AdminOrderSummary {

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private double total;
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;

    public AdminOrderSummary(Long id, LocalDateTime createdAt, OrderStatus status, PaymentMethod paymentMethod,
            double total, String customerFirstName, String customerLastName, String customerEmail) {
        this.id = id;
        this.createdAt = createdAt;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.total = total;
        this.customerFirstName = customerFirstName;
        this.customerLastName = customerLastName;
        this.customerEmail = customerEmail;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public double getTotal() {
        return total;
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
}
