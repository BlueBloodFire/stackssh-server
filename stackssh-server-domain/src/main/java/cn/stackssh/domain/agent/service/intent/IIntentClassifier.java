package cn.stackssh.domain.agent.service.intent;

import cn.stackssh.domain.agent.model.valobj.intent.ConversationContextVO;
import cn.stackssh.domain.agent.model.valobj.intent.IntentResultVO;

public interface IIntentClassifier {
    IntentResultVO classify(String message, ConversationContextVO context);
}
