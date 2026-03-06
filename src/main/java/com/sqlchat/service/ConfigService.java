package com.sqlchat.service;

import com.sqlchat.connection.DatabaseConnection;
import com.sqlchat.connection.DatabaseConnectionFactory;
import com.sqlchat.entity.DatabaseConfigEntity;
import com.sqlchat.entity.QueryTemplateEntity;
import com.sqlchat.model.DatabaseConfig;
import com.sqlchat.model.QueryTemplate;
import com.sqlchat.model.TableInfo;
import com.sqlchat.repository.DatabaseConfigRepository;
import com.sqlchat.repository.QueryTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 配置服务（数据库配置和查询模板配置）
 * @author sqlChat
 */
@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    @Autowired
    private DatabaseConfigRepository databaseConfigRepository;

    @Autowired
    private QueryTemplateRepository queryTemplateRepository;

    @Autowired
    private DatabaseConnectionFactory connectionFactory;

    @Autowired
    private SchemaInfoService schemaInfoService;

    // ========== 数据库配置相关 ==========

    /**
     * 保存数据库配置
     */
    public DatabaseConfig saveDatabaseConfig(String userId, DatabaseConfig config) {
        DatabaseConfigEntity entity = new DatabaseConfigEntity();
        if (config.getId() != null) {
            // 更新
            entity = databaseConfigRepository.findById(config.getId())
                .orElseThrow(() -> new RuntimeException("配置不存在"));
            if (!entity.getUserId().equals(userId)) {
                throw new RuntimeException("无权访问此配置");
            }
        } else {
            // 新建
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
            entity.setUserId(userId);
        }

        entity.setName(config.getName());
        entity.setType(config.getType());
        entity.setHost(config.getHost());
        entity.setPort(config.getPort());
        entity.setDatabase(config.getDatabase());
        entity.setUsername(config.getUsername());
        entity.setPassword(config.getPassword());

        DatabaseConfigEntity saved = databaseConfigRepository.save(entity);
        DatabaseConfig result = convertToDatabaseConfig(saved);

        // 保存/更新配置后自动拉取并缓存Schema
        try {
            schemaInfoService.fetchAndSaveSchema(result);
            logger.info("已自动拉取并保存数据库配置[{}]的Schema", result.getId());
        } catch (Exception e) {
            logger.warn("自动拉取Schema失败（不影响配置保存）: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 获取用户的数据库配置列表
     */
    public List<DatabaseConfig> getDatabaseConfigsByUserId(String userId) {
        return databaseConfigRepository.findByUserId(userId).stream()
            .map(this::convertToDatabaseConfig)
            .collect(Collectors.toList());
    }

    /**
     * 获取用户的数据库配置列表（分页）
     */
    public Map<String, Object> getDatabaseConfigsByUserIdPaged(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DatabaseConfigEntity> entityPage = databaseConfigRepository.findByUserId(userId, pageable);
        List<DatabaseConfig> content = entityPage.getContent().stream()
            .map(this::convertToDatabaseConfig)
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
     * 获取单个数据库配置
     */
    public DatabaseConfig getDatabaseConfig(String userId, String id) {
        DatabaseConfigEntity entity = databaseConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("配置不存在"));
        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此配置");
        }
        return convertToDatabaseConfig(entity);
    }

    /**
     * 删除数据库配置（同时删除关联的Schema信息）
     */
    public void deleteDatabaseConfig(String userId, String id) {
        DatabaseConfigEntity entity = databaseConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("配置不存在"));
        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此配置");
        }
        // 先删除关联的Schema信息
        schemaInfoService.deleteByConfigId(id);
        databaseConfigRepository.delete(entity);
    }

    /**
     * 获取数据库Schema（优先从本地MySQL获取，无缓存则从远程拉取）
     */
    public List<TableInfo> getDatabaseSchema(String userId, String id) throws Exception {
        DatabaseConfig config = getDatabaseConfig(userId, id);

        // 优先从本地获取
        if (schemaInfoService.hasLocalSchema(id)) {
            return schemaInfoService.getLocalSchema(id);
        }

        // 本地无缓存，从远程拉取并保存
        return schemaInfoService.fetchAndSaveSchema(config);
    }

    /**
     * 刷新数据库Schema（从远程重新拉取，保留用户自定义注释）
     */
    public List<TableInfo> refreshDatabaseSchema(String userId, String id) throws Exception {
        DatabaseConfig config = getDatabaseConfig(userId, id);
        return schemaInfoService.fetchAndSaveSchema(config);
    }

    /**
     * 更新表注释
     */
    public void updateTableComment(String userId, String configId, String tableName, String tableComment) {
        // 验证权限
        getDatabaseConfig(userId, configId);
        schemaInfoService.updateTableComment(configId, tableName, tableComment);
    }

    /**
     * 更新列注释
     */
    public void updateColumnComment(String userId, String configId, String tableName, String columnName, String columnComment) {
        // 验证权限
        getDatabaseConfig(userId, configId);
        schemaInfoService.updateColumnComment(configId, tableName, columnName, columnComment);
    }

    /**
     * 实体转模型
     */
    private DatabaseConfig convertToDatabaseConfig(DatabaseConfigEntity entity) {
        DatabaseConfig config = new DatabaseConfig();
        config.setId(entity.getId());
        config.setType(entity.getType());
        config.setName(entity.getName());
        config.setHost(entity.getHost());
        config.setPort(entity.getPort());
        config.setDatabase(entity.getDatabase());
        config.setUsername(entity.getUsername());
        config.setPassword(entity.getPassword());
        return config;
    }

    // ========== 查询模板相关 ==========

    /**
     * 保存查询模板
     */
    public QueryTemplate saveQueryTemplate(String userId, QueryTemplate template) {
        QueryTemplateEntity entity = new QueryTemplateEntity();
        if (template.getId() != null) {
            // 更新
            entity = queryTemplateRepository.findById(template.getId())
                .orElseThrow(() -> new RuntimeException("模板不存在"));
            if (!entity.getUserId().equals(userId)) {
                throw new RuntimeException("无权访问此模板");
            }
        } else {
            // 新建
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
            entity.setUserId(userId);
        }

        entity.setName(template.getName());
        entity.setDescription(template.getDescription());
        entity.setQueryExample(template.getQueryExample());
        entity.setIsDefault(template.getIsDefault());

        QueryTemplateEntity saved = queryTemplateRepository.save(entity);
        return convertToQueryTemplate(saved);
    }

    /**
     * 获取用户的查询模板列表
     */
    public List<QueryTemplate> getQueryTemplatesByUserId(String userId) {
        return queryTemplateRepository.findByUserId(userId).stream()
            .map(this::convertToQueryTemplate)
            .collect(Collectors.toList());
    }

    /**
     * 获取用户的查询模板列表（分页）
     */
    public Map<String, Object> getQueryTemplatesByUserIdPaged(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QueryTemplateEntity> entityPage = queryTemplateRepository.findByUserId(userId, pageable);
        List<QueryTemplate> content = entityPage.getContent().stream()
            .map(this::convertToQueryTemplate)
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
     * 获取单个查询模板
     */
    public QueryTemplate getQueryTemplate(String userId, String id) {
        QueryTemplateEntity entity = queryTemplateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("模板不存在"));
        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此模板");
        }
        return convertToQueryTemplate(entity);
    }

    /**
     * 删除查询模板
     */
    public void deleteQueryTemplate(String userId, String id) {
        QueryTemplateEntity entity = queryTemplateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("模板不存在"));
        if (!entity.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此模板");
        }
        queryTemplateRepository.delete(entity);
    }

    /**
     * 从Markdown文件批量导入查询模板
     */
    public Map<String, Object> importTemplatesFromMarkdown(String userId, String mdContent) {
        List<Map<String, String>> records = parseMdRecords(mdContent);
        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            Map<String, String> record = records.get(i);
            try {
                QueryTemplate template = new QueryTemplate();
                template.setName(record.getOrDefault("name", "").trim());
                template.setDescription(record.getOrDefault("description", "").trim());
                template.setQueryExample(record.getOrDefault("queryexample", "").trim());

                if (template.getName().isEmpty()) {
                    errors.add("第" + (i + 1) + "条: 模板名称为空，已跳过");
                    failed++;
                    continue;
                }

                saveQueryTemplate(userId, template);
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
     * 解析Markdown导入文件
     */
    private List<Map<String, String>> parseMdRecords(String rawContent) {
        String content = rawContent.replace("\r\n", "\n").replace("\r", "\n");
        String[] chunks = content.split("\\n\\s*---\\s*\\n|\\n\\s*---\\s*$");

        List<Map<String, String>> records = new ArrayList<>();
        Pattern kvPattern = Pattern.compile("^([a-zA-Z_\\u4e00-\\u9fa5]+)\\s*[:：]\\s*(.*)$");

        for (String chunk : chunks) {
            chunk = chunk.trim();
            if (chunk.isEmpty()) continue;

            Map<String, String> record = new LinkedHashMap<>();
            String[] lines = chunk.split("\\n");

            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) continue;
                Matcher matcher = kvPattern.matcher(trimmedLine);
                if (matcher.matches()) {
                    String key = matcher.group(1).trim().toLowerCase();
                    String value = matcher.group(2).trim();
                    record.put(key, value);
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
    private QueryTemplate convertToQueryTemplate(QueryTemplateEntity entity) {
        QueryTemplate template = new QueryTemplate();
        template.setId(entity.getId());
        template.setName(entity.getName());
        template.setDescription(entity.getDescription());
        template.setQueryExample(entity.getQueryExample());
        template.setIsDefault(entity.getIsDefault());
        return template;
    }
}
