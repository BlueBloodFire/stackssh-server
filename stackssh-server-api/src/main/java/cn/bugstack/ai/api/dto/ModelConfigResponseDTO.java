package cn.bugstack.ai.api.dto;

import lombok.Data;

/**
 * AI 模型配置响应（apiKey 脱敏）
 */
@Data
public class ModelConfigResponseDTO {
    private String apiKey;
    private String baseUrl;
    private String model;
    private String completionsPath;
    private String embeddingsPath;
}
