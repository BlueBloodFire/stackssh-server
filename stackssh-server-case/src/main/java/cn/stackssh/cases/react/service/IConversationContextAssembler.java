package cn.stackssh.cases.react.service;

import cn.stackssh.api.dto.ChatRequestDTO;
import cn.stackssh.cases.react.factory.DefaultReActFactory;
import cn.stackssh.domain.agent.model.valobj.context.PromptEnvelopeVO;

public interface IConversationContextAssembler {

    PromptEnvelopeVO assemble(
            ChatRequestDTO requestDTO,
            DefaultReActFactory.DynamicContext dynamicContext
    );
}
