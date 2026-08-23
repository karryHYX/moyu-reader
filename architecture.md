# 墨屿阅读 Android 架构

## 决策摘要

V1 采用 **单业务模块 + 清晰分包 + 独立性能测试模块**。原因：功能链较长，但团队规模与版本阶段尚不需要十余个 Gradle module；先确保边界、可测试性和构建速度，再按变化频率拆分。

## 依赖方向

```text
ui → model
ui → data façade
data → db / preferences / parser / files
parser → model
reader → model
```

Composable 不直接访问 DAO、ContentResolver 或解析器。所有业务请求先进入 ViewModel，再由 Repository 驱动数据库和文件。

## Single Source of Truth

- 书架、详情、章节、书签：Room Flow；
- 全局阅读设置：DataStore Flow；
- Reader 当前章正文：ViewModel + LRU；
- 导入过程：ImportViewModel 的 immutable state；
- 主题：DataStore → MainActivity → MoyuTheme。

## 文件边界

导入后的私有布局：

```text
files/
├─ books/{bookId}/
│  ├─ source.txt | source.epub
│  └─ cover.jpg | cover.png
└─ fonts/
   ├─ {uuid}.ttf | {uuid}.otf
   └─ {uuid}.name
```

数据库只保存私有绝对路径；用户原 URI 仅在导入期间读取。这样原文件移动、删除或 URI 权限失效后仍可阅读。

## TXT 大文件策略

`ByteLineScanner` 以 64 KiB 缓冲流扫描标题，单行只保留前 8 KiB 供识别，防止异常巨行占满内存。章节 span 最大 1 MiB；无目录小说也会生成内部 span。Reader 只加载当前 span，LRU 总字符预算为 300 万。

## 搜索策略

SQLite FTS4 的 unicode61 不会替中文做自然分词，因此索引层把汉字转换为单字 token，查询使用 phrase expression；ASCII 保留单词。显示正文仍存放在 `search_chunks`，FTS 只负责候选召回，Repository 再用原始查询精确过滤，避免“两个字分散出现”误报。

## EPUB 防御性解析

- 单 XML 最大 2 MiB；
- 单 XHTML 最大 16 MiB；
- cover 最大 10 MiB；
- 缺 container、OPF、spine 或 entry 时返回可理解错误；
- ZIP locator 正规化 `.` / `..`；
- 备份恢复额外校验 canonical path，阻止 Zip Slip。

## 事务

- 导入：Book + Chapters 同一 Room transaction；
- 重解析：清旧索引/书签/标注/章节并插入新章节同一 transaction；
- 备份：在 transaction 中取得一致 snapshot；
- 恢复：书籍、章节、书签、标注、会话同一 transaction，全文索引随后重建。

## Reader

分页输入：viewport width/height、font size、Typeface、line-height、paragraph spacing、horizontal margin、orientation。输出仅保存 `[start, endExclusive]` 范围，避免复制页面正文。

锚点使用章节 ID 与字符偏移。翻页后 350 ms debounce 持久化，防止每帧写数据库。控制层叠加在正文之上，显示/隐藏不会改变正文约束。

## 主题与动效

四套主题共享同一语义角色：background、surface、readerBackground、readerText、accent、divider、selection。主题颜色在 340 ms 内插值；Reduced Motion 将时长降为 0。主按钮使用“收笔”反馈：96.5% scale、不对称圆角、14→30 dp 的前导线。

## 安全与隐私

- 合并 APK 无 INTERNET permission；
- 无 API key、服务端或 Analytics；
- SAF 最小权限；
- FileProvider `exported=false`；
- 备份和字体文件均有尺寸/路径校验；
- 用户可在设置中看到与代码一致的隐私说明。

