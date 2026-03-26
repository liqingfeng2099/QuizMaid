// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /question/import/status/${param0} */
export async function getImportStatus(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getImportStatusParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseMapObjectObject>(`/question/import/status/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /question/import/upload */
export async function uploadExcel(body: {}, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/question/import/upload', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
