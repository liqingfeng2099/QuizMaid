package com.kanade.backend.service;

import com.kanade.backend.model.dto.PaperStrategyAddDTO;
import com.kanade.backend.model.dto.PaperStrategyQueryDTO;
import com.kanade.backend.model.dto.PaperStrategyUpdateDTO;
import com.kanade.backend.model.entity.PaperStrategy;
import com.kanade.backend.model.vo.PaperStrategyVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

public interface PaperStrategyService extends IService<PaperStrategy> {

    PaperStrategyVO createStrategy(PaperStrategyAddDTO dto, Long userId);

    PaperStrategyVO updateStrategy(PaperStrategyUpdateDTO dto, Long userId);

    boolean deleteStrategy(Long id, Long userId);

    PaperStrategyVO getStrategyVOById(Long id);

    Page<PaperStrategyVO> getStrategyPage(PaperStrategyQueryDTO queryDTO);

    boolean setDefault(Long id, Long userId);

    PaperStrategyVO copyStrategy(Long id, Long userId);
}
