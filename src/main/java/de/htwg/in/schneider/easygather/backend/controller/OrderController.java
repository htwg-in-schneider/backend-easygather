package de.htwg.in.schneider.easygather.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.htwg.in.schneider.easygather.backend.dto.OrderAddressRequest;
import de.htwg.in.schneider.easygather.backend.dto.OrderItemRequest;
import de.htwg.in.schneider.easygather.backend.dto.OrderRequest;
import de.htwg.in.schneider.easygather.backend.model.Order;
import de.htwg.in.schneider.easygather.backend.model.OrderItem;
import de.htwg.in.schneider.easygather.backend.model.OrderStatus;
import de.htwg.in.schneider.easygather.backend.model.PaymentMethod;
import de.htwg.in.schneider.easygather.backend.model.Product;
import de.htwg.in.schneider.easygather.backend.model.User;
import de.htwg.in.schneider.easygather.backend.repository.OrderRepository;
import de.htwg.in.schneider.easygather.backend.repository.ProductRepository;
import de.htwg.in.schneider.easygather.backend.repository.UserRepository;
import de.htwg.in.schneider.easygather.backend.service.DeliveryService;
import de.htwg.in.schneider.easygather.backend.service.OrderNumberService;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private static final Logger LOG = LoggerFactory.getLogger(OrderController.class);
    private static final double SHIPPING_COST = 4.90;
    private static final String VALID_COUPON_CODE = "EASY10";
    private static final double COUPON_DISCOUNT_RATE = 0.10;

    @Value("${spring.mail.username:}")
    private String mailSenderUsername;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private OrderNumberService orderNumberService;

    @Autowired(required = false)
    private JavaMailSender emailSender;

    @GetMapping
    public ResponseEntity<List<Order>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        Optional<User> user = findUser(jwt);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        return ResponseEntity.ok(orderRepository.findByUserOrderByCreatedAtDesc(user.get()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Optional<User> user = findUser(jwt);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        return orderRepository.findByIdAndUser(id, user.get())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@AuthenticationPrincipal Jwt jwt, @RequestBody OrderRequest orderRequest) {
        if (jwt == null) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        Optional<User> userOpt = findUser(jwt);
        if (userOpt.isEmpty()) {
            LOG.warn("Order attempted by unknown user: {}", jwt.getSubject());
            return ResponseEntity.status(404).body("User not found");
        }

        User user = userOpt.get();
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body("User has no email address");
        }

        if (!isValidOrderRequest(orderRequest)) {
            return ResponseEntity.badRequest().body("Invalid order data");
        }

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(orderRequest.getPaymentMethod());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Invalid payment method");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStreet(orderRequest.getAddress().getStreet().trim());
        order.setPostalCode(orderRequest.getAddress().getPostalCode().trim());
        order.setCity(orderRequest.getAddress().getCity().trim());
        order.setPaymentMethod(paymentMethod);
        order.setStatus(OrderStatus.BESTAETIGT);
        order.setShippingCost(SHIPPING_COST);
        order.setCreatedAt(LocalDateTime.now());

        double subtotal = 0.0;
        for (OrderItemRequest itemRequest : orderRequest.getItems()) {
            if (itemRequest.getQuantity() <= 0) {
                return ResponseEntity.badRequest().body("Invalid item quantity");
            }
            Optional<Product> productOpt = productRepository.findById(itemRequest.getProductId());
            if (productOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("One or more products in the order could not be found.");
            }
            Product product = productOpt.get();
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductTitle(product.getTitle());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            order.addItem(orderItem);
            subtotal += product.getPrice() * itemRequest.getQuantity();
        }

        double discountAmount = 0.0;
        String couponCode = normalizeCouponCode(orderRequest.getCouponCode());
        if (couponCode != null) {
            if (!VALID_COUPON_CODE.equals(couponCode)) {
                return ResponseEntity.badRequest().body("Invalid coupon code");
            }
            discountAmount = roundMoney(subtotal * COUPON_DISCOUNT_RATE);
            order.setCouponCode(couponCode);
        }
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmount);
        order.setTotal(roundMoney(subtotal - discountAmount + SHIPPING_COST));

        orderNumberService.assignNextOrderNumber(order);
        Order savedOrder = orderRepository.save(order);
        deliveryService.createFromOrder(savedOrder);
        sendOrderEmail(user, savedOrder);

        return ResponseEntity.ok(savedOrder);
    }

    private Optional<User> findUser(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            return Optional.empty();
        }
        return userRepository.findByOauthId(jwt.getSubject());
    }

    private boolean isValidOrderRequest(OrderRequest orderRequest) {
        if (orderRequest == null || orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            return false;
        }
        OrderAddressRequest address = orderRequest.getAddress();
        if (address == null) {
            return false;
        }
        return isNotBlank(address.getStreet())
                && isNotBlank(address.getPostalCode())
                && isNotBlank(address.getCity())
                && isNotBlank(orderRequest.getPaymentMethod());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeCouponCode(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        return couponCode.trim().toUpperCase();
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void sendOrderEmail(User user, Order order) {
        if (emailSender == null || mailSenderUsername == null || mailSenderUsername.isBlank()) {
            LOG.warn("Mail sender not configured. Skipping order confirmation email for order id={}", order.getId());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("EasyGather <" + mailSenderUsername + ">");
            message.setTo(user.getEmail());
            message.setSubject("Ihre EasyGather Bestellbestätigung " + order.getOrderNumber());
            message.setText(buildReceiptText(user, order));
            emailSender.send(message);
            LOG.info("Order receipt sent to {} for order id={}", user.getEmail(), order.getId());
        } catch (Exception e) {
            LOG.error("Failed to send order email for order id={}", order.getId(), e);
        }
    }

    private String buildReceiptText(User user, Order order) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Vielen Dank für deine Bestellung");
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            receipt.append(", ").append(user.getFirstName());
        }
        receipt.append("!\n\n");
        receipt.append("Bestellnummer: ").append(order.getOrderNumber()).append("\n");
        receipt.append("Status: ").append(order.getStatus()).append("\n");
        receipt.append("Zahlungsart: ").append(order.getPaymentMethod()).append("\n\n");
        receipt.append("Lieferadresse:\n");
        receipt.append(order.getStreet()).append("\n");
        receipt.append(order.getPostalCode()).append(" ").append(order.getCity()).append("\n\n");
        receipt.append("Bestelldetails:\n");
        receipt.append("------------------------------------------------\n");
        for (OrderItem item : order.getItems()) {
            receipt.append(String.format("%-30s x%d   %8.2f €\n",
                    item.getProductTitle(), item.getQuantity(), item.getLineTotal()));
        }
        receipt.append("------------------------------------------------\n");
        receipt.append(String.format("Zwischensumme:                    %8.2f €\n", order.getSubtotal()));
        if (order.getDiscountAmount() > 0) {
            receipt.append(String.format("Rabatt (%s):                     -%7.2f €\n",
                    order.getCouponCode(), order.getDiscountAmount()));
        }
        receipt.append(String.format("Versand:                          %8.2f €\n", order.getShippingCost()));
        receipt.append(String.format("Gesamt:                           %8.2f €\n", order.getTotal()));
        return receipt.toString();
    }
}
