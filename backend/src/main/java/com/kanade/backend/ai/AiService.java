package com.kanade.backend.ai;

import com.kanade.backend.ai.model.LabelResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AiService {

    @SystemMessage(fromResource = "prompt/label.txt")
    LabelResult generateQuestionLabel(String userMessage);

    @SystemMessage(fromResource = "prompt/label.txt")
    Flux<String> generateStreamingQuestionLabel(String userMessage);

    @SystemMessage(fromResource = "prompt/judge.txt")
    String generateQuestionJudge(String userMessage);

    @SystemMessage(fromResource = "prompt/paperAssembly.txt")
    String generatePaperAssembly(String userMessage);
}
