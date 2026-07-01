package cn.stackssh.test.domain.agent;

import cn.stackssh.domain.agent.adapter.repository.IChatHistoryRepository;
import cn.stackssh.domain.agent.adapter.repository.IConversationRoundTraceRepository;
import cn.stackssh.domain.agent.model.entity.ConversationRoundTraceEntity;
import cn.stackssh.domain.agent.adapter.repository.IConversationStateRepository;
import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;
import cn.stackssh.domain.agent.model.entity.RoundResultEntity;
import cn.stackssh.domain.agent.service.IPromptService;
import cn.stackssh.domain.agent.service.IToolSummaryService;
import cn.stackssh.domain.agent.service.chat.ConversationRoundService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConversationRoundServiceTest {

    @Mock
    private IChatHistoryRepository chatHistoryRepository;
    @Mock
    private IConversationStateRepository conversationStateRepository;
    @Mock
    private IConversationRoundTraceRepository conversationRoundTraceRepository;
    @Mock
    private IToolSummaryService toolSummaryService;
    @Mock
    private IPromptService promptService;

    @Test
    public void shouldCompactIntentSummaryAndPersistCaches() {
        ConversationRoundService service = new ConversationRoundService();
        ReflectionTestUtils.setField(service, "chatHistoryRepository", chatHistoryRepository);
        ReflectionTestUtils.setField(service, "conversationStateRepository", conversationStateRepository);
        ReflectionTestUtils.setField(service, "conversationRoundTraceRepository", conversationRoundTraceRepository);
        ReflectionTestUtils.setField(service, "toolSummaryService", toolSummaryService);
        ReflectionTestUtils.setField(service, "promptService", promptService);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        when(conversationStateRepository.findBySessionId("s1")).thenReturn(ConversationStateEntity.builder()
                .sessionId("s1")
                .userId("u1")
                .agentId("a1")
                .turnCount(2)
                .intentSummary("CHAT | EXECUTE | CHAT | SEARCH")
                .taskSummary("task")
                .build());
        when(toolSummaryService.buildSummary("s1")).thenReturn("tool-summary");

        service.finishRound(RoundResultEntity.builder()
                .sessionId("s1")
                .userId("u1")
                .agentId("a1")
                .terminalSessionId("t1")
                .rawUserMessage("show me nginx config")
                .assistantMessage("ok")
                .currentIntent("EXECUTE")
                .enhancementCacheHit(Boolean.FALSE)
                .enhancementCacheReason("intent_changed")
                .ragChunks(List.of("chunk-1"))
                .success(true)
                .totalSteps(1)
                .build());

        ArgumentCaptor<ConversationStateEntity> captor = ArgumentCaptor.forClass(ConversationStateEntity.class);
        verify(conversationStateRepository).upsert(captor.capture());
        ConversationStateEntity persisted = captor.getValue();

        Assert.assertEquals("EXECUTE | CHAT | SEARCH", persisted.getIntentSummary());
        Assert.assertEquals("tool-summary", persisted.getToolSummary());
        Assert.assertNotNull(persisted.getLastUserMessageDigest());
        Assert.assertEquals("[\"chunk-1\"]", persisted.getCachedRagChunks());
        Assert.assertEquals(Boolean.FALSE, persisted.getLastEnhancementCacheHit());
        Assert.assertEquals("intent_changed", persisted.getLastEnhancementCacheReason());

        ArgumentCaptor<ConversationRoundTraceEntity> traceCaptor = ArgumentCaptor.forClass(ConversationRoundTraceEntity.class);
        verify(conversationRoundTraceRepository).save(traceCaptor.capture());
        Assert.assertEquals(Integer.valueOf(3), traceCaptor.getValue().getTurnNumber());
        Assert.assertEquals(Boolean.FALSE, traceCaptor.getValue().getEnhancementCacheHit());
        Assert.assertEquals("intent_changed", traceCaptor.getValue().getEnhancementCacheReason());
    }
}
