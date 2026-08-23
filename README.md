# 墨屿阅读 / Moyu Reader

<p align="center">
  <img src="release/store-assets/icon-512.png" width="128" alt="墨屿阅读图标" />
</p>

<p align="center"><strong>一款专注本地阅读体验的 Android 小说阅读器</strong></p>

<p align="center">
  <a href="https://github.com/karryHYX/moyu-reader/releases">下载 v1.4.5</a> ·
  <a href="release/store-listing/store-listing-zh-CN.md">商店文案</a> ·
  <a href="release/store-listing/privacy-policy-zh-CN.md">隐私政策</a>
</p>

## 项目简介

墨屿阅读是一款以“本地优先、安静阅读、细腻排版”为核心方向的 Android 小说阅读器。它面向拥有 TXT、EPUB 等本地书籍的读者，提供从文件导入、章节解析、书架管理到阅读设置、书签搜索和备份恢复的一整套离线体验。

应用采用 Kotlin 与 Jetpack Compose 构建，书籍正文、阅读进度、书签、笔记、主题和统计默认保存在设备本地。打开应用后即可通过 Android 系统文件选择器导入自己的书籍，不需要注册账户，也不依赖在线书源。

## 核心特点

### 本地书架

- 支持单本导入、批量导入和文件夹扫描；
- 支持 TXT 与 DRM-Free EPUB；
- 自动生成封面，也支持自定义书名、作者和封面；
- 最近阅读、阅读进度、已读章节、收藏和置顶；
- 全部、未读、在读、已读、收藏筛选；
- 网格与列表布局；
- 按最近阅读、导入时间、书名排序；
- 书籍详情页集中查看文件类型、大小、字数、章节数和阅读时长。

### 小说解析

- TXT 编码自动识别；
- 支持 UTF-8、UTF-8 BOM、UTF-16、GBK、GB18030、Big5；
- 中文与英文章节标题识别；
- 自动生成目录并支持重新解析；
- 超大 TXT 流式读取，避免一次性载入整本文件；
- 自动整理重复空行、全角空格和首行缩进；
- EPUB 2 / EPUB 3 metadata、spine、NCX、nav 和封面解析；
- 章节按需读取，减少长篇书籍打开时的等待。

### 阅读器

- 上下滚动阅读；
- 左右分页阅读；
- 点击屏幕左右区域翻页；
- 左右滑动翻页；
- 音量加键上一页、音量减键下一页；
- 自动翻页与自动滚动；
- 阅读位置自动保存；
- 章节进度、全书进度、页码和阅读百分比；
- 本地 Android TTS 听书；
- 文字选择、复制、分享、书签和笔记。

### 多种翻页方式

- 即时：立即切换页面；
- 平移：页面水平滑动；
- 淡入：旧页淡出、新页淡入；
- 封面：带透视和位移的书封翻动效果；
- 纸页：带纸张边缘和轻微卷动效果；
- 翻页速度支持 160 / 280 / 420 / 650 ms 调整；
- 动画可在阅读设置中快速切换。

### 排版与主题

- 字体大小、粗细、颜色和自定义字体；
- 行间距、段间距、字间距；
- 左右边距、上下边距、页面宽度；
- 首行缩进支持直接填写数值；
- 标题字号、标题样式和段落对齐；
- 日间、夜间、护眼、羊皮纸、纯白、灰色、墨黑主题；
- 自定义背景色、文字颜色和阅读主题预设；
- 跟随系统深色模式；
- 阅读器独立亮度、跟随系统亮度、屏幕常亮和沉浸式阅读。

### 目录、搜索与标注

- 章节目录抽屉；
- 打开目录自动定位当前章节；
- 当前、已读、未读章节状态；
- 目录搜索与正序 / 倒序；
- 全书搜索、结果数量、结果高亮和点击跳转；
- 添加、删除和快速跳转书签；
- 笔记目录与章节关联；
- 阅读位置和多次跳转位置记录。

### 数据与隐私

- 书籍内容保存在应用私有目录；
- 阅读记录、书签、笔记、主题和设置保存在设备本地；
- 应用清单未声明 `android.permission.INTERNET`；
- 不含广告 SDK 和在线统计 SDK；
- 支持导出本地备份并从备份恢复；
- 删除应用或清除应用数据前，建议先导出备份文件。

## 截图预览

| 书架 | 阅读器 |
|---|---|
| ![书架](release/store-assets/screenshots/01-library.png) | ![阅读器](release/store-assets/screenshots/02-reader.png) |

| 目录 | 排版设置 |
|---|---|
| ![目录](release/store-assets/screenshots/03-directory.png) | ![排版设置](release/store-assets/screenshots/04-reading-settings.png) |

| 全书搜索 |
|---|
| ![搜索](release/store-assets/screenshots/05-search.png) |

## 支持格式

### 当前支持

- `.txt`
- `.epub`（DRM-Free）

### 规划中

- MOBI / AZW / AZW3
- HTML / Markdown
- PDF
- CBZ / CBR

## 下载与安装

前往 [Releases](https://github.com/karryHYX/moyu-reader/releases) 下载最新版本：

- `MoyuReader-1.4.5-release-signed.apk`：直接安装包；
- `MoyuReader-1.4.5-release.aab`：应用商店构建包；
- `MoyuReader-1.4.5-SHA256SUMS.txt`：完整性校验文件。

APK 使用项目正式 Release 证书签名，后续更新将持续复用相同签名。安装前可使用 SHA-256 校验文件确认下载包完整性。

## v1.4.5 更新内容

- 目录抽屉尺寸与条目密度优化；
- 打开目录时自动滚动到当前章节；
- 阅读器排版入口与其他工具按钮统一高度和中心线；
- 新增淡入翻页方式；
- 翻页速度支持自由调整；
- 首行缩进改为数值输入；
- 强化平移、封面、纸页和淡入的视觉区别；
- 改善分页重排时的页尾空缺、残字和刷新闪现；
- 重绘书架排序和阅读器设置图标；
- 生成正式签名 APK、AAB、校验清单和商店资料包。

## 技术栈

- Kotlin 2.3.21
- Jetpack Compose + Material 3
- Android Gradle Plugin 9.3.0 / Gradle 9.5
- `minSdk 26` / `compileSdk 37` / `targetSdk 37`
- Room + FTS4
- DataStore Preferences
- Storage Access Framework
- Kotlin Coroutines / Flow / StateFlow
- Jsoup
- juniversalchardet
- Android Text-to-Speech
- R8、Baseline Profile、Macrobenchmark

## 项目结构

```text
app/src/main/java/com/moyu/reader/
├─ data/                 Room、DataStore、文件和备份仓储
├─ model/                领域模型与不可变 UI 状态
├─ parser/               TXT / EPUB / 编码 / 章节解析
├─ reader/               分页、滚动、进度、翻页动画
├─ ui/designsystem/      颜色、字体、图标、动效和通用组件
├─ ui/library/           书架、搜索、筛选、排序
├─ ui/importbook/        文件选择、导入和解析进度
├─ ui/book/              书籍详情、目录、书签和搜索
├─ ui/reader/            阅读器、控制层、主题和排版
└─ ui/settings/          全局设置、备份和主题管理

docs/                    架构、功能矩阵、测试和视觉回归
release/                 发布说明、商店资料、隐私政策和截图
.github/workflows/       GitHub Actions Android CI
```

## 本地开发

### 环境

- JDK 17；
- Android SDK 37；
- Android Studio；
- Windows、macOS 或 Linux 均可构建。

### 构建 Debug APK

```powershell
.\gradlew.bat :app:assembleDebug
```

### 运行测试与 Lint

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

### 构建正式 APK / AAB

正式签名信息通过环境变量传入，密钥文件不进入 Git：

```text
MOYU_KEYSTORE_PATH
MOYU_KEYSTORE_PASSWORD
MOYU_KEY_ALIAS
MOYU_KEY_PASSWORD
```

```powershell
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:bundleRelease
```

## 架构说明

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

阅读进度使用 `bookId + chapterId + characterOffset + percentage` 保存。字体、字号、边距或方向变化后，分页器会重新计算页面，再使用字符偏移恢复接近原阅读位置，减少设置变化造成的跳页。

## 测试覆盖

- 章节识别与标题清理；
- UTF-8 / GBK / GB18030 / Big5 编码检测；
- TXT 和 EPUB fixture 导入；
- 目录、全文搜索和阅读进度落盘；
- 分页边界、标点优先断句和安全页脚；
- 书签、备份恢复与主题设置；
- Compose UI smoke test；
- API 26 兼容性和 Android Lint；
- 冷启动、暖启动和帧耗时基准。

## 贡献方式

欢迎通过 Issue 反馈问题，也欢迎提交 Pull Request。提交问题时，建议附上：

1. Android 版本和设备型号；
2. 书籍格式与大致文件大小；
3. 复现步骤；
4. 预期表现与实际表现；
5. 相关截图或日志（请先移除个人信息和书籍正文）。

## 许可证

当前仓库暂未附加开源许可证。若后续确定采用 MIT、Apache-2.0 或 GPL-3.0，将在仓库根目录补充对应 `LICENSE` 文件。

## 联系方式

- GitHub Issues：<https://github.com/karryHYX/moyu-reader/issues>
- 隐私政策草稿：[`release/store-listing/privacy-policy-zh-CN.md`](release/store-listing/privacy-policy-zh-CN.md)
- 商店资料：[`release/store-listing/`](release/store-listing/)
