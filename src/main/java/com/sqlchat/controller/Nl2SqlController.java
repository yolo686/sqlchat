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
//@CrossOrigin(allowedOriginPatterns = "*", allowCredentials = "true")
public class Nl2SqlController {

    @Autowired
    private Nl2SqlService nl2SqlService;

    /**
     * 自然语言转SQL（前端调用，需要Session登录）
     */
    @PostMapping("/convert")
    public ResponseEntity<Nl2SqlResponse> convert(@RequestBody Nl2SqlRequest request, jakarta.servlet.http.HttpSession session) {
        // 从Session获取用户ID
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            Nl2SqlResponse errorResponse = new Nl2SqlResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("请先登录");
            return ResponseEntity.status(401).body(errorResponse);
        }
        request.setUserId(userId);
        
        Nl2SqlResponse response = nl2SqlService.convert(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 评测专用NL2SQL接口（独立于前端，无需Session认证）
     * 直接传递数据库连接参数，脚本通过此接口调用消融实验
     */
    @PostMapping("/eval")
    public ResponseEntity<Nl2SqlResponse> evalConvert(@RequestBody Nl2SqlEvalRequest request) {
        Nl2SqlResponse response = nl2SqlService.evalConvert(request);
        return ResponseEntity.ok(response);
    }
}
