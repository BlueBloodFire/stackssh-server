package cn.stackssh.trigger.http;

import cn.stackssh.api.IAgentService;
import cn.stackssh.api.dto.*;
import cn.stackssh.api.response.Response;
import cn.stackssh.cases.IAIAgentReActServiceCase;
import cn.stackssh.domain.agent.model.valobj.AiAgentConfigTableVO;
import cn.stackssh.domain.agent.service.IArmoryService;
import cn.stackssh.domain.agent.service.IChatService;
import cn.stackssh.domain.agent.service.RuntimeModelConfigService;
import cn.stackssh.types.enums.ResponseCode;
import cn.stackssh.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author wangjin
 * 2026/1/20 08:23
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/")
@CrossOrigin(origins = "*")
public class AgentServiceController implements IAgentService {

    @Resource
    private IChatService chatService;

    @Resource
    private IAIAgentReActServiceCase reactServiceCase;

    @Resource
    private RuntimeModelConfigService runtimeModelConfigService;

    @Resource
    private IArmoryService armoryService;

    @RequestMapping(value = "query_ai_agent_config_list", method = RequestMethod.GET)
    @Override
    public Response<List<AiAgentConfigResponseDTO>> queryAiAgentConfigList() {
        try {
            log.info("查询智能体配置列表");

            List<AiAgentConfigTableVO.Agent> agentConfigs = chatService.queryAiAgentConfigList();

            List<AiAgentConfigResponseDTO> responseDTOS = agentConfigs.stream().map(agentConfig -> {
                AiAgentConfigResponseDTO responseDTO = new AiAgentConfigResponseDTO();
                responseDTO.setAgentId(agentConfig.getAgentId());
                responseDTO.setAgentName(agentConfig.getAgentName());
                responseDTO.setAgentDesc(agentConfig.getAgentDesc());
                return responseDTO;
            }).collect(Collectors.toList());

            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTOS)
                    .build();

        } catch (AppException e) {
            log.error("查询智能体配置列表异常", e);
            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("查询智能体配置列表失败", e);
            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "create_session", method = RequestMethod.POST)
    @Override
    public Response<CreateSessionResponseDTO> createSession(@RequestBody CreateSessionRequestDTO requestDTO) {
        try {
            log.info("创建会话 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId());
            String sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());

            CreateSessionResponseDTO responseDTO = new CreateSessionResponseDTO();
            responseDTO.setSessionId(sessionId);

            return Response.<CreateSessionResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            log.error("查询智能体配置列表异常", e);
            return Response.<CreateSessionResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("创建会话失败 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId(), e);
            return Response.<CreateSessionResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "create_session", method = RequestMethod.GET)
    public Response<CreateSessionResponseDTO> createSession(@RequestParam("agentId") String agentId, @RequestParam("userId") String userId) {
        CreateSessionRequestDTO requestDTO = new CreateSessionRequestDTO();
        requestDTO.setAgentId(agentId);
        requestDTO.setUserId(userId);
        return createSession(requestDTO);
    }

    @RequestMapping(value = "chat", method = RequestMethod.POST)
    @Override
    public Response<ChatResponseDTO> chat(@RequestBody ChatRequestDTO requestDTO) {
        try {
            log.info("智能体对话 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId());
            String sessionId = requestDTO.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());
            }

            List<String> messages = chatService.handleMessage(requestDTO.getAgentId(), requestDTO.getUserId(), sessionId, requestDTO.getMessage());

            ChatResponseDTO responseDTO = new ChatResponseDTO();
            responseDTO.setContent(String.join("\n", messages));

            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            log.error("智能体对话异常", e);
            return Response.<ChatResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("智能体对话败 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId(), e);
            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    // ============================================================
    // 模型配置端点
    // ============================================================

    @RequestMapping(value = "agent-config/model/{agentId}", method = RequestMethod.GET)
    public Response<ModelConfigResponseDTO> getModelConfig(@PathVariable("agentId") String agentId) {
        try {
            AiAgentConfigTableVO config = runtimeModelConfigService.getConfig(agentId);
            if (config == null) {
                return Response.<ModelConfigResponseDTO>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("Agent 配置不存在: " + agentId)
                        .build();
            }
            AiAgentConfigTableVO.Module.AiApi aiApi = config.getModule().getAiApi();
            AiAgentConfigTableVO.Module.ChatModel chatModel = config.getModule().getChatModel();

            ModelConfigResponseDTO dto = new ModelConfigResponseDTO();
            dto.setBaseUrl(aiApi.getBaseUrl());
            dto.setModel(chatModel.getModel());
            dto.setCompletionsPath(aiApi.getCompletionsPath());
            dto.setEmbeddingsPath(aiApi.getEmbeddingsPath());
            // API Key 脱敏：保留前4位和后4位
            String apiKey = aiApi.getApiKey();
            if (apiKey != null && apiKey.length() > 8) {
                dto.setApiKey(apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4));
            } else {
                dto.setApiKey("****");
            }
            return Response.<ModelConfigResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(dto)
                    .build();
        } catch (Exception e) {
            log.error("获取模型配置失败 agentId={}", agentId, e);
            return Response.<ModelConfigResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "agent-config/model/{agentId}", method = RequestMethod.POST)
    public Response<Void> updateModelConfig(@PathVariable("agentId") String agentId, @RequestBody ModelConfigRequestDTO request) {
        try {
            log.info("更新模型配置 agentId={} model={} baseUrl={}", agentId, request.getModel(), request.getBaseUrl());
            AiAgentConfigTableVO config = runtimeModelConfigService.getConfig(agentId);
            if (config == null) {
                return Response.<Void>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("Agent 配置不存在: " + agentId)
                        .build();
            }
            AiAgentConfigTableVO.Module.AiApi aiApi = config.getModule().getAiApi();
            AiAgentConfigTableVO.Module.ChatModel chatModel = config.getModule().getChatModel();

            // 只更新非空字段
            if (StringUtils.hasText(request.getApiKey()) && !request.getApiKey().contains("****")) {
                aiApi.setApiKey(request.getApiKey());
            }
            if (StringUtils.hasText(request.getBaseUrl())) {
                aiApi.setBaseUrl(request.getBaseUrl());
            }
            if (StringUtils.hasText(request.getModel())) {
                chatModel.setModel(request.getModel());
            }
            if (StringUtils.hasText(request.getCompletionsPath())) {
                aiApi.setCompletionsPath(request.getCompletionsPath());
            }
            if (StringUtils.hasText(request.getEmbeddingsPath())) {
                aiApi.setEmbeddingsPath(request.getEmbeddingsPath());
            }

            // 重新装配 Agent
            armoryService.acceptArmoryAgents(List.of(config));
            runtimeModelConfigService.registerConfig(agentId, config);

            return Response.<Void>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("模型配置更新成功")
                    .build();
        } catch (Exception e) {
            log.error("更新模型配置失败 agentId={}", agentId, e);
            return Response.<Void>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    // ============================================================
    // 工具配置端点（MCPs + Skills）
    // ============================================================

    @RequestMapping(value = "agent-config/tools/{agentId}", method = RequestMethod.GET)
    public Response<ToolsConfigDTO> getToolsConfig(@PathVariable("agentId") String agentId) {
        try {
            AiAgentConfigTableVO config = runtimeModelConfigService.getConfig(agentId);
            if (config == null) {
                return Response.<ToolsConfigDTO>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("Agent 配置不存在: " + agentId)
                        .build();
            }
            AiAgentConfigTableVO.Module.ChatModel chatModel = config.getModule().getChatModel();
            ToolsConfigDTO dto = new ToolsConfigDTO();

            // 转换 MCP 列表
            if (chatModel.getToolMcpList() != null) {
                List<ToolsConfigDTO.McpConfig> mcpList = new ArrayList<>();
                for (AiAgentConfigTableVO.Module.ChatModel.ToolMcp mcp : chatModel.getToolMcpList()) {
                    ToolsConfigDTO.McpConfig mcpDto = new ToolsConfigDTO.McpConfig();
                    if (mcp.getLocal() != null) {
                        mcpDto.setType("local");
                        mcpDto.setName(mcp.getLocal().getName());
                    } else if (mcp.getSse() != null) {
                        mcpDto.setType("sse");
                        mcpDto.setName(mcp.getSse().getName());
                        mcpDto.setBaseUri(mcp.getSse().getBaseUri());
                        mcpDto.setSseEndpoint(mcp.getSse().getSseEndpoint());
                        mcpDto.setRequestTimeout(mcp.getSse().getRequestTimeout());
                    } else if (mcp.getStdio() != null) {
                        mcpDto.setType("stdio");
                        mcpDto.setName(mcp.getStdio().getName());
                        mcpDto.setRequestTimeout(mcp.getStdio().getRequestTimeout());
                        if (mcp.getStdio().getServerParameters() != null) {
                            mcpDto.setCommand(mcp.getStdio().getServerParameters().getCommand());
                            mcpDto.setArgs(mcp.getStdio().getServerParameters().getArgs());
                            mcpDto.setEnv(mcp.getStdio().getServerParameters().getEnv());
                        }
                    }
                    mcpList.add(mcpDto);
                }
                dto.setMcpList(mcpList);
            } else {
                dto.setMcpList(new ArrayList<>());
            }

            // 转换 Skills 列表
            if (chatModel.getToolSkillsList() != null) {
                List<ToolsConfigDTO.SkillsConfig> skillsList = new ArrayList<>();
                for (AiAgentConfigTableVO.Module.ChatModel.ToolSkills skills : chatModel.getToolSkillsList()) {
                    ToolsConfigDTO.SkillsConfig skillsDto = new ToolsConfigDTO.SkillsConfig();
                    skillsDto.setType(skills.getType());
                    skillsDto.setPath(skills.getPath());
                    skillsList.add(skillsDto);
                }
                dto.setSkillsList(skillsList);
            } else {
                dto.setSkillsList(new ArrayList<>());
            }

            return Response.<ToolsConfigDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(dto)
                    .build();
        } catch (Exception e) {
            log.error("获取工具配置失败 agentId={}", agentId, e);
            return Response.<ToolsConfigDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "agent-config/tools/{agentId}", method = RequestMethod.POST)
    public Response<Void> updateToolsConfig(@PathVariable("agentId") String agentId, @RequestBody ToolsConfigDTO request) {
        try {
            log.info("更新工具配置 agentId={} mcpCount={} skillsCount={}",
                    agentId,
                    request.getMcpList() != null ? request.getMcpList().size() : 0,
                    request.getSkillsList() != null ? request.getSkillsList().size() : 0);
            AiAgentConfigTableVO config = runtimeModelConfigService.getConfig(agentId);
            if (config == null) {
                return Response.<Void>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("Agent 配置不存在: " + agentId)
                        .build();
            }
            AiAgentConfigTableVO.Module.ChatModel chatModel = config.getModule().getChatModel();

            // 转换并更新 MCP 列表
            if (request.getMcpList() != null) {
                List<AiAgentConfigTableVO.Module.ChatModel.ToolMcp> mcpList = new ArrayList<>();
                for (ToolsConfigDTO.McpConfig mcpDto : request.getMcpList()) {
                    AiAgentConfigTableVO.Module.ChatModel.ToolMcp mcp = new AiAgentConfigTableVO.Module.ChatModel.ToolMcp();
                    if ("local".equals(mcpDto.getType())) {
                        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.LocalParameters local = new AiAgentConfigTableVO.Module.ChatModel.ToolMcp.LocalParameters();
                        local.setName(mcpDto.getName());
                        mcp.setLocal(local);
                    } else if ("sse".equals(mcpDto.getType())) {
                        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters sse = new AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters();
                        sse.setName(mcpDto.getName());
                        sse.setBaseUri(mcpDto.getBaseUri());
                        sse.setSseEndpoint(mcpDto.getSseEndpoint());
                        if (mcpDto.getRequestTimeout() != null) sse.setRequestTimeout(mcpDto.getRequestTimeout());
                        mcp.setSse(sse);
                    } else if ("stdio".equals(mcpDto.getType())) {
                        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters stdio = new AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters();
                        stdio.setName(mcpDto.getName());
                        if (mcpDto.getRequestTimeout() != null) stdio.setRequestTimeout(mcpDto.getRequestTimeout());
                        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters.ServerParameters sp = new AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters.ServerParameters();
                        sp.setCommand(mcpDto.getCommand());
                        sp.setArgs(mcpDto.getArgs());
                        sp.setEnv(mcpDto.getEnv());
                        stdio.setServerParameters(sp);
                        mcp.setStdio(stdio);
                    }
                    mcpList.add(mcp);
                }
                chatModel.setToolMcpList(mcpList);
            }

            // 转换并更新 Skills 列表
            if (request.getSkillsList() != null) {
                List<AiAgentConfigTableVO.Module.ChatModel.ToolSkills> skillsList = new ArrayList<>();
                for (ToolsConfigDTO.SkillsConfig skillsDto : request.getSkillsList()) {
                    AiAgentConfigTableVO.Module.ChatModel.ToolSkills skills = new AiAgentConfigTableVO.Module.ChatModel.ToolSkills();
                    skills.setType(skillsDto.getType());
                    skills.setPath(skillsDto.getPath());
                    skillsList.add(skills);
                }
                chatModel.setToolSkillsList(skillsList);
            }

            // 重新装配 Agent
            armoryService.acceptArmoryAgents(List.of(config));
            runtimeModelConfigService.registerConfig(agentId, config);

            return Response.<Void>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("工具配置更新成功")
                    .build();
        } catch (Exception e) {
            log.error("更新工具配置失败 agentId={}", agentId, e);
            return Response.<Void>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "chat_stream", method = RequestMethod.POST)
    @Override
    public ResponseBodyEmitter chatStream(@RequestBody ChatRequestDTO requestDTO) {
        try {
            log.info("ReAct 流式对话 agentId:{} userId:{} sessionId:{} terminalSessionId:{} message:{}",
                    requestDTO.getAgentId(), requestDTO.getUserId(), requestDTO.getSessionId(),
                    requestDTO.getTerminalSessionId(), requestDTO.getMessage());

            // 如果未指定 sessionId，先创建
            String sessionId = requestDTO.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());
                requestDTO.setSessionId(sessionId);
            }

            // 路由到 ReAct 服务（Case 层）
            return reactServiceCase.chatStream(requestDTO);
        } catch (Exception e) {
            log.error("ReAct 流式对话失败", e);
            ResponseBodyEmitter emitter = new ResponseBodyEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }

}
