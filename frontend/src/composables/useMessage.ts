/**
 * 消息通知 Composable
 * 封装 ElMessage，提供便捷的消息提示方法
 */
import { ElMessage, ElMessageBox } from 'element-plus'
import type { MessageBoxData } from 'element-plus'

export type MessageType = 'success' | 'warning' | 'info' | 'error'

/**
 * 显示消息提示
 */
export function showMessage(
  message: string,
  type: MessageType = 'info',
  duration = 1500,
  showClose = false
) {
  return ElMessage({
    message,
    type,
    duration,
    showClose,
    customClass: 'modern-message',
  })
}

/**
 * 显示成功消息
 */
export function showSuccess(message: string, duration = 800) {
  console.log('duration' + duration)
  return showMessage(message, 'success', duration)
}

/**
 * 显示错误消息
 */
export function showError(message: string, duration = 3000) {
  return showMessage(message, 'error', duration, true)
}

/**
 * 显示警告消息
 */
export function showWarning(message: string, duration = 2500) {
  return showMessage(message, 'warning', duration)
}

/**
 * 显示信息消息
 */
export function showInfo(message: string, duration = 2000) {
  return showMessage(message, 'info', duration)
}

/**
 * 显示确认对话框
 */
export function showConfirm(
  message: string,
  title = '提示',
  confirmButtonText = '确定',
  cancelButtonText = '取消'
): Promise<MessageBoxData> {
  return ElMessageBox.confirm(message, title, {
    confirmButtonText,
    cancelButtonText,
    type: 'warning',
    customClass: 'modern-confirm',
  })
}

/**
 * 显示警告对话框
 */
export function showAlert(
  message: string,
  title = '提示',
  type: MessageType = 'info'
): Promise<MessageBoxData> {
  return ElMessageBox.alert(message, title, {
    confirmButtonText: '确定',
    type,
    customClass: 'modern-alert',
  })
}

/**
 * useMessage Composable
 */
export function useMessage() {
  return {
    success: showSuccess,
    error: showError,
    warning: showWarning,
    info: showInfo,
    show: showMessage,
    confirm: showConfirm,
    alert: showAlert,
  }
}
