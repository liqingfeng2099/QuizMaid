// @ts-ignore
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
