package cn.stackssh.domain.agent.service;

import cn.stackssh.domain.agent.model.entity.RoundResultEntity;

public interface IConversationRoundService {

    void commitUserMessage(
            String sessionId,
            String rawUserMessage,
            int estimatedTokens
    );

    void commitAssistantMessage(
            String sessionId,
            String assistantMessage,
            int estimatedTokens
    );

    void commitToolResult(
            String sessionId,
            String toolName,
            String toolCallId,
            String resultContent,
            String status,
            int estimatedTokens
    );

    void finishRound(RoundResultEntity roundResult);
}
