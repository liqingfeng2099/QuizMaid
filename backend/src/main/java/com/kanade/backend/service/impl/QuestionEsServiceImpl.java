package com.kanade.backend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.kanade.backend.model.dto.QuestionQueryDTO;
import com.kanade.backend.model.es.QuestionDocument;
import com.kanade.backend.model.vo.QuestionVO;
import com.kanade.backend.service.QuestionEsService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuestionEsServiceImpl implements QuestionEsService {

    private static final Set<String> ES_SORT_FIELDS = Set.of(
            "id", "createTime", "updateTime", "difficulty", "correctCount", "totalCount"
    );

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    public void syncQuestionToEs(QuestionDocument document) {
        elasticsearchOperations.save(document);
        log.debug("Synced question to ES, id: {}", document.getId());
    }

    @Override
    public void deleteQuestionFromEs(Long id) {
        elasticsearchOperations.delete(id.toString(), QuestionDocument.class);
        log.debug("Deleted question from ES, id: {}", id);
    }

    @Override
    public void batchSyncQuestionsToEs(List<QuestionDocument> documents) {
        if (CollUtil.isEmpty(documents)) {
            return;
        }
        elasticsearchOperations.save(documents);
        log.info("Batch synced {} questions to ES", documents.size());
    }

    @Override
    public Page<QuestionVO> searchQuestions(String keyword, QuestionQueryDTO queryDTO) {
        int pageNum = queryDTO.getPageNum();
        int pageSize = queryDTO.getPageSize();

        // Build sort
        Sort sort = buildSort(queryDTO.getSortField(), queryDTO.getSortOrder());

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    // MUST: multi_match for keyword
                    if (StrUtil.isNotBlank(keyword)) {
                        b.must(m -> m.multiMatch(mm -> mm
                                .fields("content^3", "chapter^2", "knowledgePoints")
                                .query(keyword)
                                .fuzziness("1")
                        ));
                    }
                    // FILTER clauses
                    if (queryDTO.getType() != null) {
                        b.filter(f -> f.term(t -> t.field("type").value(queryDTO.getType())));
                    }
                    if (StrUtil.isNotBlank(queryDTO.getSubject())) {
                        b.filter(f -> f.term(t -> t.field("subject").value(queryDTO.getSubject())));
                    }
                    if (queryDTO.getDifficulty() != null) {
                        b.filter(f -> f.term(t -> t.field("difficulty").value(queryDTO.getDifficulty())));
                    }
                    if (queryDTO.getCreatorId() != null) {
                        b.filter(f -> f.term(t -> t.field("creatorId").value(queryDTO.getCreatorId())));
                    }
                    if (queryDTO.getStatus() != null) {
                        b.filter(f -> f.term(t -> t.field("status").value(queryDTO.getStatus())));
                    }
                    // Ensure must clause exists (match_all if no keyword)
                    b.minimumShouldMatch("0");
                    return b;
                }))
                .withPageable(PageRequest.of(pageNum - 1, pageSize, sort))
                .build();

        SearchHits<QuestionDocument> searchHits = elasticsearchOperations.search(
                nativeQuery, QuestionDocument.class);

        long total = searchHits.getTotalHits();
        List<QuestionVO> voList = searchHits.getSearchHits().stream()
                .map(this::hitToVO)
                .collect(Collectors.toList());

        Page<QuestionVO> result = new Page<>(pageNum, pageSize, total);
        result.setRecords(voList);
        return result;
    }

    // =========== Helpers ===========

    private Sort buildSort(String sortField, String sortOrder) {
        if (StrUtil.isBlank(sortField) || !ES_SORT_FIELDS.contains(sortField)) {
            return Sort.by(Sort.Direction.DESC, "createTime");
        }
        Sort.Direction direction = "ascend".equals(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortField);
    }

    private QuestionVO hitToVO(SearchHit<QuestionDocument> hit) {
        QuestionDocument doc = hit.getContent();
        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(doc, vo);

        // Convert List<String> back to String for knowledgePoints and tags
        if (doc.getKnowledgePoints() != null) {
            vo.setKnowledgePoints(String.join(",", doc.getKnowledgePoints()));
        }
        if (doc.getTags() != null) {
            vo.setTags(JSONUtil.toJsonStr(doc.getTags()));
        }

        // Apply highlight to content if available
        var highlights = hit.getHighlightFields();
        if (highlights != null && highlights.containsKey("content")) {
            List<String> fragments = highlights.get("content");
            if (!fragments.isEmpty()) {
                vo.setContent(String.join("...", fragments));
            }
        }

        return vo;
    }
}
