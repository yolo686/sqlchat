package com.sqlchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NL2SQL请求
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Nl2SqlRequest {
    private String userId; // 用户ID
    private String question; // 用户问题
    private String databaseConfigId; // 数据库配置ID
    private Boolean executeSql; // 是否执行SQL
    private Boolean enableVoting; // 是否启用多候选投票策略

    // ====== 消融实验开关 (Ablation Study Flags) ======
    // 默认全部开启(null视为true)，设为false可关闭对应模块
    private Boolean enableQuestionParsing;  // 是否开启提问解析（领域识别、意图分类、实体抽取）
    private Boolean enableSqlExamples;      // 是否开启SQL示例检索
    private Boolean enableRagDocuments;     // 是否开启其他RAG文档（通用文档、业务规则、术语映射）
}
