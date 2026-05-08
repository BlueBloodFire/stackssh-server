package cn.bugstack.ai.domain.ssh.adapter.port;

import cn.bugstack.ai.domain.ssh.model.entity.SshFileContentEntity;
import cn.bugstack.ai.domain.ssh.model.entity.SshFileTreeEntity;

/**
 * SSH 文件访问 Port（基础设施实现）
 */
public interface ISshFilePort {

    SshFileTreeEntity listDirectory(String connectionId, String path) throws Exception;

    SshFileContentEntity readFile(String connectionId, String path) throws Exception;

    void createFile(String connectionId, String path, boolean useSudo) throws Exception;

    void createDirectory(String connectionId, String path, boolean useSudo) throws Exception;

    void rename(String connectionId, String oldPath, String newPath, boolean useSudo) throws Exception;

    void delete(String connectionId, String path, boolean useSudo) throws Exception;

    void saveFile(String connectionId, String path, String content, boolean useSudo) throws Exception;

}
