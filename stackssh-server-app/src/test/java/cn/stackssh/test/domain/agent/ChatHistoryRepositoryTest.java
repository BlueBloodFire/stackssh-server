package cn.stackssh.test.domain.agent;

import cn.stackssh.domain.agent.model.entity.ChatMessageEntity;
import cn.stackssh.infrastructure.adapter.repository.ChatHistoryRepository;
import cn.stackssh.infrastructure.dao.IChatMessageDao;
import cn.stackssh.infrastructure.dao.IChatMilestoneDao;
import cn.stackssh.infrastructure.dao.IChatSessionDao;
import cn.stackssh.infrastructure.dao.po.ChatMessagePO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ChatHistoryRepositoryTest {

    @Mock
    private IChatSessionDao chatSessionDao;
    @Mock
    private IChatMessageDao chatMessageDao;
    @Mock
    private IChatMilestoneDao chatMilestoneDao;

    @Test
    public void shouldExpandRecentFetchWhenBudgetExceedsFirstWindow() {
        ChatHistoryRepository repository = new ChatHistoryRepository();
        ReflectionTestUtils.setField(repository, "chatSessionDao", chatSessionDao);
        ReflectionTestUtils.setField(repository, "chatMessageDao", chatMessageDao);
        ReflectionTestUtils.setField(repository, "chatMilestoneDao", chatMilestoneDao);

        when(chatMessageDao.queryRecentBySessionId("s1", 100)).thenReturn(buildMessages(100, 2));
        when(chatMessageDao.queryRecentBySessionId("s1", 200)).thenReturn(buildMessages(150, 4));

        List<ChatMessageEntity> messages = repository.getMessagesWithBudget("s1", 400);

        Assert.assertEquals(100, messages.size());
        verify(chatMessageDao, times(1)).queryRecentBySessionId("s1", 100);
        verify(chatMessageDao, times(1)).queryRecentBySessionId("s1", 200);
    }

    private List<ChatMessagePO> buildMessages(int size, int tokenCount) {
        List<ChatMessagePO> pos = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            pos.add(ChatMessagePO.builder()
                    .id((long) i + 1)
                    .sessionId("s1")
                    .role("user")
                    .content("message-" + i)
                    .tokenCount(tokenCount)
                    .build());
        }
        return pos;
    }
}
