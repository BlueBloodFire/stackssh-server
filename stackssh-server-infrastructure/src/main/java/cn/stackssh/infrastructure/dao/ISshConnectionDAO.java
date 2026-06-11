package cn.stackssh.infrastructure.dao;

import cn.stackssh.infrastructure.dao.po.SshConnectionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SSH连接配置DAO
 *
 * @author waissh dev
 */
@Mapper
public interface ISshConnectionDAO {

    void insert(SshConnectionPO po);

    void update(SshConnectionPO po);

    void delete(@Param("connectionId") String connectionId);

    SshConnectionPO queryByConnectionId(@Param("connectionId") String connectionId);

    List<SshConnectionPO> queryListByUserId(@Param("userId") String userId);

    /** 服务启动时将所有 CONNECTED 状态重置为 DISCONNECTED（内存 Session 随进程销毁） */
    void resetAllConnectedToDisconnected();

}
