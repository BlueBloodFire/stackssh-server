package cn.stackssh.api.dto;

import lombok.Data;

/**
 * AI 模型配置更新请求
 */
@Data
public class ModelConfigRequestDTO {
    private String apiKey;
    private String baseUrl;
    private String model;
    private String completionsPath;
    private String embeddingsPath;
}
