package com.sqlchat.voting;

import com.sqlchat.executor.SqlExecutor;
import com.sqlchat.executor.SqlExecutorFactory;
import com.sqlchat.model.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Self-Consistency 投票器
 * 实现策略A（文本投票）+ 策略C（执行结果投票）的混合方案
 * @author sqlChat
 */
@Component
public class SelfConsistencyVoter {

    private static final Logger logger = LoggerFactory.getLogger(SelfConsistencyVoter.class);

    /** 置信度阈值，得票率 >= 60% 才认为结果可信 */
    private static final double CONFIDENCE_THRESHOLD = 0.6;

    @Autowired
    private SqlNormalizer sqlNormalizer;

    @Autowired
    private SqlExecutorFactory executorFactory;

    @Autowired
    private CandidateGenerator candidateGenerator;

    /**
     * 混合投票：先文本投票，无明确多数票时降级到执行结果投票
     * @param candidates 候选SQL列表
     * @param prompt 原始提示词（用于降级生成）
     * @param dbConfig 数据库配置（用于执行结果投票）
     * @return 投票结果
     */
    public VoteResult vote(List<String> candidates, String prompt, DatabaseConfig dbConfig) {
        if (candidates == null || candidates.isEmpty()) {
            return createEmptyResult();
        }

        // 只有1个候选，直接返回
        if (candidates.size() == 1) {
            return createSingleResult(candidates.get(0), candidates);
        }

        // 策略A：文本规范化投票
        logger.info("=== 策略A：文本投票 ===");
        VoteResult textResult = textVote(candidates);
        logger.info("文本投票结果：置信度={}, 得票={}/{}", 
                textResult.getConfidence(), textResult.getVoteCount(), textResult.getTotalCandidates());

        if (textResult.getConfidence() >= CONFIDENCE_THRESHOLD) {
            logger.info("文本投票置信度 >= {}，直接返回", CONFIDENCE_THRESHOLD);
            textResult.setStrategy("文本投票(策略A)");
            return textResult;
        }

        // 策略C：执行结果投票
        logger.info("=== 策略C：执行结果投票 ===");
        if (dbConfig != null) {
            VoteResult execResult = executionVote(candidates, dbConfig);
            if (execResult != null && execResult.getConfidence() >= CONFIDENCE_THRESHOLD) {
                logger.info("执行结果投票置信度 >= {}，返回执行投票结果", CONFIDENCE_THRESHOLD);
                execResult.setStrategy("执行结果投票(策略C)");
                return execResult;
            }
            // 执行投票也没有明确结果，但如果有可执行的SQL，优先选它
            if (execResult != null && execResult.getBestSql() != null) {
                logger.info("执行结果投票置信度不足，但有可执行SQL，尝试使用");
                execResult.setStrategy("执行结果投票(策略C-低置信)");
                return execResult;
            }
        }

        // 降级策略：用temperature=0重新生成
        logger.info("=== 降级策略：temperature=0 确定性生成 ===");
        String fallbackSql = candidateGenerator.generateFallback(prompt);
        if (fallbackSql != null && !fallbackSql.trim().isEmpty()) {
            VoteResult fallbackResult = new VoteResult();
            fallbackResult.setBestSql(fallbackSql);
            fallbackResult.setVoteCount(1);
            fallbackResult.setTotalCandidates(candidates.size());
            fallbackResult.setConfidence(1.0 / candidates.size());
            fallbackResult.setStrategy("降级生成(temperature=0)");
            fallbackResult.setCandidateSqls(candidates);
            fallbackResult.setDegraded(true);
            return fallbackResult;
        }

        // 兜底：返回文本投票中的最佳结果
        textResult.setStrategy("文本投票(策略A-兜底)");
        return textResult;
    }

    /**
     * 策略A：文本规范化投票
     */
    private VoteResult textVote(List<String> candidates) {
        // 规范化SQL并分组计数
        Map<String, Long> frequencyMap = candidates.stream()
                .map(sqlNormalizer::normalize)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        if (frequencyMap.isEmpty()) {
            return createEmptyResult();
        }

        // 找出出现次数最多的
        Map.Entry<String, Long> bestEntry = frequencyMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (bestEntry == null) {
            return createEmptyResult();
        }

        String bestNormalized = bestEntry.getKey();
        long bestCount = bestEntry.getValue();

        // 返回原始SQL（保留格式）
        String bestOriginalSql = candidates.stream()
                .filter(sql -> sqlNormalizer.normalize(sql).equals(bestNormalized))
                .findFirst()
                .orElse(candidates.get(0));

        VoteResult result = new VoteResult();
        result.setBestSql(bestOriginalSql);
        result.setVoteCount((int) bestCount);
        result.setTotalCandidates(candidates.size());
        result.setConfidence((double) bestCount / candidates.size());
        result.setCandidateSqls(candidates);
        result.setDegraded(false);

        return result;
    }

    /**
     * 策略C：执行结果投票
     * 实际执行每条SQL，按结果集分组，选择最大组
     */
    private VoteResult executionVote(List<String> candidates, DatabaseConfig dbConfig) {
        try {
            SqlExecutor executor = executorFactory.getExecutor(dbConfig.getType());

            // 执行每条SQL，记录结果哈希
            Map<String, List<String>> resultGroups = new LinkedHashMap<>();
            Map<String, String> sqlResultHash = new LinkedHashMap<>();

            for (String sql : candidates) {
                try {
                    if (!executor.validateSql(sql)) {
                        logger.warn("SQL验证不通过，跳过: {}", sql);
                        continue;
                    }
                    List<Map<String, Object>> data = executor.executeQuery(dbConfig, sql);
                    String hash = computeResultHash(data);
                    resultGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(sql);
                    sqlResultHash.put(sql, hash);
                } catch (Exception e) {
                    logger.warn("SQL执行失败，跳过: {} - {}", sql, e.getMessage());
                }
            }

            if (resultGroups.isEmpty()) {
                logger.warn("所有候选SQL执行都失败");
                return null;
            }

            // 选择结果集最大组
            Map.Entry<String, List<String>> bestGroup = resultGroups.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue().size()))
                    .orElse(null);

            if (bestGroup == null) {
                return null;
            }

            List<String> bestSqls = bestGroup.getValue();
            String bestSql = bestSqls.get(0);

            // 在同一结果组内，优先选文本投票中得票最高的
            if (bestSqls.size() > 1) {
                VoteResult innerText = textVote(bestSqls);
                bestSql = innerText.getBestSql();
            }

            int executableCount = resultGroups.values().stream()
                    .mapToInt(List::size)
                    .sum();

            VoteResult result = new VoteResult();
            result.setBestSql(bestSql);
            result.setVoteCount(bestSqls.size());
            result.setTotalCandidates(executableCount);
            result.setConfidence((double) bestSqls.size() / executableCount);
            result.setCandidateSqls(candidates);
            result.setDegraded(false);

            return result;
        } catch (Exception e) {
            logger.error("执行结果投票失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 计算查询结果集的哈希值
     */
    private String computeResultHash(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "EMPTY";
        }

        try {
            StringBuilder sb = new StringBuilder();
            // 排序列名确保一致性
            for (Map<String, Object> row : data) {
                List<String> sortedKeys = new ArrayList<>(row.keySet());
                Collections.sort(sortedKeys);
                for (String key : sortedKeys) {
                    Object val = row.get(key);
                    sb.append(key).append("=").append(val == null ? "NULL" : val.toString()).append("|");
                }
                sb.append("\n");
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sb.toString().getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return data.toString();
        }
    }

    private VoteResult createEmptyResult() {
        VoteResult result = new VoteResult();
        result.setBestSql("");
        result.setVoteCount(0);
        result.setTotalCandidates(0);
        result.setConfidence(0);
        result.setStrategy("无候选");
        result.setCandidateSqls(Collections.emptyList());
        result.setDegraded(false);
        return result;
    }

    private VoteResult createSingleResult(String sql, List<String> candidates) {
        VoteResult result = new VoteResult();
        result.setBestSql(sql);
        result.setVoteCount(1);
        result.setTotalCandidates(1);
        result.setConfidence(1.0);
        result.setStrategy("单候选");
        result.setCandidateSqls(candidates);
        result.setDegraded(false);
        return result;
    }
}
