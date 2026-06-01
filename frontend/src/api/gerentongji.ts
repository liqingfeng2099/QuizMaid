import request from '@/request'

/** 获取个人多维度统计 GET /personal/stats */
export async function getPersonalStats(params?: {
  subject?: string; period?: string; startDate?: string; endDate?: string
}) {
  return request<API.BaseResponsePersonalStatsVO>('/personal/stats', {
    method: 'GET', params,
  })
}

/** 导出个人统计Excel GET /personal/stats/export */
export async function exportPersonalStats(params?: { subject?: string; period?: string }) {
  return request<Blob>('/personal/stats/export', {
    method: 'GET', params, responseType: 'blob',
  })
}

/** 错题列表 POST /error-book/list */
export async function listErrors(body: any) {
  return request<API.BaseResponsePageErrorBook>('/error-book/list', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: body,
  })
}

/** 更新复习状态 POST /error-book/review-status/{id} */
export async function updateReviewStatus(id: number, reviewStatus: number) {
  return request<API.BaseResponseBoolean>(`/error-book/review-status/${id}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    data: { reviewStatus },
  })
}

/** 更新错误类型 POST /error-book/error-type/{id} */
export async function updateErrorType(id: number, errorType: number) {
  return request<API.BaseResponseBoolean>(`/error-book/error-type/${id}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    data: { errorType },
  })
}

/** 归档/取消归档 POST /error-book/archive/{id} */
export async function toggleArchive(id: number) {
  return request<API.BaseResponseBoolean>(`/error-book/archive/${id}`, { method: 'POST' })
}

/** 删除错题 POST /error-book/delete */
export async function deleteError(id: number) {
  return request<API.BaseResponseBoolean>('/error-book/delete', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    data: { id },
  })
}

/** 错题统计 GET /error-book/stats */
export async function getErrorStats() {
  return request<API.BaseResponseMapObjectObject>('/error-book/stats', { method: 'GET' })
}

/** 薄弱知识点 GET /error-book/weak-points */
export async function getWeakKnowledgePoints() {
  return request<API.BaseResponseListPersonalDimensionVO>('/error-book/weak-points', { method: 'GET' })
}

// ===== 同类错题推荐 =====
export async function recommendQuestions(body: { count?: number; difficultyTendency?: string; includeAnalysis?: boolean; filterRecentEnabled?: boolean; filterRecentDays?: number }) {
  return request<API.BaseResponseListQuestionVO>('/error-book/recommend', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: body,
  })
}
export async function recommendFeedback(questionId: number, correct: boolean) {
  return request<API.BaseResponseBoolean>(`/error-book/recommend/feedback/${questionId}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: { correct },
  })
}

// ===== 批量操作 =====
export async function batchDeleteErrors(ids: number[]) {
  return request<API.BaseResponseInteger>('/error-book/batch/delete', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: ids,
  })
}
export async function batchUpdateReviewStatus(ids: number[], reviewStatus: number) {
  return request<API.BaseResponseInteger>('/error-book/batch/review-status', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: { ids, reviewStatus },
  })
}

// ===== 分组 =====
export async function createGroup(groupName: string, description?: string) {
  return request<API.BaseResponseErrorBookGroup>('/error-book/group/create', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: { groupName, description },
  })
}
export async function listGroups() {
  return request<API.BaseResponseListErrorBookGroup>('/error-book/group/list', { method: 'GET' })
}

// ===== 备注 =====
export async function addNote(errorBookId: number, content: string, noteType?: number) {
  return request<API.BaseResponseErrorBookNote>('/error-book/note/add', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    data: { errorBookId, content, noteType: noteType || 1 },
  })
}
export async function listNotes(errorBookId: number) {
  return request<API.BaseResponseListErrorBookNote>(`/error-book/note/list/${errorBookId}`, { method: 'GET' })
}

// ===== 导出 =====
export async function exportErrorBookExcel() {
  return request<Blob>('/error-book/export/excel', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: {},
    responseType: 'blob',
  })
}
export async function previewErrorBook() {
  return request<API.BaseResponseString>('/error-book/export/preview', { method: 'GET' })
}

// ===== 强化组卷 =====
export async function reinforceAssemble(body: { paperName?: string; questionCount?: number; difficultyAvg?: number; duration?: number }) {
  return request<API.BaseResponseExamPaperVO>('/error-book/assemble/reinforce', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: body,
  })
}
