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
    data?: Record<string, any>
    message?: string
  }

  type BaseResponseMapObjectObject = {
    code?: number
    data?: Record<string, any>
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
    keyComparator?: Record<string, any>
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
    options?: Record<string, any>[]
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
}
