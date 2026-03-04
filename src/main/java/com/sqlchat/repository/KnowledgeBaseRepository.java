package com.sqlchat.repository;

import com.sqlchat.entity.KnowledgeBaseEntity;
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
}
