-- 1. 用户账号表（修复语法，删除冗余role_name，RBAC不建议直接存角色名）
CREATE TABLE `user_account` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                                `username` varchar(50) NOT NULL COMMENT '登录账号',
                                `password` varchar(100) NOT NULL COMMENT 'BCrypt加密密码',
                                `nickname` varchar(50) NOT NULL COMMENT '昵称',
                                `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
                                `email_verified` tinyint DEFAULT 0 COMMENT '邮箱认证 0-未认证 1-已认证',
                                `oauth_type` varchar(20) DEFAULT NULL COMMENT '第三方登录类型(gitee/github等)',
                                `oauth_openid` varchar(100) DEFAULT NULL COMMENT '第三方openid',
                                `status` tinyint DEFAULT 1 COMMENT '账号状态 1-正常 0-禁用',
                                `role_name` varchar(50) NOT NULL COMMENT '角色名称',
                                `answer_num` int DEFAULT 0 COMMENT '总做题数',
                                `correct_num` int DEFAULT 0 COMMENT '总做对题数',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                `is_deleted` tinyint DEFAULT 0,
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_username` (`username`),
                                UNIQUE KEY `uk_email` (`email`),
                                UNIQUE KEY `uk_oauth` (`oauth_type`,`oauth_openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账号表';

-- 2. 试题主表（无语法错误，保留标签字段）
CREATE TABLE `question` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `question_md5` char(32) NOT NULL COMMENT '题干MD5指纹，用于查重',
                            `type` tinyint NOT NULL COMMENT '题型 1-单选 2-多选 3-填空 4-简答',
                            `subject` varchar(50) NOT NULL COMMENT '学科',
                            `chapter` varchar(100) DEFAULT NULL COMMENT '章节',
                            `difficulty` tinyint NOT NULL COMMENT '难度 1-易 2-中 3-难',
                            `knowledge_points` varchar(500) DEFAULT NULL COMMENT '知识点，逗号分隔',
                            `tags` json DEFAULT NULL COMMENT '题目标签（字符串数组）',
                            `content` text NOT NULL COMMENT '题干内容',
                            `options` json DEFAULT NULL COMMENT '选项JSON',
                            `answer` text NOT NULL COMMENT '标准答案',
                            `analysis` text DEFAULT NULL COMMENT '解析',
                            `creator_id` bigint NOT NULL COMMENT '创建人ID',
                            `status` tinyint DEFAULT 1 COMMENT '状态 1-草稿 2-已发布 3-停用',
                            `correct_count` bigint DEFAULT 0 COMMENT '做对次数',
                            `total_count` bigint DEFAULT 0 COMMENT '做题总次数',
                            `accuracy` decimal(5,2) DEFAULT 0.00 COMMENT '正确率',
                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            `is_deleted` tinyint DEFAULT 0,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_question_md5` (`question_md5`),
                            KEY `idx_creator` (`creator_id`),
                            KEY `idx_type_difficulty` (`type`,`difficulty`),
                            KEY `idx_subject_chapter` (`subject`,`chapter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试题主表';

-- 3. 试卷表（修复空COMMENT）
CREATE TABLE `exam_paper` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
                              `paper_name` varchar(100) NOT NULL COMMENT '试卷名称',
                              `subject` varchar(50) NOT NULL COMMENT '学科',
                              `total_score` int NOT NULL DEFAULT 100 COMMENT '总分',
                              `creator_id` bigint NOT NULL COMMENT '创建人(仅自己可管理)',
                              `status` tinyint DEFAULT 0 COMMENT '状态 0-草稿 1-已发布 2-已归档 3-已停用',
                              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              `is_deleted` tinyint DEFAULT 0,
                              PRIMARY KEY (`id`),
                              KEY `idx_creator` (`creator_id`),
                              KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 4. 试卷试题关联表（修复空COMMENT）
CREATE TABLE `paper_question` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
                                  `paper_id` bigint NOT NULL COMMENT '试卷ID',
                                  `question_id` bigint NOT NULL COMMENT '试题ID',
                                  `question_score` int NOT NULL COMMENT '试题分值',
                                  `sort` int DEFAULT 0 COMMENT '排序',
                                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `is_deleted` tinyint DEFAULT 0,
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_paper_question` (`paper_id`,`question_id`),
                                  KEY `idx_paper_id` (`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷试题关联表';

-- 5. 用户考试记录表（修复空COMMENT）
CREATE TABLE `user_exam_record` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考试记录ID',
                                    `user_id` bigint NOT NULL COMMENT '用户ID',
                                    `paper_id` bigint NOT NULL COMMENT '试卷ID',
                                    `total_score` int DEFAULT 0 COMMENT '试卷总分',
                                    `user_score` int DEFAULT 0 COMMENT '用户得分',
                                    `status` tinyint DEFAULT 0 COMMENT '状态 0-未完成 1-已完成 2-已批改',
                                    `start_time` datetime DEFAULT NULL COMMENT '开始答题时间',
                                    `end_time` datetime DEFAULT NULL COMMENT '交卷时间',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    `is_deleted` tinyint DEFAULT 0,
                                    PRIMARY KEY (`id`),
                                    KEY `idx_user_paper` (`user_id`,`paper_id`),
                                    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户考试记录表';

-- 答题详情表
CREATE TABLE `user_answer_detail` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                      `record_id` bigint NOT NULL COMMENT '考试记录ID',
                                      `paper_id` bigint NOT NULL COMMENT '试卷ID',
                                      `question_id` bigint NOT NULL COMMENT '试题ID',
                                      `question_type` tinyint NOT NULL COMMENT '试题类型',
                                      `user_answer` text NOT NULL COMMENT '用户答案',
                                      `question_score` int NOT NULL COMMENT '试题总分',
                                      `actual_score` int DEFAULT 0 COMMENT '实际得分',
                                      `correct_status` tinyint DEFAULT 0 COMMENT '批改状态 0-待批改 1-正确 2-错误',
                                      `ai_review_msg` text DEFAULT NULL COMMENT 'AI批改评语',
                                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      `is_deleted` tinyint DEFAULT 0,
                                      PRIMARY KEY (`id`),
                                      KEY `idx_record_id` (`record_id`),
                                      KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题详情表';

-- 用户签到表
CREATE TABLE `user_sign` (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                             `user_id` bigint NOT NULL COMMENT '用户ID',
                             `sign_date` date NOT NULL COMMENT '签到日期',
                             `continue_days` int DEFAULT 1 COMMENT '连续签到天数',
                             `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `is_deleted` tinyint DEFAULT 0,
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `uk_user_date` (`user_id`,`sign_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户签到表';

-- 用户做题日统计表
CREATE TABLE `user_answer_stats` (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                     `user_id` bigint NOT NULL COMMENT '用户ID',
                                     `stats_date` date NOT NULL COMMENT '统计日期',
                                     `answer_num` int DEFAULT 0 COMMENT '当日做题数',
                                     `correct_num` int DEFAULT 0 COMMENT '当日做对题数',
                                     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     `is_deleted` tinyint DEFAULT 0,
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_user_date` (`user_id`,`stats_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户做题日统计表';