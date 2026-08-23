# 墨屿阅读 / Moyu Reader

墨屿阅读是一款 **100% 本地、100% 离线** 的 Android 小说阅读器。项目采用 Kotlin、Jetpack Compose、Room、DataStore 与 Storage Access Framework，实现 TXT / DRM-Free EPUB 导入、章节索引、分页/滚动阅读、全文搜索、书签、本地 TTS、自定义字体、主题、统计和本地备份恢复。

> 设计方向：C — 现代编辑部（typography-first、calm、editorial、tactile）。

## 当前状态

- Gate 1 产品定义：完成
- Gate 2 Image Gen 风格探索：完成
- Gate 3 Figma / Design System：完成并通过
- Gate 4 Android 工程：实现与构建验证完成
- Gate 5 视觉回归：五轮模拟器截图对照完成
- Gate 6 性能实测：1/10/50/100 MiB 解析与模拟器启动基准完成；物理机复测待发布设备
- Gate 7 APK 交付：v1.4.5 正式签名 APK / AAB、校验文件与商店资料包已生成

## v1.4.5 发布与体验修订

- 生成 Google Play 可提交的正式签名 AAB、侧载 APK 与 SHA-256 校验清单；
- 在 `release/` 目录附带商店文案、隐私政策、图标与截图资料包；
- 目录抽屉收紧尺寸并自动定位当前章节，阅读器排版入口对齐优化；
- 新增淡入翻页、翻页速度调节和自由首行缩进输入。
## v1.4.0 核心阅读能力补充

- 书籍详情新增书名/作者编辑、标记读完、重置进度、删除确认；
- Reader 目录新增章节搜索、正倒序切换、当前/已读/未读状态标识；
- 阅读进度面板新增 6/12/20 秒自动阅读，适配分页与滚动两种模式；选择“手动”即可暂停；
- 批量导入、TXT/EPUB、文件夹扫描、全书搜索、书签、备份、字体和离线隐私能力保持可用。

## v1.3.1 真机分页与流畅度修订

- 分页器按实际渲染的段落/续段分别测量，断页不再沿用跨段布局结果，修复页尾残字、下一页续字和非末页大空缺；
- 重排期间暂不显示旧分页内容，改为短暂加载态，消除翻页或换章时页尾字符闪现；
- Cover/Paper 改为轻量合成层变换，并预取相邻页，降低低性能设备拖动与翻页卡顿；
- 书架排序入口改为独立的细线排序图标，移除占位过大的“排序”块。

## v1.3.0 分页、详情与书架修订

- 分页测量改为与 Compose 的绝对行高一致；非末页优先在句末停住，并限制为句末回退牺牲的空白高度；章节末页增加“本章完”收束；
- 书籍详情中的目录、全文搜索、书签改为各自独立的可操作面板：可跳转到章节、搜索命中或书签位置；
- 阅读排版重组为文字、段落、翻页、显示与按键四个分区，翻页方式以可辨别的 2 × 2 卡片呈现；
- Instant、Slide、Cover、Paper 四种翻页分别应用即时、平移、书封透视与纸页卷动效果；
- 书架新增可点按的全部 / 未读 / 在读 / 已读 / 收藏筛选，排序入口和排序 Sheet 同步重绘。

## v1.2.0 Reader 交互与动效修订

- 阻断控制层点击穿透，目录、全文搜索、书签现在稳定打开各自真实面板；
- 分页正文按段落独立布局，段间距 `0–28 dp` 会真实参与显示与分页计算，并提供无/4/8/16 dp 快捷值；
- 分页高度增加页脚安全区，避免页面末行被页码或系统手势区遮挡；
- 亮度设置加入“跟随系统”按钮；
- 加入音量键翻页：音量加上一页、音量减下一页；朗读面板打开时恢复系统音量行为；
- 新增“纸页”翻页：轻微 Y 轴透视、位移与纸边渐层；Cover 移除黑色 elevation，浅色主题不再出现黑边；
- 主导航、设置子页与 Reader Sheet 加入方向感淡入滑动，图标按钮加入弹簧缩放和轻旋转反馈。

## v1.1.0 阅读体验修订

- 修复分页触控冲突：左右 30% 点击稳定翻页，中间点击控制层；分页横滑与滚动模式纵滑互不抢占；
- 加入章节边界连续导航：末页继续向右进入下一章，下一章首页向左返回上一章末页；
- 修复 `StaticLayout.setMaxLines` 导致整章被误判成单页的问题，改为按真实行底部高度分页；
- 修复异步分页完成前覆盖语义锚点的问题，重新排版与重启会恢复到正确页面；
- 清理源文件自带空格/全角空格，按真实段落应用首行缩进，续页首行不再产生额外缩进；
- 新增本地 TTS、语速与定时停止、阅读时钟、文本选择复制、进度检查点和搜索命中高亮；
- 重做 Reader 控制层与排版面板，加入目录、进度、搜索、书签、主题、排版六项工具及排版预设。

## 技术栈

- Kotlin 2.3.21
- Android Gradle Plugin 9.3.0 / Gradle 9.5
- `minSdk 26` / `compileSdk 37` / `targetSdk 37`
- Jetpack Compose + Material 3（Compose BOM 2026.08.00）
- ViewModel、Coroutines、Flow / StateFlow、Navigation Compose
- Room + FTS4、DataStore Preferences
- Storage Access Framework、DocumentFile
- Jsoup（EPUB HTML）、juniversalchardet（编码检测）
- Baseline Profile、Macrobenchmark、R8

## 架构

```text
Compose UI
  ↓ immutable UiState / event
ViewModel
  ↓
Repository
  ├─ Room / FTS
  ├─ DataStore
  ├─ SAF / Private Storage
  └─ BookParser
       ├─ TxtBookParser
       └─ EpubBookParser
```

代码按职责分包，当前保持单 `app` 业务模块，避免 V1 过度模块化；性能测试使用独立 `baselineprofile` 测试模块。详细说明见 [`docs/architecture.md`](docs/architecture.md)。

## 目录结构

```text
app/src/main/java/com/moyu/reader/
├─ data/
│  ├─ db/                 Room entities / DAO / FTS
│  ├─ preferences/        DataStore
│  ├─ BookImportRepository.kt
│  ├─ LibraryRepository.kt
│  ├─ BackupRepository.kt
│  └─ FontRepository.kt
├─ model/                 immutable domain models
├─ parser/                TXT / EPUB / charset / chapter detection
├─ reader/                pagination and semantic progress
├─ ui/
│  ├─ designsystem/       colors / type / components / motion
│  ├─ onboarding/
│  ├─ library/
│  ├─ importbook/
│  ├─ book/
│  ├─ reader/
│  └─ settings/
└─ util/

baselineprofile/          Baseline Profile + Macrobenchmark tests
design/imagegen/          A/B/C/D Image Gen outputs
figma-capture/            Figma capture / local interaction reference
docs/                     UX, architecture, feature and QA documents
```

## 支持格式

### V1 一级支持

- TXT：UTF-8、UTF-8 BOM、UTF-16 LE/BE、GBK、GB18030、Big5
- EPUB 2 / EPUB 3：DRM-Free；metadata、manifest、spine、NCX、nav、cover 与 XHTML 正文

### 暂未支持

- MOBI / AZW / AZW3
- PDF
- CBZ / CBR
- DRM EPUB

## 如何运行

1. 安装 JDK 17 与 Android SDK 37。
2. 用 Android Studio 打开项目，或创建本地 `local.properties`：

   ```properties
   sdk.dir=C\:\\path\\to\\Android\\Sdk
   ```

3. 构建：

   ```powershell
   .\gradlew.bat :app:assembleDebug
   ```

4. Debug APK：

   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

已生成的 Release 签名安装包：

```text
dist/MoyuReader-1.4.5-release-signed.apk
```

证书为本项目本地 Release 证书（RSA 4096 / SHA-256，APK Signature Scheme v2 + v3，并附 v4 `.idsig`）。签名密钥保存在本工作区忽略提交的 `.signing/` 目录，以便后续版本保持相同证书升级安装。

本工作区已生成 APK。应用合并清单中没有 `android.permission.INTERNET`。

## Build 与检查

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :baselineprofile:assemble
```

Release 变体启用 R8 与资源压缩；正式分发前通过 Gradle property 或同名环境变量配置签名，仓库不保存密钥：

```text
MOYU_KEYSTORE_PATH
MOYU_KEYSTORE_PASSWORD
MOYU_KEY_ALIAS
MOYU_KEY_PASSWORD
```

未配置时生成 unsigned Release APK/AAB，配置后同一构建命令生成签名产物。

## 文件导入机制

1. 通过 SAF 选择一个/多个文件或授权目录；
2. 使用 128 KiB 缓冲流复制到 staging，同时计算 SHA-256；
3. 以 hash 查重并检查剩余空间；
4. 后台调度器解析编码、metadata 与章节；
5. 原文件复制到 `/files/books/{bookId}/source.*`；
6. metadata、章节 byte span / EPUB locator 写入 Room；
7. 分块建立本地全文索引；
8. 失败时清理 staging，不影响现有书库。

目录扫描最多递归 12 层，只接收已支持格式，不请求“管理全部文件”权限。

## TXT 解析机制

- BOM 优先，随后使用 universal detector + 多候选可读性评分；
- 支持用户指定编码重新解析；
- 流式扫描原始字节，不调用 `readText()` 读取整本大书；
- 多组中英文章节规则、标题长度、空行上下文、候选分数与最小间距共同抑制误判；
- 章节保存 `byteOffset / byteLength`；
- 超长章节以不超过 1 MiB 的内部段切分；
- 无章节文件生成可读的 1 MiB 虚拟段，100 MB 文件不会整本常驻内存。

## EPUB 解析机制

- 校验 mimetype / container；
- 解析 OPF metadata、manifest、spine；
- EPUB 3 nav 与 EPUB 2 NCX 均用于章节标题；
- Jsoup 清理脚本、样式和 SVG，仅保留正文块；
- ZIP entry 有尺寸上限并拒绝缺失资源；
- 阅读时按 spine locator 解压单章，不把整本 EPUB 常驻内存。

## 阅读位置机制

持久化语义位置：

```text
bookId + chapterId + characterOffset + percentage
```

分页由 viewport、字体像素、行高、页边距与方向计算。字体、尺寸或方向改变后重新分页，再用 `characterOffset` 找回接近原位置，而不是保存脆弱的全书页码。

## 数据存储

- Room：书籍、章节、书签、标注、阅读会话、FTS 搜索块；
- DataStore：主题、字体、排版、翻页、方向、亮度、书架布局；
- App Private Storage：小说副本、EPUB cover、自定义字体；
- MoyuBackup.zip：metadata、章节、书签、标注、统计、设置，可选小说源文件与字体。

## 性能设计

- 复制、hash、解析、EPUB 解压、索引、备份和恢复均运行在 IO dispatcher；
- TXT 流式扫描；章节按需读取；
- 3,000,000 字符上限的访问顺序 LRU chapter cache；
- 全文按 800 字符、80 字符重叠分块索引；
- 中文 FTS 使用逐汉字 token + phrase query，点击结果回到章节字符偏移；
- Compose 列表使用稳定 key；
- Release 开启 R8；
- `baselineprofile` 覆盖启动、书架、导入、打开书籍、Reader 与目录路径。

## 测试方法

- Unit：章节识别、编码、TXT、EPUB 3、分页边界、进度、SHA-256；
- Instrumentation：真实 TXT / EPUB fixture 导入、章节读取、搜索与进度落盘；
- UI smoke：Onboarding → Library；
- Lint：API 26 兼容、Manifest、Compose、资源；
- Macrobenchmark：Cold / Warm startup 与 FrameTiming。

测试 fixture 均为项目自写内容，见 `app/src/test/resources/fixtures/`。

## 隐私

- 不需要账户；
- 不上传小说或阅读记录；
- 不含广告与统计 SDK；
- 不申请 `android.permission.INTERNET`；
- 所有业务数据保存在设备本地。

## 已知限制

见 [`docs/known-issues.md`](docs/known-issues.md)。

