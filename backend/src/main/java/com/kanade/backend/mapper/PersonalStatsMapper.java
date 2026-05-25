package com.kanade.backend.mapper;

import com.kanade.backend.model.vo.PersonalDimensionVO;
import com.kanade.backend.model.vo.PersonalTrendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PersonalStatsMapper {

    @Select("<script>" +
            "SELECT uad.questionType AS dimensionKey, COUNT(*) AS totalCount, " +
            "SUM(CASE WHEN uad.correctStatus = 1 THEN 1 ELSE 0 END) AS correctCount " +
            "FROM useranswerdetail uad " +
            "INNER JOIN userexamrecord uer ON uad.recordId = uer.id " +
            "WHERE uer.userId = #{userId} AND uer.status IN (1,2) " +
            "AND uad.isDeleted = 0 AND uer.isDeleted = 0 " +
            "<if test='startDate != null'>AND uad.createTime >= #{startDate}</if> " +
            "<if test='endDate != null'>AND uad.createTime &lt;= #{endDate}</if> " +
            "<if test='subject != null and subject != \"\"'>AND uad.paperId IN (SELECT id FROM examPaper WHERE subject = #{subject})</if> " +
            "GROUP BY uad.questionType</script>")
    List<PersonalDimensionVO> statsByType(@Param("userId") Long userId,
            @Param("subject") String subject,
            @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>" +
            "SELECT CAST(q.difficulty AS CHAR) AS dimensionKey, COUNT(*) AS totalCount, " +
            "SUM(CASE WHEN uad.correctStatus = 1 THEN 1 ELSE 0 END) AS correctCount " +
            "FROM useranswerdetail uad " +
            "INNER JOIN question q ON uad.questionId = q.id " +
            "INNER JOIN userexamrecord uer ON uad.recordId = uer.id " +
            "WHERE uer.userId = #{userId} AND uer.status IN (1,2) " +
            "AND uad.isDeleted = 0 AND q.isDeleted = 0 AND uer.isDeleted = 0 " +
            "<if test='startDate != null'>AND uad.createTime >= #{startDate}</if> " +
            "<if test='endDate != null'>AND uad.createTime &lt;= #{endDate}</if> " +
            "<if test='subject != null and subject != \"\"'>AND uad.paperId IN (SELECT id FROM examPaper WHERE subject = #{subject})</if> " +
            "GROUP BY q.difficulty ORDER BY q.difficulty</script>")
    List<PersonalDimensionVO> statsByDifficulty(@Param("userId") Long userId,
            @Param("subject") String subject,
            @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>" +
            "SELECT q.knowledgePoints AS dimensionKey, COUNT(*) AS totalCount, " +
            "SUM(CASE WHEN uad.correctStatus = 1 THEN 1 ELSE 0 END) AS correctCount " +
            "FROM useranswerdetail uad " +
            "INNER JOIN question q ON uad.questionId = q.id " +
            "INNER JOIN userexamrecord uer ON uad.recordId = uer.id " +
            "WHERE uer.userId = #{userId} AND uer.status IN (1,2) " +
            "AND uad.isDeleted = 0 AND q.isDeleted = 0 AND uer.isDeleted = 0 " +
            "AND q.knowledgePoints IS NOT NULL AND q.knowledgePoints != '' " +
            "<if test='startDate != null'>AND uad.createTime >= #{startDate}</if> " +
            "<if test='endDate != null'>AND uad.createTime &lt;= #{endDate}</if> " +
            "<if test='subject != null and subject != \"\"'>AND uad.paperId IN (SELECT id FROM examPaper WHERE subject = #{subject})</if> " +
            "GROUP BY q.knowledgePoints</script>")
    List<PersonalDimensionVO> statsByKnowledgeRaw(@Param("userId") Long userId,
            @Param("subject") String subject,
            @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>" +
            "SELECT DATE(uad.createTime) AS period, COUNT(*) AS answerCount, " +
            "SUM(CASE WHEN uad.correctStatus = 1 THEN 1 ELSE 0 END) AS correctCount " +
            "FROM useranswerdetail uad " +
            "INNER JOIN userexamrecord uer ON uad.recordId = uer.id " +
            "WHERE uer.userId = #{userId} AND uer.status IN (1,2) " +
            "AND uad.isDeleted = 0 AND uer.isDeleted = 0 " +
            "AND uad.createTime >= #{startDate} " +
            "<if test='subject != null and subject != \"\"'>AND uad.paperId IN (SELECT id FROM examPaper WHERE subject = #{subject})</if> " +
            "GROUP BY DATE(uad.createTime) ORDER BY period ASC</script>")
    List<PersonalTrendVO> dailyTrend(@Param("userId") Long userId,
            @Param("subject") String subject, @Param("startDate") String startDate);

    @Select("SELECT COUNT(*) FROM useranswerdetail uad " +
            "INNER JOIN userexamrecord uer ON uad.recordId = uer.id " +
            "WHERE uer.userId = #{userId} AND uer.status IN (1,2) " +
            "AND uad.isDeleted = 0 AND uer.isDeleted = 0")
    long totalAnswersByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM useranswerdetail uad " +
            "INNER JOIN userexamrecord uer ON uad.recordId = uer.id " +
            "WHERE uer.userId = #{userId} AND uer.status IN (1,2) " +
            "AND uad.correctStatus = 1 AND uad.isDeleted = 0 AND uer.isDeleted = 0")
    long totalCorrectByUser(@Param("userId") Long userId);
}
