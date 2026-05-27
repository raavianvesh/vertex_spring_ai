package org.example.controller;

import org.example.dto.ChatResponse;
import org.example.service.ChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

	private final ChatService chatService;
	private final String model;

	public ChatController(ChatService chatService,
							  @Value("${spring.ai.vertex.ai.gemini.chat.options.model:unknown}") String model) {
		this.chatService = chatService;
		this.model = model;
	}

	@QueryMapping
	public ChatResponse chat(@Argument String prompt) {
		String reply = chatService.chat(prompt);
		return new ChatResponse(model, prompt, reply);
	}
}
