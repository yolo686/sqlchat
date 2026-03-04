package com.sqlchat.voting;

import com.sqlchat.llm.SqlGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 多候选SQL生成器，并发调用LLM生成多个候选SQL
 * @author sqlChat
 */
@Component
public class CandidateGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CandidateGenerator.class);

    /** 候选数量 */
    private static final int DEFAULT_CANDIDATE_COUNT = 5;

    /** 生成温度 */
    private static final double DEFAULT_TEMPERATURE = 0.7;

    /** 降级温度 */
    private static final double FALLBACK_TEMPERATURE = 0.0;

    /** 超时时间（秒） */
    private static final int TIMEOUT_SECONDS = 60;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Autowired
    private SqlGenerator sqlGenerator;

    /**
     * 并发生成多个候选SQL
     * @param prompt 提示词
     * @return 候选SQL列表
     */
    public List<String> generateCandidates(String prompt) {
        return generateCandidates(prompt, DEFAULT_CANDIDATE_COUNT, DEFAULT_TEMPERATURE);
    }

    /**
     * 并发生成多个候选SQL
     * @param prompt 提示词
     * @param count 候选数量
     * @param temperature 温度参数
     * @return 候选SQL列表
     */
    public List<String> generateCandidates(String prompt, int count, double temperature) {
        logger.info("开始并发生成 {} 个候选SQL，temperature={}", count, temperature);

        List<CompletableFuture<String>> futures = IntStream.range(0, count)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        logger.debug("生成候选SQL #{}", i + 1);
                        return sqlGenerator.generateSql(prompt, temperature);
                    } catch (Exception e) {
                        logger.warn("候选SQL #{} 生成失败: {}", i + 1, e.getMessage());
                        return null;
                    }
                }, executorService))
                .collect(Collectors.toList());

        // 等待所有结果，设置超时
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("部分候选SQL生成超时: {}", e.getMessage());
        }

        List<String> candidates = futures.stream()
                .map(f -> {
                    try {
                        return f.isDone() ? f.get() : null;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(sql -> !sql.trim().isEmpty())
                .collect(Collectors.toList());

        logger.info("成功生成 {} 个候选SQL", candidates.size());
        return candidates;
    }

    /**
     * 降级生成：使用temperature=0确定性生成一个SQL
     * @param prompt 提示词
     * @return SQL语句
     */
    public String generateFallback(String prompt) {
        logger.info("执行降级策略，temperature=0 确定性生成");
        return sqlGenerator.generateSql(prompt, FALLBACK_TEMPERATURE);
    }
}
