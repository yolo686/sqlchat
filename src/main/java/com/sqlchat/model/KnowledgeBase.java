package com.sqlchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库模型
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBase {
    private String id;
    private String userId;
    private String type; // SQL_EXAMPLE / GENERAL_DOC / BUSINESS_RULE / TERM_MAPPING
    private String domain; // 领域：预算执行/工程投资/自然资源（可空）
    private String question; // 提问（仅SQL_EXAMPLE类型使用）
    private Integer chunkIndex; // 分片编号（自动生成，对用户隐藏）
    private String content; // 分片内容（SQL语句或文档内容）
    private String embeddingId; // 向量存储中的ID
}
