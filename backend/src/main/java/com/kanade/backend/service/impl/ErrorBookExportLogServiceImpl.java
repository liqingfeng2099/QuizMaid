package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.mapper.ErrorBookExportLogMapper;
import com.kanade.backend.model.entity.ErrorBookExportLog;
import com.kanade.backend.service.ErrorBookExportLogService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ErrorBookExportLogServiceImpl
        extends ServiceImpl<ErrorBookExportLogMapper, ErrorBookExportLog>
        implements ErrorBookExportLogService {

    @Override
    public ErrorBookExportLog saveLog(String exportType, Integer exportStatus, String fileName, String exportPath, String config) {
        ErrorBookExportLog log = new ErrorBookExportLog();
        log.setUserId(StpUtil.getLoginIdAsLong());
        log.setExportType(exportType);
        log.setExportStatus(exportStatus);
        log.setFileName(fileName);
        log.setExportPath(exportPath);
        log.setExportConfig(config);
        log.setCreateTime(LocalDateTime.now());
        this.save(log);
        return log;
    }

    @Override
    public List<ErrorBookExportLog> getUserLogs() {
        Long userId = StpUtil.getLoginIdAsLong();
        QueryWrapper qw = QueryWrapper.create().eq("userId", userId)
                .orderBy("createTime", false).limit(50);
        return this.list(qw);
    }
}
