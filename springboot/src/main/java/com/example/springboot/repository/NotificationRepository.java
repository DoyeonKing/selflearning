package com.example.springboot.repository;

import com.example.springboot.entity.Notification;
import com.example.springboot.entity.enums.NotificationStatus;
import com.example.springboot.entity.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    /**
     * 根据用户ID和用户类型查询通知
     */
    List<Notification> findByUserIdAndUserTypeOrderBySentAtDesc(Integer userId, UserType userType);

    /**
     * 根据用户ID、用户类型和状态查询通知
     */
    List<Notification> findByUserIdAndUserTypeAndStatusOrderBySentAtDesc(
            Integer userId, UserType userType, NotificationStatus status);

    /**
     * 统计未读通知数量
     */
    long countByUserIdAndUserTypeAndStatus(Integer userId, UserType userType, NotificationStatus status);

    /**
     * 根据相关实体查询通知
     */
    List<Notification> findByRelatedEntityAndRelatedId(String relatedEntity, Integer relatedId);
}

