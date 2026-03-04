package com.sqlchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG检索上下文
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagContext {
    private List<TableInfo> relevantTables; // 相关的表结构
    private List<String> generalDocs; // 通用文档
    private List<String> businessRules; // 业务规则
    private List<String> termMappings; // 术语映射
    private List<String> sqlExamples; // 历史SQL示例
    private String schemaDescription; // Schema描述
    private String matchedDomain; // 命中的领域（若有）
}
