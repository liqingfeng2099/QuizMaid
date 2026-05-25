package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.model.entity.PaperShare;
import com.kanade.backend.service.PaperShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/paperShare")
@RequiredArgsConstructor
@Tag(name = "试卷分享管理")
public class PaperShareController {

    private final PaperShareService paperShareService;

    @PostMapping("/user/{paperId}/{targetUserId}")
    @SaCheckLogin
    @Operation(summary = "分享试卷给指定用户")
    public BaseResponse<PaperShare> shareToUser(@PathVariable Long paperId,
                                                 @PathVariable Long targetUserId) {
        PaperShare share = paperShareService.shareToUser(paperId, targetUserId);
        return ResultUtils.success(share);
    }

    @PostMapping("/group/{paperId}/{targetGroupId}")
    @SaCheckLogin
    @Operation(summary = "分享试卷给指定班级/组")
    public BaseResponse<PaperShare> shareToGroup(@PathVariable Long paperId,
                                                  @PathVariable Long targetGroupId) {
        PaperShare share = paperShareService.shareToGroup(paperId, targetGroupId);
        return ResultUtils.success(share);
    }

    @PostMapping("/revoke/{shareId}")
    @SaCheckLogin
    @Operation(summary = "撤销试卷分享")
    public BaseResponse<Boolean> revokeShare(@PathVariable Long shareId) {
        paperShareService.revokeShare(shareId);
        return ResultUtils.success(true);
    }

    @GetMapping("/paper/{paperId}")
    @SaCheckLogin
    @Operation(summary = "获取试卷的所有分享记录")
    public BaseResponse<List<PaperShare>> getSharesByPaper(@PathVariable Long paperId) {
        List<PaperShare> shares = paperShareService.getSharesByPaper(paperId);
        return ResultUtils.success(shares);
    }

    @GetMapping("/shared-to-me")
    @SaCheckLogin
    @Operation(summary = "获取分享给我的试卷")
    public BaseResponse<List<PaperShare>> getSharedToMe() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<PaperShare> shares = paperShareService.getSharedToMe(userId);
        return ResultUtils.success(shares);
    }
}
