package de.htwg.in.schneider.easygather.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htwg.in.schneider.easygather.backend.model.Order;
import de.htwg.in.schneider.easygather.backend.repository.OrderRepository;

@Service
public class OrderNumberService {

    @Autowired
    private OrderRepository orderRepository;

    @Transactional
    public void assignNextOrderNumber(Order order) {
        long nextSequence = orderRepository.findTopByOrderByOrderNumberSequenceDesc()
                .map(Order::getOrderNumberSequence)
                .orElse(0L) + 1;
        order.setOrderNumberSequence(nextSequence);
        order.setOrderNumber(formatOrderNumber(nextSequence));
    }

    @Transactional
    public void backfillMissingOrderNumbers() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtAsc();
        long nextSequence = orderRepository.findTopByOrderByOrderNumberSequenceDesc()
                .map(Order::getOrderNumberSequence)
                .orElse(0L);

        for (Order order : orders) {
            if (order.getOrderNumber() != null && !order.getOrderNumber().isBlank()) {
                continue;
            }
            nextSequence++;
            order.setOrderNumberSequence(nextSequence);
            order.setOrderNumber(formatOrderNumber(nextSequence));
            orderRepository.save(order);
        }
    }

    public String formatOrderNumber(long sequence) {
        return String.format("EG-%04d", sequence);
    }
}
