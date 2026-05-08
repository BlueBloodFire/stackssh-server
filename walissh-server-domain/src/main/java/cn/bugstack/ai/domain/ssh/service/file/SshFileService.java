package cn.bugstack.ai.domain.ssh.service.file;

import cn.bugstack.ai.domain.ssh.adapter.port.ISshFilePort;
import cn.bugstack.ai.domain.ssh.adapter.port.ISshSessionPort;
import cn.bugstack.ai.domain.ssh.model.entity.SshFileContentEntity;
import cn.bugstack.ai.domain.ssh.model.entity.SshFileTreeEntity;
import cn.bugstack.ai.domain.ssh.service.ISshFileDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SshFileService implements ISshFileDomainService {

    private final ISshSessionPort sshSessionPort;
    private final ISshFilePort sshFilePort;

    public SshFileService(ISshSessionPort sshSessionPort, ISshFilePort sshFilePort) {
        this.sshSessionPort = sshSessionPort;
        this.sshFilePort = sshFilePort;
    }

    @Override
    public SshFileTreeEntity tree(String connectionId, String path) throws Exception {
        validateConnection(connectionId);
        return sshFilePort.listDirectory(connectionId, path);
    }

    @Override
    public SshFileContentEntity content(String connectionId, String path) throws Exception {
        validateConnection(connectionId);
        validatePath(path);
        return sshFilePort.readFile(connectionId, path);
    }

    @Override
    public void createFile(String connectionId, String path, boolean useSudo) throws Exception {
        validateConnection(connectionId);
        validatePath(path);
        sshFilePort.createFile(connectionId, path, useSudo);
    }

    @Override
    public void createDirectory(String connectionId, String path, boolean useSudo) throws Exception {
        validateConnection(connectionId);
        validatePath(path);
        sshFilePort.createDirectory(connectionId, path, useSudo);
    }

    @Override
    public void rename(String connectionId, String oldPath, String newPath, boolean useSudo) throws Exception {
        validateConnection(connectionId);
        validatePath(oldPath);
        validatePath(newPath);
        sshFilePort.rename(connectionId, oldPath, newPath, useSudo);
    }

    @Override
    public void delete(String connectionId, String path, boolean useSudo) throws Exception {
        validateConnection(connectionId);
        validatePath(path);
        sshFilePort.delete(connectionId, path, useSudo);
    }

    @Override
    public void saveFile(String connectionId, String path, String content, boolean useSudo) throws Exception {
        validateConnection(connectionId);
        validatePath(path);
        sshFilePort.saveFile(connectionId, path, content, useSudo);
    }

    private void validateConnection(String connectionId) {
        if (connectionId == null || connectionId.isBlank()) {
            throw new IllegalArgumentException("连接ID不能为空");
        }
        if (!sshSessionPort.isConnected(connectionId)) {
            throw new IllegalStateException("SSH连接未建立，请先连接服务器");
        }
    }

    private void validatePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("路径不能为空");
        }
    }
}
