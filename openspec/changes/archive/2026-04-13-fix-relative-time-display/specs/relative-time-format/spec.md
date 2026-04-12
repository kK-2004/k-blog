## MODIFIED Requirements

### Requirement: Relative time display for articles
文章卡片和文章详情页的时间显示 SHALL 遵循以下规则：
- 距离当前时间不足1分钟（60秒）时，显示"刚刚"
- 距离当前时间1-10分钟时，显示精确分钟数，格式为"x分钟前"
- 距离当前时间超过10分钟时，显示具体日期时间（`toLocaleString('zh-CN')`）

此规则统一适用于：
- `ArticleMeta.vue` 中的发布时间（`formattedDate`）和更新时间（`formattedUpdatedDate`）
- `MacBlogCard.vue` 中的文章时间（`displayPostTime`）

#### Scenario: 刚刚发布的文章
- **WHEN** 文章发布时间距今不足60秒
- **THEN** 显示"刚刚"

#### Scenario: 发布1分钟的文章
- **WHEN** 文章发布时间距今61秒
- **THEN** 显示"1分钟前"

#### Scenario: 发布3分钟的文章
- **WHEN** 文章发布时间距今3分钟
- **THEN** 显示"3分钟前"（而非"5分钟前"）

#### Scenario: 发布9分钟的文章
- **WHEN** 文章发布时间距今9分钟
- **THEN** 显示"9分钟前"

#### Scenario: 发布超过10分钟的文章
- **WHEN** 文章发布时间距今超过10分钟
- **THEN** 显示具体日期时间字符串
