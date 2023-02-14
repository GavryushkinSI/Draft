package ru.app.draft.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.app.draft.entity.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    @Override
    List<User> findAll();
}