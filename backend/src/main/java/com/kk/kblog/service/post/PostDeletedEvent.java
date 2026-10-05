package com.kk.kblog.service.post;

/**
 * 文章已删除（事务提交后清理相关缓存）
 */
public record PostDeletedEvent(long postId) {
}
