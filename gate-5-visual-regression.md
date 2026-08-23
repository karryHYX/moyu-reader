# Gate 5 — Android 视觉回归

日期：2026-08-22  
设备：Android 16 / API 36 x86_64 AVD，WHPX，SwiftShader  
主尺寸：1080×2340 @ 440 dpi ≈ 393×851 dp

## Round 1 — 结构一致

检查页面：Onboarding、空书架、导入、导入成功、有书书架、Book Detail、Reader。

发现并修正：

1. 书架首次 Flow 发射前出现短暂空白 → 增加编辑部式骨架行；
2. Reader 把章节标题同时作为 header 和正文首行 → TXT/EPUB 按精确标题去重；
3. Reader Cover 动效在静止时仍保留 elevation，形成灰色页面框 → 仅翻页位移期间绘制 elevation；
4. 第一页章节标题未纳入分页高度 → `firstPageReservedPx` 进入分页约束。

证据：

- `design/gate5/round1-onboarding.png`
- `design/gate5/round1-empty-library-loaded.png`
- `design/gate5/round1-import-success.png`
- `design/gate5/round1-library-books.png`
- `design/gate5/round1-book-detail.png`
- `design/gate5/round1-reader-light.png`

## Round 2 — 视觉一致

检查：Reader Light/Dark、控制层、主题 Sheet。

发现并修正：

1. Material 3 新版 Slider 在 Reader 控制层过于厚重 → 替换为 2–3 dp editorial track + 9 dp anchor；
2. 控制层半透明导致正文 ghosting → 使用不透明 surface，正文仍保持原位；
3. Dark 模式中无 Surface 包裹的默认文本继承黑色 → App 根节点统一提供 background / onBackground；
4. 深色系统栏仍使用黑色 icon → WindowInsetsController 随主题切换；
5. Theme / Reading Settings Sheet 内容在 393 dp 高度下被截断 → 内容区域改为可滚动。

证据：

- `design/gate5/round2-reader-light.png`
- `design/gate5/round2-reader-controls.png`
- `design/gate5/round2-theme-sheet.png`
- `design/gate5/round2-reader-dark.png`

## Round 3 — 细节精修

最终检查：

- Dark Library：文字、divider、accent 与系统栏对比正确；
- Dark Reader Controls：薄进度线、不透明控制层、图标/标签可读；
- Theme Sheet：四宫格与实时预览完整，底部 Reduced Motion 可滚动到达；
- 412×915：Book Detail 横向信息与统计未越界；
- 360×800：Reader 的正文边距、标题层级、footer 与系统手势区稳定；
- TalkBack：顶部 glyph button 补齐 content description。

证据：

- `design/gate5/round3-dark-library.png`
- `design/gate5/round3-reader-controls-dark.png`
- `design/gate5/round3-theme-scroll.png`
- `design/gate5/round3-library-412x915.png`
- `design/gate5/round3-detail-360x800.png`
- `design/gate5/round3-reader-360x800.png`

## 功能联动验证

使用系统 DocumentsUI 从 `/sdcard/Download` 导入项目自有 TXT / EPUB：

- TXT：复制、UTF-8 检测、2 章索引、全文索引成功；
- EPUB 3：metadata、2 章 spine/nav、全文搜索索引成功；
- 两本书在书架展示不同的稳定本地封面模板；
- Detail → Reader 路径成功；
- Light → Dark 主题持久化，重启后仍恢复；
- `ImportReadPersistenceTest` 与 UI smoke 在模拟器 4/4 通过。

## Round 4 — 阅读器专项返工

用户反馈后重新检查分页交互与排版，修正：

1. 父级 `detectTapGestures` 抢占 Pager / LazyColumn 的手势 → 改为 Initial/Final pass 非消费式手势观察；
2. `StaticLayout.setMaxLines` 在目标系统没有形成分页边界 → 改为按 `getLineBottom()` 与可用高度逐窗计算，测试书第一章从错误的 `1 / 1` 恢复为真实 `1 / 6`；
3. 异步分页初值过早保存 offset 0 → 分页 ready 后再恢复/监听语义锚点；
4. 章节切换瞬间复用上一章 PageSlice 导致越界 → 按 chapterId 隔离 Reader composition；
5. 源文本空格与 `TextIndent` 叠加、续页首行重复缩进 → 等长隐藏源空格，只给真正段首应用缩进；默认缩进调整为 0 字，仍提供 0/1/2 字选项；
6. 控制层改为六项工具坞，并加入进度检查点、命中高亮、阅读时钟与本地 TTS。

证据：

- `design/gate5/round4-reader-pagination-final.png`
- `design/gate5/round4-reader-controls-final.png`
- `design/gate5/round4-local-tts.png`

## 结论

五轮回归已完成，核心页面与 C — 现代编辑部设计系统一致。普通尺寸和大屏尺寸通过基础响应式检查，360×800 Reader 通过紧凑尺寸检查。

## Round 5 — 控制层、排版和动效

- Reader 控制层增加中部专用关闭触控层，菜单点击与正文翻页彻底隔离；
- 目录、搜索、书签逐项真机化点击，均停留在各自功能界面；
- Reader Sheet 的 AnimatedContent 改为单 Column 根布局，修复搜索标题与输入框重叠；
- 分页正文改成按段落 Compose 布局，段间距实际影响视觉和分页数量；
- 新增 Paper 翻页并移除 Cover 黑色 shadow；
- 检查底部安全区、跟随系统亮度、音量键翻页与按钮弹簧反馈。

证据：

- `design/gate5/round5-directory.png`
- `design/gate5/round5-search-final.png`
- `design/gate5/round5-bookmarks.png`
- `design/gate5/round5-reading-settings.png`
- `design/gate5/round5-paper-turn.png`


