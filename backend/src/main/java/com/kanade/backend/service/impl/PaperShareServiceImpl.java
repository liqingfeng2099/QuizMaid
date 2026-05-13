package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.PaperShareMapper;
import com.kanade.backend.model.entity.ExamPaper;
import com.kanade.backend.model.entity.PaperShare;
import com.kanade.backend.service.ExamPaperService;
import com.kanade.backend.service.PaperShareService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaperShareServiceImpl extends ServiceImpl<PaperShareMapper, PaperShare>
        implements PaperShareService {

    private final ExamPaperService examPaperService;

    @Override
    @Transactional
    public PaperShare shareToUser(Long paperId, Long targetUserId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        ExamPaper paper = examPaperService.getById(paperId);
        if (paper == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试卷不存在");
        }
        if (!paper.getCreatorId().equals(currentUserId) && !StpUtil.hasRole("admin")) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权分享此试卷");
        }
        if (paper.getStatus() == null || paper.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅已发布试卷可分享");
        }

        // 检查是否已分享给该用户
        QueryWrapper qw = QueryWrapper.create()
                .eq("paperId", paperId)
                .eq("ownerId", currentUserId)
                .eq("targetUserId", targetUserId);
        PaperShare exist = this.getOne(qw);
        if (exist != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已分享给该用户，无需重复分享");
        }

        PaperShare share = new PaperShare();
        share.setPaperId(paperId);
        share.setOwnerId(currentUserId);
        share.setTargetUserId(targetUserId);
        share.setCreateTime(LocalDateTime.now());
        this.save(share);
        return share;
    }

    @Override
    @Transactional
    public PaperShare shareToGroup(Long paperId, Long targetGroupId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        ExamPaper paper = examPaperService.getById(paperId);
        if (paper == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试卷不存在");
        }
        if (!paper.getCreatorId().equals(currentUserId) && !StpUtil.hasRole("admin")) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权分享此试卷");
        }
        if (paper.getStatus() == null || paper.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅已发布试卷可分享");
        }

        PaperShare share = new PaperShare();
        share.setPaperId(paperId);
        share.setOwnerId(currentUserId);
        share.setTargetGroupId(targetGroupId);
        share.setCreateTime(LocalDateTime.now());
        this.save(share);
        return share;
    }

    @Override
    public void revokeShare(Long shareId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        PaperShare share = this.getById(shareId);
        if (share == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分享记录不存在");
        }
        if (!share.getOwnerId().equals(currentUserId) && !StpUtil.hasRole("admin")) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权撤销此分享");
        }
        this.removeById(shareId);
    }

    @Override
    public List<PaperShare> getSharesByPaper(Long paperId) {
        QueryWrapper qw = QueryWrapper.create().eq("paperId", paperId);
        return this.list(qw);
    }

    @Override
    public List<PaperShare> getSharedToMe(Long userId) {
        QueryWrapper qw = QueryWrapper.create()
                .eq("targetUserId", userId)
                .orderBy("createTime", false);
        return this.list(qw);
    }
}
