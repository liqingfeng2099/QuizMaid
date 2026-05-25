import request from '@/request'

/** 开始考试 POST /exam/start/{paperId} */
export async function startExam(paperId: number) {
  return request<API.BaseResponseExamRecordVO>(`/exam/start/${paperId}`, {
    method: 'POST',
  })
}

/** 提交考试 POST /exam/submit */
export async function submitExam(body: { recordId: number; answers: Record<string, string> }) {
  return request<API.BaseResponseExamResultVO>('/exam/submit', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}

/** 自动交卷 POST /exam/auto-submit/{recordId} */
export async function autoSubmitExam(recordId: number) {
  return request<API.BaseResponseExamResultVO>(`/exam/auto-submit/${recordId}`, {
    method: 'POST',
  })
}

/** 查看考试结果 GET /exam/result/{recordId} */
export async function getExamResult(recordId: number) {
  return request<API.BaseResponseExamResultVO>(`/exam/result/${recordId}`, {
    method: 'GET',
  })
}

/** 获取进行中考试 GET /exam/ongoing/{paperId} */
export async function getOngoingExam(paperId: number) {
  return request<API.BaseResponseExamRecordVO>(`/exam/ongoing/${paperId}`, {
    method: 'GET',
  })
}

/** 获取考试记录 GET /exam/records */
export async function getExamRecords(paperId?: number) {
  return request<API.BaseResponseListExamRecordVO>(
    `/exam/records${paperId ? `?paperId=${paperId}` : ''}`,
    { method: 'GET' }
  )
}
