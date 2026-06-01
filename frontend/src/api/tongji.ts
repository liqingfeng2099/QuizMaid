import request from '@/request'

/** 获取试卷统计 POST /statistics/paper */
export async function getPaperStatistics(
  body: API.StatisticsQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePaperStatisticsVO>('/statistics/paper', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 获取成绩趋势 POST /statistics/trend */
export async function getScoreTrend(
  body: API.TrendQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListTrendDataPointVO>('/statistics/trend', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 获取当前用户对比 GET /statistics/comparison/{paperId} */
export async function getComparison(
  paperId: number,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePaperStatisticsVO>(
    `/statistics/comparison/${paperId}`,
    { method: 'GET', ...(options || {}) }
  )
}

/** 导出统计Excel POST /statistics/export/excel */
export async function exportStatisticsExcel(
  body: API.StatisticsQueryDTO,
  options?: { [key: string]: any }
) {
  return request<Blob>('/statistics/export/excel', {
    method: 'POST',
    responseType: 'blob',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 导出高频错题Excel GET /statistics/export/wrong-excel/{paperId} */
export async function exportWrongExcel(
  paperId: number,
  options?: { [key: string]: any }
) {
  return request<Blob>(`/statistics/export/wrong-excel/${paperId}`, {
    method: 'GET',
    responseType: 'blob',
    ...(options || {}),
  })
}

// 获取全部题型正确率统计 GET /statistics/type-accuracy .hml
export async function getTypeAccuracy(options?: { [key: string]: any }) {
  return request<API.BaseResponseListQuestionTypeStatVO>('/statistics/type-accuracy', {
    method: 'GET',
    ...(options || {}),
  })
}

// 获取指定试卷的题型正确率统计 GET /statistics/paper/{paperId}/type-accuracy .hml
export async function getPaperTypeAccuracy(
  paperId: number,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListQuestionTypeStatVO>(
    `/statistics/paper/${paperId}/type-accuracy`,
    { method: 'GET', ...(options || {}) }
  )
}

/** 获取可统计试卷列表 POST /statistics/papers-available */
export async function getAvailablePapers(options?: { [key: string]: any }) {
  return request<API.BaseResponseListPaperStatisticsVO>(
    '/statistics/papers-available',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      ...(options || {}),
    }
  )
}
