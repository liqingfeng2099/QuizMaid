package com.kanade.backend.mapper;

import com.kanade.backend.model.entity.ErrorBook;
import com.kanade.backend.model.vo.PersonalDimensionVO;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ErrorBookMapper extends BaseMapper<ErrorBook> {

    @Select("SELECT q.knowledgePoints AS dimensionKey, COUNT(*) AS totalCount " +
            "FROM errorBook eb INNER JOIN question q ON eb.questionId = q.id " +
            "WHERE eb.userId = #{userId} AND eb.isDeleted = 0 AND q.isDeleted = 0 " +
            "AND q.knowledgePoints IS NOT NULL AND q.knowledgePoints != '' " +
            "GROUP BY q.knowledgePoints ORDER BY totalCount DESC")
    List<PersonalDimensionVO> errorKnowledgeStats(@Param("userId") Long userId);

    @Select("SELECT CAST(eb.errorType AS CHAR) AS dimensionKey, COUNT(*) AS totalCount " +
            "FROM errorBook eb WHERE eb.userId = #{userId} AND eb.isDeleted = 0 " +
            "GROUP BY eb.errorType")
    List<PersonalDimensionVO> errorTypeStats(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM errorBook WHERE userId = #{userId} AND isDeleted = 0")
    long totalErrors(@Param("userId") Long userId);
}
