package com.tradev.domain.user.repository;

import com.tradev.domain.user.entity.OAuthProvider;
import com.tradev.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByOauthProviderAndOauthProviderId(OAuthProvider provider, String providerId);

    long countByCreatedAtAfter(LocalDateTime after);

    @Query("""
        SELECT u FROM User u
        WHERE (:cursorCreatedAt IS NULL OR u.createdAt < :cursorCreatedAt
               OR (u.createdAt = :cursorCreatedAt AND u.id < :cursorId))
        ORDER BY u.createdAt DESC, u.id DESC
        LIMIT :pageSize
        """)
    List<User> findAllWithCursor(
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorId") Long cursorId,
        @Param("pageSize") int pageSize
    );
}
