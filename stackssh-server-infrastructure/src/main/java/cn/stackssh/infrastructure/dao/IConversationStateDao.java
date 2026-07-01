package cn.stackssh.infrastructure.dao;

import cn.stackssh.infrastructure.dao.po.ConversationStatePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IConversationStateDao {

    ConversationStatePO queryBySessionId(@Param("sessionId") String sessionId);

    void insert(ConversationStatePO po);

    void upsert(ConversationStatePO po);

    void updateTaskSummary(@Param("sessionId") String sessionId, @Param("taskSummary") String taskSummary);

    void updateIntentState(@Param("sessionId") String sessionId,
                           @Param("currentIntent") String currentIntent,
                           @Param("intentSummary") String intentSummary,
                           @Param("turnCount") Integer turnCount,
                           @Param("lastRoundAt") Long lastRoundAt);

    void updateToolSummary(@Param("sessionId") String sessionId,
                           @Param("toolSummary") String toolSummary,
                           @Param("lastRoundAt") Long lastRoundAt);

    void updateTerminalBinding(@Param("sessionId") String sessionId,
                               @Param("lastTerminalSessionId") String lastTerminalSessionId);
}
