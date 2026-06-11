package cn.stackssh.api.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工具配置 DTO（MCPs + Skills）- 请求和响应复用
 */
@Data
public class ToolsConfigDTO {

    private List<McpConfig> mcpList;
    private List<SkillsConfig> skillsList;

    @Data
    public static class McpConfig {
        /** 类型：local | sse | stdio */
        private String type;
        /** 通用名称 */
        private String name;
        /** SSE: 服务地址 */
        private String baseUri;
        /** SSE: SSE 端点路径 */
        private String sseEndpoint;
        /** SSE/Stdio: 超时毫秒 */
        private Integer requestTimeout;
        /** Stdio: 命令 */
        private String command;
        /** Stdio: 参数列表 */
        private List<String> args;
        /** Stdio: 环境变量 */
        private Map<String, String> env;
    }

    @Data
    public static class SkillsConfig {
        /** 类型：resource | directory */
        private String type;
        /** 路径 */
        private String path;
    }
}
