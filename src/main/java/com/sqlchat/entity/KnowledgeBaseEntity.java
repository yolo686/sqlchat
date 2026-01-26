package com.sqlchat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库实体
 * @author sqlChat
 */
@Entity
@Table(name = "knowledge_base")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // SQL_EXAMPLE 或 DOCUMENT

    @Column(name = "question", columnDefinition = "TEXT")
    private String question; // 提问（仅SQL_EXAMPLE类型使用）

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex; // 分片编号，从0开始，自动生成

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content; // 分片内容（SQL语句或文档内容）

    @Column(name = "embedding_id", length = 100)
    private String embeddingId; // 向量存储中的ID，用于增量更新

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
