/* eslint-disable */
import request from '@/request'

/** 创建导出模板 POST /export-template/create */
export async function createExportTemplate(body: { name: string; exportType: string; config: string }, options?: { [key: string]: any }) {
  return request<API.BaseResponseExportTemplate>('/export-template/create', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: body, ...(options || {}),
  })
}

/** 更新导出模板 POST /export-template/update/{id} */
export async function updateExportTemplate(id: number, body: { name?: string; config?: string }, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>(`/export-template/update/${id}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: body, ...(options || {}),
  })
}

/** 删除导出模板 POST /export-template/delete/{id} */
export async function deleteExportTemplate(id: number, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>(`/export-template/delete/${id}`, { method: 'POST', ...(options || {}) })
}

/** 设为默认模板 POST /export-template/set-default/{id} */
export async function setDefaultExportTemplate(id: number, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>(`/export-template/set-default/${id}`, { method: 'POST', ...(options || {}) })
}

/** 获取模板列表 GET /export-template/list */
export async function listExportTemplates(exportType?: string, options?: { [key: string]: any }) {
  return request<API.BaseResponseListExportTemplate>(`/export-template/list${exportType ? `?exportType=${exportType}` : ''}`, {
    method: 'GET', ...(options || {}),
  })
}
