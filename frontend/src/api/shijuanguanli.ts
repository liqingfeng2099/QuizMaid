/* eslint-disable */
import request from '@/request'

/** 添加试卷 POST /examPaper/add */
export async function addExamPaper(body: API.ExamPaperAddDTO, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/examPaper/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 逻辑删除试卷 POST /examPaper/delete */
export async function deleteExamPaper(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/examPaper/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 根据ID获取试卷详情（含试题列表） GET /examPaper/get/${param0} */
export async function getExamPaperById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getExamPaperByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseExamPaperVO>(`/examPaper/get/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 分页查询试卷（普通用户只能看到自己的，管理员可查所有） POST /examPaper/list/page */
export async function listExamPaperByPage(
  body: API.ExamPaperQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageExamPaperVO>('/examPaper/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 修改试卷状态 POST /examPaper/status */
export async function updateStatus1(
  body: API.ExamPaperStatusDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/examPaper/status', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 更新试卷 POST /examPaper/update */
export async function updateExamPaper(
  body: API.ExamPaperUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/examPaper/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** AI智能组卷 POST /examPaper/ai/assemble */
export async function aiAssemblePaper(
  body: API.AIPaperAssemblyDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAIPaperAssemblyResultVO>('/examPaper/ai/assemble', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 匹配计数 POST /question/matchCount */
export async function matchQuestionCount(
  body: API.MatchCountDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMatchCountVO>('/question/matchCount', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 保存组卷结果为试卷 POST /examPaper/assemble/save */
export async function saveAssembleResult(
  body: API.AssemblySaveDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseExamPaperVO>('/examPaper/assemble/save', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 创建组卷策略 POST /paperStrategy/add */
export async function addPaperStrategy(
  body: API.PaperStrategyAddDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePaperStrategyVO>('/paperStrategy/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取策略列表 POST /paperStrategy/list/page */
export async function listPaperStrategyByPage(
  body: API.PaperStrategyQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePaperStrategyVO>('/paperStrategy/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 贪心算法组卷 POST /paperStrategy/assemble/greedy */
export async function greedyAssemble(
  body: API.AssemblyRequestDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAssemblyResultVO>('/paperStrategy/assemble/greedy', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 遗传算法组卷 POST /paperStrategy/assemble/genetic */
export async function geneticAssemble(
  body: API.AssemblyRequestDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAssemblyResultVO>('/paperStrategy/assemble/genetic', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 增强版AI组卷 POST /examPaper/ai/assemble/v2 */
export async function aiAssemblePaperV2(
  body: API.AIPaperAssemblyV2DTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAIAssemblyStrategyVO>('/examPaper/ai/assemble/v2', {
    ...(options || {}),
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}

/** 确认AI组卷方案 POST /examPaper/ai/confirm */
export async function confirmAIAssembly(
  body: any,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseExamPaperVO>('/examPaper/ai/confirm', {
    ...(options || {}),
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}

/** 获取用户学习画像 POST /examPaper/ai/profile */
export async function getAIProfile(options?: { [key: string]: any }) {
  return request<API.BaseResponseAIProfileVO>('/examPaper/ai/profile', {
    ...(options || {}),
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  })
}

/** 查询AI对话历史 POST /examPaper/ai/chat/history */
export async function getAIChatHistory(
  body: API.AIChatQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAIChatVO>('/examPaper/ai/chat/history', {
    ...(options || {}),
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}

/** 复用历史对话策略 POST /examPaper/ai/chat/reuse/{chatId} */
export async function reuseChatStrategy(
  chatId: number,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAIAssemblyStrategyVO>(`/examPaper/ai/chat/reuse/${chatId}`, {
    ...(options || {}),
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  })
}

// ===== 试卷导出 API =====
export async function exportWord(body: API.ExportConfigDTO) {
  return request<Blob>('/examPaper/export/word', {
    method: 'POST', responseType: 'blob',
    headers: { 'Content-Type': 'application/json' }, data: body,
  })
}
export async function exportPdf(body: API.ExportConfigDTO) {
  return request<Blob>('/examPaper/export/pdf', {
    method: 'POST', responseType: 'blob',
    headers: { 'Content-Type': 'application/json' }, data: body,
  })
}
export async function previewPaper(body: API.ExportConfigDTO) {
  return request<API.BaseResponseString>('/examPaper/export/preview', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: body,
  })
}
export async function listExportFiles() {
  return request<API.BaseResponseListExportFileVO>('/examPaper/export/files', { method: 'GET' })
}
export async function deleteExportFile(index: number) {
  return request<API.BaseResponseBoolean>(`/examPaper/export/delete/${index}`, { method: 'POST' })
}

/** 复制试卷 POST /examPaper/copy/{id} */
export async function copyExamPaper(id: number, options?: { [key: string]: any }) {
  return request<API.BaseResponseExamPaperVO>(`/examPaper/copy/${id}`, {
    method: 'POST',
    ...(options || {}),
  })
}

/** 分享试卷给用户 POST /paperShare/user/{paperId}/{targetUserId} */
export async function sharePaperToUser(paperId: number, targetUserId: number) {
  return request<API.BaseResponseLong>(`/paperShare/user/${paperId}/${targetUserId}`, {
    method: 'POST',
  })
}

/** 获取分享记录 GET /paperShare/paper/{paperId} */
export async function getPaperShares(paperId: number) {
  return request<API.BaseResponseListLong>(`/paperShare/paper/${paperId}`, { method: 'GET' })
}

/** 撤销分享 POST /paperShare/revoke/{shareId} */
export async function revokeShare(shareId: number) {
  return request<API.BaseResponseBoolean>(`/paperShare/revoke/${shareId}`, { method: 'POST' })
}

/** 获取未读通知数 GET /notification/count */
export async function getUnreadNotificationCount() {
  return request<API.BaseResponseInteger>('/notification/count', { method: 'GET' })
}

/** 获取未读通知 GET /notification/unread */
export async function getUnreadNotifications() {
  return request<API.BaseResponseListMapObjectObject>('/notification/unread', { method: 'GET' })
}

/** 标记通知已读 POST /notification/read/{id} */
export async function markNotificationRead(id: number) {
  return request<API.BaseResponseBoolean>(`/notification/read/${id}`, { method: 'POST' })
}
