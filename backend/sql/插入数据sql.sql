-- ============================================================
-- QuizMaid 测试数据插入脚本
-- 生成日期：2026-05-11
-- 说明：除 userAccount 外所有表先删后插，每表5条合法数据
--       question 表 20 条，覆盖多学科/多题型/多难度
--       含新增表自动建表（如不存在则创建）
-- ============================================================

-- ============================================================
-- 第〇步：补充建表（先删旧再建新，确保列名匹配）
-- ============================================================
DROP TABLE IF EXISTS `aipaperchat`;
DROP TABLE IF EXISTS `papershare`;
DROP TABLE IF EXISTS `systemnotification`;
DROP TABLE IF EXISTS `errorbook`;
DROP TABLE IF EXISTS `errorbookgroup`;
DROP TABLE IF EXISTS `errorbooknote`;
DROP TABLE IF EXISTS `errorbookgroupitem`;
DROP TABLE IF EXISTS `exporttemplate`;
DROP TABLE IF EXISTS `recommendfeedback`;
DROP TABLE IF EXISTS `errorbookexportlog`;

CREATE TABLE `aipaperchat` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `userId` bigint NOT NULL,
    `paperId` bigint DEFAULT NULL,
    `strategyId` bigint DEFAULT NULL,
    `sessionRound` int DEFAULT 1,
    `chatContent` text NOT NULL,
    `aiResponse` text,
    `status` tinyint DEFAULT 0,
    `retryCount` tinyint DEFAULT 0,
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDeleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`), KEY `idxUserId` (`userId`), KEY `idxPaperId` (`paperId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI组卷对话记录表';

CREATE TABLE `papershare` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `paperId` bigint NOT NULL, `ownerId` bigint NOT NULL,
    `targetUserId` bigint DEFAULT NULL, `targetGroupId` bigint DEFAULT NULL,
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDeleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`), KEY `idxPaperId` (`paperId`), KEY `idxOwnerId` (`ownerId`), KEY `idxTargetUser` (`targetUserId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷分享表';

CREATE TABLE `systemnotification` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `userId` bigint NOT NULL, `title` varchar(200) NOT NULL, `content` text,
    `type` tinyint DEFAULT 1, `isRead` tinyint DEFAULT 0, `link` varchar(500) DEFAULT NULL,
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `isDeleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`), KEY `idxUserId` (`userId`), KEY `idxRead` (`isRead`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';

CREATE TABLE `errorbook` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `userId` bigint NOT NULL, `questionId` bigint NOT NULL,
    `errorType` tinyint DEFAULT 1, `reviewStatus` tinyint DEFAULT 0, `errorCount` int DEFAULT 1,
    `firstErrorTime` datetime NOT NULL, `lastErrorTime` datetime NOT NULL,
    `isArchived` tinyint DEFAULT 0,
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDeleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`), UNIQUE KEY `ukUserQuestion` (`userId`,`questionId`),
    KEY `idxUserId` (`userId`), KEY `idxErrorType` (`errorType`), KEY `idxReviewStatus` (`reviewStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本主表';

CREATE TABLE `errorbookgroup` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `userId` bigint NOT NULL, `groupName` varchar(50) NOT NULL,
    `description` varchar(200) DEFAULT NULL, `sort` int DEFAULT 0,
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDeleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`), KEY `idxUserId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题分组表';

CREATE TABLE `errorbooknote` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `errorBookId` bigint NOT NULL, `userId` bigint NOT NULL,
    `noteType` tinyint DEFAULT 1, `content` text, `imageUrl` varchar(500) DEFAULT NULL,
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDeleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`), KEY `idxErrorBookId` (`errorBookId`), KEY `idxUserId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题备注表';

CREATE TABLE `errorbookgroupitem` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `groupId` bigint NOT NULL, `errorBookId` bigint NOT NULL,
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `isDeleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`), UNIQUE KEY `ukGroupError` (`groupId`,`errorBookId`),
    KEY `idxGroupId` (`groupId`), KEY `idxErrorBookId` (`errorBookId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题分组成员表';

CREATE TABLE `exporttemplate` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `templateName` varchar(100) NOT NULL, `userId` bigint NOT NULL,
    `exportType` varchar(20) NOT NULL, `config` text,
    `isDefault` tinyint DEFAULT 0,
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDeleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`), KEY `idxUserId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出模板表';

CREATE TABLE `errorbookexportlog` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `userId` bigint NOT NULL, `exportType` varchar(20) NOT NULL,
    `exportStatus` tinyint DEFAULT 0, `fileName` varchar(200) DEFAULT NULL,
    `exportPath` varchar(500) DEFAULT NULL, `exportConfig` text,
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDeleted` tinyint DEFAULT 0,
    PRIMARY KEY (`id`), KEY `idxUserId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题集导出日志表';

CREATE TABLE `recommendfeedback` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐反馈记录表';

-- ============================================================
-- 第一步：清除所有表数据（userAccount 除外）
-- 注意：先删子表再删父表，避免外键约束冲突
-- ============================================================
DELETE FROM recommendfeedback;
DELETE FROM errorbookgroupitem;
DELETE FROM errorbooknote;
DELETE FROM errorbookexportlog;
DELETE FROM exporttemplate;
DELETE FROM errorbookgroup;
DELETE FROM errorbook;
DELETE FROM systemnotification;
DELETE FROM papershare;
DELETE FROM aipaperchat;
DELETE FROM strategyweight;
DELETE FROM paperstrategy;
DELETE FROM useranswerstats;
DELETE FROM useranswerdetail;
DELETE FROM userexamrecord;
DELETE FROM paperquestion;
DELETE FROM exampaper;
DELETE FROM usersign;
DELETE FROM question;

-- ============================================================
-- 第二步：试题 question（20条，覆盖4学科×4题型×3难度）
-- ============================================================
INSERT INTO question (id, questionMd5, type, subject, chapter, difficulty, knowledgePoints, tags, content, options, answer, analysis, creatorId, status, correctCount, totalCount, accuracy, discrimination, calcLevel, examFrequency, gradeStageId, isDeleted) VALUES
-- === 数学-单选题 ===
(1,  'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6', 1, '数学', '二次函数', 1,
 '顶点坐标,对称轴',
 '["中考","基础"]',
 '二次函数 y = x² - 4x + 3 的顶点坐标是？',
 '[{"key":"A","value":"(2, -1)"},{"key":"B","value":"(2, 1)"},{"key":"C","value":"(-2, -1)"},{"key":"D","value":"(1, 2)"}]',
 'A', '由 y = (x-2)² - 1 得顶点坐标为 (2, -1)',
 2, 2, 15, 20, 75.00, 3, 1, 60, 3, 0),

(2,  'b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7', 1, '数学', '概率统计', 2,
 '古典概型,排列组合',
 '["月考"]',
 '从1,2,3,4,5中任取2个不同的数，和为偶数的概率是？',
 '[{"key":"A","value":"0.2"},{"key":"B","value":"0.4"},{"key":"C","value":"0.6"},{"key":"D","value":"0.8"}]',
 'B', '总数C(5,2)=10，和为偶数需两奇或两偶，(3,5)取+C(2,2)=4，4/10=0.4',
 2, 2, 10, 16, 62.50, 4, 2, 35, 3, 0),

-- === 数学-多选题 ===
(3,  'c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8', 2, '数学', '集合', 2,
 '并集,交集,补集',
 '["高考","易错"]',
 '已知全集 U={1,2,3,4,5}，A={1,2,3}，B={2,3,4}，则下列正确的是？',
 '[{"key":"A","value":"A∩B={2,3}"},{"key":"B","value":"A∪B={1,2,3,4,5}"},{"key":"C","value":"∁ᵤA={4,5}"},{"key":"D","value":"以上都对"}]',
 'AC', 'A∩B={2,3}正确，A∪B={1,2,3,4}≠全集故B错误，∁ᵤA={4,5}正确，选AC',
 2, 2, 8, 18, 44.44, 4, 2, 45, 3, 0),

(4,  'd4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9', 2, '数学', '数列', 3,
 '等差数列,等比数列,通项公式',
 '["高考","压轴"]',
 '关于数列，下列说法正确的有？',
 '[{"key":"A","value":"等差数列的通项公式为 aₙ=a₁+(n-1)d"},{"key":"B","value":"等比数列的前n项和 Sₙ=a₁(1-qⁿ)/(1-q)（q≠1）"},{"key":"C","value":"常数列既是等差又是等比数列"},{"key":"D","value":"斐波那契数列是等比数列"}]',
 'AB', 'AB正确；常数列(非零)才是等比，all-0常数不是，C不严谨；斐波那契不是等比，D错',
 2, 2, 5, 14, 35.71, 5, 3, 30, 3, 0),

-- === 数学-填空题 ===
(5,  'e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0', 3, '数学', '三角函数', 2,
 '正弦定理,余弦定理',
 '["月考","计算"]',
 '在△ABC中，a=3，b=4，∠C=60°，则c=______。',
 NULL,
 '√13', '由余弦定理 c²=a²+b²-2ab·cosC=9+16-24×0.5=13，c=√13',
 2, 2, 12, 22, 54.55, 3, 2, 35, 3, 0),

(6,  'f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1', 3, '数学', '不等式', 2,
 '一元二次不等式,区间',
 '["期中"]',
 '不等式 x² - 5x + 6 < 0 的解集为______。',
 NULL,
 '(2, 3)', 'x²-5x+6=(x-2)(x-3)<0，解集为(2,3)',
 2, 2, 16, 20, 80.00, 2, 1, 50, 3, 0),

-- === 数学-简答题 ===
(7,  'a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2', 4, '数学', '导数', 3,
 '导数应用,单调性,极值',
 '["高考","压轴"]',
 '已知函数 f(x)=x³-3x²+2，求 f(x) 的单调区间和极值。',
 NULL,
 '增区间(-∞,0)和(2,+∞)，减区间(0,2)；极大值f(0)=2，极小值f(2)=-2',
 'f\'(x)=3x²-6x=3x(x-2)，令f\'(x)=0得x=0或x=2。列表分析符号变化即得。',
 2, 2, 6, 15, 40.00, 5, 3, 25, 3, 0),

(8,  'b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3', 4, '数学', '解析几何', 3,
 '椭圆,离心率,标准方程',
 '["高考","综合"]',
 '已知椭圆 x²/a² + y²/b² = 1（a>b>0）的离心率为 √3/2，且过点(2,1)，求椭圆的标准方程。',
 NULL,
 'x²/8 + y²/2 = 1',
 '由e²=c²/a²=(a²-b²)/a²=3/4得a²=4b²；代入(2,1)得4/4b²+1/b²=1 ⇒ b²=2，a²=8',
 2, 2, 4, 12, 33.33, 5, 3, 20, 3, 0),

-- === 英语-单选题 ===
(9,  'c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4', 1, '英语', '语法-时态', 1,
 '现在完成时,一般过去时',
 '["基础"]',
 'She ___ in Beijing since 2010.',
 '[{"key":"A","value":"lived"},{"key":"B","value":"has lived"},{"key":"C","value":"lives"},{"key":"D","value":"is living"}]',
 'B', 'since 2010 表示从过去延续至今，用现在完成时 has lived',
 2, 2, 18, 20, 90.00, 2, 1, 70, 3, 0),

(10, 'd0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5', 1, '英语', '词汇-短语', 1,
 '动词短语,take搭配',
 '["基础","易错"]',
 'He decided to ___ a new hobby to reduce stress.',
 '[{"key":"A","value":"take up"},{"key":"B","value":"take off"},{"key":"C","value":"take over"},{"key":"D","value":"take down"}]',
 'A', 'take up = 开始从事（某项活动/爱好）；take off=起飞；take over=接管；take down=取下',
 2, 2, 14, 16, 87.50, 3, 1, 55, 3, 0),

-- === 英语-多选题 ===
(11, 'e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6', 2, '英语', '语法-从句', 2,
 '定语从句,关系代词',
 '["月考"]',
 '下列关于定语从句的说法，正确的有？',
 '[{"key":"A","value":"关系代词who指人，which指物"},{"key":"B","value":"that既可以指人也可以指物"},{"key":"C","value":"非限制性定语从句中可用that引导"},{"key":"D","value":"whose表示所属关系"}]',
 'ABD', '非限制性定语从句不能用that引导，只能用which/who/whom/whose，C错误',
 2, 2, 9, 14, 64.29, 4, 1, 40, 3, 0),

-- === 英语-填空题 ===
(12, 'f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7', 3, '英语', '词汇-拼写', 2,
 '单词拼写,常用词汇',
 '["基础","听写"]',
 'The government has taken measures to protect the ______ (环境).',
 NULL,
 'environment', 'environment 意为"环境"，注意 n 前的 r 和 o 的拼写',
 2, 2, 11, 15, 73.33, 2, 1, 60, 3, 0),

-- === 英语-简答题 ===
(13, 'a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8', 4, '英语', '写作', 2,
 '英语作文,议论文',
 '["月考","写作"]',
 'Write a short paragraph (80-100 words) about the importance of reading.',
 NULL,
 'Reading broadens our horizons and enriches our knowledge... (参考范文)',
 '评分标准：内容完整、语法准确、词汇丰富、逻辑连贯',
 2, 2, 7, 10, 70.00, 3, 2, 30, 3, 0),

-- === 语文-单选题 ===
(14, 'b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9', 1, '语文', '文言文阅读', 2,
 '文言实词,古今异义',
 '["高考","文言文"]',
 '下列加点词解释正确的一项是？',
 '[{"key":"A","value":"先帝不以臣卑鄙（卑鄙：品质低劣）"},{"key":"B","value":"牺牲玉帛，弗敢加也（牺牲：为正义而死）"},{"key":"C","value":"率妻子邑人来此绝境（妻子：妻子儿女）"},{"key":"D","value":"未尝不叹息痛恨于桓灵也（痛恨：非常仇恨）"}]',
 'C', 'A卑鄙=地位低微见识浅陋；B牺牲=祭祀用的猪牛羊；C妻子=妻子儿女(古今异义)正确；D痛恨=痛心遗憾',
 2, 2, 10, 18, 55.56, 4, 2, 45, 3, 0),

-- === 语文-多选题 ===
(15, 'c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0', 2, '语文', '古诗词鉴赏', 2,
 '意象,修辞手法,思想感情',
 '["高考","鉴赏"]',
 '下列对杜甫《登高》的理解，正确的有？',
 '[{"key":"A","value":"首联《风急天高猿啸哀》营造了萧瑟凄凉的氛围"},{"key":"B","value":"颔联《无边落木萧萧下》与《不尽长江滚滚来》对仗工整"},{"key":"C","value":"全诗表达了诗人忧国忧民的情怀"},{"key":"D","value":"尾联《艰难苦恨繁霜鬓》直抒胸臆"}]',
 'ABCD', '全诗四联均正确：《登高》被誉为古今七律第一，四联层层推进，情景交融',
 2, 2, 8, 14, 57.14, 4, 1, 55, 3, 0),

-- === 语文-填空题 ===
(16, 'd6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1', 3, '语文', '名句默写', 1,
 '古诗文默写',
 '["高考","默写"]',
 '大漠孤烟直，______。',
 NULL,
 '长河落日圆', '出自王维《使至塞上》',
 2, 2, 17, 20, 85.00, 1, 1, 65, 3, 0),

-- === 语文-简答题 ===
(17, 'e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2', 4, '语文', '现代文阅读', 2,
 '主旨概括,表现手法',
 '["月考","阅读理解"]',
 '阅读短文，概括文章的主旨大意并分析作者使用了哪些表现手法。（不超过200字）',
 NULL,
 '主旨：通过对自然景物的描写抒发了作者对故乡的思念之情...',
 '评分标准：主旨概括准确(40分)、手法分析全面(40分)、语言流畅(20分)',
 2, 2, 5, 10, 50.00, 4, 2, 30, 3, 0),

-- === 物理-单选题 ===
(18, 'f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3', 1, '物理', '力学', 2,
 '牛顿第二定律,受力分析',
 '["期中","计算"]',
 '质量为2kg的物体受到10N的水平拉力，在光滑水平面上产生的加速度大小为？',
 '[{"key":"A","value":"2 m/s²"},{"key":"B","value":"5 m/s²"},{"key":"C","value":"10 m/s²"},{"key":"D","value":"20 m/s²"}]',
 'B', 'F=ma，a=F/m=10/2=5 m/s²',
 2, 2, 13, 18, 72.22, 3, 2, 40, 3, 0),

-- === 物理-填空题 ===
(19, 'a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4', 3, '物理', '电学', 2,
 '欧姆定律,电阻',
 '["月考"]',
 '一个电阻两端电压为12V，通过的电流为0.5A，则该电阻的阻值为______Ω。',
 NULL,
 '24', 'R=U/I=12/0.5=24Ω',
 2, 2, 14, 16, 87.50, 2, 1, 45, 3, 0),

-- === 物理-多选题 ===
(20, 'b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5', 2, '物理', '运动学', 2,
 '匀变速直线运动,自由落体',
 '["高考"]',
 '关于自由落体运动，下列说法正确的有？',
 '[{"key":"A","value":"自由落体是初速度为零的匀加速直线运动"},{"key":"B","value":"所有物体在同一地点的重力加速度相同"},{"key":"C","value":"自由落体的速度与质量成正比"},{"key":"D","value":"自由落体的位移与时间成正比"}]',
 'AB', 'AB正确；C错误，速度v=gt与质量无关；D错误，位移h=½gt²与时间平方成正比',
 2, 2, 6, 12, 50.00, 4, 2, 30, 3, 0);

-- ============================================================
-- 第三步：组卷策略 paperStrategy（5条）
-- ============================================================
INSERT INTO paperstrategy (id, strategyName, userId, totalScore, difficultyAvg, duration, questionTypeConfig, difficultyConfig, knowledgePointScope, isDefault, isDeleted) VALUES
(1, '数学期中标准策略', 2, 100, 2, 90,
 '[{"type":1,"count":8,"score":5},{"type":2,"count":4,"score":6},{"type":3,"count":2,"score":8},{"type":4,"count":1,"score":20}]',
 '[{"level":1,"ratio":0.2},{"level":2,"ratio":0.5},{"level":3,"ratio":0.3}]',
 '["二次函数","集合","三角函数"]',
 1, 0),
(2, '数学期末提高策略', 2, 120, 3, 120,
 '[{"type":1,"count":10,"score":4},{"type":2,"count":5,"score":6},{"type":3,"count":3,"score":8},{"type":4,"count":2,"score":16}]',
 '[{"level":1,"ratio":0.1},{"level":2,"ratio":0.4},{"level":3,"ratio":0.5}]',
 '["导数","数列","解析几何"]',
 0, 0),
(3, '英语基础练习策略', 2, 80, 1, 45,
 '[{"type":1,"count":15,"score":4},{"type":2,"count":5,"score":4}]',
 '[{"level":1,"ratio":0.8},{"level":2,"ratio":0.2}]',
 '["语法","词汇"]',
 0, 0),
(4, '语文综合测评策略', 1, 150, 2, 120,
 '[{"type":1,"count":10,"score":5},{"type":2,"count":6,"score":5},{"type":3,"count":4,"score":8},{"type":4,"count":2,"score":20}]',
 '[{"level":1,"ratio":0.3},{"level":2,"ratio":0.5},{"level":3,"ratio":0.2}]',
 '["古诗词","文言文","现代文阅读"]',
 0, 0),
(5, '数学速测快检策略', 2, 50, 1, 30,
 '[{"type":1,"count":5,"score":5},{"type":3,"count":5,"score":5}]',
 '[{"level":1,"ratio":0.6},{"level":2,"ratio":0.4}]',
 '["二次函数","集合","概率统计"]',
 0, 0);

-- ============================================================
-- 第四步：策略权重 strategyWeight（5策略×6权重=30条）
-- ============================================================
INSERT INTO strategyweight (id, strategyId, weightType, weightValue, isDeleted) VALUES
-- 策略1：均衡型
(1,  1, 'difficulty', 30, 0), (2,  1, 'accuracy', 15, 0), (3,  1, 'discrimination', 20, 0),
(4,  1, 'calcLevel', 10, 0), (5,  1, 'examFrequency', 10, 0), (6,  1, 'knowledgeCount', 15, 0),
-- 策略2：侧重难度+区分度
(7,  2, 'difficulty', 35, 0), (8,  2, 'accuracy', 10, 0), (9,  2, 'discrimination', 25, 0),
(10, 2, 'calcLevel', 15, 0), (11, 2, 'examFrequency', 5, 0), (12, 2, 'knowledgeCount', 10, 0),
-- 策略3：侧重正确率+考频
(13, 3, 'difficulty', 10, 0), (14, 3, 'accuracy', 30, 0), (15, 3, 'discrimination', 5, 0),
(16, 3, 'calcLevel', 5, 0), (17, 3, 'examFrequency', 35, 0), (18, 3, 'knowledgeCount', 15, 0),
-- 策略4：均匀分布
(19, 4, 'difficulty', 20, 0), (20, 4, 'accuracy', 20, 0), (21, 4, 'discrimination', 20, 0),
(22, 4, 'calcLevel', 10, 0), (23, 4, 'examFrequency', 15, 0), (24, 4, 'knowledgeCount', 15, 0),
-- 策略5：侧重准确率
(25, 5, 'difficulty', 10, 0), (26, 5, 'accuracy', 35, 0), (27, 5, 'discrimination', 10, 0),
(28, 5, 'calcLevel', 5, 0), (29, 5, 'examFrequency', 25, 0), (30, 5, 'knowledgeCount', 15, 0);

-- ============================================================
-- 第五步：试卷 examPaper（5条）
-- ============================================================
INSERT INTO exampaper (id, paperName, subject, totalScore, creatorId, status, strategyId, paperType, difficultyRate, duration, exportStatus, isDeleted) VALUES
(1, '高一数学第一章测试', '数学', 100, 2, 1, 1, 2, 2.00, 90, 0, 0),
(2, '高一数学期末模拟卷', '数学', 120, 2, 1, 2, 2, 2.80, 120, 1, 0),
(3, '英语基础语法练习', '英语', 80, 2, 0, 3, 1, 1.20, 45, 0, 0),
(4, '语文综合能力测评', '语文', 150, 1, 1, 4, 2, 2.00, 120, 0, 0),
(5, '数学速测小卷', '数学', 50, 2, 0, 5, 1, 1.40, 30, 0, 0);

-- ============================================================
-- 第六步：试卷-试题关联 paperQuestion（每卷5题，共25条）
-- ============================================================
INSERT INTO paperquestion (id, paperId, questionId, questionScore, sort, isAutoAdd, isDeleted) VALUES
-- 试卷1：数学第一章测试（5题：单选x1 + 多选x1 + 填空x1 + 简答x1 + 单选x1）
(1,  1, 1,  5, 1, 1, 0),
(2,  1, 3,  6, 2, 1, 0),
(3,  1, 5,  8, 3, 0, 0),
(4,  1, 7, 20, 4, 0, 0),
(5,  1, 2,  5, 5, 0, 0),

-- 试卷2：数学期末模拟（5题：多选+填空+简答+单选+填空）
(6,  2, 4,  6, 1, 1, 0),
(7,  2, 6,  8, 2, 1, 0),
(8,  2, 7, 16, 3, 0, 0),
(9,  2, 2,  4, 4, 0, 0),
(10, 2, 5,  8, 5, 0, 0),

-- 试卷3：英语基础练习（5题：单选x2 + 多选+填空+简答）
(11, 3, 9,  4, 1, 0, 0),
(12, 3, 10, 4, 2, 0, 0),
(13, 3, 11, 6, 3, 0, 0),
(14, 3, 12, 4, 4, 0, 0),
(15, 3, 13, 8, 5, 0, 0),

-- 试卷4：语文综合测评（5题：单选+多选+填空+简答+单选）
(16, 4, 14, 5, 1, 1, 0),
(17, 4, 15, 6, 2, 0, 0),
(18, 4, 16, 4, 3, 0, 0),
(19, 4, 17, 20, 4, 0, 0),
(20, 4, 18, 5, 5, 0, 0),

-- 试卷5：数学速测（5题：单选x2 + 填空x2 + 单选x1）
(21, 5, 1,  5, 1, 0, 0),
(22, 5, 2,  5, 2, 0, 0),
(23, 5, 5,  5, 3, 0, 0),
(24, 5, 6,  5, 4, 0, 0),
(25, 5, 18, 5, 5, 0, 0);

-- ============================================================
-- 第七步：考试记录 userExamRecord（5条）
-- ============================================================
INSERT INTO userexamrecord (id, userId, paperId, totalScore, userScore, status, startTime, endTime, paperStrategyId, isDeleted) VALUES
(1, 2, 1, 100, 82, 2, '2026-05-01 09:00:00', '2026-05-01 10:25:00', 1, 0),
(2, 2, 2, 120, 95, 2, '2026-05-05 08:30:00', '2026-05-05 10:15:00', 2, 0),
(3, 2, 4, 150, 108, 2, '2026-05-08 10:00:00', '2026-05-08 11:50:00', 4, 0),
(4, 1, 4, 150, 126, 2, '2026-05-09 09:00:00', '2026-05-09 10:55:00', 4, 0),
(5, 2, 1, 100, 0, 0, '2026-05-11 15:00:00', NULL, 1, 0);

-- ============================================================
-- 第八步：答题详情 userAnswerDetail（每记录5题，共25条）
-- ============================================================
INSERT INTO useranswerdetail (id, recordId, paperId, questionId, questionType, userAnswer, questionScore, actualScore, correctStatus, aiReviewMsg, questionDifficulty, isDeleted) VALUES
-- 记录1（用户2，试卷1，82分）
(1,  1, 1, 1, 1, 'A', 5, 5, 1, NULL, 1, 0),
(2,  1, 1, 3, 2, 'AC', 6, 6, 1, NULL, 2, 0),
(3,  1, 1, 5, 3, '√13', 8, 8, 1, NULL, 2, 0),
(4,  1, 1, 7, 4, '增区间(0,2)减区间其他', 20, 5, 2, '单调区间判断反了，极值计算未完成', 3, 0),
(5,  1, 1, 2, 1, 'C', 5, 0, 2, NULL, 2, 0),

-- 记录2（用户2，试卷2，95分）
(6,  2, 2, 4, 2, 'AB', 6, 6, 1, NULL, 3, 0),
(7,  2, 2, 6, 3, '(2,3)', 8, 8, 1, NULL, 2, 0),
(8,  2, 2, 7, 4, '增(-∞,0)(2,+∞)减(0,2)；极大2极小-2', 16, 16, 1, '答案完整正确', 3, 0),
(9,  2, 2, 2, 1, 'B', 4, 4, 1, NULL, 2, 0),
(10, 2, 2, 5, 3, '4', 8, 0, 2, '余弦定理公式记错', 2, 0),

-- 记录3（用户2，试卷4，108分）
(11, 3, 4, 14, 1, 'C', 5, 5, 1, NULL, 2, 0),
(12, 3, 4, 15, 2, 'ABC', 6, 0, 2, '漏选D', 2, 0),
(13, 3, 4, 16, 3, '长河落日圆', 4, 4, 1, NULL, 1, 0),
(14, 3, 4, 17, 4, '文章通过景物描写表达思乡之情，运用了借景抒情的手法', 20, 10, 2, '主旨部分正确，手法分析不够全面', 2, 0),
(15, 3, 4, 14, 1, 'A', 5, 0, 2, NULL, 2, 0),

-- 记录4（用户1，试卷4，126分）
(16, 4, 4, 14, 1, 'C', 5, 5, 1, NULL, 2, 0),
(17, 4, 4, 15, 2, 'ABCD', 6, 6, 1, NULL, 2, 0),
(18, 4, 4, 16, 3, '长河落日圆', 4, 4, 1, NULL, 1, 0),
(19, 4, 4, 17, 4, '主旨准确+手法分析全面+语言流畅', 20, 18, 1, '优秀，逻辑清晰', 2, 0),
(20, 4, 4, 14, 1, 'B', 5, 0, 2, NULL, 2, 0),

-- 记录5（用户2，试卷1，进行中，5题均已答，取当前保存的答案）
(21, 5, 1, 1, 1, 'A', 5, 0, 0, NULL, 1, 0),
(22, 5, 1, 3, 2, 'AD', 6, 0, 0, NULL, 2, 0),
(23, 5, 1, 5, 3, '', 8, 0, 0, NULL, 2, 0),
(24, 5, 1, 7, 4, '', 20, 0, 0, NULL, 3, 0),
(25, 5, 1, 2, 1, 'B', 5, 0, 0, NULL, 2, 0);

-- ============================================================
-- 第九步：做题日统计 userAnswerStats（5条）
-- ============================================================
INSERT INTO useranswerstats (id, userId, statsDate, answerNum, correctNum, statsSubjectId, isDeleted) VALUES
(1, 2, '2026-05-01', 5, 3, NULL, 0),
(2, 2, '2026-05-05', 5, 4, NULL, 0),
(3, 2, '2026-05-08', 5, 2, NULL, 0),
(4, 1, '2026-05-09', 5, 3, NULL, 0),
(5, 2, '2026-05-10', 10, 7, NULL, 0);

-- ============================================================
-- 第十步：错题本 errorBook（5条）
-- ============================================================
INSERT INTO errorbook (id, userId, questionId, errorType, reviewStatus, errorCount, firstErrorTime, lastErrorTime, isArchived, isDeleted) VALUES
(1, 2, 7, 3, 1, 1, '2026-05-01 10:25:00', '2026-05-01 10:25:00', 0, 0),
(2, 2, 2, 2, 0, 2, '2026-05-01 10:25:00', '2026-05-05 10:15:00', 0, 0),
(3, 2, 5, 2, 0, 1, '2026-05-05 10:15:00', '2026-05-05 10:15:00', 0, 0),
(4, 2, 15, 1, 0, 1, '2026-05-08 11:50:00', '2026-05-08 11:50:00', 0, 0),
(5, 1, 14, 1, 0, 1, '2026-05-09 10:55:00', '2026-05-09 10:55:00', 0, 0);

-- ============================================================
-- 第十一步：错题备注 errorBookNote（5条）
-- ============================================================
INSERT INTO errorbooknote (id, errorBookId, userId, noteType, content, imageUrl, isDeleted) VALUES
(1, 1, 2, 1, '导数单调性口诀：导函数>0递增，<0递减。极值点在f\'(x)=0处且符号变化。', NULL, 0),
(2, 2, 2, 1, '古典概型P=满足条件的事件数/基本事件总数，注意有序与无序的区别。', NULL, 0),
(3, 4, 2, 1, '古诗鉴赏多选题务必逐项对照原文，不要凭感觉选择。', NULL, 0),
(4, 1, 2, 1, '注意极值点和极值的区别：极值点是x坐标，极值是y坐标（函数值）。', NULL, 0),
(5, 5, 1, 1, '文言实词需结合语境判断，古今异义是重点考点。', NULL, 0);

-- ============================================================
-- 第十二步：AI对话记录 aiPaperChat（5条）
-- ============================================================
INSERT INTO aipaperchat (id, userId, paperId, strategyId, sessionRound, chatContent, aiResponse, status, retryCount, isDeleted) VALUES
(1, 2, 1, 1, 1, '请帮我组一份侧重基础知识的数学试卷', '{"questionIds":[1,3,5,7,2],"totalQuestions":5,"actualTotalScore":50}', 1, 0, 0),
(2, 2, 2, 2, 1, '需要高难度期末模拟卷，侧重导数和数列', '{"questionIds":[4,6,7,2,5],"totalQuestions":5,"actualTotalScore":50}', 1, 0, 0),
(3, 2, NULL, NULL, 1, '帮我看看最近薄弱知识点', '{"weakPoints":[{"knowledgePoint":"导数应用","accuracy":40.0},{"knowledgePoint":"古典概型","accuracy":62.5}]}', 1, 0, 0),
(4, 2, NULL, NULL, 1, '给我推荐一些英语基础题', '{}', 2, 1, 0), -- 失败记录
(5, 2, NULL, NULL, 1, '英语基础语法练习推荐', '{"questionIds":[9,10,11],"totalQuestions":3,"actualTotalScore":30}', 1, 1, 0);

-- ============================================================
-- 第十三步：系统通知 systemNotification（5条）
-- ============================================================
INSERT INTO systemnotification (id, userId, title, content, type, isRead, link, isDeleted) VALUES
(1, 2, '组卷完成', '试卷《高一数学第一章测试》AI组卷完成，共5题，总分100分', 1, 1, '/paper', 0),
(2, 2, '考试批改完成', '您在《高一数学期末模拟卷》中的考试已批改完成，得分95分', 2, 0, '/exam', 0),
(3, 2, '导出完成', '试卷《语文综合能力测评》已成功导出为PDF', 2, 0, NULL, 0),
(4, 2, '系统更新通知', 'QuizMaid已更新：新增错题本智能推荐和专项强化组卷功能', 3, 0, '/error-book', 0),
(5, 1, '分享通知', '用户 root 已将其错题集分享给您', 2, 1, '/error-book', 0);

-- ============================================================
-- 第十四步：试卷分享 paperShare（5条）
-- ============================================================
INSERT INTO papershare (id, paperId, ownerId, targetUserId, targetGroupId, isDeleted) VALUES
(1, 1, 2, 1, NULL, 0),
(2, 4, 1, 2, NULL, 0),
(3, 2, 2, NULL, 1, 0),
(4, 1, 2, NULL, 2, 0),
(5, 3, 2, 1, NULL, 0);

-- ============================================================
-- 第十五步：签到记录 userSign（5条，补充测试数据）
-- ============================================================
INSERT INTO usersign (id, userId, signDate, continueDays, isDeleted) VALUES
(1, 2, '2026-05-07', 1, 0),
(2, 2, '2026-05-08', 2, 0),
(3, 2, '2026-05-09', 3, 0),
(4, 2, '2026-05-10', 4, 0),
(5, 2, '2026-05-11', 5, 0);

-- ============================================================
-- 第十六步：导出模板 exportTemplate（5条）
-- ============================================================
INSERT INTO exporttemplate (id, templateName, userId, exportType, config, isDefault, isDeleted) VALUES
(1, '标准试卷模板', 2, 'Word',
 '{"titleAlign":"center","showAnswer":false,"showAnalysis":false,"scorePosition":"right","fontSize":"14px"}',
 1, 0),
(2, '含答案解析模板', 2, 'Word',
 '{"titleAlign":"center","showAnswer":true,"showAnalysis":true,"scorePosition":"right","fontSize":"14px"}',
 0, 0),
(3, '紧凑排版模板', 2, 'PDF',
 '{"titleAlign":"left","showAnswer":false,"showAnalysis":false,"scorePosition":"inline","fontSize":"12px"}',
 1, 0),
(4, '试卷+答案分离模板', 1, 'Word',
 '{"titleAlign":"center","showAnswer":true,"showAnalysis":true,"scorePosition":"right","fontSize":"14px","separateAnswerPage":true}',
 1, 0),
(5, '手机适配模板', 2, 'PDF',
 '{"titleAlign":"center","showAnswer":false,"showAnalysis":false,"scorePosition":"inline","fontSize":"16px","mobileOptimized":true}',
 0, 0);

-- ============================================================
-- 第十七步：错题集导出日志 errorBookExportLog（5条）
-- ============================================================
INSERT INTO errorbookexportlog (id, userId, exportType, exportStatus, fileName, exportPath, exportConfig, isDeleted) VALUES
(1, 2, 'CSV', 2, '错题集-20260501-093000.csv', './exports/错题集-20260501.csv', '{"format":"csv","showAnswer":true}', 0),
(2, 2, 'CSV', 2, '错题集-20260503-150000.csv', './exports/错题集-20260503.csv', '{"format":"csv","showAnswer":true}', 0),
(3, 2, 'CSV', 3, '错题集-20260508-120000.csv', './exports/错题集-20260508.csv', '{"format":"csv","showAnswer":true}', 0),
(4, 2, 'CSV', 2, '错题集-20260510-180000.csv', './exports/错题集-20260510.csv', '{"format":"csv","showAnswer":true,"showAnalysis":true}', 0),
(5, 1, 'CSV', 2, '错题集-20260509-110000.csv', './exports/错题集-20260509.csv', '{"format":"csv","showAnswer":true}', 0);

-- ============================================================
-- 第十八步：错题分组 errorBookGroup（5条，需在GroupItem前插入）
-- ============================================================
INSERT INTO errorbookgroup (id, userId, groupName, description, sort, isDeleted) VALUES
(1, 2, '函数相关错题', '所有函数知识点的错题', 1, 0),
(2, 2, '几何相关错题', '解析几何相关错题', 2, 0),
(3, 2, '英语语法错题', '语法时态相关错题', 3, 0),
(4, 1, '文言文错题', '文言文相关错题汇总', 1, 0),
(5, 2, '高频易错题', '错误次数≥2的重点错题', 4, 0);

-- ============================================================
-- 第十九步：错题分组成员 errorBookGroupItem（5条）
-- ============================================================
INSERT INTO errorbookgroupitem (id, groupId, errorBookId, isDeleted) VALUES
(1, 1, 1, 0),
(2, 1, 2, 0),
(3, 2, 3, 0),
(4, 2, 4, 0),
(5, 3, 5, 0);

-- ============================================================
-- 第二十步：推荐反馈 recommendFeedback（5条）
-- 说明：与错题本 errorBook 联动，每条约1个用户对某推荐的反馈
--   feedback: 1=已掌握 2=仍困难
--   知识点的反馈分布用于动态调整推荐策略
-- ============================================================
INSERT INTO recommendfeedback (id, userId, questionId, knowledgePoints, feedback, isDeleted) VALUES
-- 用户2 错题1(导数) → 推荐同知识点题8(解析几何/导数) → 仍困难，该知识点需加推
(1, 2, 8, '导数应用,单调性,极值', 2, 0),
-- 用户2 错题2(古典概型) → 推荐同知识点题 → 已掌握，该知识点权重降低
(2, 2, 2, '古典概型,排列组合', 1, 0),
-- 用户2 错题3(余弦定理) → 推荐同知识点题5(三角函数) → 仍困难，三角函数需加推
(3, 2, 5, '正弦定理,余弦定理', 2, 0),
-- 用户2 错题4(古诗鉴赏) → 推荐题17(现代文阅读同属语文) → 已掌握
(4, 2, 17, '意象,修辞手法,思想感情', 1, 0),
-- 用户1 错题5(文言文) → 推荐题14(文言文同类) → 仍困难，文言实词是薄弱点
(5, 1, 14, '文言实词,古今异义', 2, 0);

-- ============================================================
-- 完成！
-- ============================================================
