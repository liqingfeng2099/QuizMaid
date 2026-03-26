// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 添加试题到试卷 POST /paperQuestion/add */
export async function addQuestionToPaper(
  body: API.PaperQuestionAddDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/paperQuestion/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 从试卷中移除试题 POST /paperQuestion/delete */
export async function removeQuestionFromPaper(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/paperQuestion/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 更新试卷中试题的分值或排序 POST /paperQuestion/update */
export async function updatePaperQuestion(
  body: API.PaperQuestionUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/paperQuestion/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
