package com.sqlchat.repository;

import com.sqlchat.entity.KnowledgeBaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库Repository
 * @author sqlChat
 */
@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBaseEntity, String> {
    /**
     * 根据用户ID和类型查找
     */
    List<KnowledgeBaseEntity> findByUserIdAndType(String userId, String type);
}
