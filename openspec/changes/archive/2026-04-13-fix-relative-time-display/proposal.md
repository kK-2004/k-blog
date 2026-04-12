## Why

文章发布/更新后的相对时间显示存在精度问题：1-5分钟内始终显示"5分钟前"而非精确的"1分钟前"、"2分钟前"等。原因是时间格式化逻辑将分钟数向下取整到5的倍数并以5为最小值，导致短时间范围内的显示不精确。

## What Changes

- 修改 `ArticleMeta.vue` 中 `formattedDate` 和 `formattedUpdatedDate` 的时间格式化逻辑
- 修改 `MacBlogCard.vue` 中 `displayPostTime` 的时间格式化逻辑
- 统一时间显示规则：
  - 0-1分钟：显示"刚刚"
  - 1-10分钟：显示精确分钟数（"x分钟前"）
  - 超过10分钟：显示具体日期时间

## Capabilities

### New Capabilities

(无)

### Modified Capabilities

- `relative-time-format`: 文章卡片和文章详情页的相对时间显示规则调整

## Impact

- `frontend/src/components/article/ArticleMeta.vue`：`formattedDate` 和 `formattedUpdatedDate` 计算属性
- `frontend/src/components/mac/MacBlogCard.vue`：`displayPostTime` 计算属性
- 纯前端变更，不影响后端API
