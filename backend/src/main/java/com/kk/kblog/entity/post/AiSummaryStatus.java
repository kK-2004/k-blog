package com.kk.kblog.entity.post;

/**
 * 文章 AI 摘要生成状态
 */
public enum AiSummaryStatus {
    /** 生成中（尚未落库） */
    GENERATING,
    /** 已生成并落库 */
    READY,
    /** 生成失败（再次编辑或应用重启时重新生成） */
    FAILED
}
