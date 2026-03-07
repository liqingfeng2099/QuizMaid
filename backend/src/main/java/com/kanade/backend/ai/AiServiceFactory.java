package com.kanade.backend.ai;

import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.model.enums.TaskEnum;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AiServiceFactory {

    @Resource
    private static OpenAiChatModel chatModel;

    /**
     * 创建新的 AI 服务实例
     */


    public static AiService getAiService(TaskEnum taskEnum) {

        // 根据类型构建服务
        return switch (taskEnum) {
            case LABEL -> AiServices.builder(AiService.class)
                    .chatModel(chatModel)
                    //.streamingChatModel(reasoningStreamingChatModel)
                    //.chatMemoryProvider(memoryId -> chatMemory)
                    //.tools(new FileWriteTool())
                    .hallucinatedToolNameStrategy((toolExecutionRequest) ->
                            ToolExecutionResultMessage.from(toolExecutionRequest,
                                    "Error: there is no tool called " + toolExecutionRequest.name()))
                    .build();
            case JUDGE -> AiServices.builder(AiService.class)
                    .chatModel(chatModel)
                    //.streamingChatModel(openAiStreamingChatModel)
                    //.chatMemory(chatMemory)
                    .build();
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的类型: " + taskEnum.getValue());
        };
    }
    @Bean
    public AiService createAiCodeGeneratorService(/*TaskEnum taskEnum*/) {
        return AiServices.create(AiService.class, chatModel);
    }
}
