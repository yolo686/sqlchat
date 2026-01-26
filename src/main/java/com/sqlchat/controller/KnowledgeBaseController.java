package com.sqlchat.controller;

import com.sqlchat.model.KnowledgeBase;
import com.sqlchat.rag.impl.VectorRagService;
import com.sqlchat.service.KnowledgeBaseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库控制器
 * @author sqlChat
 */
@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private VectorRagService vectorRagService;

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

    /**
     * 获取用户的知识库（列表形式）
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getKnowledgeBases(
            @RequestParam String type,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            List<KnowledgeBase> knowledgeBases = 
                knowledgeBaseService.getKnowledgeBasesByUser(userId, type);
            response.put("success", true);
            response.put("data", knowledgeBases);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 保存单个知识库分片
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveKnowledgeBase(
            @RequestBody KnowledgeBase knowledgeBase,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            KnowledgeBase saved = knowledgeBaseService.saveKnowledgeBase(userId, knowledgeBase);
            // 清除缓存，使新向量生效
            vectorRagService.clearUserCache(userId);
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
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteKnowledgeBase(
            @PathVariable String id,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String userId = getUserId(session);
            knowledgeBaseService.deleteKnowledgeBase(userId, id);
            // 清除缓存
            vectorRagService.clearUserCache(userId);
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
