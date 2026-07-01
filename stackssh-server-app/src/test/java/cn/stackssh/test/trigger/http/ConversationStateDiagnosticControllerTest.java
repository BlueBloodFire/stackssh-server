package cn.stackssh.test.trigger.http;

import cn.stackssh.api.dto.ConversationStateDiagnosticResponseDTO;
import cn.stackssh.api.dto.ConversationRoundTraceResponseDTO;
import cn.stackssh.api.response.Response;
import cn.stackssh.domain.agent.adapter.repository.IConversationRoundTraceRepository;
import cn.stackssh.domain.agent.model.entity.ConversationRoundTraceEntity;
import cn.stackssh.domain.agent.adapter.repository.IConversationStateRepository;
import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;
import cn.stackssh.trigger.http.ConversationStateDiagnosticController;
import cn.stackssh.types.enums.ResponseCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConversationStateDiagnosticControllerTest {

    @Mock
    private IConversationStateRepository conversationStateRepository;
    @Mock
    private IConversationRoundTraceRepository conversationRoundTraceRepository;

    @Test
    public void shouldBuildDiagnosticResponse() throws Exception {
        ConversationStateDiagnosticController controller = new ConversationStateDiagnosticController();
        ReflectionTestUtils.setField(controller, "conversationStateRepository", conversationStateRepository);
        ReflectionTestUtils.setField(controller, "conversationRoundTraceRepository", conversationRoundTraceRepository);
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());

        when(conversationStateRepository.findBySessionId("s1")).thenReturn(ConversationStateEntity.builder()
                .sessionId("s1")
                .userId("u1")
                .agentId("a1")
                .currentIntent("EXECUTE")
                .taskSummary("repair nginx")
                .intentSummary("EXECUTE | CHAT")
                .toolSummary("executeCommand: ok")
                .turnCount(3)
                .contextVersion(1)
                .lastRoundAt(123456789L)
                .lastTerminalSessionId("t1")
                .lastUserMessageDigest("abc123")
                .lastEnhancementCacheHit(Boolean.FALSE)
                .lastEnhancementCacheReason("intent_changed")
                .cachedSearchContext("{\"serviceStatus\":{\"nginx\":\"active\"},\"fileContents\":{\"/etc/nginx/nginx.conf\":\"events {}\"},\"recentLogs\":{\"error.log\":\"ok\"}}")
                .cachedRagChunks(new ObjectMapper().writeValueAsString(List.of("chunk-1", "chunk-2")))
                .build());

        Response<ConversationStateDiagnosticResponseDTO> response = controller.queryDiagnostic("s1");

        Assert.assertEquals(ResponseCode.SUCCESS.getCode(), response.getCode());
        Assert.assertNotNull(response.getData());
        Assert.assertEquals("s1", response.getData().getSessionId());
        Assert.assertTrue(response.getData().isCachedSearchContextPresent());
        Assert.assertTrue(response.getData().isCachedRagChunksPresent());
        Assert.assertEquals(Boolean.FALSE, response.getData().getLastEnhancementCacheHit());
        Assert.assertEquals("intent_changed", response.getData().getLastEnhancementCacheReason());
        Assert.assertEquals(1, response.getData().getCachedServiceStatusCount());
        Assert.assertEquals(1, response.getData().getCachedFileContentCount());
        Assert.assertEquals(1, response.getData().getCachedRecentLogCount());
        Assert.assertEquals(2, response.getData().getCachedRagChunkCount());
    }

    @Test
    public void shouldBuildRoundTimelineResponse() {
        ConversationStateDiagnosticController controller = new ConversationStateDiagnosticController();
        ReflectionTestUtils.setField(controller, "conversationStateRepository", conversationStateRepository);
        ReflectionTestUtils.setField(controller, "conversationRoundTraceRepository", conversationRoundTraceRepository);
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());

        when(conversationRoundTraceRepository.queryRecentBySessionId("s1", 2)).thenReturn(List.of(
                ConversationRoundTraceEntity.builder()
                        .sessionId("s1")
                        .turnNumber(3)
                        .currentIntent("EXECUTE")
                        .enhancementCacheHit(Boolean.TRUE)
                        .enhancementCacheReason("matched_previous_round")
                        .rawUserMessage("run ls")
                        .assistantMessage("listing complete")
                        .toolSummary("executeCommand: total 8")
                        .success(Boolean.TRUE)
                        .totalSteps(1)
                        .createdAt(123L)
                        .build()
        ));

        Response<ConversationRoundTraceResponseDTO> response = controller.queryRoundTimeline("s1", 2);

        Assert.assertEquals(ResponseCode.SUCCESS.getCode(), response.getCode());
        Assert.assertEquals("s1", response.getData().getSessionId());
        Assert.assertEquals(1, response.getData().getItems().size());
        Assert.assertEquals(Integer.valueOf(3), response.getData().getItems().get(0).getTurnNumber());
        Assert.assertEquals("matched_previous_round", response.getData().getItems().get(0).getEnhancementCacheReason());
    }
}
