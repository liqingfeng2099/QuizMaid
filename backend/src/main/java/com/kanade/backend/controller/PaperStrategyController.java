package com.kanade.backend.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.assembly.AssemblyOrchestrator;
import com.kanade.backend.common.BaseResponse;
import com.kanade.backend.common.DeleteRequest;
import com.kanade.backend.common.ResultUtils;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.model.dto.AssemblyRequestDTO;
import com.kanade.backend.model.dto.PaperStrategyAddDTO;
import com.kanade.backend.model.dto.PaperStrategyQueryDTO;
import com.kanade.backend.model.dto.PaperStrategyUpdateDTO;
import com.kanade.backend.model.entity.PaperStrategy;
import com.kanade.backend.model.entity.StrategyWeight;
import com.kanade.backend.model.vo.AssemblyResultVO;
import com.kanade.backend.model.vo.PaperStrategyVO;
import com.kanade.backend.service.PaperStrategyService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/paperStrategy")
@RequiredArgsConstructor
@Tag(name = "组卷策略管理")
public class PaperStrategyController {

    private final PaperStrategyService paperStrategyService;
    private final AssemblyOrchestrator assemblyOrchestrator;
    private final com.kanade.backend.mapper.StrategyWeightMapper strategyWeightMapper;

    @PostMapping("/add")
    @SaCheckLogin
    @Operation(summary = "创建组卷策略")
    public BaseResponse<PaperStrategyVO> addStrategy(@Valid @RequestBody PaperStrategyAddDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        PaperStrategyVO vo = paperStrategyService.createStrategy(dto, userId);
        return ResultUtils.success(vo);
    }

    @PostMapping("/update")
    @SaCheckLogin
    @Operation(summary = "更新组卷策略")
    public BaseResponse<PaperStrategyVO> updateStrategy(@Valid @RequestBody PaperStrategyUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        PaperStrategyVO vo = paperStrategyService.updateStrategy(dto, userId);
        return ResultUtils.success(vo);
    }

    @PostMapping("/delete")
    @SaCheckLogin
    @Operation(summary = "删除组卷策略")
    public BaseResponse<Boolean> deleteStrategy(@RequestBody DeleteRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = paperStrategyService.deleteStrategy(request.getId(), userId);
        return ResultUtils.success(result);
    }

    @GetMapping("/get/{id}")
    @SaCheckLogin
    @Operation(summary = "获取策略详情（含权重）")
    public BaseResponse<PaperStrategyVO> getStrategy(@PathVariable Long id) {
        PaperStrategyVO vo = paperStrategyService.getStrategyVOById(id);
        return ResultUtils.success(vo);
    }

    @PostMapping("/list/page")
    @SaCheckLogin
    @Operation(summary = "分页查询策略")
    public BaseResponse<Page<PaperStrategyVO>> listStrategyByPage(@RequestBody PaperStrategyQueryDTO queryDTO) {
        Page<PaperStrategyVO> page = paperStrategyService.getStrategyPage(queryDTO);
        return ResultUtils.success(page);
    }

    @PostMapping("/setDefault/{id}")
    @SaCheckLogin
    @Operation(summary = "设为默认策略")
    public BaseResponse<Boolean> setDefault(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = paperStrategyService.setDefault(id, userId);
        return ResultUtils.success(result);
    }

    @PostMapping("/copy/{id}")
    @SaCheckLogin
    @Operation(summary = "复制策略")
    public BaseResponse<PaperStrategyVO> copyStrategy(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        PaperStrategyVO vo = paperStrategyService.copyStrategy(id, userId);
        return ResultUtils.success(vo);
    }

    @PostMapping("/assemble/greedy")
    @SaCheckLogin
    @Operation(summary = "贪心算法组卷")
    public BaseResponse<AssemblyResultVO> greedyAssemble(@Valid @RequestBody AssemblyRequestDTO request) {
        Long userId = StpUtil.getLoginIdAsLong();

        PaperStrategy strategy = paperStrategyService.getById(request.getStrategyId());
        if (strategy == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "策略不存在");
        }

        QueryWrapper weightWrapper = QueryWrapper.create().eq("strategyId", request.getStrategyId());
        List<StrategyWeight> weights = strategyWeightMapper.selectListByQuery(weightWrapper);

        AssemblyResultVO result = assemblyOrchestrator.greedyAssemble(request, strategy, weights, userId);
        return ResultUtils.success(result);
    }

    @PostMapping("/assemble/genetic")
    @SaCheckLogin
    @Operation(summary = "遗传算法组卷（高精度）")
    public BaseResponse<AssemblyResultVO> geneticAssemble(@Valid @RequestBody AssemblyRequestDTO request) {
        Long userId = StpUtil.getLoginIdAsLong();

        PaperStrategy strategy = paperStrategyService.getById(request.getStrategyId());
        if (strategy == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "策略不存在");
        }

        QueryWrapper weightWrapper = QueryWrapper.create().eq("strategyId", request.getStrategyId());
        List<StrategyWeight> weights = strategyWeightMapper.selectListByQuery(weightWrapper);

        AssemblyResultVO result = assemblyOrchestrator.geneticAssemble(request, strategy, weights, userId);
        return ResultUtils.success(result);
    }
}
