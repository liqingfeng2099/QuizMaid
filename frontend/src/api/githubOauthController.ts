/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /oauth/github/callback */
export async function callback(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.callbackParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUserLoginVO>('/oauth/github/callback', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /oauth/github/login */
export async function githubLogin(options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/oauth/github/login', {
    method: 'GET',
    ...(options || {}),
  })
}
