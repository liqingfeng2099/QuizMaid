/* eslint-disable */
import request from '@/request'

/** 管理员查看所有试卷（含已删除） POST /admin/paper/list-all */
export async function listAllPapers(body: any, options?: { [key: string]: any }) {
  return request<API.BaseResponsePageExamPaperVO>('/admin/paper/list-all', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: body, ...(options || {}),
  })
}

/** 管理员批量删除试卷 POST /admin/paper/batch-delete */
export async function batchDeletePapers(ids: number[], options?: { [key: string]: any }) {
  return request<API.BaseResponseInteger>('/admin/paper/batch-delete', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: ids, ...(options || {}),
  })
}

/** 管理员强制删除试卷 POST /admin/paper/force-delete */
export async function forceDeletePaper(id: number, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/admin/paper/force-delete', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: { id }, ...(options || {}),
  })
}

/** 管理员批量修改试卷状态 POST /admin/paper/batch-status */
export async function batchUpdatePaperStatus(status: number, ids: number[], options?: { [key: string]: any }) {
  return request<API.BaseResponseInteger>(`/admin/paper/batch-status?status=${status}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, data: ids, ...(options || {}),
  })
}
