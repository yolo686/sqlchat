package com.sqlchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * NL2SQL响应
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Nl2SqlResponse {
    private String sql; // 生成的SQL语句
    private SqlResult executionResult; // SQL执行结果（如果执行了）
    private ParsedQuestion parsedQuestion; // 解析后的问题
    private Boolean success; // 是否成功
    private String errorMessage; // 错误信息

    // 多候选投票相关字段
    private Boolean votingEnabled;         // 是否启用了投票
    private List<String> candidateSqls;    // 所有候选SQL列表
    private Integer voteCount;             // 最佳SQL得票数
    private Integer totalCandidates;       // 总候选数
    private Double confidence;             // 置信度
    private String votingStrategy;         // 使用的投票策略
    private Boolean degraded;              // 是否降级生成

    // ====== 消融实验信息 (Ablation Study Info) ======
    private AblationConfig ablationConfig; // 消融实验配置回显

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AblationConfig {
        private boolean questionParsing;   // 是否开启了提问解析
        private boolean sqlExamples;       // 是否开启了SQL示例
        private boolean ragDocuments;      // 是否开启了RAG文档
        private boolean voting;            // 是否开启了候选投票
    }
}
