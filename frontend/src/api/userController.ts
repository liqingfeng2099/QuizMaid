/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /users/ */
export async function healthCheck(options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/users/', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/add */
export async function addUser(body: API.UserAddDTO, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/users/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/admin/update */
export async function updateUserByAdmin(
  body: API.UserUpdateByAdminDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUserVO>('/users/admin/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/bindemail */
export async function bindEmail(body: API.BindEmailDTO, options?: { [key: string]: any }) {
  return request<API.BaseResponseUserVO>('/users/bindemail', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /users/continuous_sign_in_days */
export async function getSignInDays1(options?: { [key: string]: any }) {
  return request<API.BaseResponseInteger>('/users/continuous_sign_in_days', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/del/user */
export async function deleteUser(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/users/del/user', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/email/login */
export async function emailLogin(body: API.UserLoginByEmailDTO, options?: { [key: string]: any }) {
  return request<API.BaseResponseUserLoginVO>('/users/email/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/email/registry */
export async function emailRegister(
  body: API.UserRegisterByEmailDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUser>('/users/email/registry', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /users/get/login */
export async function getLoginUserInfo(options?: { [key: string]: any }) {
  return request<API.BaseResponseUserLoginVO>('/users/get/login', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /users/get/user */
export async function getUserById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUser>('/users/get/user', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /users/get/uservo */
export async function getUserVoById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserVoByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUser>('/users/get/uservo', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /users/heatmap/${param0} */
export async function getSignInDays(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getSignInDaysParams,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params
  return request<API.BaseResponseListUserHeatMapVO>(`/users/heatmap/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/list/page/vo */
export async function listUserByPage(body: API.UserQueryDTO, options?: { [key: string]: any }) {
  return request<API.BaseResponsePageUserVO>('/users/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/login */
export async function login(body: API.UserLoginDTO, options?: { [key: string]: any }) {
  return request<API.BaseResponseUserLoginVO>('/users/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/logout */
export async function logout(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/users/logout', {
    method: 'POST',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/register */
export async function register(body: API.UserRegisterDTO, options?: { [key: string]: any }) {
  return request<API.BaseResponseUser>('/users/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/resetpassword */
export async function resetPassword(body: API.ResetPasswordDTO, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/users/resetpassword', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/send */
export async function sendEmail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.sendEmailParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>('/users/send', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/signdata */
export async function getUserSignData(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserSignDataParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapLocalDateBoolean>('/users/signdata', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/signin */
export async function userSignIn(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/users/signin', {
    method: 'POST',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /users/update */
export async function updateSelf(body: API.UserUpdateDTO, options?: { [key: string]: any }) {
  return request<API.BaseResponseUserVO>('/users/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
