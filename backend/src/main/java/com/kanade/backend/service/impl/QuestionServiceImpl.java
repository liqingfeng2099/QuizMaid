package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.kanade.backend.ai.AiService;
import com.kanade.backend.ai.AiServiceFactory;
import com.kanade.backend.ai.model.LabelResult;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.QuestionMapper;
import com.kanade.backend.model.dto.QuestionQueryDTO;
import com.kanade.backend.model.entity.Question;
import com.kanade.backend.model.enums.TaskEnum;
import com.kanade.backend.model.vo.QuestionVO;
import com.kanade.backend.service.QuestionService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private AiServiceFactory aiServiceFactory;

    @Override
    public Long addQuestion(Question question) {
        if (StrUtil.isBlank(question.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题干不能为空");
        }
        String md5 = DigestUtil.md5Hex(question.getContent());
        QueryWrapper wrapper = QueryWrapper.create().eq("questionMd5", md5);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该试题已存在");
        }
        question.setQuestionMd5(md5);

        Long creatorId = StpUtil.getLoginIdAsLong();
        question.setCreatorId(creatorId);

        if (question.getStatus() == null) {
            question.setStatus(1);
        }

        boolean saved = this.save(question);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加失败");
        }
        return question.getId();
    }

    @Override
    public boolean updateQuestion(Question question) {
        Long id = question.getId();
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试题ID不能为空");
        }
        Question old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试题不存在");
        }

        if (StrUtil.isNotBlank(question.getContent()) && !question.getContent().equals(old.getContent())) {
            String newMd5 = DigestUtil.md5Hex(question.getContent());
            QueryWrapper wrapper = QueryWrapper.create()
                    .eq("questionMd5", newMd5)
                    .ne("id", id);
            if (this.count(wrapper) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改后的试题已存在");
            }
            question.setQuestionMd5(newMd5);
        }

        question.setCreatorId(null);
        return this.updateById(question);
    }

    @Override
    public Page<QuestionVO> getQuestionPage(QuestionQueryDTO queryDTO) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (queryDTO.getId() != null) {
            wrapper.eq("id", queryDTO.getId());
        }
        if (queryDTO.getType() != null) {
            wrapper.eq("type", queryDTO.getType());
        }
        if (StrUtil.isNotBlank(queryDTO.getSubject())) {
            wrapper.eq("subject", queryDTO.getSubject());
        }
        if (StrUtil.isNotBlank(queryDTO.getChapter())) {
            wrapper.like("chapter", queryDTO.getChapter());
        }
        if (queryDTO.getDifficulty() != null) {
            wrapper.eq("difficulty", queryDTO.getDifficulty());
        }
        if (StrUtil.isNotBlank(queryDTO.getKnowledgePoints())) {
            wrapper.like("knowledgePoints", queryDTO.getKnowledgePoints());
        }
        if (StrUtil.isNotBlank(queryDTO.getTags())) {
            wrapper.like("tags", queryDTO.getTags());
        }
        if (StrUtil.isNotBlank(queryDTO.getContent())) {
            wrapper.like("content", queryDTO.getContent());
        }
        if (queryDTO.getCreatorId() != null) {
            wrapper.eq("creatorId", queryDTO.getCreatorId());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq("status", queryDTO.getStatus());
        }

        if (StrUtil.isNotBlank(queryDTO.getSortField())) {
            String sortField = queryDTO.getSortField();
            boolean isAsc = "ascend".equals(queryDTO.getSortOrder());
            wrapper.orderBy(sortField, isAsc);
        } else {
            wrapper.orderBy("createTime", false);
        }

        Page<Question> page = this.page(Page.of(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);

        List<QuestionVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Page<QuestionVO> voPage = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public QuestionVO getQuestionVOById(Long id) {
        Question question = this.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试题不存在");
        }
        return toVO(question);
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        Question question = new Question();
        question.setId(id);
        question.setStatus(status);
        return this.updateById(question);
    }

    private QuestionVO toVO(Question question) {
        if (question == null) return null;
        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(question, vo);
        return vo;
    }

    // ========== AI 标注方法（原有） ==========
    @Override
    public Question addLabels(Question question) {
        AiService aiService = aiServiceFactory.getAiService(TaskEnum.LABEL);
        LabelResult labelResult = aiService.generateQuestionLabel(question.toString());
        question.setDifficulty(labelResult.getDifficult());
        question.setTags(labelResult.getKnowledgePoints().toString());
        question.setSubject(labelResult.getSubject());
        this.updateById(question);
        return null; // 原返回 null，可按需调整
    }
}