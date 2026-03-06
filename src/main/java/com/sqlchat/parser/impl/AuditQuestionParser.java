package com.sqlchat.parser.impl;

import com.sqlchat.model.ParsedQuestion;
import com.sqlchat.parser.QuestionParser;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 审计问题解析器
 * @author sqlChat
 */
@Component
public class AuditQuestionParser implements QuestionParser {

    private static final String DOMAIN_BUDGET = "预算执行";
    private static final String DOMAIN_ENGINEERING = "工程投资";
    private static final String DOMAIN_NATURAL_RESOURCES = "自然资源";

    private static final Map<String, List<String>> DOMAIN_KEYWORDS = new HashMap<>();

    static {
        DOMAIN_KEYWORDS.put(DOMAIN_BUDGET, Arrays.asList(
                "预算", "执行率", "结转", "结余", "三公经费", "财政", "拨付", "专项资金", "决算"
        ));
        DOMAIN_KEYWORDS.put(DOMAIN_ENGINEERING, Arrays.asList(
                "工程", "项目", "概算", "决算", "招投标", "中标", "合同", "变更签证", "工期", "造价"
        ));
        DOMAIN_KEYWORDS.put(DOMAIN_NATURAL_RESOURCES, Arrays.asList(
                "土地", "耕地", "林地", "矿产", "资源", "出让金", "红线", "地块", "闲置土地", "开采"
        ));
    }

    private static final Pattern YEAR_PATTERN = Pattern.compile("(20\\d{2})年");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(万|万元|亿|亿元|元)");
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");

    @Override
    public ParsedQuestion parse(String question) {
        if (question == null || question.trim().isEmpty()) {
            return new ParsedQuestion("", "", null, Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), "通用查询", Collections.emptyMap());
        }

        String normalizedQuestion = normalizeQuestion(question);
        String domain = detectDomain(normalizedQuestion);
        String intent = detectIntent(normalizedQuestion);
        Map<String, String> entities = extractEntities(normalizedQuestion);
        List<String> auditTerms = extractAuditTerms(normalizedQuestion);

        return new ParsedQuestion(
                question,
                normalizedQuestion,
                domain,
                auditTerms,
                Collections.emptyList(),
                Collections.emptyList(),
                intent,
                entities
        );
    }

    private String normalizeQuestion(String question) {
        return question.trim()
                .replaceAll("\\s+", " ")
                .replace("？", "?")
                .replace("，", ",")
                .replace("。", ".");
    }

    private String detectDomain(String question) {
        int bestScore = 0;
        String bestDomain = null;
        for (Map.Entry<String, List<String>> entry : DOMAIN_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (question.contains(keyword)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestDomain = entry.getKey();
            }
        }
        return bestDomain;
    }

    private String detectIntent(String question) {
        if (question.contains("排名") || question.contains("top") || question.contains("前")) {
            return "排名分析";
        }
        if (question.contains("趋势") || question.contains("变化") || question.contains("同比") || question.contains("环比")) {
            return "趋势分析";
        }
        if (question.contains("超") || question.contains("异常") || question.contains("预警") || question.contains("问题")) {
            return "异常筛查";
        }
        if (question.contains("占比") || question.contains("结构")) {
            return "结构分析";
        }
        if (question.contains("多少") || question.contains("总额") || question.contains("合计") || question.contains("汇总")) {
            return "统计汇总";
        }
        return "通用查询";
    }

    private Map<String, String> extractEntities(String question) {
        Map<String, String> entities = new LinkedHashMap<>();
        java.util.regex.Matcher yearMatcher = YEAR_PATTERN.matcher(question);
        if (yearMatcher.find()) {
            entities.put("year", yearMatcher.group(1));
        }
        java.util.regex.Matcher amountMatcher = AMOUNT_PATTERN.matcher(question);
        if (amountMatcher.find()) {
            entities.put("amount", amountMatcher.group(1) + amountMatcher.group(2));
        }
        java.util.regex.Matcher percentMatcher = PERCENT_PATTERN.matcher(question);
        if (percentMatcher.find()) {
            entities.put("percentage", percentMatcher.group(1) + "%");
        }
        return entities;
    }

    private List<String> extractAuditTerms(String question) {
        return DOMAIN_KEYWORDS.values().stream()
                .flatMap(Collection::stream)
                .filter(question::contains)
                .distinct()
                .collect(Collectors.toList());
    }

}
