package com.kanade.backend.service;

import com.kanade.backend.model.entity.ErrorBookExportLog;
import com.mybatisflex.core.service.IService;
import java.util.List;

public interface ErrorBookExportLogService extends IService<ErrorBookExportLog> {
    ErrorBookExportLog saveLog(String exportType, Integer exportStatus, String fileName, String exportPath, String config);
    List<ErrorBookExportLog> getUserLogs();
}
