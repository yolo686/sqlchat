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
     * 自然语言转SQL
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
}
