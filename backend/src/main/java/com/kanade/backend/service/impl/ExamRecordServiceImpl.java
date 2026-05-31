package com.kanade.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.kanade.backend.exception.BusinessException;
import com.kanade.backend.exception.ErrorCode;
import com.kanade.backend.mapper.*;
import com.kanade.backend.model.entity.*;
import com.kanade.backend.model.vo.*;
import com.kanade.backend.service.ErrorBookService;
import com.kanade.backend.service.ExamRecordService;
import com.kanade.backend.service.QuestionService;
import com.kanade.backend.service.UserService;
import com.kanade.backend.config.RabbitMQConfig;
import com.kanade.backend.model.dto.QuestionCorrectionMessageDTO;
import com.kanade.backend.model.entity.User;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamRecordServiceImpl extends ServiceImpl<UserexamrecordMapper, Userexamrecord>
        implements ExamRecordService {

    private final ExamPaperMapper examPaperMapper;
    private final PaperquestionMapper paperquestionMapper;
    private final UseranswerdetailMapper useranswerdetailMapper;
    private final UserMapper userMapper;
    private final QuestionService questionService;
    private final ErrorBookService errorBookService;
    private final UserService userService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public ExamRecordVO startExam(Long paperId) {
        Long userId = StpUtil.getLoginIdAsLong();
        ExamPaper paper = examPaperMapper.selectOneById(paperId);
        if (paper == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试卷不存在");
        if (paper.getStatus() == null || paper.getStatus() != 1)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅已发布试卷可考试");

        QueryWrapper ongoingQw = QueryWrapper.create()
                .eq("userId", userId).eq("paperId", paperId).eq("status", 0);
        Userexamrecord ongoing = this.getOne(ongoingQw);
        if (ongoing != null) {
            return buildExamRecordVO(ongoing, paper);
        }

        QueryWrapper pqQw = QueryWrapper.create().eq("paperId", paperId).orderBy("sort", true);
        List<Paperquestion> pqList = paperquestionMapper.selectListByQuery(pqQw);
        if (pqList.isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷无题目");

        Userexamrecord record = new Userexamrecord();
        record.setUserId(userId);
        record.setPaperId(paperId);
        record.setTotalScore(paper.getTotalScore());
        record.setUserScore(0);
        record.setStatus(0);
        record.setStartTime(LocalDateTime.now());
        this.save(record);

        List<Long> qids = pqList.stream().map(Paperquestion::getQuestionId).toList();
        Map<Long, Question> qMap = questionService.listByIds(qids).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        LocalDateTime now = LocalDateTime.now();
        for (Paperquestion pq : pqList) {
            Question q = qMap.get(pq.getQuestionId());
            Useranswerdetail detail = new Useranswerdetail();
            detail.setRecordId(record.getId());
            detail.setPaperId(paperId);
            detail.setQuestionId(pq.getQuestionId());
            detail.setQuestionType(q != null ? q.getType() : null);
            detail.setQuestionScore(pq.getQuestionScore());
            detail.setUserAnswer("");
            detail.setActualScore(0);
            detail.setCorrectStatus(0);
            detail.setCreateTime(now);
            detail.setUpdateTime(now);
            useranswerdetailMapper.insert(detail);
        }

        return buildExamRecordVO(record, paper);
    }

    @Override
    @Transactional
    public ExamResultVO submitExam(Long recordId, Map<Long, String> answers) {
        Long userId = StpUtil.getLoginIdAsLong();
        Userexamrecord record = this.getById(recordId);
        if (record == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "考试记录不存在");
        if (!record.getUserId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权操作");
        if (record.getStatus() != null && record.getStatus() != 0)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "考试已提交");

        QueryWrapper detailQw = QueryWrapper.create().eq("recordId", recordId);
        List<Useranswerdetail> details = useranswerdetailMapper.selectListByQuery(detailQw);
        List<Long> qids = details.stream().map(Useranswerdetail::getQuestionId).toList();
        List<Question> questions = questionService.listByIds(qids);
        Map<Long, Question> qMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        int totalScore = 0;
        int wrongCount = 0;
        List<Long> subjectiveDetailIds = new ArrayList<>();
        for (Useranswerdetail detail : details) {
            String userAnswer = answers.getOrDefault(detail.getQuestionId(), "");
            detail.setUserAnswer(userAnswer != null ? userAnswer : "");
            detail.setQuestionType(qMap.containsKey(detail.getQuestionId()) ?
                    qMap.get(detail.getQuestionId()).getType() : null);

            Question q = qMap.get(detail.getQuestionId());
            if (q != null && q.getType() != null && q.getType() != 4) {
                boolean correct = gradeObjective(q.getType(), userAnswer, q.getAnswer());
                detail.setCorrectStatus(correct ? 1 : 2);
                detail.setActualScore(correct ? detail.getQuestionScore() : 0);
            } else {
                detail.setCorrectStatus(0);
                detail.setActualScore(0);
                if (q != null && q.getType() != null && q.getType() == 4) {
                    subjectiveDetailIds.add(detail.getId());
                }
            }
            useranswerdetailMapper.update(detail);
            totalScore += detail.getActualScore() != null ? detail.getActualScore() : 0;

            // 更新题目统计 (A5)
            if (q != null) {
                updateQuestionStats(q, detail.getCorrectStatus());
            }

            // 错题同步
            if (detail.getCorrectStatus() != null && detail.getCorrectStatus() == 2) {
                wrongCount++;
                try { errorBookService.syncFromExam(userId, detail.getQuestionId()); } catch (Exception e) {
                    log.warn("[错题同步] 同步失败: questionId={}", detail.getQuestionId(), e);
                }
            }
        }

        // 更新每日做题数 (热力图数据)
        userService.addUserQuestionCount(userId, details.size());

        // 更新考试记录
        record.setEndTime(LocalDateTime.now());
        record.setUserScore(totalScore);
        boolean allGraded = details.stream().allMatch(d ->
                d.getCorrectStatus() != null && d.getCorrectStatus() != 0);
        record.setStatus(allGraded ? 2 : 1);
        this.updateById(record);

        // 更新用户答题计数 (B1)
        updateUserAnswerStats(userId, details.size(), details.size() - wrongCount);

        // 事务提交后发送AI批改消息，避免Consumer读到未提交的userAnswer
        if (!subjectiveDetailIds.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (Long detailId : subjectiveDetailIds) {
                        QuestionCorrectionMessageDTO msg = new QuestionCorrectionMessageDTO();
                        msg.setDetailId(detailId);
                        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_CORRECTION,
                                RabbitMQConfig.ROUTING_KEY_CORRECTION, msg);
                    }
                }
            });
        }

        return getExamResult(recordId);
    }

    @Override
    @Transactional
    public ExamResultVO autoSubmit(Long recordId) {
        QueryWrapper detailQw = QueryWrapper.create().eq("recordId", recordId);
        List<Useranswerdetail> details = useranswerdetailMapper.selectListByQuery(detailQw);
        Map<Long, String> answers = new HashMap<>();
        for (Useranswerdetail d : details) {
            answers.put(d.getQuestionId(), d.getUserAnswer() != null ? d.getUserAnswer() : "");
        }
        return submitExam(recordId, answers);
    }

    @Override
    public ExamResultVO getExamResult(Long recordId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Userexamrecord record = this.getById(recordId);
        if (record == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "考试记录不存在");
        if (!record.getUserId().equals(userId) && !StpUtil.hasRole("admin"))
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权查看");

        ExamPaper paper = examPaperMapper.selectOneById(record.getPaperId());
        ExamResultVO vo = new ExamResultVO();
        vo.setRecordId(record.getId());
        vo.setPaperId(record.getPaperId());
        vo.setPaperName(paper != null ? paper.getPaperName() : "");
        vo.setSubject(paper != null ? paper.getSubject() : "");
        vo.setTotalScore(record.getTotalScore());
        vo.setUserScore(record.getUserScore());
        vo.setStatus(record.getStatus());
        vo.setStartTime(record.getStartTime());
        vo.setEndTime(record.getEndTime());
        if (record.getStartTime() != null && record.getEndTime() != null) {
            vo.setUsedSeconds(Duration.between(record.getStartTime(), record.getEndTime()).getSeconds());
        }

        QueryWrapper detailQw = QueryWrapper.create().eq("recordId", recordId);
        List<Useranswerdetail> details = useranswerdetailMapper.selectListByQuery(detailQw);
        List<Long> qids = details.stream().map(Useranswerdetail::getQuestionId).toList();

        List<Question> questions = questionService.listByIds(qids);
        Map<Long, Question> qMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));
        QueryWrapper pqQw = QueryWrapper.create().eq("paperId", record.getPaperId());
        List<Paperquestion> pqList = paperquestionMapper.selectListByQuery(pqQw);
        Map<Long, Paperquestion> pqMap = pqList.stream()
                .collect(Collectors.toMap(Paperquestion::getQuestionId, pq -> pq));

        int correct = 0, wrong = 0, pending = 0;
        List<ExamQuestionItem> items = new ArrayList<>();
        for (Useranswerdetail d : details) {
            ExamQuestionItem item = new ExamQuestionItem();
            item.setQuestionId(d.getQuestionId());
            item.setUserAnswer(d.getUserAnswer());
            item.setCorrectStatus(d.getCorrectStatus());
            item.setActualScore(d.getActualScore());
            Question q = qMap.get(d.getQuestionId());
            if (q != null) {
                item.setType(q.getType());
                item.setContent(q.getContent());
                item.setOptions(q.getOptions());
                item.setCorrectAnswer(q.getAnswer());
                item.setAnalysis(q.getAnalysis());
            }
            Paperquestion pq = pqMap.get(d.getQuestionId());
            if (pq != null) {
                item.setScore(pq.getQuestionScore());
                item.setSort(pq.getSort());
            }
            items.add(item);
            if (d.getCorrectStatus() != null) {
                if (d.getCorrectStatus() == 1) correct++;
                else if (d.getCorrectStatus() == 2) wrong++;
                else pending++;
            } else pending++;
        }
        items.sort(Comparator.comparing(ExamQuestionItem::getSort, Comparator.nullsLast(Comparator.naturalOrder())));
        vo.setQuestions(items);
        vo.setTotalQuestions(items.size());
        vo.setCorrectCount(correct);
        vo.setWrongCount(wrong);
        vo.setPendingCount(pending);
        return vo;
    }

    @Override
    public List<ExamRecordVO> getUserExamRecords(Long userId, Long paperId) {
        QueryWrapper qw = QueryWrapper.create()
                .eq("userId", userId).orderBy("createTime", false);
        if (paperId != null) qw.eq("paperId", paperId);
        List<Userexamrecord> records = this.list(qw);
        Map<Long, ExamPaper> paperMap = new HashMap<>();
        return records.stream().map(r -> {
            ExamPaper paper = paperMap.computeIfAbsent(r.getPaperId(),
                    id -> examPaperMapper.selectOneById(id));
            return buildExamRecordVO(r, paper);
        }).collect(Collectors.toList());
    }

    @Override
    public ExamRecordVO getOngoingExam(Long paperId) {
        Long userId = StpUtil.getLoginIdAsLong();
        QueryWrapper qw = QueryWrapper.create()
                .eq("userId", userId).eq("paperId", paperId).eq("status", 0);
        Userexamrecord record = this.getOne(qw);
        if (record == null) return null;
        ExamPaper paper = examPaperMapper.selectOneById(paperId);
        return buildExamRecordVO(record, paper);
    }

    // ========== A5: 更新题目统计 ==========

    private void updateQuestionStats(Question q, Integer correctStatus) {
        if (q == null || correctStatus == null || correctStatus == 0) return;
        try {
            long newTotal = (q.getTotalCount() != null ? q.getTotalCount() : 0) + 1;
            long newCorrect = (q.getCorrectCount() != null ? q.getCorrectCount() : 0)
                    + (correctStatus == 1 ? 1 : 0);
            BigDecimal newAccuracy = newTotal > 0
                    ? BigDecimal.valueOf(newCorrect).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(newTotal), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            q.setTotalCount(newTotal);
            q.setCorrectCount(newCorrect);
            q.setAccuracy(newAccuracy);
            questionService.updateById(q);

            // 难度自动校准 (A6): 正确率偏离预期≥15%时调整
            calibrateDifficulty(q, newAccuracy);
        } catch (Exception e) {
            log.warn("[题目统计] 更新失败: questionId={}", q.getId(), e);
        }
    }

    private void calibrateDifficulty(Question q, BigDecimal actualAccuracy) {
        if (q.getDifficulty() == null || actualAccuracy == null) return;
        int currentDiff = q.getDifficulty();
        double acc = actualAccuracy.doubleValue();

        // 期望: 难度1→正确率≥85%, 难度2→55%-85%, 难度3→25%-55%, 难度4→10%-25%, 难度5→≤10%
        int expectedDiff = currentDiff;
        if (acc >= 90 && currentDiff >= 2) expectedDiff = currentDiff - 1;
        else if (acc >= 85 && currentDiff >= 3) expectedDiff = currentDiff - 1;
        else if (acc < 25 && currentDiff <= 3) expectedDiff = currentDiff + 1;
        else if (acc < 15 && currentDiff <= 2) expectedDiff = currentDiff + 1;

        if (expectedDiff != currentDiff) {
            log.info("[难度校准] questionId={} accuracy={} difficulty: {}→{}",
                    q.getId(), acc, currentDiff, expectedDiff);
            q.setDifficulty(expectedDiff);
            questionService.updateById(q);
        }
    }

    // ========== B1: 更新用户答题计数 ==========

    private void updateUserAnswerStats(Long userId, int answered, int correct) {
        try {
            User user = userMapper.selectOneById(userId);
            if (user != null) {
                Long newAnswerNum = (long)(user.getAnswerNum() != null ? user.getAnswerNum() : 0) + answered;
                Long newCorrectNum = (long)(user.getCorrectNum() != null ? user.getCorrectNum() : 0) + correct;
                user.setAnswerNum(newAnswerNum.intValue());
                user.setCorrectNum(newCorrectNum.intValue());
                userMapper.update(user);
            }
        } catch (Exception e) {
            log.warn("[用户统计] 更新失败: userId={}", userId, e);
        }
    }

    // ========== 私有辅助 ==========

    private ExamRecordVO buildExamRecordVO(Userexamrecord record, ExamPaper paper) {
        ExamRecordVO vo = new ExamRecordVO();
        vo.setRecordId(record.getId());
        vo.setPaperId(record.getPaperId());
        vo.setPaperName(paper != null ? paper.getPaperName() : "");
        vo.setTotalScore(record.getTotalScore());
        vo.setUserScore(record.getUserScore());
        vo.setStatus(record.getStatus());
        vo.setStartTime(record.getStartTime());
        vo.setEndTime(record.getEndTime());
        if (paper != null && paper.getDuration() != null) {
            vo.setDurationText(paper.getDuration() + "分钟");
            if (record.getStartTime() != null && record.getStatus() != null && record.getStatus() == 0) {
                long elapsed = Duration.between(record.getStartTime(), LocalDateTime.now()).getSeconds();
                long total = paper.getDuration() * 60L;
                vo.setRemainingSeconds(Math.max(0, total - elapsed));
            }
        }

        QueryWrapper pqQw = QueryWrapper.create().eq("paperId", record.getPaperId()).orderBy("sort", true);
        List<Paperquestion> pqList = paperquestionMapper.selectListByQuery(pqQw);
        List<Long> qids = pqList.stream().map(Paperquestion::getQuestionId).toList();
        List<Question> questions = questionService.listByIds(qids);
        Map<Long, Question> qMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        Map<Long, String> savedAnswers = new HashMap<>();
        if (record.getId() != null) {
            QueryWrapper dq = QueryWrapper.create().eq("recordId", record.getId());
            List<Useranswerdetail> details = useranswerdetailMapper.selectListByQuery(dq);
            for (Useranswerdetail d : details) {
                savedAnswers.put(d.getQuestionId(), d.getUserAnswer());
            }
        }

        List<ExamQuestionItem> items = new ArrayList<>();
        for (Paperquestion pq : pqList) {
            ExamQuestionItem item = new ExamQuestionItem();
            item.setQuestionId(pq.getQuestionId());
            item.setScore(pq.getQuestionScore());
            item.setSort(pq.getSort());
            Question q = qMap.get(pq.getQuestionId());
            if (q != null) {
                item.setType(q.getType());
                item.setContent(q.getContent());
                item.setOptions(q.getOptions());
                item.setUserAnswer(savedAnswers.getOrDefault(pq.getQuestionId(), ""));
            }
            items.add(item);
        }
        items.sort(Comparator.comparing(ExamQuestionItem::getSort, Comparator.nullsLast(Comparator.naturalOrder())));
        vo.setQuestions(items);
        vo.setTotalQuestions(items.size());
        return vo;
    }

    private boolean gradeObjective(Integer type, String userAnswer, String correctAnswer) {
        if (userAnswer == null) userAnswer = "";
        if (correctAnswer == null) correctAnswer = "";
        String ua = userAnswer.trim();
        String ca = correctAnswer.trim();

        if (type == null) return false;
        if (type == 1) return ua.equalsIgnoreCase(ca);
        if (type == 2) return sortChars(ua).equalsIgnoreCase(sortChars(ca));
        if (type == 3) return ua.replaceAll("\\s+", "").equalsIgnoreCase(ca.replaceAll("\\s+", ""));
        return false;
    }

    private String sortChars(String s) {
        if (s == null || s.isEmpty()) return "";
        char[] chars = s.replaceAll("[,\\s]", "").toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }
}
