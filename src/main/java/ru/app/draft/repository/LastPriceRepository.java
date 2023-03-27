package ru.app.draft.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.app.draft.entity.LastPrice;

public interface LastPriceRepository extends JpaRepository<LastPrice, Integer> {
}