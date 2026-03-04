package com.sqlchat.voting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 投票结果
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteResult {
    private String bestSql;            // 最佳SQL
    private int voteCount;              // 最佳SQL得票数
    private int totalCandidates;        // 总候选数
    private double confidence;          // 置信度 = voteCount / totalCandidates
    private String strategy;            // 使用的投票策略
    private List<String> candidateSqls; // 所有候选SQL
    private boolean degraded;           // 是否降级生成
}
