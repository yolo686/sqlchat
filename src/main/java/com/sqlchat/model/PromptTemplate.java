package com.sqlchat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提示词模板
 * @author sqlChat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {
    private String id;
    private String name;
    private String description;
    private String template;
    private Boolean isDefault;
}
