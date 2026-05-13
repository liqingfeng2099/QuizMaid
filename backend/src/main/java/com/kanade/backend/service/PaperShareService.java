package com.kanade.backend.service;

import com.kanade.backend.model.entity.PaperShare;
import com.mybatisflex.core.service.IService;
import java.util.List;

public interface PaperShareService extends IService<PaperShare> {

    PaperShare shareToUser(Long paperId, Long targetUserId);

    PaperShare shareToGroup(Long paperId, Long targetGroupId);

    void revokeShare(Long shareId);

    List<PaperShare> getSharesByPaper(Long paperId);

    List<PaperShare> getSharedToMe(Long userId);
}
