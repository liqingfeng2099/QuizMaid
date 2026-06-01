package com.kanade.backend.mapper;

import com.kanade.backend.model.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StatisticsMapper {

    @Select("SELECT MAX(uer.userScore) AS maxScore, MIN(uer.userScore) AS minScore, " +
            "AVG(uer.userScore) AS avgScore, COUNT(*) AS totalRecords " +
            "FROM userexamrecord uer " +
            "WHERE uer.paperId = #{paperId} AND uer.status IN (1, 2) AND uer.isDeleted = 0")
    PaperScoreAggregateVO selectPaperScoreAggregate(@Param("paperId") Long paperId);

    @Select("SELECT uer.userScore FROM userexamrecord uer " +
            "WHERE uer.paperId = #{paperId} AND uer.status IN (1, 2) AND uer.isDeleted = 0 " +
            "ORDER BY uer.userScore ASC")
    List<Integer> selectScoresByPaperId(@Param("paperId") Long paperId);

    @Select("SELECT uad.questionType, " +
            "COUNT(*) AS totalCount, " +
            "SUM(CASE WHEN uad.correctStatus = 1 THEN 1 ELSE 0 END) AS correctCount, " +
            "SUM(COALESCE(uad.actualScore, 0)) AS totalActualScore, " +
            "SUM(COALESCE(uad.questionScore, 0)) AS totalQuestionScore " +
            "FROM useranswerdetail uad " +
            "INNER JOIN userexamrecord uer ON uad.recordId = uer.id " +
            "WHERE uad.paperId = #{paperId} AND uer.status IN (1, 2) " +
            "AND uad.isDeleted = 0 AND uer.isDeleted = 0 " +
            "GROUP BY uad.questionType ORDER BY uad.questionType")
    List<QuestionTypeStatVO> selectByQuestionType(@Param("paperId") Long paperId);

    @Select("SELECT q.difficulty, " +
            "COUNT(*) AS totalCount, " +
            "SUM(CASE WHEN uad.correctStatus = 1 THEN 1 ELSE 0 END) AS correctCount, " +
            "SUM(COALESCE(uad.actualScore, 0)) AS totalActualScore, " +
            "SUM(COALESCE(uad.questionScore, 0)) AS totalQuestionScore " +
            "FROM useranswerdetail uad " +
            "INNER JOIN question q ON uad.questionId = q.id " +
            "INNER JOIN userexamrecord uer ON uad.recordId = uer.id " +
            "WHERE uad.paperId = #{paperId} AND uer.status IN (1, 2) " +
            "AND uad.isDeleted = 0 AND q.isDeleted = 0 AND uer.isDeleted = 0 " +
            "GROUP BY q.difficulty ORDER BY q.difficulty")
    List<DifficultyStatVO> selectByDifficulty(@Param("paperId") Long paperId);

    @Select("SELECT q.knowledgePoints AS knowledgePoint, " +
            "COUNT(*) AS totalCount, " +
            "SUM(CASE WHEN uad.correctStatus = 1 THEN 1 ELSE 0 END) AS correctCount, " +
            "SUM(COALESCE(uad.actualScore, 0)) AS totalActualScore, " +
            "SUM(COALESCE(uad.questionScore, 0)) AS totalQuestionScore " +
            "FROM useranswerdetail uad " +
            "INNER JOIN question q ON uad.questionId = q.id " +
            "INNER JOIN userexamrecord uer ON uad.recordId = uer.id " +
            "WHERE uad.paperId = #{paperId} AND uer.status IN (1, 2) " +
            "AND uad.isDeleted = 0 AND q.isDeleted = 0 AND uer.isDeleted = 0 " +
            "AND q.knowledgePoints IS NOT NULL AND q.knowledgePoints != '' " +
            "GROUP BY q.knowledgePoints")
    List<KnowledgePointStatVO> selectByKnowledgePointRaw(@Param("paperId") Long paperId);

    @Select("SELECT uad.questionId AS questionId, " +
            "q.content AS questionContent, q.type AS questionType, q.difficulty, " +
            "q.knowledgePoints AS knowledgePoints, " +
            "COUNT(*) AS wrongCount, " +
            "SUM(COALESCE(uad.questionScore, 0) - COALESCE(uad.actualScore, 0)) AS totalScoreLost " +
            "FROM useranswerdetail uad " +
            "INNER JOIN question q ON uad.questionId = q.id " +
            "INNER JOIN userexamrecord uer ON uad.recordId = uer.id " +
            "WHERE uad.paperId = #{paperId} AND uad.correctStatus = 2 " +
            "AND uer.status IN (1, 2) " +
            "AND uad.isDeleted = 0 AND q.isDeleted = 0 AND uer.isDeleted = 0 " +
            "GROUP BY uad.questionId, q.content, q.type, q.difficulty, q.knowledgePoints " +
            "ORDER BY wrongCount DESC LIMIT #{limit}")
    List<HighFreqWrongQuestionVO> selectHighFreqWrongQuestions(
            @Param("paperId") Long paperId, @Param("limit") int limit);

    @Select("SELECT bucket, COUNT(*) AS count FROM (" +
            "SELECT FLOOR(COALESCE(uer.userScore, 0) / #{bucketSize}) * #{bucketSize} AS bucket " +
            "FROM userexamrecord uer " +
            "WHERE uer.paperId = #{paperId} AND uer.status IN (1, 2) AND uer.isDeleted = 0" +
            ") t GROUP BY bucket ORDER BY bucket ASC")
    List<ScoreDistributionVO> selectScoreDistribution(
            @Param("paperId") Long paperId, @Param("bucketSize") int bucketSize);

    @Select("<script>" +
            "SELECT uer.userId, uer.userScore AS score, " +
            "uer.endTime AS examTime, uer.paperId AS paperId, " +
            "ep.paperName, ep.totalScore " +
            "FROM userexamrecord uer " +
            "INNER JOIN examPaper ep ON uer.paperId = ep.id " +
            "WHERE uer.userId = #{userId} AND uer.status IN (1, 2) " +
            "AND uer.isDeleted = 0 AND ep.isDeleted = 0 " +
            "<if test='subject != null and subject != \"\"'>" +
            "AND ep.subject = #{subject} " +
            "</if>" +
            "ORDER BY uer.endTime DESC " +
            "<if test='limit != null'>LIMIT #{limit}</if>" +
            "</script>")
    List<TrendDataPointVO> selectUserScoreTrend(
            @Param("userId") Long userId,
            @Param("subject") String subject,
            @Param("limit") Integer limit);

    @Select("<script>" +
            "SELECT uer.userId, uer.userScore AS score, " +
            "uer.endTime AS examTime, uer.paperId AS paperId, " +
            "ep.paperName, ep.totalScore " +
            "FROM userexamrecord uer " +
            "INNER JOIN examPaper ep ON uer.paperId = ep.id " +
            "WHERE uer.status IN (1, 2) " +
            "AND uer.isDeleted = 0 AND ep.isDeleted = 0 " +
            "<if test='subject != null and subject != \"\"'>" +
            "AND ep.subject = #{subject} " +
            "</if>" +
            "ORDER BY uer.endTime DESC " +
            "<if test='limit != null'>LIMIT #{limit}</if>" +
            "</script>")
    List<TrendDataPointVO> selectAllUsersScoreTrend(
            @Param("subject") String subject,
            @Param("limit") Integer limit);

    @Select("SELECT DISTINCT ep.id, ep.paperName, ep.subject, " +
            "ep.totalScore, ep.creatorId, " +
            "ep.status, ep.paperType, ep.createTime " +
            "FROM examPaper ep " +
            "INNER JOIN userexamrecord uer ON ep.id = uer.paperId " +
            "WHERE uer.status IN (1, 2) AND uer.isDeleted = 0 AND ep.isDeleted = 0 " +
            "ORDER BY ep.createTime DESC")
    List<PaperStatisticsVO> selectAvailablePapers();
}
