package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.DeleteRequest;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.model.entity.ExamPaper;
import com.kanade.backend.service.ExamPaperService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SaCheckRole("admin")
@Tag(name = "管理员操作")
public class AdminController {

    private final ExamPaperService examPaperService;

    @PostMapping("/paper/list-all")
    @Operation(summary = "管理员查看所有试卷（含已删除）")
    public BaseResponse<Page<ExamPaper>> listAllPapers(@RequestBody com.kanade.backend.model.dto.ExamPaperQueryDTO dto) {
        QueryWrapper qw = QueryWrapper.create();
        int pageNum = dto.getPageNum() > 0 ? dto.getPageNum() : 1;
        int pageSize = dto.getPageSize() > 0 ? dto.getPageSize() : 20;
        Page<ExamPaper> page = examPaperService.page(Page.of(pageNum, pageSize), qw);
        return ResultUtils.success(page);
    }

    @PostMapping("/paper/force-delete")
    @Operation(summary = "管理员强制删除试卷")
    public BaseResponse<Boolean> forceDeletePaper(@RequestBody DeleteRequest req) {
        if (req.getId() == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        ExamPaper paper = examPaperService.getById(req.getId());
        if (paper == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        examPaperService.removeById(req.getId());
        return ResultUtils.success(true);
    }

    @PostMapping("/paper/batch-delete")
    @Operation(summary = "管理员批量删除试卷")
    public BaseResponse<Integer> batchDeletePapers(@RequestBody List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            examPaperService.removeById(id);
            count++;
        }
        return ResultUtils.success(count);
    }

    @PostMapping("/paper/batch-status")
    @Operation(summary = "管理员批量修改试卷状态")
    public BaseResponse<Integer> batchUpdateStatus(@RequestParam Integer status,
                                                    @RequestBody List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            examPaperService.updateStatus(id, status);
            count++;
        }
        return ResultUtils.success(count);
    }
}
