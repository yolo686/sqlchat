package com.sqlchat.controller;

import com.sqlchat.model.DatabaseConfig;
import com.sqlchat.model.QueryTemplate;
import com.sqlchat.model.TableInfo;
import com.sqlchat.service.ConfigService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置控制器（数据库配置和查询模板配置）
 * @author sqlChat
 */
@RestController
@RequestMapping("/api/config")
//@CrossOrigin(allowedOriginPatterns = "*", allowCredentials = "true")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    /**
     * 获取当前用户ID
     */
    private String getUserId(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("请先登录");
        }
        return userId;
    }

    // ========== 数据库配置相关 ==========

    /**
     * 获取所有数据库配置（支持可选分页：不传page返回全量列表，传page返回分页数据）
     */
    @GetMapping("/database-configs")
    public ResponseEntity<?> getAllDatabaseConfigs(
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "5") int size,
            HttpSession session) {
        String userId = getUserId(session);
        if (page != null) {
            Map<String, Object> pageData = configService.getDatabaseConfigsByUserIdPaged(userId, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", pageData.get("content"));
            response.put("totalElements", pageData.get("totalElements"));
            response.put("totalPages", pageData.get("totalPages"));
            response.put("currentPage", pageData.get("currentPage"));
            response.put("pageSize", pageData.get("pageSize"));
            return ResponseEntity.ok(response);
        } else {
            List<DatabaseConfig> configs = configService.getDatabaseConfigsByUserId(userId);
            return ResponseEntity.ok(configs);
        }
    }

    /**
     * 获取单个数据库配置
     */
    @GetMapping("/database-configs/{id}")
    public ResponseEntity<DatabaseConfig> getDatabaseConfig(@PathVariable String id, HttpSession session) {
        try {
            String userId = getUserId(session);
            DatabaseConfig config = configService.getDatabaseConfig(userId, id);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 保存数据库配置
     */
    @PostMapping("/database-configs")
    public ResponseEntity<Map<String, Object>> saveDatabaseConfig(@RequestBody DatabaseConfig config, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            DatabaseConfig saved = configService.saveDatabaseConfig(userId, config);
            response.put("success", true);
            response.put("message", "保存成功");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新数据库配置
     */
    @PutMapping("/database-configs/{id}")
    public ResponseEntity<Map<String, Object>> updateDatabaseConfig(@PathVariable String id, @RequestBody DatabaseConfig config, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            config.setId(id);
            DatabaseConfig updated = configService.saveDatabaseConfig(userId, config);
            response.put("success", true);
            response.put("message", "更新成功");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除数据库配置
     */
    @DeleteMapping("/database-configs/{id}")
    public ResponseEntity<Map<String, Object>> deleteDatabaseConfig(@PathVariable String id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            configService.deleteDatabaseConfig(userId, id);
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取数据库Schema（优先从本地MySQL获取）
     */
    @GetMapping("/database-configs/{id}/schema")
    public ResponseEntity<Map<String, Object>> getDatabaseSchema(@PathVariable String id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            List<TableInfo> schema = configService.getDatabaseSchema(userId, id);
            response.put("success", true);
            response.put("data", schema);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 刷新数据库Schema（从远程数据库重新拉取，保留用户自定义注释）
     */
    @PostMapping("/database-configs/{id}/schema/refresh")
    public ResponseEntity<Map<String, Object>> refreshDatabaseSchema(@PathVariable String id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            List<TableInfo> schema = configService.refreshDatabaseSchema(userId, id);
            response.put("success", true);
            response.put("data", schema);
            response.put("message", "Schema已刷新");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "刷新Schema失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新表注释
     */
    @PutMapping("/database-configs/{id}/schema/table-comment")
    public ResponseEntity<Map<String, Object>> updateTableComment(
            @PathVariable String id,
            @RequestBody Map<String, String> request,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            String tableName = request.get("tableName");
            String tableComment = request.get("tableComment");
            if (tableName == null || tableName.isEmpty()) {
                throw new RuntimeException("表名不能为空");
            }
            configService.updateTableComment(userId, id, tableName, tableComment);
            response.put("success", true);
            response.put("message", "表注释已更新");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新列注释
     */
    @PutMapping("/database-configs/{id}/schema/column-comment")
    public ResponseEntity<Map<String, Object>> updateColumnComment(
            @PathVariable String id,
            @RequestBody Map<String, String> request,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            String tableName = request.get("tableName");
            String columnName = request.get("columnName");
            String columnComment = request.get("columnComment");
            if (tableName == null || tableName.isEmpty() || columnName == null || columnName.isEmpty()) {
                throw new RuntimeException("表名和列名不能为空");
            }
            configService.updateColumnComment(userId, id, tableName, columnName, columnComment);
            response.put("success", true);
            response.put("message", "列注释已更新");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ========== 查询模板相关 ==========

    /**
     * 获取所有查询模板（支持可选分页：不传page返回全量列表，传page返回分页数据）
     */
    @GetMapping("/query-templates")
    public ResponseEntity<?> getAllQueryTemplates(
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "5") int size,
            HttpSession session) {
        String userId = getUserId(session);
        if (page != null) {
            Map<String, Object> pageData = configService.getQueryTemplatesByUserIdPaged(userId, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", pageData.get("content"));
            response.put("totalElements", pageData.get("totalElements"));
            response.put("totalPages", pageData.get("totalPages"));
            response.put("currentPage", pageData.get("currentPage"));
            response.put("pageSize", pageData.get("pageSize"));
            return ResponseEntity.ok(response);
        } else {
            List<QueryTemplate> templates = configService.getQueryTemplatesByUserId(userId);
            return ResponseEntity.ok(templates);
        }
    }

    /**
     * 批量导入查询模板（MD文件）
     */
    @PostMapping("/query-templates/import")
    public ResponseEntity<Map<String, Object>> importQueryTemplates(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            if (file.isEmpty()) {
                throw new RuntimeException("上传文件为空");
            }
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            Map<String, Object> result = configService.importTemplatesFromMarkdown(userId, content);
            response.put("success", true);
            response.put("message", "导入完成：成功" + result.get("success") + "条，失败" + result.get("failed") + "条");
            response.putAll(result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取单个查询模板
     */
    @GetMapping("/query-templates/{id}")
    public ResponseEntity<QueryTemplate> getQueryTemplate(@PathVariable String id, HttpSession session) {
        try {
            String userId = getUserId(session);
            QueryTemplate template = configService.getQueryTemplate(userId, id);
            return ResponseEntity.ok(template);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 保存查询模板
     */
    @PostMapping("/query-templates")
    public ResponseEntity<Map<String, Object>> saveQueryTemplate(@RequestBody QueryTemplate template, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            QueryTemplate saved = configService.saveQueryTemplate(userId, template);
            response.put("success", true);
            response.put("message", "保存成功");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新查询模板
     */
    @PutMapping("/query-templates/{id}")
    public ResponseEntity<Map<String, Object>> updateQueryTemplate(@PathVariable String id, @RequestBody QueryTemplate template, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            template.setId(id);
            QueryTemplate updated = configService.saveQueryTemplate(userId, template);
            response.put("success", true);
            response.put("message", "更新成功");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除查询模板
     */
    @DeleteMapping("/query-templates/{id}")
    public ResponseEntity<Map<String, Object>> deleteQueryTemplate(@PathVariable String id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            configService.deleteQueryTemplate(userId, id);
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
