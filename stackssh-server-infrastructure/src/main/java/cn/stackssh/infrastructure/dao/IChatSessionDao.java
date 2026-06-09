package cn.stackssh.infrastructure.dao;

import cn.stackssh.infrastructure.dao.po.ChatSessionPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IChatSessionDao {
    void insert(ChatSessionPO po);
    void updateMessageCount(String id);
    ChatSessionPO queryById(String id);
}
