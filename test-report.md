# Gate 4 构建与测试报告

日期：2026-08-22  
环境：Windows / JDK 17.0.12 / Android SDK 37.0 / Build Tools 36.0.0 & 37.0.0

## 已执行

| 命令 | 结果 |
|---|---|
| `:app:testDebugUnitTest` | 通过 |
| `:app:lintDebug` | 通过，0 error |
| `:app:assembleDebug` | 通过 |
| `:baselineprofile:assemble` | 通过 |
| `connectedDebugAndroidTest`（API 36 AVD） | 4/4 通过：TXT/EPUB/位置恢复 + Android 真实分页 + UI smoke |
| `LargeTxtParserTest`（1/10/50/100 MiB） | 4/4 通过 |
| `StartupBenchmark`（benchmark AVD） | Cold/Warm 2/2 通过 |
| `BaselineProfileGenerator`（benchmark AVD） | 通过 |
| `:app:assembleRelease` | 通过，R8 / resource shrink / lintVital 通过 |
| `:app:bundleRelease` | 通过，生成 Release AAB |

## 单元测试覆盖

- ChapterDetector：中文数字、阿拉伯数字、卷、英文 Chapter、特殊标题、误判过滤；
- CharsetDetector：UTF-8 BOM、UTF-16 BOM、Big5 手动覆盖；
- TxtBookParser：GB18030、多章节、无章节 fallback、按 span 读取；
- EpubBookParser：EPUB 3 metadata、spine、nav、章节正文；
- ReadingProgress：章节 + 字符偏移稳定进度；
- PageBreakCalculator：边界连续且覆盖全文；
- ReaderInteraction：左右点击区、横向滑动阈值、纵滑忽略、首/中/末语义锚点；
- ReaderTextFormatter：清理行首空格但保持字符索引长度；
- SHA-256：去重 hash 稳定。

## v1.1.0 手势回归（API 36 AVD，1080 × 2340）

使用真实导入的两章中文 TXT，在 Reader 页面直接注入触控并读取页脚状态：

| 操作 | 页脚变化 | 结果 |
|---|---|---|
| 点击右侧 | `1 / 6` → `2 / 6` | 通过 |
| 点击左侧 | `2 / 6` → `1 / 6` | 通过 |
| 向左横滑 | `1 / 6` → `2 / 6` | 通过 |
| 向右横滑 | `2 / 6` → `1 / 6` | 通过 |
| 第一章末页向右 | 进入第二章首页 | 通过 |
| 第二章首页向左 | 回到第一章末页语义位置 | 通过 |

截图：`design/gate5/round4-reader-pagination-final.png`、`design/gate5/round4-reader-controls-final.png`、`design/gate5/round4-local-tts.png`。

## APK 检查

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`  
本轮 SHA-256 在每次重新构建后更新。通过 `aapt dump permissions` 检查：

- 没有 `android.permission.INTERNET`；
- 没有存储、相机、定位、联系人等危险权限；
- 只有 AndroidX 为内部动态 receiver 自动生成的签名级权限。

最终 Release：`dist/MoyuReader-1.4.0-release-signed.apk`

- `versionCode=7` / `versionName=1.4.0`；
- APK v2 / v3 签名验证通过，配套 v4 `.idsig` 验证通过；
- 证书 SHA-256：`759ff3cb8b56d60e5d346e2ab237937521121f1ff2ea90de10519dab5eb24f32`；
- APK SHA-256：`7a7fe02d660c393e68e5895251691e7e559cfed3ed9edbbd3240cfae3b4aceb0`；
- API 36 AVD 安装成功，系统报告 `apkSigningVersion=3`，启动进程正常。

## v1.2.0 Reader 专项回归

| 验证项 | 实测结果 |
|---|---|
| 目录 | 打开右侧抽屉，展示 2 章与当前 READING 状态 |
| 全书搜索 | 打开独立搜索 Sheet，搜索输入框与结果列表可操作 |
| 书签 | 顶部保存后，书签 Sheet 显示 `全部 1`、章节和正文摘要 |
| 点击穿透 | 控制层显示时 Reader 手势暂停；点击菜单不再触发正文翻页 |
| 段间距 | 测试章从 `16 dp = 6 页` 调整为 `0 dp = 5 页`，显示和分页同步变化 |
| 音量键 | 音量减 `1/6 → 2/6`，音量加 `2/6 → 1/6` |
| Paper 动效 | 纸页透视、平移与浅色纸边渐层生效，浅色背景无黑色 elevation |
| 页底安全区 | 分页器增加 footer reserve，末行保持在页码与系统手势区上方 |
| 面板/页面动效 | Reader Sheet、主导航、设置子页执行淡入与方向滑动；按钮执行弹簧反馈 |

证据截图位于：`design/gate5/round5-directory.png`、`round5-search-final.png`、`round5-bookmarks.png`、`round5-reading-settings.png`、`round5-paper-turn.png`。

## v1.3.0 分页与详情专项回归

| 验证项 | 实测结果 |
|---|---|
| 绝对行高分页 | `StaticLayout` 改用 Compose 对应绝对行高；首个非末页在 1080 × 2340 AVD 中由 14 行提升至 17 行，底部留白明显收紧。 |
| 句末分页 | 仪器测试断言长中文章节的每个非末页均在 `。！？；…!?;` 结束。 |
| 章节尾页 | 末页显示“本章完”作为收束，避免章节结尾呈现无意义大空白。 |
| 详情目录 | 目录打开独立 Sheet，点击“第二章 清晨”后进入对应章节。 |
| 详情搜索/书签 | 均打开各自独立的可操作 Sheet，不再直接跳入阅读页。 |
| 书架筛选/排序 | 全部、未读、在读、已读、收藏五项均为可点按筛选；排序采用带当前态的卡片列表。 |
| 自动化 | Unit + Lint 通过；API 36 AVD 仪器测试 `4 tests / 0 failures / 0 errors`。 |

## v1.3.1 真机反馈定向修复

| 验证项 | 实测结果 |
|---|---|
| 段落级分页 | 每个源段落/跨页续段单独以同样的 `StaticLayout` 测量；分页边界与 `ReaderPage` 的实际 `Text` 片段一致。 |
| 段首缩进 | 分页测量同步应用首行缩进，避免段首实际排版与分页宽度不一致。 |
| 重排闪烁 | 字体、边距、章节或视口变化时先清空旧页切片并显示短加载态，随后展示新切片。 |
| 翻页流畅度 | 移除拖动帧中的纸边渐层绘制，Cover/Paper 保留轻量 `graphicsLayer` 变换并预取相邻页。 |
| 回归 | Debug Unit / Lint 通过；API 36 AVD 仪器测试 `4 tests / 0 failures / 0 errors`；Release v1.3.1 安装成功。 |

## 尚需物理设备执行

- Library scrolling、Open book、Reader turning / scrolling 的物理机 FrameTiming；
- 普通手机与大屏手机的最终截图复核；
- 50/100 MiB 在物理机上的峰值 PSS。

这些项目进入 Gate 5–6 后在模拟器/目标机执行，不能用 Debug 理论值替代。
