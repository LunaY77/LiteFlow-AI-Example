package com.lunay.liteflow.ai.history.context;

import com.yomahub.liteflow.ai.context.ChatContext;
import com.yomahub.liteflow.ai.engine.model.chat.message.AssistantMessage;
import com.yomahub.liteflow.ai.engine.model.chat.message.Message;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatHistoryContext extends ChatContext {

    private List<Message> conversationHistory;

    public void addMessage(Message message) {
        this.conversationHistory.add(message);
    }

    public AssistantMessage getLastAssistantMessage() {
        for (int i = conversationHistory.size() - 1; i >= 0; i--) {
            Message msg = conversationHistory.get(i);
            if (msg instanceof AssistantMessage) {
                return (AssistantMessage) msg;
            }
        }
        return null;
    }
}
