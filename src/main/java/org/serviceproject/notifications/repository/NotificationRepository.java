package org.serviceproject.notifications.repository;

import org.serviceproject.notifications.entity.Notification;
import org.serviceproject.notifications.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Finds paginated notifications for a specific user ordered by newest first.
     */
    Page<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Finds unread notifications for a specific user ordered by newest first.
     */
    Page<Notification> findAllByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Counts unread notifications for a user (used for badge counters).
     */
    long countByUserIdAndReadFalse(Long userId);

    /**
     * Checks if a notification with a specific deduplication reference already exists for the user.
     */
    boolean existsByUserIdAndTypeAndReferenceId(Long userId, NotificationType type, String referenceId);

    /**
     * Marks all unread notifications for a user as read.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.user.id = :userId AND n.read = false")
    int markAllAsReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    /**
     * Marks a specific notification as read if it belongs to the given user.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.id = :id AND n.user.id = :userId")
    int markAsReadByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
