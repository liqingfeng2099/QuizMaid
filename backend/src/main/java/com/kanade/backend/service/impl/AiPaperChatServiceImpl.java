package com.kanade.backend.service.impl;

import com.kanade.backend.mapper.AiPaperChatMapper;
import com.kanade.backend.model.entity.AiPaperChat;
import com.kanade.backend.model.vo.AIChatVO;
import com.kanade.backend.service.AiPaperChatService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiPaperChatServiceImpl extends ServiceImpl<AiPaperChatMapper, AiPaperChat> implements AiPaperChatService {

    @Override
    public AiPaperChat saveChat(Long userId, Long paperId, Long strategyId, String chatContent, String aiResponse, Integer status, Integer retryCount) {
        LocalDateTime now = LocalDateTime.now();
        AiPaperChat chat = AiPaperChat.builder()
                .userId(userId)
                .paperId(paperId)
                .strategyId(strategyId)
                .chatContent(chatContent)
                .aiResponse(aiResponse)
                .status(status)
                .retryCount(retryCount)
                .createTime(now)
                .updateTime(now)
                .build();
        this.save(chat);
        return chat;
    }

    @Override
    public List<AIChatVO> getChatHistory(Long userId, int limit) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("userId", userId)
                .orderBy("createTime", false)
                .limit(limit);
        List<AiPaperChat> chats = this.list(wrapper);
        return chats.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public AIChatVO getChatById(Long chatId, Long userId) {
        AiPaperChat chat = this.getById(chatId);
        if (chat == null || !chat.getUserId().equals(userId)) return null;
        return toVO(chat);
    }

    private AIChatVO toVO(AiPaperChat chat) {
        return AIChatVO.builder()
                .id(chat.getId())
                .userId(chat.getUserId())
                .paperId(chat.getPaperId())
                .strategyId(chat.getStrategyId())
                .sessionRound(chat.getSessionRound())
                .chatContent(chat.getChatContent())
                .aiResponse(chat.getAiResponse())
                .status(chat.getStatus())
                .retryCount(chat.getRetryCount())
                .createTime(chat.getCreateTime())
                .build();
    }
}
