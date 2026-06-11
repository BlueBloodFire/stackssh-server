package cn.stackssh.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class VectorStoreConfig {

    @Value("${knowledge.embedding.base-url}")
    private String baseUrl;

    @Value("${knowledge.embedding.api-key}")
    private String apiKey;

    @Value("${knowledge.embedding.model}")
    private String model;

    @Bean("knowledgeEmbeddingModel")
    public EmbeddingModel knowledgeEmbeddingModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        return new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder().model(model).build()
        );
    }

}
