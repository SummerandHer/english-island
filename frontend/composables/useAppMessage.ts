import { useMessage } from 'naive-ui'
import type { MessageApi } from 'naive-ui'

const noopMessage: MessageApi = {
  info: () => {},
  success: () => {},
  warning: () => {},
  error: () => {},
  loading: () => {},
  create: () => {},
  destroyAll: () => {}
} as MessageApi

/** Naive UI 消息提示，SSR 阶段返回空实现避免 useMessage 未定义 */
export function useAppMessage(): MessageApi {
  if (import.meta.server) {
    return noopMessage
  }
  return useMessage()
}
