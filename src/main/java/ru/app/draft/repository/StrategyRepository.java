package ru.app.draft.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.app.draft.entity.Strategy;
import ru.app.draft.entity.User;

import java.util.Optional;

public interface StrategyRepository extends JpaRepository<Strategy, Long> {
}