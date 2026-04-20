/** 后端统一响应体 —— 与 Java Result<T> 对齐 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  requestId: string
  timestamp: number
}

/** 分页响应 data 字段 */
export interface PageResult<T = unknown> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  hasNext: boolean
}

/** 业务错误码枚举 */
export enum ErrorCode {
  SUCCESS = 200,
  BAD_REQUEST = 40001,
  UNAUTHORIZED = 40101,
  FORBIDDEN = 40301,
  NOT_FOUND = 40401,
  SERVER_ERROR = 50001,
  SECKILL_NOT_START = 50901,
  SECKILL_ENDED = 50902,
  SECKILL_SOLD_OUT = 50903,
  SECKILL_REPEATED = 50904,
}

/** Axios 抛出的业务错误 */
export interface ApiError {
  code: number
  message: string
  requestId: string
}
