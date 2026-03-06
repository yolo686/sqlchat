package com.sqlchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NL2SQL评测专用请求（独立于前端接口，直接传递数据库连接参数）
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Nl2SqlEvalRequest {
    private String question;          // 用户问题

    // ====== 数据库连接参数（直接连接，不依赖配置ID） ======
    private String dbHost = "127.0.0.1";
    private Integer dbPort = 3306;
    private String dbUser = "root";
    private String dbPass = "";
    private String dbName;            // 数据库名，如 financial、test_bird
    private String dbType = "mysql";  // 数据库类型

    // ====== 消融实验开关 (null 视为 true，即默认全开) ======
    private Boolean enableQuestionParsing;  // 是否开启提问解析
    private Boolean enableSqlExamples;      // 是否开启SQL示例检索
    private Boolean enableRagDocuments;     // 是否开启RAG文档
    private Boolean enableVoting;           // 是否启用多候选投票

    // ====== 可选参数 ======
    private String userId;            // 用户ID（用于RAG知识库检索，为空则不使用知识库）
    private Boolean executeSql;       // 是否执行SQL
}
