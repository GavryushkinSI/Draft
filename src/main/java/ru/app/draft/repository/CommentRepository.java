package ru.app.draft.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.app.draft.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
}