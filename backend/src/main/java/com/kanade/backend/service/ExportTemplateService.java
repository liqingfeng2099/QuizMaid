package com.kanade.backend.service;

import com.kanade.backend.model.entity.ExportTemplate;
import com.mybatisflex.core.service.IService;
import java.util.List;

public interface ExportTemplateService extends IService<ExportTemplate> {
    ExportTemplate createTemplate(String name, String exportType, String config);
    ExportTemplate updateTemplate(Long id, String name, String config);
    void deleteTemplate(Long id);
    void setDefault(Long id);
    List<ExportTemplate> getUserTemplates(String exportType);
    ExportTemplate getDefaultTemplate(Long userId, String exportType);
}
