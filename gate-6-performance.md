# Gate 6 — 性能验证

日期：2026-08-22

## 大型 TXT 流式解析

运行环境：JDK 17 单元测试进程，最大堆 512 MiB。Fixture 在测试中流式生成，测试结束删除。

| 文件 | 解析耗时 | 章节 span | 结果 |
|---:|---:|---:|---|
| 1 MiB | 173 ms | 9 | 通过 |
| 10 MiB | 297 ms | 81 | 通过 |
| 50 MiB | 948 ms | 404 | 通过 |
| 100 MiB | 1,908 ms | 807 | 通过 |

每档均执行：流式章节扫描、章节列表构建、随机读取首章和末章。100 MiB 在 512 MiB test worker 中完成，验证路径没有把整本小说永久载入内存。

测试：`LargeTxtParserTest`。

## Startup Macrobenchmark

变体：`benchmark`（基于 release、不可调试、R8、Baseline Profile）。  
设备：API 36 x86_64 模拟器，4 core / 2.5 GiB；结果只用于同机趋势，不等同真实手机。

| 指标 | min | median | max | n |
|---|---:|---:|---:|---:|
| Cold TTID | 2,127 ms | 2,144 ms | 2,348 ms | 5 |
| Warm TTID | 404 ms | 647 ms | 829 ms | 5 |

Perfetto trace 与 JSON：

`baselineprofile/build/outputs/connected_android_test_additional_output/benchmark/connected/MoyuPhone(AVD) - 16/`

模拟器结果波动较大（Warm CoV 31.2%），正式结论仍需物理设备。

## Baseline Profile

- `BaselineProfileGenerator.criticalUserJourney` 在 API 36 AVD 通过；
- 覆盖启动、书架、导入入口，并在已有数据时继续覆盖详情、Reader、控制层与目录；
- 生成 startup profile 1,541,620 bytes；
- App 自带 `app/src/main/baseline-prof.txt`，Release 构建的 `compileReleaseArtProfile` 已通过。

## 结构性约束

- TXT 章节最大 1 MiB；
- Reader LRU 预算 3,000,000 字符；
- FTS chunk 800 字符、80 overlap；
- EPUB entry 限制 2/10/16 MiB（XML/cover/XHTML）；
- 导入、hash、解析、索引、备份、恢复均不在主线程；
- Debug APK 合并清单无 INTERNET 和危险权限。

## 物理设备复测项

发布前在普通手机与大屏手机补充：

- Cold/Warm TTID；
- Library scroll frame timing；
- Open book；
- Reader page turning / scrolling；
- 50/100 MiB 导入峰值 PSS 与索引耗时。

