package com.sqlchat.controller;

import com.sqlchat.model.DatabaseConfig;
import com.sqlchat.model.QueryTemplate;
import com.sqlchat.service.ConfigService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * 获取所有数据库配置
     */
    @GetMapping("/database-configs")
    public ResponseEntity<List<DatabaseConfig>> getAllDatabaseConfigs(HttpSession session) {
        String userId = getUserId(session);
        List<DatabaseConfig> configs = configService.getDatabaseConfigsByUserId(userId);
        return ResponseEntity.ok(configs);
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

    // ========== 查询模板相关 ==========

    /**
     * 获取所有查询模板
     */
    @GetMapping("/query-templates")
    public ResponseEntity<List<QueryTemplate>> getAllQueryTemplates(HttpSession session) {
        String userId = getUserId(session);
        List<QueryTemplate> templates = configService.getQueryTemplatesByUserId(userId);
        return ResponseEntity.ok(templates);
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
