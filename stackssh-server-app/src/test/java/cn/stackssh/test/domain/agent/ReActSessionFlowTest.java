package cn.stackssh.test.domain.agent;

import cn.stackssh.api.dto.ChatRequestDTO;
import cn.stackssh.api.dto.ReActResultDTO;
import cn.stackssh.cases.react.factory.DefaultReActFactory;
import cn.stackssh.cases.react.node.AiCallNode;
import cn.stackssh.cases.react.node.LoopDecisionNode;
import cn.stackssh.cases.react.node.RootNode;
import cn.stackssh.cases.react.node.ToolCallNode;
import cn.stackssh.cases.react.node.UserFeedbackNode;
import cn.stackssh.cases.react.service.IConversationContextAssembler;
import cn.stackssh.domain.agent.adapter.repository.IChatHistoryRepository;
import cn.stackssh.domain.agent.adapter.repository.IConversationStateRepository;
import cn.stackssh.domain.agent.model.entity.ChatMessageEntity;
import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;
import cn.stackssh.domain.agent.model.entity.RoundResultEntity;
import cn.stackssh.domain.agent.model.valobj.AiAgentRegisterVO;
import cn.stackssh.domain.agent.model.valobj.context.PromptEnvelopeVO;
import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;
import cn.stackssh.domain.agent.service.IChatContextService;
import cn.stackssh.domain.agent.service.IConversationRoundService;
import cn.stackssh.domain.agent.service.armory.factory.DefaultArmoryFactory;
import cn.stackssh.domain.agent.service.armory.matter.tools.SshExecuteAdkTool;
import com.google.adk.events.Event;
import com.google.adk.events.EventActions;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.BaseSessionService;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReActSessionFlowTest {

    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private IChatHistoryRepository chatHistoryRepository;
    @Mock
    private IConversationStateRepository conversationStateRepository;
    @Mock
    private DefaultArmoryFactory defaultArmoryFactory;
    @Mock
    private IConversationContextAssembler conversationContextAssembler;
    @Mock
    private IConversationRoundService conversationRoundService;
    @Mock
    private IChatContextService chatContextService;
    @Mock
    private SshExecuteAdkTool sshExecuteAdkTool;
    @Mock
    private Runner runner;
    @Mock
    private BaseSessionService sessionService;

    private RootNode rootNode;

    @Before
    public void setUp() {
        rootNode = new RootNode();
        AiCallNode aiCallNode = new AiCallNode();
        ToolCallNode toolCallNode = new ToolCallNode();
        LoopDecisionNode loopDecisionNode = new LoopDecisionNode();
        UserFeedbackNode userFeedbackNode = new UserFeedbackNode();

        wireNode(rootNode);
        wireNode(aiCallNode);
        wireNode(toolCallNode);
        wireNode(loopDecisionNode);
        wireNode(userFeedbackNode);

        ReflectionTestUtils.setField(rootNode, "chatHistoryRepository", chatHistoryRepository);
        ReflectionTestUtils.setField(rootNode, "conversationStateRepository", conversationStateRepository);

        ReflectionTestUtils.setField(aiCallNode, "defaultArmoryFactory", defaultArmoryFactory);
        ReflectionTestUtils.setField(aiCallNode, "conversationContextAssembler", conversationContextAssembler);
        ReflectionTestUtils.setField(aiCallNode, "conversationRoundService", conversationRoundService);

        ReflectionTestUtils.setField(toolCallNode, "sshExecuteAdkTool", sshExecuteAdkTool);
        ReflectionTestUtils.setField(toolCallNode, "chatContextService", chatContextService);
        ReflectionTestUtils.setField(toolCallNode, "conversationRoundService", conversationRoundService);

        when(applicationContext.getBean("reactAiCallNode", Object.class)).thenReturn(aiCallNode);
        when(applicationContext.getBean("reactToolCallNode", Object.class)).thenReturn(toolCallNode);
        when(applicationContext.getBean("reactLoopDecisionNode", Object.class)).thenReturn(loopDecisionNode);
        when(applicationContext.getBean("reactUserFeedbackNode", Object.class)).thenReturn(userFeedbackNode);

        when(defaultArmoryFactory.getAiAgentRegisterVO("a1")).thenReturn(AiAgentRegisterVO.builder()
                .appName("app")
                .agentId("a1")
                .runner(runner)
                .build());
        when(runner.sessionService()).thenReturn(sessionService);
        when(sessionService.getSession("app", "u1", "s1", null))
                .thenReturn(Maybe.just(Session.builder("s1").appName("app").userId("u1").build()));
    }

    @Test
    public void shouldFinishConversationOnFirstRound() throws Exception {
        ChatRequestDTO requestDTO = new ChatRequestDTO();
        requestDTO.setSessionId("s1");
        requestDTO.setUserId("u1");
        requestDTO.setAgentId("a1");
        requestDTO.setMessage("check disk usage");
        requestDTO.setTerminalSessionId("t-cleanup");

        when(conversationStateRepository.findBySessionId("s1")).thenReturn(null);
        when(conversationContextAssembler.assemble(eq(requestDTO), any(DefaultReActFactory.DynamicContext.class)))
                .thenReturn(buildEnvelope("check disk usage", "final prompt", "CHAT", List.of(), List.of()));
        when(runner.runAsync(eq("u1"), eq("s1"), any(Content.class), any()))
                .thenReturn(Flowable.just(eventWithText("finish(message=\"disk looks healthy\")")));

        ReActResultDTO result = rootNode.apply(requestDTO, DefaultReActFactory.DynamicContext.builder()
                .emitter(new ResponseBodyEmitter(1000L))
                .build());

        Assert.assertEquals("finish", result.getStopReason());
        Assert.assertEquals(1, result.getTotalSteps());
        Assert.assertEquals(0, result.getTotalToolCalls());
        Assert.assertTrue(result.getContent().contains("finish(message=\"disk looks healthy\")"));
        Assert.assertNull(SshExecuteAdkTool.getTerminalSession("s1"));

        verify(conversationStateRepository).upsert(any(ConversationStateEntity.class));
        verify(conversationRoundService).commitUserMessage("s1", "check disk usage", 8);
        verify(conversationRoundService).commitAssistantMessage("s1", "finish(message=\"disk looks healthy\")", 18);
        verify(conversationRoundService, never()).commitToolResult(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());

        ArgumentCaptor<RoundResultEntity> roundCaptor = ArgumentCaptor.forClass(RoundResultEntity.class);
        verify(conversationRoundService).finishRound(roundCaptor.capture());
        Assert.assertEquals("check disk usage", roundCaptor.getValue().getRawUserMessage());
        Assert.assertTrue(roundCaptor.getValue().isSuccess());
    }

    @Test
    public void shouldRestoreHistoryAndPersistToolDeltaRound() throws Exception {
        ChatRequestDTO requestDTO = new ChatRequestDTO();
        requestDTO.setSessionId("s1");
        requestDTO.setUserId("u1");
        requestDTO.setAgentId("a1");

        when(conversationStateRepository.findBySessionId("s1")).thenReturn(ConversationStateEntity.builder()
                .sessionId("s1")
                .userId("u1")
                .agentId("a1")
                .turnCount(2)
                .currentIntent("EXECUTE")
                .build());
        when(chatHistoryRepository.getRecentMessages("s1", 20)).thenReturn(List.of(
                ChatMessageEntity.builder().role("assistant").content("previous answer").build(),
                ChatMessageEntity.builder().role("user").content("run ls").build()
        ));
        when(conversationContextAssembler.assemble(eq(requestDTO), any(DefaultReActFactory.DynamicContext.class)))
                .thenReturn(buildEnvelope("run ls", "prompt for execute", "EXECUTE",
                        List.of(Map.of("role", "user", "content", "run ls")),
                        List.of("rag-1")));
        when(runner.runAsync(eq("u1"), eq("s1"), any(Content.class), any()))
                .thenReturn(Flowable.just(eventWithTextAndState("listing complete", Map.of("ssh_result", "total 8"))));

        ReActResultDTO result = rootNode.apply(requestDTO, DefaultReActFactory.DynamicContext.builder()
                .emitter(new ResponseBodyEmitter(1000L))
                .build());

        Assert.assertEquals("completed", result.getStopReason());
        Assert.assertEquals(1, result.getTotalSteps());
        Assert.assertEquals(1, result.getTotalToolCalls());

        verify(chatHistoryRepository).getRecentMessages("s1", 20);
        verify(conversationRoundService, never()).commitUserMessage(anyString(), anyString(), anyInt());
        ArgumentCaptor<String> toolCallIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(conversationRoundService).commitToolResult(eq("s1"), eq("executeCommand"), toolCallIdCaptor.capture(), eq("total 8"), eq("success"), eq(3));
        Assert.assertTrue(toolCallIdCaptor.getValue().startsWith("call_ssh_result_"));

        ArgumentCaptor<RoundResultEntity> roundCaptor = ArgumentCaptor.forClass(RoundResultEntity.class);
        verify(conversationRoundService).finishRound(roundCaptor.capture());
        RoundResultEntity roundResult = roundCaptor.getValue();
        Assert.assertEquals("run ls", roundResult.getRawUserMessage());
        Assert.assertEquals(1, roundResult.getToolExecutions().size());
        Assert.assertEquals("executeCommand", roundResult.getToolExecutions().get(0).getToolName());
        Assert.assertEquals("rag-1", roundResult.getRagChunks().get(0));
    }

    private void wireNode(Object node) {
        ReflectionTestUtils.setField(node, "applicationContext", applicationContext);
    }

    private PromptEnvelopeVO buildEnvelope(String rawUserMessage,
                                           String finalUserMessage,
                                           String currentIntent,
                                           List<Map<String, Object>> history,
                                           List<String> ragChunks) {
        return PromptEnvelopeVO.builder()
                .sessionId("s1")
                .userId("u1")
                .terminalSessionId("t1")
                .rawUserMessage(rawUserMessage)
                .finalUserMessage(finalUserMessage)
                .conversationState(ConversationStateEntity.builder()
                        .sessionId("s1")
                        .userId("u1")
                        .agentId("a1")
                        .currentIntent(currentIntent)
                        .build())
                .searchContext(SearchContext.builder().build())
                .ragChunks(ragChunks)
                .trimmedHistory(history)
                .estimatedTokens(rawUserMessage.length() / 2)
                .build();
    }

    private Event eventWithText(String text) {
        return eventWithTextAndState(text, Map.of());
    }

    private Event eventWithTextAndState(String text, Map<String, Object> stateDelta) {
        return Event.builder()
                .author("model")
                .content(Content.fromParts(Part.fromText(text)))
                .actions(EventActions.builder().stateDelta(stateDelta).build())
                .build();
    }
}
