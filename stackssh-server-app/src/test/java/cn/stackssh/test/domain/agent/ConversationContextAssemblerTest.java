package cn.stackssh.test.domain.agent;

import cn.stackssh.api.dto.ChatRequestDTO;
import cn.stackssh.cases.react.factory.DefaultReActFactory;
import cn.stackssh.cases.react.service.ConversationContextAssembler;
import cn.stackssh.domain.agent.adapter.repository.IChatHistoryRepository;
import cn.stackssh.domain.agent.adapter.repository.IConversationStateRepository;
import cn.stackssh.domain.agent.model.entity.ConversationStateEntity;
import cn.stackssh.domain.agent.model.valobj.context.ContextBudgetVO;
import cn.stackssh.domain.agent.model.valobj.context.PromptEnvelopeVO;
import cn.stackssh.domain.agent.model.valobj.enhance.SearchContext;
import cn.stackssh.domain.agent.model.valobj.intent.IntentResultVO;
import cn.stackssh.domain.agent.model.valobj.intent.IntentTypeEnumVO;
import cn.stackssh.domain.agent.model.valobj.prompt.PromptContextVO;
import cn.stackssh.domain.agent.service.IChatContextService;
import cn.stackssh.domain.agent.service.IContextBudgetService;
import cn.stackssh.domain.agent.service.IIntentEnhancerService;
import cn.stackssh.domain.agent.service.IIntentService;
import cn.stackssh.domain.agent.service.IPromptService;
import cn.stackssh.domain.knowledge.service.IKnowledgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConversationContextAssemblerTest {

    @Mock
    private IConversationStateRepository conversationStateRepository;
    @Mock
    private IIntentService intentService;
    @Mock
    private IIntentEnhancerService intentEnhancerService;
    @Mock
    private IKnowledgeService knowledgeService;
    @Mock
    private IContextBudgetService contextBudgetService;
    @Mock
    private IChatHistoryRepository chatHistoryRepository;
    @Mock
    private IChatContextService chatContextService;
    @Mock
    private IPromptService promptService;

    @Test
    public void shouldUseResolvedHistoryUserMessageAndAvoidSecondHistoryLoad() {
        ConversationContextAssembler assembler = new ConversationContextAssembler();
        ReflectionTestUtils.setField(assembler, "conversationStateRepository", conversationStateRepository);
        ReflectionTestUtils.setField(assembler, "intentService", intentService);
        ReflectionTestUtils.setField(assembler, "intentEnhancerService", intentEnhancerService);
        ReflectionTestUtils.setField(assembler, "knowledgeService", knowledgeService);
        ReflectionTestUtils.setField(assembler, "contextBudgetService", contextBudgetService);
        ReflectionTestUtils.setField(assembler, "chatHistoryRepository", chatHistoryRepository);
        ReflectionTestUtils.setField(assembler, "chatContextService", chatContextService);
        ReflectionTestUtils.setField(assembler, "promptService", promptService);
        ReflectionTestUtils.setField(assembler, "objectMapper", new ObjectMapper());

        ConversationStateEntity state = ConversationStateEntity.builder()
                .sessionId("s1")
                .userId("u1")
                .agentId("a1")
                .turnCount(1)
                .currentIntent(IntentTypeEnumVO.CHAT.name())
                .lastUserMessageDigest(ConversationContextAssembler.digestMessage("history-user-message"))
                .lastTerminalSessionId("t1")
                .cachedSearchContext(new ObjectMapper().valueToTree(SearchContext.builder().build()).toString())
                .cachedRagChunks("[]")
                .build();

        DefaultReActFactory.DynamicContext dynamicContext = DefaultReActFactory.DynamicContext.builder()
                .sessionId("s1")
                .userId("u1")
                .agentId("a1")
                .terminalSessionId("t1")
                .messageHistory(List.of(Map.of("role", "user", "content", "history-user-message")))
                .historyLoadedFromRepository(true)
                .build();

        ChatRequestDTO requestDTO = new ChatRequestDTO();
        requestDTO.setSessionId("s1");
        requestDTO.setUserId("u1");
        requestDTO.setAgentId("a1");
        requestDTO.setConnectionId("c1");

        when(conversationStateRepository.findBySessionId("s1")).thenReturn(state);
        when(intentService.classify("s1", "u1", "history-user-message"))
                .thenReturn(IntentResultVO.builder().intent(IntentTypeEnumVO.CHAT).build());
        when(contextBudgetService.allocate(anyString(), eq(state), org.mockito.ArgumentMatchers.any(SearchContext.class), anyList(), anyInt()))
                .thenReturn(ContextBudgetVO.builder().historyBudget(1024).build());
        when(chatContextService.buildPromptContext(eq("s1"), eq("u1"), eq("t1"), anyList(), eq(state),
                org.mockito.ArgumentMatchers.any(SearchContext.class), anyList()))
                .thenReturn(PromptContextVO.builder().build());
        when(promptService.buildFinalUserMessage(org.mockito.ArgumentMatchers.any(PromptEnvelopeVO.class),
                org.mockito.ArgumentMatchers.any(PromptContextVO.class)))
                .thenReturn("final-message");

        PromptEnvelopeVO envelope = assembler.assemble(requestDTO, dynamicContext);

        Assert.assertEquals("history-user-message", envelope.getRawUserMessage());
        Assert.assertEquals("final-message", envelope.getFinalUserMessage());
        Assert.assertEquals(Boolean.TRUE, envelope.getEnhancementCacheHit());
        Assert.assertEquals("matched_previous_round", envelope.getEnhancementCacheReason());
        Assert.assertEquals(Boolean.TRUE, envelope.getConversationState().getLastEnhancementCacheHit());
        Assert.assertEquals("matched_previous_round", envelope.getConversationState().getLastEnhancementCacheReason());
        verify(chatHistoryRepository, never()).getMessagesWithBudget(anyString(), anyInt());
        verify(intentEnhancerService, never()).enhance(anyString(), anyString());
        verify(knowledgeService, never()).searchRelevant(anyString(), anyString(), anyInt());
    }
}
