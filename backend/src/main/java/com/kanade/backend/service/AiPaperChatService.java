package com.kanade.backend.service;

import com.kanade.backend.model.entity.AiPaperChat;
import com.kanade.backend.model.vo.AIChatVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

public interface AiPaperChatService extends IService<AiPaperChat> {

    AiPaperChat saveChat(Long userId, Long paperId, Long strategyId, String chatContent, String aiResponse, Integer status, Integer retryCount);

    List<AIChatVO> getChatHistory(Long userId, int limit);

    AIChatVO getChatById(Long chatId, Long userId);
}
