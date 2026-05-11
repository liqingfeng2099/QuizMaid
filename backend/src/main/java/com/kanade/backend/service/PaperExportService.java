package com.kanade.backend.service;

import com.kanade.backend.model.dto.ExportConfigDTO;
import com.kanade.backend.model.vo.ExportFileVO;

import java.util.List;

public interface PaperExportService {

    byte[] exportWord(Long paperId, ExportConfigDTO config);

    byte[] exportPdf(Long paperId, ExportConfigDTO config);

    String previewHtml(Long paperId, ExportConfigDTO config);

    ExportFileVO saveAndRecord(Long paperId, String exportType, String fileName);

    List<ExportFileVO> listExportFiles(Long userId);

    boolean deleteExportFile(Long fileId);

    byte[] batchExport(List<Long> paperIds, ExportConfigDTO config);
}
