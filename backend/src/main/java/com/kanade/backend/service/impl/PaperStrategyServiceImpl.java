package com.kanade.backend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.PaperStrategyMapper;
import com.kanade.backend.mapper.StrategyWeightMapper;
import com.kanade.backend.model.dto.PaperStrategyAddDTO;
import com.kanade.backend.model.dto.PaperStrategyQueryDTO;
import com.kanade.backend.model.dto.PaperStrategyUpdateDTO;
import com.kanade.backend.model.dto.StrategyWeightDTO;
import com.kanade.backend.model.entity.PaperStrategy;
import com.kanade.backend.model.entity.StrategyWeight;
import com.kanade.backend.model.vo.PaperStrategyVO;
import com.kanade.backend.model.vo.StrategyWeightVO;
import com.kanade.backend.service.PaperStrategyService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperStrategyServiceImpl extends ServiceImpl<PaperStrategyMapper, PaperStrategy>
        implements PaperStrategyService {

    private final StrategyWeightMapper strategyWeightMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperStrategyVO createStrategy(PaperStrategyAddDTO dto, Long userId) {
        // 1. 校验权重总和 = 100
        validateWeightSum(dto.getWeights());

        // 2. 创建策略
        PaperStrategy strategy = PaperStrategy.builder()
                .strategyName(dto.getStrategyName())
                .userId(userId)
                .totalScore(dto.getTotalScore())
                .difficultyAvg(dto.getDifficultyAvg())
                .duration(dto.getDuration())
                .questionTypeConfig(dto.getQuestionTypeConfig())
                .difficultyConfig(dto.getDifficultyConfig())
                .knowledgePointScope(dto.getKnowledgePointScope())
                .isDefault(0)
                .build();
        this.save(strategy);

        // 3. 保存权重
        saveWeights(strategy.getId(), dto.getWeights());

        return buildVOFromDTOs(strategy, dto.getWeights());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperStrategyVO updateStrategy(PaperStrategyUpdateDTO dto, Long userId) {
        PaperStrategy strategy = this.getById(dto.getId());
        if (strategy == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "策略不存在");
        }
        if (!strategy.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权修改他人策略");
        }

        if (dto.getStrategyName() != null) strategy.setStrategyName(dto.getStrategyName());
        if (dto.getTotalScore() != null) strategy.setTotalScore(dto.getTotalScore());
        if (dto.getDifficultyAvg() != null) strategy.setDifficultyAvg(dto.getDifficultyAvg());
        if (dto.getDuration() != null) strategy.setDuration(dto.getDuration());
        if (dto.getQuestionTypeConfig() != null) strategy.setQuestionTypeConfig(dto.getQuestionTypeConfig());
        if (dto.getDifficultyConfig() != null) strategy.setDifficultyConfig(dto.getDifficultyConfig());
        if (dto.getKnowledgePointScope() != null) strategy.setKnowledgePointScope(dto.getKnowledgePointScope());
        this.updateById(strategy);

        // 更新权重
        if (dto.getWeights() != null && !dto.getWeights().isEmpty()) {
            validateWeightSum(dto.getWeights());

            // 删除旧权重
            QueryWrapper deleteWrapper = QueryWrapper.create().eq("strategyId", dto.getId());
            strategyWeightMapper.deleteByQuery(deleteWrapper);

            // 保存新权重
            saveWeights(dto.getId(), dto.getWeights());
        }

        // 重新读取权重
        List<StrategyWeight> currentWeights = loadWeights(dto.getId());
        return buildVO(strategy, currentWeights);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteStrategy(Long id, Long userId) {
        PaperStrategy strategy = this.getById(id);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "策略不存在");
        }
        if (!strategy.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权删除他人策略");
        }
        // 逻辑删除权重
        QueryWrapper deleteWrapper = QueryWrapper.create().eq("strategyId", id);
        strategyWeightMapper.deleteByQuery(deleteWrapper);
        return this.removeById(id);
    }

    @Override
    public PaperStrategyVO getStrategyVOById(Long id) {
        PaperStrategy strategy = this.getById(id);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "策略不存在");
        }
        List<StrategyWeight> weights = loadWeights(id);
        return buildVO(strategy, weights);
    }

    @Override
    public Page<PaperStrategyVO> getStrategyPage(PaperStrategyQueryDTO queryDTO) {
        QueryWrapper wrapper = QueryWrapper.create();
        // 仅查当前用户
        wrapper.eq("userId", cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong());

        if (StrUtil.isNotBlank(queryDTO.getStrategyName())) {
            wrapper.like("strategyName", queryDTO.getStrategyName());
        }
        if (queryDTO.getDifficultyAvg() != null) {
            wrapper.eq("difficultyAvg", queryDTO.getDifficultyAvg());
        }
        if (queryDTO.getIsDefault() != null) {
            wrapper.eq("isDefault", queryDTO.getIsDefault());
        }
        wrapper.orderBy("createTime", false);

        Page<PaperStrategy> page = this.page(Page.of(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);

        List<PaperStrategyVO> voList = page.getRecords().stream()
                .map(s -> buildVO(s, loadWeights(s.getId())))
                .collect(Collectors.toList());

        Page<PaperStrategyVO> voPage = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefault(Long id, Long userId) {
        PaperStrategy strategy = this.getById(id);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "策略不存在");
        }
        if (!strategy.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权操作");
        }
        // 清除该用户其他默认策略
        QueryWrapper clearWrapper = QueryWrapper.create()
                .eq("userId", userId)
                .eq("isDefault", 1);
        List<PaperStrategy> defaults = this.list(clearWrapper);
        for (PaperStrategy s : defaults) {
            s.setIsDefault(0);
            this.updateById(s);
        }
        strategy.setIsDefault(1);
        return this.updateById(strategy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperStrategyVO copyStrategy(Long id, Long userId) {
        PaperStrategy source = this.getById(id);
        if (source == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "策略不存在");
        }
        List<StrategyWeight> sourceWeights = loadWeights(id);

        PaperStrategy copy = PaperStrategy.builder()
                .strategyName(source.getStrategyName() + "-副本")
                .userId(userId)
                .totalScore(source.getTotalScore())
                .difficultyAvg(source.getDifficultyAvg())
                .duration(source.getDuration())
                .questionTypeConfig(source.getQuestionTypeConfig())
                .difficultyConfig(source.getDifficultyConfig())
                .knowledgePointScope(source.getKnowledgePointScope())
                .isDefault(0)
                .build();
        this.save(copy);

        List<StrategyWeightDTO> copyWeights = sourceWeights.stream()
                .map(w -> {
                    StrategyWeightDTO dto = new StrategyWeightDTO();
                    dto.setWeightType(w.getWeightType());
                    dto.setWeightValue(w.getWeightValue());
                    return dto;
                })
                .collect(Collectors.toList());
        saveWeights(copy.getId(), copyWeights);

        return buildVOFromDTOs(copy, copyWeights);
    }

    private void validateWeightSum(List<StrategyWeightDTO> weights) {
        int sum = weights.stream().mapToInt(StrategyWeightDTO::getWeightValue).sum();
        if (sum != 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "6个指标权重之和必须等于100%，当前为" + sum + "%");
        }
    }

    private void saveWeights(Long strategyId, List<StrategyWeightDTO> dtos) {
        LocalDateTime now = LocalDateTime.now();
        for (StrategyWeightDTO dto : dtos) {
            StrategyWeight weight = StrategyWeight.builder()
                    .strategyId(strategyId)
                    .weightType(dto.getWeightType())
                    .weightValue(dto.getWeightValue())
                    .createTime(now)
                    .updateTime(now)
                    .build();
            strategyWeightMapper.insert(weight);
        }
    }

    private List<StrategyWeight> loadWeights(Long strategyId) {
        QueryWrapper wrapper = QueryWrapper.create().eq("strategyId", strategyId);
        return strategyWeightMapper.selectListByQuery(wrapper);
    }

    private PaperStrategyVO buildVOFromDTOs(PaperStrategy strategy, List<StrategyWeightDTO> weightDTOs) {
        List<StrategyWeightVO> weightVOs = weightDTOs.stream()
                .map(dto -> StrategyWeightVO.builder()
                        .weightType(dto.getWeightType())
                        .weightValue(dto.getWeightValue())
                        .build())
                .collect(Collectors.toList());
        int sum = weightDTOs.stream().mapToInt(StrategyWeightDTO::getWeightValue).sum();

        return PaperStrategyVO.builder()
                .id(strategy.getId())
                .strategyName(strategy.getStrategyName())
                .userId(strategy.getUserId())
                .totalScore(strategy.getTotalScore())
                .difficultyAvg(strategy.getDifficultyAvg())
                .duration(strategy.getDuration())
                .questionTypeConfig(strategy.getQuestionTypeConfig())
                .difficultyConfig(strategy.getDifficultyConfig())
                .knowledgePointScope(strategy.getKnowledgePointScope())
                .isDefault(strategy.getIsDefault())
                .createTime(strategy.getCreateTime())
                .updateTime(strategy.getUpdateTime())
                .weights(weightVOs)
                .weightSum(sum)
                .build();
    }

    private PaperStrategyVO buildVO(PaperStrategy strategy, List<StrategyWeight> weightEntities) {
        List<StrategyWeightVO> weightVOs = weightEntities.stream()
                .map(w -> StrategyWeightVO.builder()
                        .id(w.getId())
                        .strategyId(w.getStrategyId())
                        .weightType(w.getWeightType())
                        .weightValue(w.getWeightValue())
                        .build())
                .collect(Collectors.toList());
        int sum = weightEntities.stream().mapToInt(StrategyWeight::getWeightValue).sum();

        return PaperStrategyVO.builder()
                .id(strategy.getId())
                .strategyName(strategy.getStrategyName())
                .userId(strategy.getUserId())
                .totalScore(strategy.getTotalScore())
                .difficultyAvg(strategy.getDifficultyAvg())
                .duration(strategy.getDuration())
                .questionTypeConfig(strategy.getQuestionTypeConfig())
                .difficultyConfig(strategy.getDifficultyConfig())
                .knowledgePointScope(strategy.getKnowledgePointScope())
                .isDefault(strategy.getIsDefault())
                .createTime(strategy.getCreateTime())
                .updateTime(strategy.getUpdateTime())
                .weights(weightVOs)
                .weightSum(sum)
                .build();
    }
}
