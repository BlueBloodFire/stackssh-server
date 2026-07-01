package cn.stackssh.infrastructure.dao;

import cn.stackssh.infrastructure.dao.po.ConversationRoundTracePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IConversationRoundTraceDao {

    void insert(ConversationRoundTracePO po);

    List<ConversationRoundTracePO> queryRecentBySessionId(@Param("sessionId") String sessionId, @Param("limit") int limit);
}
