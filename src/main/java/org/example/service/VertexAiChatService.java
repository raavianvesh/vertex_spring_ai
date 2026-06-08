package org.example.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.example.utils.Utils;

@Service
public class VertexAiChatService implements ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("${app.prompt.system.prompt}")
    private Resource systemPromptResource;

    public VertexAiChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    @Override
    public String chat(String prompt) {
        return this.chatClient.prompt()
                .user(prompt)
                .advisors(Utils.buildQuestionAnswerAdvisor(
                        this.vectorStore,
                        Utils.buildSearchRequest(0.1d, 1),
                        this.systemPromptResource))
                .call()
                .content();
    }
}
