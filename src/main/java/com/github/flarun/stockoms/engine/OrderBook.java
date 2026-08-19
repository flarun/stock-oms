package com.github.flarun.stockoms.engine;

import com.github.flarun.stockoms.model.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.PriorityQueue;

@Service
public class OrderBook {
    // Bids: Max-Heap (Highest price matched first)
    private final PriorityQueue<Order> bids = new PriorityQueue<>((a, b) -> Long.compare(b.getPrice(), a.getPrice()));
    // Asks: Min-Heap (Lowest price matched first)
    private final PriorityQueue<Order> asks = new PriorityQueue<>((a, b) -> Long.compare(a.getPrice(), b.getPrice()));

    private final SimpMessagingTemplate messagingTemplate;

    public OrderBook(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public synchronized void processOrder(Order order) {
        if (order.getSide().equalsIgnoreCase("BUY")) {
            matchOrder(order, bids, asks);
        } else {
            matchOrder(order, asks, bids);
        }
    }

    private void matchOrder(Order order, PriorityQueue<Order> sameSide, PriorityQueue<Order> oppositeSide) {
        while (order.getQuantity() > 0 && !oppositeSide.isEmpty()) {
            Order bestOpposite = oppositeSide.peek();

            boolean isMatch = order.getSide().equalsIgnoreCase("BUY")
                    ? order.getPrice() >= bestOpposite.getPrice()
                    : order.getPrice() <= bestOpposite.getPrice();

            if (!isMatch) break;

            long tradeQty = Math.min(order.getQuantity(), bestOpposite.getQuantity());
            long tradePrice = bestOpposite.getPrice();

            order.setQuantity(order.getQuantity() - tradeQty);
            bestOpposite.setQuantity(bestOpposite.getQuantity() - tradeQty);

            messagingTemplate.convertAndSend("/topic/trades",
                    "TRADE: " + tradeQty + " " + order.getSymbol() + " @ " + tradePrice);

            if (bestOpposite.getQuantity() == 0) {
                oppositeSide.poll();
            }
        }

        if (order.getQuantity() > 0) {
            sameSide.add(order);
            messagingTemplate.convertAndSend("/topic/depth",
                    order.getSide() + " ADDED: " + order.getQuantity() + " @ " + order.getPrice());
        }
    }
}