package de.htwg.in.schneider.easygather.backend.dto;

import java.util.List;

public class OrderRequest {

    private List<OrderItemRequest> items;
    private OrderAddressRequest address;
    private String paymentMethod;
    private String couponCode;

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    public OrderAddressRequest getAddress() {
        return address;
    }

    public void setAddress(OrderAddressRequest address) {
        this.address = address;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}
