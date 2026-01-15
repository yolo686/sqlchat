package com.sqlchat.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * @author 33954
 * #Description ChatServiceInterface
 * #Date: 2026/1/2 18:48
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel"
)
public interface ChatService {

    @SystemMessage("You are a polite assistant")
    String chat(String userMessage);
}
