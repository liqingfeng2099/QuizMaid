declare namespace API {
  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseExamPaperVO = {
    code?: number
    data?: ExamPaperVO
    message?: string
  }

  type BaseResponseInteger = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponseListLong = {
    code?: number
    data?: number[]
    message?: string
  }

  type BaseResponseListUserHeatMapVO = {
    code?: number
    data?: UserHeatMapVO[]
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponseMapLocalDateBoolean = {
    code?: number
    data?: Record<string, unknown>
    message?: string
  }

  type BaseResponseMapObjectObject = {
    code?: number
    data?: Record<string, unknown>
    message?: string
  }

  type BaseResponsePageExamPaperVO = {
    code?: number
    data?: PageExamPaperVO
    message?: string
  }

  type BaseResponsePageQuestionVO = {
    code?: number
    data?: PageQuestionVO
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseQuestionVO = {
    code?: number
    data?: QuestionVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserLoginVO = {
    code?: number
    data?: UserLoginVO
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type BaseResponseAIPaperAssemblyResultVO = {
    code?: number
    data?: AIPaperAssemblyResultVO
    message?: string
  }

  type BindEmailDTO = {
    email?: string
    code?: string
  }

  type callbackParams = {
    code: string
  }

  type DeleteRequest = {
    id?: number
  }

  type ExamPaperAddDTO = {
    paperName?: string
    subject?: string
    totalScore?: number
    status?: number
  }

  type ExamPaperQueryDTO = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    paperName?: string
    subject?: string
    status?: number
    creatorId?: number
  }

  type ExamPaperStatusDTO = {
    id?: number
    status?: number
  }

  type ExamPaperUpdateDTO = {
    id?: number
    paperName?: string
    subject?: string
    totalScore?: number
  }

  type AIPaperAssemblyDTO = {
    paperName?: string
    subject?: string
    chapter?: string
    difficulty?: number
    totalScore?: number
    status?: number
    userRequirement?: string
  }

  type AIPaperAssemblyResultVO = {
    paperId?: number
    questionIds?: number[]
    totalQuestions?: number
    actualTotalScore?: number
  }

  type ExamPaperVO = {
    id?: number
    paperName?: string
    subject?: string
    totalScore?: number
    creatorId?: number
    status?: number
    createTime?: string
    updateTime?: string
    questions?: PaperQuestionVO[]
  }

  type getExamPaperByIdParams = {
    id: number
  }

  type getImportStatusParams = {
    taskId: string
  }

  type getQuestionByIdParams = {
    id: number
  }

  type getSignInDaysParams = {
    userId: number
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserSignDataParams = {
    year: number
  }

  type getUserVoByIdParams = {
    id: number
  }

  type JSONConfig = {
    keyComparator?: Record<string, unknown>
    ignoreError?: boolean
    ignoreCase?: boolean
    dateFormat?: string
    ignoreNullValue?: boolean
    transientSupport?: boolean
    stripTrailingZeros?: boolean
    checkDuplicate?: boolean
    writeLongAsString?: boolean
    order?: boolean
  }

  type PageExamPaperVO = {
    records?: ExamPaperVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageQuestionVO = {
    records?: QuestionVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PaperQuestionAddDTO = {
    paperId?: number
    questionId?: number
    questionScore?: number
    sort?: number
  }

  type PaperQuestionUpdateDTO = {
    id?: number
    questionScore?: number
    sort?: number
  }

  type PaperQuestionVO = {
    id?: number
    questionId?: number
    questionContent?: string
    questionScore?: number
    sort?: number
    type?: number
  }

  type QuestionAddDTO = {
    type?: number
    subject?: string
    chapter?: string
    difficulty?: number
    knowledgePoints?: string
    tags?: string[]
    content?: string
    options?: Record<string, unknown>[]
    answer?: string
    analysis?: string
    status?: number
  }

  type QuestionQueryDTO = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    type?: number
    subject?: string
    chapter?: string
    difficulty?: number
    knowledgePoints?: string
    tags?: string
    content?: string
    creatorId?: number
    status?: number
  }

  type QuestionStatusDTO = {
    id?: number
    status?: number
  }

  type QuestionUpdateDTO = {
    id?: number
    type?: number
    subject?: string
    chapter?: string
    difficulty?: number
    knowledgePoints?: string
    tags?: string
    content?: string
    options?: string
    answer?: string
    analysis?: string
    status?: number
  }

  type QuestionVO = {
    id?: number
    questionMd5?: string
    type?: number
    subject?: string
    chapter?: string
    difficulty?: number
    knowledgePoints?: string
    tags?: string
    content?: string
    options?: string
    answer?: string
    analysis?: string
    creatorId?: number
    status?: number
    correctCount?: number
    totalCount?: number
    accuracy?: number
    createTime?: string
    updateTime?: string
  }

  type ResetPasswordDTO = {
    password?: string
    email?: string
    code?: string
  }

  type sendEmailParams = {
    email: string
  }

  type User = {
    id?: number
    username?: string
    password?: string
    nickname?: string
    email?: string
    role?: string
    emailVerified?: number
    oauthType?: string
    oauthOpenid?: string
    status?: number
    answerNum?: number
    correctNum?: number
    createTime?: string
    updateTime?: string
    isDeleted?: number
  }

  type UserAddDTO = {
    username?: string
    nickname?: string
    role?: string
  }

  type UserHeatMapVO = {
    date?: string
    count?: number
    level?: number
  }

  type UserLoginByEmailDTO = {
    email?: string
    code?: string
  }

  type UserLoginDTO = {
    username?: string
    userPassword?: string
  }

  type UserLoginVO = {
    id?: number
    username?: string
    password?: string
    nickname?: string
    email?: string
    role?: string
    emailVerified?: number
    oauthType?: string
    oauthOpenid?: string
    status?: number
    answerNum?: number
    correctNum?: number
    createTime?: string
    updateTime?: string
  }

  type UserQueryDTO = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    nickname?: string
    username?: string
    role?: string
  }

  type UserRegisterByEmailDTO = {
    userName?: string
    userPassword?: string
    checkUserPassword?: string
    email?: string
    code?: string
  }

  type UserRegisterDTO = {
    userName?: string
    userPassword?: string
    checkUserPassword?: string
  }

  type UserUpdateByAdminDTO = {
    id?: number
    username?: string
    nickname?: string
    email?: string
    role?: string
    emailVerified?: number
    status?: number
    answerNum?: number
    correctNum?: number
  }

  type UserUpdateDTO = {
    id?: number
    nickname?: string
    email?: string
  }

  type UserVO = {
    id?: number
    username?: string
    nickname?: string
    email?: string
    role?: string
    emailVerified?: number
    oauthType?: string
    oauthOpenid?: string
    status?: number
    answerNum?: number
    correctNum?: number
    createTime?: string
    updateTime?: string
  }

  // ===== [2026-05-10 新增] 手动组卷相关类型 =====

  type MatchCountDTO = {
    subject?: string
    chapter?: string
    difficulty?: number
    types?: number[]
    knowledgePoints?: string
  }

  type MatchCountVO = {
    totalCount?: number
    byType?: Record<string, number>
    byDifficulty?: Record<string, number>
  }

  type BaseResponseMatchCountVO = {
    code?: number
    data?: MatchCountVO
    message?: string
  }

  type AssemblySaveDTO = {
    paperName?: string
    subject?: string
    status?: number
    strategyId?: number
    questions?: AssemblyQuestionItem[]
  }

  type AssemblyQuestionItem = {
    questionId?: number
    questionScore?: number
    sort?: number
  }

  // 试卷策略相关
  type PaperStrategyAddDTO = {
    strategyName?: string
    totalScore?: number
    difficultyAvg?: number
    duration?: number
    questionTypeConfig?: string
    difficultyConfig?: string
    knowledgePointScope?: string
    weights?: StrategyWeightDTO[]
  }

  type StrategyWeightDTO = {
    weightType?: string
    weightValue?: number
  }

  type PaperStrategyVO = {
    id?: number
    strategyName?: string
    userId?: number
    totalScore?: number
    difficultyAvg?: number
    duration?: number
    questionTypeConfig?: string
    difficultyConfig?: string
    knowledgePointScope?: string
    isDefault?: number
    createTime?: string
    updateTime?: string
    weights?: StrategyWeightVO[]
    weightSum?: number
  }

  type StrategyWeightVO = {
    id?: number
    strategyId?: number
    weightType?: string
    weightValue?: number
  }

  type BaseResponsePaperStrategyVO = {
    code?: number
    data?: PaperStrategyVO
    message?: string
  }

  type BaseResponsePagePaperStrategyVO = {
    code?: number
    data?: PagePaperStrategyVO
    message?: string
  }

  type PagePaperStrategyVO = {
    records?: PaperStrategyVO[]
    totalRow?: number
  }

  type PaperStrategyQueryDTO = {
    pageNum?: number
    pageSize?: number
    strategyName?: string
    difficultyAvg?: number
    isDefault?: number
  }

  type AssemblyRequestDTO = {
    strategyId?: number
    subject?: string
    chapter?: string
    paperName?: string
    paperStatus?: number
  }

  type AssemblyResultVO = {
    questions?: AssemblyQuestionScoreVO[]
    totalQuestions?: number
    actualTotalScore?: number
    dimensionResults?: Record<string, string>
    fitness?: number
    algorithmType?: string
    degradeHints?: AssemblyDegradeHintDTO[]
  }

  type AssemblyQuestionScoreVO = {
    questionId?: number
    type?: number
    content?: string
    difficulty?: number
    score?: number
    compositeScore?: number
  }

  type AssemblyDegradeHintDTO = {
    degradedIndicator?: string
    originalConstraint?: string
    degradedConstraint?: string
    reason?: string
  }

  type BaseResponseAssemblyResultVO = {
    code?: number
    data?: AssemblyResultVO
    message?: string
  }

  // ===== [2026-05-10 新增] AI增强组卷类型 =====

  type AIPaperAssemblyV2DTO = {
    paperName?: string
    subject?: string
    chapter?: string
    difficulty?: number
    totalScore?: number
    status?: number
    userRequirement?: string
    usePersonalization?: boolean
    includeWeakAreas?: boolean
    previousChatId?: number
  }

  type AIAssemblyStrategyVO = {
    difficultyAvg?: number
    difficultyConfig?: { level?: number; ratio?: number }[]
    questionTypeConfig?: { type?: number; count?: number; score?: number }[]
    knowledgePointScope?: string
    questionIds?: number[]
    totalQuestions?: number
    actualTotalScore?: number
  }

  type BaseResponseAIAssemblyStrategyVO = {
    code?: number
    data?: AIAssemblyStrategyVO
    message?: string
  }

  type AIProfileVO = {
    answerNum?: number
    correctNum?: number
    accuracy?: number
    weakPoints?: { knowledgePoint?: string; accuracy?: number; totalCount?: number }[]
  }

  type BaseResponseAIProfileVO = {
    code?: number
    data?: AIProfileVO
    message?: string
  }

  type AIChatVO = {
    id?: number
    userId?: number
    paperId?: number
    strategyId?: number
    sessionRound?: number
    chatContent?: string
    aiResponse?: string
    status?: number
    retryCount?: number
    createTime?: string
  }

  type BaseResponseListAIChatVO = {
    code?: number
    data?: AIChatVO[]
    message?: string
  }

  type AIChatQueryDTO = {
    limit?: number
  }

  // ===== [2026-05-10 新增] 试卷导出类型 =====
  type ExportConfigDTO = {
    paperId?: number
    paperIds?: number[]
    showAnswer?: boolean
    showAnalysis?: boolean
    titleAlign?: string
    scorePosition?: string
    exportType?: string
  }

  type ExportFileVO = {
    id?: number
    paperId?: number
    fileName?: string
    filePath?: string
    exportType?: string
    exportStatus?: number
    createTime?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseListExportFileVO = {
    code?: number
    data?: ExportFileVO[]
    message?: string
  }

  // ===== [2026-05-11 新增] 成绩统计与可视化类型 =====

  type StatisticsQueryDTO = {
    paperId?: number
    subject?: string
    paperType?: number
    startTime?: string
    endTime?: string
  }

  type TrendQueryDTO = {
    userId?: number
    subject?: string
    limit?: number
  }

  type PaperStatisticsVO = {
    paperId?: number
    paperName?: string
    subject?: string
    totalScore?: number
    creatorId?: number
    status?: number
    paperType?: number
    createTime?: string
    maxScore?: number
    minScore?: number
    avgScore?: number
    medianScore?: number
    totalExaminees?: number
    highScoreRate?: number
    passRate?: number
    scoreDistribution?: ScoreDistributionVO[]
    questionTypeStats?: QuestionTypeStatVO[]
    difficultyStats?: DifficultyStatVO[]
    knowledgePointStats?: KnowledgePointStatVO[]
    highFreqWrongQuestions?: HighFreqWrongQuestionVO[]
    calculationTimestamp?: string
  }

  type QuestionTypeStatVO = {
    questionType?: number
    questionTypeName?: string
    totalCount?: number
    correctCount?: number
    correctRate?: number
    totalActualScore?: number
    totalQuestionScore?: number
    scoreRate?: number
  }

  type DifficultyStatVO = {
    difficulty?: number
    difficultyName?: string
    totalCount?: number
    correctCount?: number
    correctRate?: number
    totalActualScore?: number
    totalQuestionScore?: number
    scoreRate?: number
  }

  type KnowledgePointStatVO = {
    knowledgePoint?: string
    totalCount?: number
    correctCount?: number
    correctRate?: number
    totalActualScore?: number
    totalQuestionScore?: number
    scoreRate?: number
  }

  type HighFreqWrongQuestionVO = {
    questionId?: number
    questionContent?: string
    questionType?: number
    questionTypeName?: string
    difficulty?: number
    difficultyName?: string
    knowledgePoints?: string
    wrongCount?: number
    totalScoreLost?: number
  }

  type ScoreDistributionVO = {
    scoreBucket?: number
    count?: number
  }

  type TrendDataPointVO = {
    userId?: number
    paperId?: number
    paperName?: string
    score?: number
    scoreRate?: number
    examTime?: string
  }

  type BaseResponsePaperStatisticsVO = {
    code?: number
    data?: PaperStatisticsVO
    message?: string
  }

  type BaseResponseListPaperStatisticsVO = {
    code?: number
    data?: PaperStatisticsVO[]
    message?: string
  }

  // 题型正确率统计列表响应类型 .hml
  type BaseResponseListQuestionTypeStatVO = {
    code?: number
    data?: QuestionTypeStatVO[]
    message?: string
  }

  type BaseResponseListTrendDataPointVO = {
    code?: number
    data?: TrendDataPointVO[]
    message?: string
  }

  // ===== [2026-05-11 新增] 在线考试类型 =====

  type ExamRecordVO = {
    recordId?: number
    paperId?: number
    paperName?: string
    totalScore?: number
    userScore?: number
    status?: number       // 0-进行中 1-已完成 2-已批改
    durationText?: string
    startTime?: string
    endTime?: string
    remainingSeconds?: number
    totalQuestions?: number
    questions?: ExamQuestionItem[]
  }

  type ExamQuestionItem = {
    questionId?: number
    type?: number         // 1-单选 2-多选 3-填空 4-简答
    content?: string
    options?: string      // 选项JSON
    score?: number
    sort?: number
    userAnswer?: string
    correctStatus?: number // 0-待批改 1-正确 2-错误
    actualScore?: number
    correctAnswer?: string
    analysis?: string
  }

  type ExamResultVO = {
    recordId?: number
    paperId?: number
    paperName?: string
    subject?: string
    totalScore?: number
    userScore?: number
    status?: number
    startTime?: string
    endTime?: string
    usedSeconds?: number
    totalQuestions?: number
    correctCount?: number
    wrongCount?: number
    pendingCount?: number
    questions?: ExamQuestionItem[]
  }

  type BaseResponseExamRecordVO = {
    code?: number
    data?: ExamRecordVO
    message?: string
  }

  type BaseResponseExamResultVO = {
    code?: number
    data?: ExamResultVO
    message?: string
  }

  type BaseResponseListExamRecordVO = {
    code?: number
    data?: ExamRecordVO[]
    message?: string
  }

  // ===== [2026-05-11 新增] 个人统计与错题本类型 =====

  type PersonalDimensionVO = {
    dimensionKey?: string
    totalCount?: number
    correctCount?: number
    correctRate?: number
    scoreRate?: number
  }

  type PersonalTrendVO = {
    period?: string
    answerCount?: number
    correctCount?: number
    accuracy?: number
  }

  type PersonalStatsVO = {
    totalAnswers?: number
    totalCorrect?: number
    totalAccuracy?: number
    byType?: PersonalDimensionVO[]
    byDifficulty?: PersonalDimensionVO[]
    byKnowledge?: PersonalDimensionVO[]
    trend?: PersonalTrendVO[]
  }

  type BaseResponsePersonalStatsVO = {
    code?: number
    data?: PersonalStatsVO
    message?: string
  }

  type BaseResponseListPersonalDimensionVO = {
    code?: number
    data?: PersonalDimensionVO[]
    message?: string
  }

  type ErrorBookVO = {
    id?: number
    userId?: number
    questionId?: number
    errorType?: number
    reviewStatus?: number
    errorCount?: number
    firstErrorTime?: string
    lastErrorTime?: string
    isArchived?: number
  }

  type PageErrorBook = {
    records?: ErrorBookVO[]
    pageNumber?: number
    pageSize?: number
    totalRow?: number
  }

  type BaseResponsePageErrorBook = {
    code?: number
    data?: PageErrorBook
    message?: string
  }

  type BaseResponseListQuestionVO = {
    code?: number
    data?: QuestionVO[]
    message?: string
  }

  type ErrorBookGroup = {
    id?: number; userId?: number; groupName?: string; description?: string; sort?: number
  }

  type BaseResponseListErrorBookGroup = { code?: number; data?: ErrorBookGroup[]; message?: string }
  type BaseResponseErrorBookGroup = { code?: number; data?: ErrorBookGroup; message?: string }

  type ErrorBookNote = {
    id?: number; errorBookId?: number; content?: string; imageUrl?: string; noteType?: number
  }

  type BaseResponseListErrorBookNote = { code?: number; data?: ErrorBookNote[]; message?: string }
  type BaseResponseErrorBookNote = { code?: number; data?: ErrorBookNote; message?: string }
}
