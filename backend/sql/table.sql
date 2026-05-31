-- ============================================================
-- QuizMaid 数据库建表脚本
-- 最后更新：2026-05-10
-- ============================================================

-- ============================================================
-- 第一部分：原有8张表（无结构变更，仅标注新增字段位置）
-- ============================================================

-- 1. 用户账号表（无新增字段）
CREATE TABLE `userAccount` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                               `username` varchar(50) NOT NULL COMMENT '登录账号',
                               `password` varchar(100) NOT NULL COMMENT 'BCrypt加密密码',
                               `nickname` varchar(50) NOT NULL COMMENT '昵称',
                               `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
                               `emailVerified` tinyint DEFAULT 0 COMMENT '邮箱认证 0-未认证 1-已认证',
                               `oauthType` varchar(20) DEFAULT NULL COMMENT '第三方登录类型(gitee/github等)',
                               `oauthOpenid` varchar(100) DEFAULT NULL COMMENT '第三方openid',
                               `role` varchar(10) NOT NULL DEFAULT 'user' COMMENT '用户角色：user-普通用户 admin-管理员',
                               `status` tinyint DEFAULT 1 COMMENT '账号状态 1-正常 0-禁用',
                               `answerNum` int DEFAULT 0 COMMENT '总做题数',
                               `correctNum` int DEFAULT 0 COMMENT '总做对题数',
                               `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               `isDeleted` tinyint DEFAULT 0,
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `ukUsername` (`username`),
                               UNIQUE KEY `ukEmail` (`email`),
                               UNIQUE KEY `ukOauth` (`oauthType`,`oauthOpenid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账号表';

-- 2. 试题主表
-- 【新增4字段】: discrimination, calcLevel, examFrequency, gradeStageId
CREATE TABLE `question` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `questionMd5` char(32) NOT NULL COMMENT '题干MD5指纹，用于查重',
                            `type` tinyint NOT NULL COMMENT '题型 1-单选 2-多选 3-填空 4-简答',
                            `subject` varchar(50) NULL COMMENT '学科',
                            `chapter` varchar(100) DEFAULT NULL COMMENT '章节',
                            `difficulty` tinyint NULL COMMENT '难度 1-易 2-中 3-难',
                            `knowledgePoints` varchar(500) DEFAULT NULL COMMENT '知识点，逗号分隔',
                            `tags` json DEFAULT NULL COMMENT '题目标签（字符串数组）',
                            `content` text NOT NULL COMMENT '题干内容',
                            `options` json DEFAULT NULL COMMENT '选项JSON',
                            `answer` text NOT NULL COMMENT '标准答案',
                            `analysis` text DEFAULT NULL COMMENT '解析',
                            `creatorId` bigint NOT NULL COMMENT '创建人ID',
                            `status` tinyint DEFAULT 1 COMMENT '状态 1-草稿 2-已发布 3-停用',
                            `correctCount` bigint DEFAULT 0 COMMENT '做对次数',
                            `totalCount` bigint DEFAULT 0 COMMENT '做题总次数',
                            `accuracy` decimal(5,2) DEFAULT 0.00 COMMENT '正确率',
                            `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            -- >>> [2026-05-10 新增] 组卷模块专用字段 <<<
                            `discrimination` tinyint DEFAULT 0 COMMENT '【新增】区分度 1-5',
                            `calcLevel` tinyint DEFAULT 0 COMMENT '【新增】计算量等级 1-3',
                            `examFrequency` int DEFAULT 0 COMMENT '【新增】考频 0-100',
                            `gradeStageId` bigint DEFAULT NULL COMMENT '【新增】学段ID',
                            -- >>> [新增结束] <<<
                            `isDeleted` tinyint DEFAULT 0,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `ukQuestionMd5` (`questionMd5`),
                            KEY `idxCreator` (`creatorId`),
                            KEY `idxTypeDifficulty` (`type`,`difficulty`),
                            KEY `idxSubjectChapter` (`subject`,`chapter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试题主表';

-- 3. 试卷表
-- 【新增5字段】: strategyId, paperType, difficultyRate, duration, exportStatus
CREATE TABLE `examPaper` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
                             `paperName` varchar(100) NOT NULL COMMENT '试卷名称',
                             `subject` varchar(50) NOT NULL COMMENT '学科',
                             `totalScore` int NOT NULL DEFAULT 100 COMMENT '总分',
                             `creatorId` bigint NOT NULL COMMENT '创建人(仅自己可管理)',
                             `status` tinyint DEFAULT 0 COMMENT '状态 0-草稿 1-已发布 2-已归档 3-已停用',
                             `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             -- >>> [2026-05-10 新增] 组卷模块专用字段 <<<
                             `strategyId` bigint DEFAULT NULL COMMENT '【新增】组卷策略ID',
                             `paperType` tinyint DEFAULT 1 COMMENT '【新增】试卷类型 1-手动组卷 2-自动组卷 3-AI组卷',
                             `difficultyRate` decimal(3,2) DEFAULT 0.0 COMMENT '【新增】整体难度系数',
                             `duration` int DEFAULT NULL COMMENT '【新增】答题时长（分钟）',
                             `exportStatus` tinyint DEFAULT 0 COMMENT '【新增】导出状态 0-未导出 1-已导出',
                             -- >>> [新增结束] <<<
                             `isDeleted` tinyint DEFAULT 0,
                             PRIMARY KEY (`id`),
                             KEY `idxCreator` (`creatorId`),
                             KEY `idxStatus` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 4. 试卷试题关联表
-- 【新增1字段】: isAutoAdd
CREATE TABLE `paperQuestion` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
                                 `paperId` bigint NOT NULL COMMENT '试卷ID',
                                 `questionId` bigint NOT NULL COMMENT '试题ID',
                                 `questionScore` int NOT NULL COMMENT '试题分值',
                                 `sort` int DEFAULT 0 COMMENT '排序',
                                 `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 -- >>> [2026-05-10 新增] <<<
                                 `isAutoAdd` tinyint DEFAULT 0 COMMENT '【新增】是否自动组卷添加 0-否 1-是',
                                 -- >>> [新增结束] <<<
                                 `isDeleted` tinyint DEFAULT 0,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `ukPaperQuestion` (`paperId`,`questionId`),
                                 KEY `idxPaperId` (`paperId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷试题关联表';

-- 5. 用户考试记录表
-- 【新增1字段】: paperStrategyId
CREATE TABLE `userExamRecord` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考试记录ID',
                                  `userId` bigint NOT NULL COMMENT '用户ID',
                                  `paperId` bigint NOT NULL COMMENT '试卷ID',
                                  `totalScore` int DEFAULT 0 COMMENT '试卷总分',
                                  `userScore` int DEFAULT 0 COMMENT '用户得分',
                                  `status` tinyint DEFAULT 0 COMMENT '状态 0-未完成 1-已完成 2-已批改',
                                  `startTime` datetime DEFAULT NULL COMMENT '开始答题时间',
                                  `endTime` datetime DEFAULT NULL COMMENT '交卷时间',
                                  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  -- >>> [2026-05-10 新增] <<<
                                  `paperStrategyId` bigint DEFAULT NULL COMMENT '【新增】关联组卷策略ID',
                                  -- >>> [新增结束] <<<
                                  `isDeleted` tinyint DEFAULT 0,
                                  PRIMARY KEY (`id`),
                                  KEY `idxUserPaper` (`userId`,`paperId`),
                                  KEY `idxStatus` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户考试记录表';

-- 6. 答题详情表
-- 【新增1字段】: questionDifficulty
CREATE TABLE `userAnswerDetail` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                    `recordId` bigint NOT NULL COMMENT '考试记录ID',
                                    `paperId` bigint NOT NULL COMMENT '试卷ID',
                                    `questionId` bigint NOT NULL COMMENT '试题ID',
                                    `questionType` tinyint NOT NULL COMMENT '试题类型',
                                    `userAnswer` text NOT NULL COMMENT '用户答案',
                                    `questionScore` int NOT NULL COMMENT '试题总分',
                                    `actualScore` int DEFAULT 0 COMMENT '实际得分',
                                    `correctStatus` tinyint DEFAULT 0 COMMENT '批改状态 0-待批改 1-正确 2-错误',
                                    `aiReviewMsg` text DEFAULT NULL COMMENT 'AI批改评语',
                                    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    -- >>> [2026-05-10 新增] <<<
                                    `questionDifficulty` tinyint DEFAULT 0 COMMENT '【新增】题目难度（冗余）',
                                    -- >>> [新增结束] <<<
                                    `isDeleted` tinyint DEFAULT 0,
                                    PRIMARY KEY (`id`),
                                    KEY `idxRecordId` (`recordId`),
                                    KEY `idxQuestionId` (`questionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题详情表';

-- 7. 用户签到表（无新增字段）
CREATE TABLE `userSign` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `userId` bigint NOT NULL COMMENT '用户ID',
                            `signDate` date NOT NULL COMMENT '签到日期',
                            `continueDays` int DEFAULT 1 COMMENT '连续签到天数',
                            `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `isDeleted` tinyint DEFAULT 0,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `ukUserDate` (`userId`,`signDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户签到表';

-- 8. 用户做题日统计表
-- 【新增1字段】: statsSubjectId
CREATE TABLE `userAnswerStats` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `userId` bigint NOT NULL COMMENT '用户ID',
                                   `statsDate` date NOT NULL COMMENT '统计日期',
                                   `answerNum` int DEFAULT 0 COMMENT '当日做题数',
                                   `correctNum` int DEFAULT 0 COMMENT '当日做对题数',
                                   `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   -- >>> [2026-05-10 新增] <<<
                                   `statsSubjectId` bigint DEFAULT NULL COMMENT '【新增】统计学科ID',
                                   -- >>> [新增结束] <<<
                                   `isDeleted` tinyint DEFAULT 0,
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `ukUserDate` (`userId`,`statsDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户做题日统计表';


-- ============================================================
-- 第二部分：新增表（2026-05-10，组卷模块）
-- ============================================================

-- 9. 【新表】组卷策略表
CREATE TABLE `paperStrategy` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '策略ID',
    `strategyName` varchar(100) NOT NULL COMMENT '策略名称',
    `userId` bigint NOT NULL COMMENT '创建人ID',
    `totalScore` int NOT NULL DEFAULT 100 COMMENT '目标总分',
    `difficultyAvg` tinyint DEFAULT 3 COMMENT '平均难度 1-5',
    `duration` int DEFAULT NULL COMMENT '答题时长（分钟）',
    `questionTypeConfig` json DEFAULT NULL COMMENT '各题型数量/分值配置',
    `difficultyConfig` json DEFAULT NULL COMMENT '各难度占比配置',
    `knowledgePointScope` json DEFAULT NULL COMMENT '知识点范围',
    `isDefault` tinyint DEFAULT 0 COMMENT '是否默认策略 0-否 1-是',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDeleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idxUserId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【新表】组卷策略表';

-- 10. 【新表】组卷策略权重表
CREATE TABLE `strategyWeight` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `strategyId` bigint NOT NULL COMMENT '组卷策略ID',
    `weightType` varchar(50) NOT NULL COMMENT '权重类型: difficulty/accuracy/discrimination/calcLevel/examFrequency/knowledgeCount',
    `weightValue` int NOT NULL DEFAULT 0 COMMENT '权重值 0-100',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDeleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idxStrategyId` (`strategyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【新表】组卷策略权重表';


-- 11. 【新表】推荐反馈记录表
CREATE TABLE `recommendFeedback` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
    `userId` bigint NOT NULL COMMENT '用户ID',
    `questionId` bigint NOT NULL COMMENT '推荐题目ID',
    `knowledgePoints` varchar(500) DEFAULT NULL COMMENT '题目关联知识点',
    `feedback` tinyint NOT NULL COMMENT '反馈 1-已掌握 2-仍困难',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `isDeleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idxUserId` (`userId`),
    KEY `idxFeedback` (`feedback`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='【新表】推荐反馈记录表';


-- ============================================================
-- 第三部分：ALTER TABLE（给已有数据库补字段用）
-- 如果使用上面的完整 CREATE TABLE，则无需执行本部分
-- ============================================================
#
# ALTER TABLE `question`
#     ADD COLUMN `discrimination` tinyint DEFAULT 0 COMMENT '【新增】区分度 1-5',
#     ADD COLUMN `calcLevel` tinyint DEFAULT 0 COMMENT '【新增】计算量等级 1-3',
#     ADD COLUMN `examFrequency` int DEFAULT 0 COMMENT '【新增】考频 0-100',
#     ADD COLUMN `gradeStageId` bigint DEFAULT NULL COMMENT '【新增】学段ID';
#
# ALTER TABLE `examPaper`
#     ADD COLUMN `strategyId` bigint DEFAULT NULL COMMENT '【新增】组卷策略ID',
#     ADD COLUMN `paperType` tinyint DEFAULT 1 COMMENT '【新增】试卷类型',
#     ADD COLUMN `difficultyRate` decimal(3,2) DEFAULT 0.0 COMMENT '【新增】整体难度系数',
#     ADD COLUMN `duration` int DEFAULT NULL COMMENT '【新增】答题时长',
#     ADD COLUMN `exportStatus` tinyint DEFAULT 0 COMMENT '【新增】导出状态';
#
# ALTER TABLE `paperQuestion`
#     ADD COLUMN `isAutoAdd` tinyint DEFAULT 0 COMMENT '【新增】是否自动组卷添加';
#
# ALTER TABLE `userExamRecord`
#     ADD COLUMN `paperStrategyId` bigint DEFAULT NULL COMMENT '【新增】关联组卷策略ID';
#
# ALTER TABLE `userAnswerDetail`
#     ADD COLUMN `questionDifficulty` tinyint DEFAULT 0 COMMENT '【新增】题目难度（冗余）';
#
# ALTER TABLE `userAnswerStats`
#     ADD COLUMN `statsSubjectId` bigint DEFAULT NULL COMMENT '【新增】统计学科ID';
