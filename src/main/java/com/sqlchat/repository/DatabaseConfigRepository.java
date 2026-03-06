package com.sqlchat.repository;

import com.sqlchat.entity.DatabaseConfigEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 数据库配置Repository
 * @author sqlChat
 */
@Repository
public interface DatabaseConfigRepository extends JpaRepository<DatabaseConfigEntity, String> {
    List<DatabaseConfigEntity> findByUserId(String userId);

    Page<DatabaseConfigEntity> findByUserId(String userId, Pageable pageable);
}
