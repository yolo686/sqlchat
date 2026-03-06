package com.sqlchat.repository;

import com.sqlchat.entity.KnowledgeBaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库Repository
 * @author sqlChat
 */
@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBaseEntity, String> {
    List<KnowledgeBaseEntity> findByUserId(String userId);

    List<KnowledgeBaseEntity> findByUserIdAndType(String userId, String type);

    List<KnowledgeBaseEntity> findByUserIdAndDomain(String userId, String domain);

    List<KnowledgeBaseEntity> findByUserIdAndTypeAndDomain(String userId, String type, String domain);

    boolean existsByUserIdAndDomain(String userId, String domain);

    // 分页查询方法
    Page<KnowledgeBaseEntity> findByUserId(String userId, Pageable pageable);

    Page<KnowledgeBaseEntity> findByUserIdAndType(String userId, String type, Pageable pageable);

    Page<KnowledgeBaseEntity> findByUserIdAndDomain(String userId, String domain, Pageable pageable);

    Page<KnowledgeBaseEntity> findByUserIdAndTypeAndDomain(String userId, String type, String domain, Pageable pageable);
}
