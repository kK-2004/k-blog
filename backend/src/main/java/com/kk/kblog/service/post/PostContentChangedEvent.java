package com.kk.kblog.service.post;

/**
 * 文章新建或标题/正文变更（事务提交后由 AI 摘要服务监听，按该版本号生成摘要）
 */
public record PostContentChangedEvent(long postId, long contentVersion) {
}
