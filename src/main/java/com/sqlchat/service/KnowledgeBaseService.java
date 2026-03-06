package com.sqlchat.service;

import com.sqlchat.entity.KnowledgeBaseEntity;
import com.sqlchat.model.KnowledgeBase;
import com.sqlchat.repository.KnowledgeBaseRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 知识库服务
 * @author sqlChat
 */
@Service
public class KnowledgeBaseService {

    public static final String TYPE_SQL_EXAMPLE = "SQL_EXAMPLE";
    public static final String TYPE_GENERAL_DOC = "GENERAL_DOC";
    public static final String TYPE_BUSINESS_RULE = "BUSINESS_RULE";
    public static final String TYPE_TERM_MAPPING = "TERM_MAPPING";

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            TYPE_SQL_EXAMPLE,
            TYPE_GENERAL_DOC,
            TYPE_BUSINESS_RULE,
            TYPE_TERM_MAPPING
    );

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    private final AllMiniLmL6V2QuantizedEmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

    // 用户+类型级别存储（跨领域聚合，领域缺失时回退使用）
    private final Map<String, InMemoryEmbeddingStore<TextSegment>> typeStores = new HashMap<>();
    // 用户+领域+类型级别存储（领域命中时优先使用）
    private final Map<String, InMemoryEmbeddingStore<TextSegment>> domainTypeStores = new HashMap<>();

    private String typeStoreKey(String userId, String type) {
        return userId + "|" + normalizeType(type);
    }

    private String domainTypeStoreKey(String userId, String domain, String type) {
        return userId + "|" + normalizeDomain(domain) + "|" + normalizeType(type);
    }

    private String normalizeType(String type) {
        if (type == null) {
            return "";
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if ("DOCUMENT".equals(normalized)) {
            return TYPE_GENERAL_DOC;
        }
        return normalized;
    }

    private String normalizeDomain(String domain) {
        if (domain == null) {
            return "";
        }
        return domain.trim();
    }

    private void validateType(String type) {
        if (!SUPPORTED_TYPES.contains(normalizeType(type))) {
            throw new RuntimeException("不支持的知识类型: " + type);
        }
    }

    private InMemoryEmbeddingStore<TextSegment> getTypeStore(String userId, String type) {
        return typeStores.computeIfAbsent(typeStoreKey(userId, type), k -> new InMemoryEmbeddingStore<>());
    }

    private InMemoryEmbeddingStore<TextSegment> getDomainTypeStore(String userId, String domain, String type) {
        return domainTypeStores.computeIfAbsent(domainTypeStoreKey(userId, domain, type), k -> new InMemoryEmbeddingStore<>());
    }

    private InMemoryEmbeddingStore<TextSegment> getStore(String userId, String type, String domain) {
        String normalizedDomain = normalizeDomain(domain);
        if (!normalizedDomain.isEmpty()) {
            return getDomainTypeStore(userId, normalizedDomain, type);
        }
        return getTypeStore(userId, type);
    }

    private void clearUserStores(String userId) {
        String prefix = userId + "|";
        typeStores.keySet().removeIf(key -> key.startsWith(prefix));
        domainTypeStores.keySet().removeIf(key -> key.startsWith(prefix));
    }

    /**
     * 初始化用户的知识库向量存储（从数据库加载）
     * 如果向量存储已存在，先清空再重新加载，确保数据一致性
     */
    public void initializeUserKnowledgeBase(String userId) {
        clearUserStores(userId);
        List<KnowledgeBaseEntity> entities = knowledgeBaseRepository.findByUserId(userId);
        for (KnowledgeBaseEntity entity : entities) {
            addEmbedding(userId, entity);
        }
    }

    /**
     * 获取用户的所有知识库（列表形式）
     */
    public List<KnowledgeBase> getKnowledgeBasesByUser(String userId, String type) {
        return getKnowledgeBasesByUser(userId, type, null);
    }

    /**
     * 获取用户知识库（支持按类型/领域筛选；筛选条件可选）
     */
    public List<KnowledgeBase> getKnowledgeBasesByUser(String userId, String type, String domain) {
        List<KnowledgeBaseEntity> entities;
        String normalizedType = normalizeType(type);
        String normalizedDomain = normalizeDomain(domain);

        if (!normalizedType.isEmpty() && !normalizedDomain.isEmpty()) {
            entities = knowledgeBaseRepository.findByUserIdAndTypeAndDomain(userId, normalizedType, normalizedDomain);
        } else if (!normalizedType.isEmpty()) {
            entities = knowledgeBaseRepository.findByUserIdAndType(userId, normalizedType);
        } else if (!normalizedDomain.isEmpty()) {
            entities = knowledgeBaseRepository.findByUserIdAndDomain(userId, normalizedDomain);
        } else {
            entities = knowledgeBaseRepository.findByUserId(userId);
        }

        return entities.stream()
            .map(this::convertToModel)
            .sorted(Comparator.comparing(KnowledgeBase::getChunkIndex))
            .collect(Collectors.toList());
    }

    /**
     * 获取用户知识库（分页，支持按类型/领域筛选）
     */
    public Map<String, Object> getKnowledgeBasesByUserPaged(String userId, String type, String domain, int page, int size) {
        String normalizedType = normalizeType(type);
        String normalizedDomain = normalizeDomain(domain);
        Pageable pageable = PageRequest.of(page, size, Sort.by("chunkIndex").ascending());

        Page<KnowledgeBaseEntity> entityPage;
        if (!normalizedType.isEmpty() && !normalizedDomain.isEmpty()) {
            entityPage = knowledgeBaseRepository.findByUserIdAndTypeAndDomain(userId, normalizedType, normalizedDomain, pageable);
        } else if (!normalizedType.isEmpty()) {
            entityPage = knowledgeBaseRepository.findByUserIdAndType(userId, normalizedType, pageable);
        } else if (!normalizedDomain.isEmpty()) {
            entityPage = knowledgeBaseRepository.findByUserIdAndDomain(userId, normalizedDomain, pageable);
        } else {
            entityPage = knowledgeBaseRepository.findByUserId(userId, pageable);
        }

        List<KnowledgeBase> content = entityPage.getContent().stream()
            .map(this::convertToModel)
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", entityPage.getTotalElements());
        result.put("totalPages", entityPage.getTotalPages());
        result.put("currentPage", entityPage.getNumber());
        result.put("pageSize", entityPage.getSize());
        return result;
    }

    /**
     * 保存知识库（支持新增和更新）
     * chunkIndex自动生成
     */
    @Transactional
    public KnowledgeBase saveKnowledgeBase(String userId, KnowledgeBase knowledgeBase) {
        String normalizedType = normalizeType(knowledgeBase.getType());
        validateType(normalizedType);
        String normalizedDomain = normalizeDomain(knowledgeBase.getDomain());

        KnowledgeBaseEntity entity;

        if (knowledgeBase.getId() != null && !knowledgeBase.getId().isEmpty()) {
            // 更新
            entity = knowledgeBaseRepository.findById(knowledgeBase.getId())
                .orElseThrow(() -> new RuntimeException("知识库不存在"));

            // 检查用户权限
            if (!entity.getUserId().equals(userId)) {
                throw new RuntimeException("无权修改此知识库");
            }

            String oldType = entity.getType();
            String oldDomain = entity.getDomain();

            // 更新内容
            entity.setType(normalizedType);
            entity.setDomain(normalizedDomain);
            entity.setQuestion(knowledgeBase.getQuestion());
            entity.setContent(knowledgeBase.getContent());
            // chunkIndex保持不变

            if (entity.getEmbeddingId() == null || entity.getEmbeddingId().isEmpty()) {
                entity.setEmbeddingId(UUID.randomUUID().toString().replace("-", ""));
            }

            // 重新向量化
            updateEmbedding(userId, entity, oldType, oldDomain);
        } else {
            // 新增 - 自动生成chunkIndex
            int maxChunkIndex = getKnowledgeBasesByUser(userId, normalizedType, normalizedDomain).stream()
                .mapToInt(KnowledgeBase::getChunkIndex)
                .max()
                .orElse(-1);

            entity = new KnowledgeBaseEntity();
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
            entity.setUserId(userId);
            entity.setType(normalizedType);
            entity.setDomain(normalizedDomain);
            entity.setQuestion(knowledgeBase.getQuestion());
            entity.setChunkIndex(maxChunkIndex + 1); // 自动生成
            entity.setContent(knowledgeBase.getContent());
            entity.setEmbeddingId(UUID.randomUUID().toString().replace("-", ""));

            // 向量化
            addEmbedding(userId, entity);
        }

        entity = knowledgeBaseRepository.save(entity);
        return convertToModel(entity);
    }

    /**
     * 删除知识库
     */
    @Transactional
    public void deleteKnowledgeBase(String userId, String id) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("知识库不存在"));

        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此知识库");
        }

        // 从向量存储中删除（聚合store + 领域store）
        if (entity.getEmbeddingId() != null) {
            getTypeStore(userId, entity.getType()).remove(entity.getEmbeddingId());
            if (entity.getDomain() != null && !entity.getDomain().trim().isEmpty()) {
                getDomainTypeStore(userId, entity.getDomain(), entity.getType()).remove(entity.getEmbeddingId());
            }
        }

        knowledgeBaseRepository.delete(entity);
    }


    /**
     * 添加向量
     * 对于SQL_EXAMPLE，只对question进行向量化，SQL语句存储在metadata中
     */
    private void addEmbedding(String userId, KnowledgeBaseEntity entity) {
        validateType(entity.getType());

        // 构建向量化文本
        String textForEmbedding;
        TextSegment segment;

        if (TYPE_SQL_EXAMPLE.equals(normalizeType(entity.getType()))) {
            // SQL示例：只对question进行向量化
            if (entity.getQuestion() != null && !entity.getQuestion().trim().isEmpty()) {
                textForEmbedding = entity.getQuestion();
            } else {
                // 如果question为空，降级使用content
                textForEmbedding = entity.getContent() != null ? entity.getContent() : "";
            }
            
            // 创建TextSegment，将SQL语句存储在metadata中
            segment = TextSegment.from(textForEmbedding);
            if (entity.getContent() != null) {
                segment.metadata().put("sql_content", entity.getContent());
            }
            if (entity.getQuestion() != null) {
                segment.metadata().put("question", entity.getQuestion());
            }
        } else {
            // 文档：直接使用内容
            textForEmbedding = entity.getContent() != null ? entity.getContent() : "";
            segment = TextSegment.from(textForEmbedding);
        }

        segment.metadata().put("type", normalizeType(entity.getType()));
        if (entity.getDomain() != null && !entity.getDomain().trim().isEmpty()) {
            segment.metadata().put("domain", entity.getDomain().trim());
        }

        Embedding embedding = embeddingModel.embed(segment).content();
        getTypeStore(userId, entity.getType()).add(entity.getEmbeddingId(), embedding, segment);
        if (entity.getDomain() != null && !entity.getDomain().trim().isEmpty()) {
            getDomainTypeStore(userId, entity.getDomain(), entity.getType()).add(entity.getEmbeddingId(), embedding, segment);
        }
    }

    /**
     * 更新向量（删除旧向量，添加新向量）
     */
    private void updateEmbedding(String userId, KnowledgeBaseEntity entity, String oldType, String oldDomain) {
        if (entity.getEmbeddingId() != null) {
            getTypeStore(userId, oldType).remove(entity.getEmbeddingId());
            if (oldDomain != null && !oldDomain.trim().isEmpty()) {
                getDomainTypeStore(userId, oldDomain, oldType).remove(entity.getEmbeddingId());
            }
            // 防御性删除一次当前路由
            getTypeStore(userId, entity.getType()).remove(entity.getEmbeddingId());
            if (entity.getDomain() != null && !entity.getDomain().trim().isEmpty()) {
                getDomainTypeStore(userId, entity.getDomain(), entity.getType()).remove(entity.getEmbeddingId());
            }
        }

        addEmbedding(userId, entity);
    }

    /**
     * 获取用户的SQL示例向量存储
     */
    public InMemoryEmbeddingStore<TextSegment> getSqlStoreForUser(String userId) {
        return getTypeStore(userId, TYPE_SQL_EXAMPLE);
    }

    /**
     * 获取用户的文档向量存储
     */
    public InMemoryEmbeddingStore<TextSegment> getDocStoreForUser(String userId) {
        return getTypeStore(userId, TYPE_GENERAL_DOC);
    }

    /**
     * 按类型与领域获取向量存储
     */
    public InMemoryEmbeddingStore<TextSegment> getStoreForUser(String userId, String type, String domain) {
        validateType(type);
        return getStore(userId, type, domain);
    }

    /**
     * 判断用户是否存在指定领域的知识
     */
    public boolean hasDomainKnowledge(String userId, String domain) {
        String normalizedDomain = normalizeDomain(domain);
        return !normalizedDomain.isEmpty() && knowledgeBaseRepository.existsByUserIdAndDomain(userId, normalizedDomain);
    }

    /**
     * 从Markdown文件批量导入知识库
     * 格式：每条记录以 --- 分隔，元数据用 key: value，空行后为内容
     */
    @Transactional
    public Map<String, Object> importFromMarkdown(String userId, String mdContent) {
        List<Map<String, String>> records = parseMdRecords(mdContent);
        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            Map<String, String> record = records.get(i);
            try {
                KnowledgeBase kb = new KnowledgeBase();
                kb.setDomain(record.getOrDefault("domain", ""));
                kb.setType(record.getOrDefault("type", "SQL_EXAMPLE"));
                kb.setQuestion(record.getOrDefault("question", ""));
                kb.setContent(record.getOrDefault("content", ""));

                if (kb.getContent() == null || kb.getContent().trim().isEmpty()) {
                    errors.add("第" + (i + 1) + "条: 内容为空，已跳过");
                    failed++;
                    continue;
                }

                saveKnowledgeBase(userId, kb);
                success++;
            } catch (Exception e) {
                errors.add("第" + (i + 1) + "条: " + e.getMessage());
                failed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("total", records.size());
        result.put("errors", errors);
        return result;
    }

    /**
     * 解析Markdown导入文件，将内容拆分为记录列表
     * 格式说明：
     *  - 记录之间用单独一行 --- 分隔
     *  - 每条记录开头为元数据行（domain: xxx, type: xxx, question: xxx）
     *  - 元数据后空一行，再写内容（content）
     */
    private List<Map<String, String>> parseMdRecords(String rawContent) {
        String content = rawContent.replace("\r\n", "\n").replace("\r", "\n");
        // 用 --- 分隔记录
        String[] chunks = content.split("\\n\\s*---\\s*\\n|\\n\\s*---\\s*$");

        List<Map<String, String>> records = new ArrayList<>();
        Pattern kvPattern = Pattern.compile("^([a-zA-Z_\\u4e00-\\u9fa5]+)\\s*[:：]\\s*(.*)$");

        for (String chunk : chunks) {
            chunk = chunk.trim();
            if (chunk.isEmpty()) continue;

            Map<String, String> record = new LinkedHashMap<>();
            String[] lines = chunk.split("\\n");
            int contentStartLine = -1;

            for (int i = 0; i < lines.length; i++) {
                String trimmedLine = lines[i].trim();
                if (trimmedLine.isEmpty()) {
                    contentStartLine = i + 1;
                    break;
                }
                Matcher matcher = kvPattern.matcher(trimmedLine);
                if (matcher.matches()) {
                    String key = matcher.group(1).trim().toLowerCase();
                    String value = matcher.group(2).trim();
                    record.put(key, value);
                } else {
                    contentStartLine = i;
                    break;
                }
            }

            if (contentStartLine >= 0 && contentStartLine < lines.length) {
                StringBuilder sb = new StringBuilder();
                for (int i = contentStartLine; i < lines.length; i++) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(lines[i]);
                }
                String text = sb.toString().trim();
                if (!text.isEmpty()) {
                    record.put("content", text);
                }
            }

            if (!record.isEmpty()) {
                records.add(record);
            }
        }
        return records;
    }

    /**
     * 实体转模型
     */
    private KnowledgeBase convertToModel(KnowledgeBaseEntity entity) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(entity.getId());
        kb.setUserId(entity.getUserId());
        kb.setType(entity.getType());
        kb.setDomain(entity.getDomain());
        kb.setQuestion(entity.getQuestion());
        kb.setChunkIndex(entity.getChunkIndex());
        kb.setContent(entity.getContent());
        kb.setEmbeddingId(entity.getEmbeddingId());
        return kb;
    }
}
