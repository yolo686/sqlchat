package com.sqlchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询模板
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryTemplate {
    private String id;
    private String name;
    private String description;
    private String queryExample; // 查询示例，选择模板后自动填充到用户查询框
    private Boolean isDefault;
}
