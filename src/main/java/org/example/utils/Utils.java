package org.example.utils;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;

public class Utils {

    public static QuestionAnswerAdvisor buildQuestionAnswerAdvisor(VectorStore vectorStore, SearchRequest searchRequest, Resource promptTemplateResource){
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .promptTemplate(new PromptTemplate(promptTemplateResource))
                .build();
    }

    public static SearchRequest buildSearchRequest(Double threshold, int topK){
        return SearchRequest.builder().similarityThreshold(threshold).topK(topK).build();
    }
}

