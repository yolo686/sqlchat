package com.sqlchat.controller;

import com.sqlchat.model.*;
import com.sqlchat.service.Nl2SqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NL2SQL控制器
 * @author sqlChat
 */
@RestController
@RequestMapping("/api/nl2sql")
@CrossOrigin(origins = "*")
public class Nl2SqlController {

    @Autowired
    private Nl2SqlService nl2SqlService;

    /**
     * 自然语言转SQL
     */
    @PostMapping("/convert")
    public ResponseEntity<Nl2SqlResponse> convert(@RequestBody Nl2SqlRequest request) {
        Nl2SqlResponse response = nl2SqlService.convert(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有数据库配置
     */
    @GetMapping("/database-configs")
    public ResponseEntity<List<DatabaseConfig>> getAllDatabaseConfigs() {
        List<DatabaseConfig> configs = nl2SqlService.getAllDatabaseConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * 保存数据库配置
     */
    @PostMapping("/database-configs")
    public ResponseEntity<DatabaseConfig> saveDatabaseConfig(@RequestBody DatabaseConfig config) {
        nl2SqlService.saveDatabaseConfig(config);
        return ResponseEntity.ok(config);
    }

    /**
     * 删除数据库配置
     */
    @DeleteMapping("/database-configs/{id}")
    public ResponseEntity<Void> deleteDatabaseConfig(@PathVariable String id) {
        nl2SqlService.deleteDatabaseConfig(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取所有提示词模板
     */
    @GetMapping("/prompt-templates")
    public ResponseEntity<List<PromptTemplate>> getAllPromptTemplates() {
        List<PromptTemplate> templates = nl2SqlService.getAllPromptTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * 保存提示词模板
     */
    @PostMapping("/prompt-templates")
    public ResponseEntity<PromptTemplate> savePromptTemplate(@RequestBody PromptTemplate template) {
        nl2SqlService.savePromptTemplate(template);
        return ResponseEntity.ok(template);
    }

    /**
     * 删除提示词模板
     */
    @DeleteMapping("/prompt-templates/{id}")
    public ResponseEntity<Void> deletePromptTemplate(@PathVariable String id) {
        nl2SqlService.deletePromptTemplate(id);
        return ResponseEntity.ok().build();
    }
}
