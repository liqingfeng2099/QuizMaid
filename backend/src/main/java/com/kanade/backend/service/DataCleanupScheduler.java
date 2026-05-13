package com.kanade.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 数据生命周期管理：定期清理过期数据
 * - 用户注销后试卷数据留存90天
 * - 考试记录留存180天
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class DataCleanupScheduler {

    private final DataSource dataSource;

    /** 每天凌晨3点执行 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredData() {
        log.info("[数据清理] 开始执行过期数据清理...");

        // 清理超过90天的已删除试卷（创建人已注销的场景）
        int deletedPapers = cleanupDeletedPapers();
        log.info("[数据清理] 清理过期试卷: {} 条", deletedPapers);

        // 清理超过180天的考试记录
        int deletedRecords = cleanupOldExamRecords();
        log.info("[数据清理] 清理过期考试记录: {} 条", deletedRecords);

        // 清理超过180天的答题详情
        int deletedDetails = cleanupOldAnswerDetails();
        log.info("[数据清理] 清理过期答题详情: {} 条", deletedDetails);

        // 清理超过90天的错题数据
        int deletedErrors = cleanupOldErrorBooks();
        log.info("[数据清理] 清理过期错题数据: {} 条", deletedErrors);

        log.info("[数据清理] 过期数据清理完成");
    }

    private int cleanupDeletedPapers() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            String sql = "UPDATE exam_paper SET is_deleted = 1 WHERE is_deleted = 1 " +
                         "AND update_time < DATE_SUB(NOW(), INTERVAL 90 DAY)";
            return stmt.executeUpdate(sql);
        } catch (Exception e) {
            log.error("[数据清理] 清理过期试卷失败", e);
            return 0;
        }
    }

    private int cleanupOldExamRecords() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            String sql = "DELETE FROM userexamrecord WHERE create_time < DATE_SUB(NOW(), INTERVAL 180 DAY)";
            return stmt.executeUpdate(sql);
        } catch (Exception e) {
            log.error("[数据清理] 清理过期考试记录失败", e);
            return 0;
        }
    }

    private int cleanupOldAnswerDetails() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            String sql = "DELETE FROM useranswerdetail WHERE create_time < DATE_SUB(NOW(), INTERVAL 180 DAY)";
            return stmt.executeUpdate(sql);
        } catch (Exception e) {
            log.error("[数据清理] 清理过期答题详情失败", e);
            return 0;
        }
    }

    private int cleanupOldErrorBooks() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            // 清理逻辑删除且超过90天的错题
            String sql = "DELETE FROM error_book WHERE is_deleted = 1 " +
                         "AND update_time < DATE_SUB(NOW(), INTERVAL 90 DAY)";
            int count = stmt.executeUpdate(sql);
            // 清理关联的错题分组和备注
            stmt.executeUpdate("DELETE FROM error_book_group WHERE is_deleted = 1 " +
                    "AND update_time < DATE_SUB(NOW(), INTERVAL 90 DAY)");
            stmt.executeUpdate("DELETE FROM error_book_note WHERE is_deleted = 1 " +
                    "AND update_time < DATE_SUB(NOW(), INTERVAL 90 DAY)");
            return count;
        } catch (Exception e) {
            log.error("[数据清理] 清理过期错题数据失败", e);
            return 0;
        }
    }
}
