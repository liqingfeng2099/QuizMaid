// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 添加试题 POST /question/add */
export async function addQuestion(body: API.QuestionAddDTO, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/question/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 批量添加试题 POST /question/add/batch */
export async function batchAddQuestion(
  body: API.QuestionAddDTO[],
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListLong>('/question/add/batch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 逻辑删除试题 POST /question/delete */
export async function deleteQuestion(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/question/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 根据ID获取试题详情 GET /question/get/${param0} */
export async function getQuestionById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getQuestionByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseQuestionVO>(`/question/get/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 分页查询试题 POST /question/list/admin/page */
export async function listAllQuestionByPage(
  body: API.QuestionQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageQuestionVO>('/question/list/admin/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 分页查询试题 POST /question/list/page */
export async function listQuestionByPage(
  body: API.QuestionQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageQuestionVO>('/question/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 修改试题状态 POST /question/status */
export async function updateStatus(body: API.QuestionStatusDTO, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/question/status', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 更新试题 POST /question/update */
export async function updateQuestion(
  body: API.QuestionUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/question/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
