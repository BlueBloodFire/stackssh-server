package cn.stackssh.infrastructure.dao;

import cn.stackssh.infrastructure.dao.po.KnowledgeDocumentPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IKnowledgeDocumentDao {

    void insert(KnowledgeDocumentPO po);

    void updateStatus(@Param("docId") String docId,
                      @Param("status") int status,
                      @Param("chunkCount") int chunkCount);

    void delete(@Param("docId") String docId);

    KnowledgeDocumentPO queryByDocId(@Param("docId") String docId);

    List<KnowledgeDocumentPO> queryList(@Param("connectionId") String connectionId,
                                        @Param("agentId") String agentId);

}
