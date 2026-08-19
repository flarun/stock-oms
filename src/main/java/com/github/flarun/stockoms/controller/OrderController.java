package com.github.flarun.stockoms.controller;

import com.github.flarun.stockoms.engine.OrderBook;
import com.github.flarun.stockoms.model.Order;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderBook orderBook;

    public OrderController(OrderBook orderBook) {
        this.orderBook = orderBook;
    }

    @PostMapping("/order")
    public String placeOrder(@RequestBody Order order) {
        orderBook.processOrder(order);
        return "Order Received";
    }
}