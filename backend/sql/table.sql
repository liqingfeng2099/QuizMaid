-- 1. 用户账号表（驼峰命名，删除role_name，RBAC规范）
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

-- 2. 试题主表（驼峰命名）
CREATE TABLE `question` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `questionMd5` char(32) NOT NULL COMMENT '题干MD5指纹，用于查重',
                            `type` tinyint NOT NULL COMMENT '题型 1-单选 2-多选 3-填空 4-简答',
                            `subject` varchar(50) NOT NULL COMMENT '学科',
                            `chapter` varchar(100) DEFAULT NULL COMMENT '章节',
                            `difficulty` tinyint NOT NULL COMMENT '难度 1-易 2-中 3-难',
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
                            `isDeleted` tinyint DEFAULT 0,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `ukQuestionMd5` (`questionMd5`),
                            KEY `idxCreator` (`creatorId`),
                            KEY `idxTypeDifficulty` (`type`,`difficulty`),
                            KEY `idxSubjectChapter` (`subject`,`chapter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试题主表';

-- 3. 试卷表（驼峰命名）
CREATE TABLE `examPaper` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
                             `paperName` varchar(100) NOT NULL COMMENT '试卷名称',
                             `subject` varchar(50) NOT NULL COMMENT '学科',
                             `totalScore` int NOT NULL DEFAULT 100 COMMENT '总分',
                             `creatorId` bigint NOT NULL COMMENT '创建人(仅自己可管理)',
                             `status` tinyint DEFAULT 0 COMMENT '状态 0-草稿 1-已发布 2-已归档 3-已停用',
                             `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             `isDeleted` tinyint DEFAULT 0,
                             PRIMARY KEY (`id`),
                             KEY `idxCreator` (`creatorId`),
                             KEY `idxStatus` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 4. 试卷试题关联表（驼峰命名）
CREATE TABLE `paperQuestion` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
                                 `paperId` bigint NOT NULL COMMENT '试卷ID',
                                 `questionId` bigint NOT NULL COMMENT '试题ID',
                                 `questionScore` int NOT NULL COMMENT '试题分值',
                                 `sort` int DEFAULT 0 COMMENT '排序',
                                 `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `isDeleted` tinyint DEFAULT 0,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `ukPaperQuestion` (`paperId`,`questionId`),
                                 KEY `idxPaperId` (`paperId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷试题关联表';

-- 5. 用户考试记录表（驼峰命名）
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
                                  `isDeleted` tinyint DEFAULT 0,
                                  PRIMARY KEY (`id`),
                                  KEY `idxUserPaper` (`userId`,`paperId`),
                                  KEY `idxStatus` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户考试记录表';

-- 6. 答题详情表（驼峰命名）
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
                                    `isDeleted` tinyint DEFAULT 0,
                                    PRIMARY KEY (`id`),
                                    KEY `idxRecordId` (`recordId`),
                                    KEY `idxQuestionId` (`questionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题详情表';

-- 7. 用户签到表（驼峰命名）
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

-- 8. 用户做题日统计表（驼峰命名）
CREATE TABLE `userAnswerStats` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `userId` bigint NOT NULL COMMENT '用户ID',
                                   `statsDate` date NOT NULL COMMENT '统计日期',
                                   `answerNum` int DEFAULT 0 COMMENT '当日做题数',
                                   `correctNum` int DEFAULT 0 COMMENT '当日做对题数',
                                   `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   `isDeleted` tinyint DEFAULT 0,
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `ukUserDate` (`userId`,`statsDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户做题日统计表';