package cn.stackssh.api;

import cn.stackssh.api.dto.SshConnectionRequestDTO;
import cn.stackssh.api.dto.SshConnectionResponseDTO;
import cn.stackssh.api.response.Response;

import java.util.List;

/**
 * SSH连接服务远程接口
 *
 * @author waissh dev
 */
public interface ISshConnectionService {

    /**
     * 创建SSH连接
     */
    Response<SshConnectionResponseDTO> createConnection(SshConnectionRequestDTO requestDTO);

    /**
     * 更新SSH连接
     */
    Response<SshConnectionResponseDTO> updateConnection(SshConnectionRequestDTO requestDTO);

    /**
     * 删除SSH连接
     */
    Response<Void> deleteConnection(String connectionId);

    /**
     * 查询单个连接
     */
    Response<SshConnectionResponseDTO> getConnection(String connectionId);

    /**
     * 查询用户的所有连接
     */
    Response<List<SshConnectionResponseDTO>> getConnectionList(String userId);

    /**
     * 建立SSH连接
     */
    Response<Void> connect(String connectionId);

    /**
     * 断开SSH连接
     */
    Response<Void> disconnect(String connectionId);

}
