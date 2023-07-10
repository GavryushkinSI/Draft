package ru.app.draft.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.app.draft.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Integer> {
}