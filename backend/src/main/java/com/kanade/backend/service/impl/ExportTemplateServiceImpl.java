package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.ExportTemplateMapper;
import com.kanade.backend.model.entity.ExportTemplate;
import com.kanade.backend.service.ExportTemplateService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportTemplateServiceImpl extends ServiceImpl<ExportTemplateMapper, ExportTemplate>
        implements ExportTemplateService {

    @Override
    @Transactional
    public ExportTemplate createTemplate(String name, String exportType, String config) {
        Long userId = StpUtil.getLoginIdAsLong();
        ExportTemplate t = new ExportTemplate();
        t.setTemplateName(name);
        t.setUserId(userId);
        t.setExportType(exportType);
        t.setConfig(config);
        t.setIsDefault(0);
        this.save(t);
        return t;
    }

    @Override
    @Transactional
    public ExportTemplate updateTemplate(Long id, String name, String config) {
        Long userId = StpUtil.getLoginIdAsLong();
        ExportTemplate t = this.getById(id);
        if (t == null || !t.getUserId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        if (name != null) t.setTemplateName(name);
        if (config != null) t.setConfig(config);
        this.updateById(t);
        return t;
    }

    @Override
    public void deleteTemplate(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        ExportTemplate t = this.getById(id);
        if (t == null || !t.getUserId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        this.removeById(id);
    }

    @Override
    @Transactional
    public void setDefault(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        ExportTemplate t = this.getById(id);
        if (t == null || !t.getUserId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        // 取消同类型其他默认
        QueryWrapper qw = QueryWrapper.create()
                .eq("userId", userId).eq("exportType", t.getExportType()).eq("isDefault", 1);
        for (ExportTemplate other : this.list(qw)) {
            other.setIsDefault(0);
            this.updateById(other);
        }
        t.setIsDefault(1);
        this.updateById(t);
    }

    @Override
    public List<ExportTemplate> getUserTemplates(String exportType) {
        Long userId = StpUtil.getLoginIdAsLong();
        QueryWrapper qw = QueryWrapper.create().eq("userId", userId);
        if (exportType != null) qw.eq("exportType", exportType);
        qw.orderBy("isDefault", false).orderBy("createTime", false);
        return this.list(qw);
    }

    @Override
    public ExportTemplate getDefaultTemplate(Long userId, String exportType) {
        QueryWrapper qw = QueryWrapper.create()
                .eq("userId", userId).eq("exportType", exportType).eq("isDefault", 1);
        return this.getOne(qw);
    }
}
